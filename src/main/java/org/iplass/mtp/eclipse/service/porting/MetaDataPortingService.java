/*
 * Copyright 2018 DENTSU SOKEN INC. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.service.porting;

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.iplass.mtp.eclipse.Activator;
import org.iplass.mtp.eclipse.service.ServiceRegistryThinWrapper;
import org.iplass.mtp.impl.entity.EntityService;
import org.iplass.mtp.impl.metadata.MetaDataContext;
import org.iplass.mtp.impl.metadata.MetaDataEntry;
import org.iplass.mtp.impl.metadata.MetaDataEntry.State;
import org.iplass.mtp.impl.metadata.MetaDataJAXBService;
import org.iplass.mtp.impl.metadata.xmlresource.ContextPath;
import org.iplass.mtp.impl.metadata.xmlresource.MetaDataEntryList;
import org.iplass.mtp.impl.metadata.xmlresource.XmlResourceMetaDataEntryThinWrapper;
import org.iplass.mtp.impl.script.GroovyScriptService;
import org.iplass.mtp.impl.xml.jaxb.SecureSAXParserFactory;
import org.iplass.mtp.util.StringUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * toolsモジュールの同名サービスのEclipse版.
 * <p>コールバックロギングクラスをILogでの実装に切り替えて、必要機能に絞ったバージョン.
 * @author T.Nishida
 *
 */
public class MetaDataPortingService {
	private static final String META_AGGREGATE_PATH_PREFIX = "/aggregate";
	
	private static MetaDataPortingService instance;
	
	private MetaDataPortingService() {
	}
	
	public static MetaDataPortingService getInstance() {
		if(instance == null) {
			instance = new MetaDataPortingService();
		}
		return instance;
	}
	
	public void write(PrintWriter writer, List<String> paths) {
		MetaDataJAXBService jaxbService = ServiceRegistryThinWrapper.getRegistry().getService(MetaDataJAXBService.class);
		MetaDataWriteCallback callback = new MetaDataWritePluginLoggingCallback();
		try {
			JAXBContext jaxbContext = jaxbService.getJAXBContext();
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);
			marshaller.setProperty("jaxb.fragment", Boolean.TRUE);

			//Header出力
			callback.onStarted();
			writeHeader(writer);

			String beforeContextPath = "";
			boolean isWriteContext = false;	//有効なEntityがない場合の時用
			for (String path : paths) {
				//aggregateは除外する
				if (path.startsWith(META_AGGREGATE_PATH_PREFIX)){
					if (callback.onWarning(path, "aggregate metadata is deprecated.", null)) {
						continue;
					} else {
						break;
					}
				}

				MetaDataEntry entry = MetaDataContext.getContext().getMetaDataEntry(path);
				if (entry == null) {
					if (callback.onWarning(path, "not found metadata configure.", null)) {
						continue;
					} else {
						break;
					}
				}
				String contextPath = getContextPath(entry.getPath(), entry.getMetaData().getName());
				if (!beforeContextPath.equals(contextPath)) {
					if (!beforeContextPath.isEmpty()) {
						writer.println("</contextPath>");
					}
					beforeContextPath = contextPath;
					writer.println("<contextPath name=\"" + contextPath + "\">");
					isWriteContext = true;
				}
				//Entry出力
				writeMetaDataEntry(writer, marshaller, entry);
				callback.onWrited(path, String.valueOf(entry.getVersion()));
			}
			if (isWriteContext) {
				writer.println("</contextPath>");
			}

			//Footer出力
			writeFooter(writer);
			callback.onFinished();
		} catch (JAXBException e) {
			throw new MetaDataPortingRuntimeException("MetaData definition cannot be parsed.:" + getJAXBExceptionMessage(e), e);
		}
	}
	
	private void writeHeader(PrintWriter writer) {
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
		writer.println("<metaDataList>");
	}

	private void writeFooter(PrintWriter writer) {
		writer.println("</metaDataList>");
	}
	
	private void writeMetaDataEntry(PrintWriter writer, Marshaller marshaller, MetaDataEntry entry) throws JAXBException {
		XmlResourceMetaDataEntryThinWrapper meta = new XmlResourceMetaDataEntryThinWrapper(entry.getMetaData());
		meta.setOverwritable(entry.isOverwritable());
		meta.setSharable(entry.isSharable());
		meta.setDataSharable(entry.isDataSharable());
		meta.setPermissionSharable(entry.isPermissionSharable());
		marshaller.marshal(meta, writer);
		writer.println();
	}

	private String getContextPath(String path, String name) {
		String checkName = name.replace(".", "/");
		String contextPath = null;
		if (path.endsWith(checkName)) {
			contextPath = path.substring(0, path.length() - checkName.length() - 1);
		} else if (path.endsWith(name)) {
				contextPath = path.substring(0, path.length() - name.length() - 1);
		} else {
			contextPath = path;
		}
		return contextPath;
	}
	
	/**
	 * エラー内容がUnmarshalExceptionのSAXParseExceptionに格納されているのでチェック
	 * @param e
	 * @return
	 */
	private String getJAXBExceptionMessage(JAXBException e) {
		String detail = "";
		if (e.getMessage() != null) {
			detail = e.getMessage();
		} else {
			if (e.getCause() != null) {
				if (e.getCause().getMessage() != null) {
					detail = e.getCause().getMessage();
				}
			}
		}
		return detail;
	}
	
	public XMLEntryInfo getXMLMetaDataEntryInfo(InputStream is) {
		MetaDataJAXBService jaxbService = ServiceRegistryThinWrapper.getRegistry().getService(MetaDataJAXBService.class);
		//XML->RootMetaDataの変換
		JAXBContext jaxbContext = jaxbService.getJAXBContext();	
		MetaDataEntryList metaDataList = null;
		try {
			Unmarshaller um = jaxbContext.createUnmarshaller();
			//一旦、クライアントにダウンロードされた可能性のあるMetaDataなのでXXE対策しとく
			metaDataList = (MetaDataEntryList) um.unmarshal(toSaxSource(is));
		} catch (JAXBException e) {
			throw new MetaDataPortingRuntimeException("MetaData definition cannot be parsed.:" + getJAXBExceptionMessage(e), e);
		}

		return parse(metaDataList);
	}
	
	private SAXSource toSaxSource(InputStream is) throws JAXBException {
		//外部参照を処理しない
		SAXParserFactory f = SAXParserFactory.newInstance();
		f.setNamespaceAware(true);
		f.setValidating(false);
	    f = new SecureSAXParserFactory(f);
	    try {
			return new SAXSource(f.newSAXParser().getXMLReader(), new InputSource(is));
		} catch (SAXException | ParserConfigurationException e) {
			throw new JAXBException(e);
		}
	}
	
	private XMLEntryInfo parse(MetaDataEntryList metaList) {
		XMLEntryInfo entryInfo = new XMLEntryInfo();
		if (metaList.getContextPath() != null) {
			for (ContextPath context: metaList.getContextPath()) {
				parseContextPath(entryInfo, context, "", context.getName());
			}
		}
		return entryInfo;
	}
	
	private void parseContextPath(XMLEntryInfo entryInfo, ContextPath context, String prefixPath, String rootPath) {
		if (context.getContextPath() != null) {
			for (ContextPath child: context.getContextPath()) {
				parseContextPath(entryInfo, child, prefixPath + context.getName() + "/", rootPath);
			}
		}

		if (context.getEntry() != null) {
			for (XmlResourceMetaDataEntryThinWrapper xmlEntry: context.getEntry()) {

				if (xmlEntry.getMetaData() == null) {
					//現在存在しないMetaDataが指定されたなどして、MetaDataが読めていない可能性。
					String path = xmlEntry.getName();
					if (path == null) {
						path = prefixPath + context.getName() + "/null(unknown)";
					}
					Activator.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, path + "'s Entry is null, maybe Old(No longer available) MetaData Type specified."));

					continue;
				}

				//pathがMetaDataEntryのnameとして指定されている場合はそれを利用
				String path = xmlEntry.getName();
				if (path == null) {
					//aggregateは除外する
					if ((prefixPath + context.getName()).startsWith(META_AGGREGATE_PATH_PREFIX)){
						continue;
					}

					//指定されていない場合はContextPathから生成
					path = convertPath(prefixPath + context.getName() + "/" + xmlEntry.getMetaData().getName());
				}

				//aggregateは除外する
				if (path.startsWith(META_AGGREGATE_PATH_PREFIX)){
					continue;
				}

				MetaDataEntry entry = new MetaDataEntry(path, xmlEntry.getMetaData(), State.VALID, 0, xmlEntry.isOverwritable(), xmlEntry.isSharable(), xmlEntry.isDataSharable(), xmlEntry.isPermissionSharable());

				entryInfo.putPathEntry(path, entry);
				if (StringUtil.isEmpty(entry.getMetaData().getId())) {
					//IDが未指定のものは別で保持(Import時に採番される)
					entryInfo.addIdBlankEntry(entry);
				} else {
					entryInfo.putIdEntry(entry.getMetaData().getId(), entry);
				}

			}
		}
	}
	
	private String convertPath(String path) {
		//FIXME coreモジュールでpathを知っていないか？
		if (path.startsWith(EntityService.ENTITY_META_PATH)
				|| path.startsWith("/view/generic/") 
				|| path.startsWith("/view/filter/")
				|| path.startsWith("/entityWebapi/")
				|| path.startsWith(GroovyScriptService.UTILITY_CLASS_META_PATH)) {
			return path.replace(".","/");
		}
		return path;
	}
}
