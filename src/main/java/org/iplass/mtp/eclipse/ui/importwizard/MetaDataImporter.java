/*
 * Copyright 2016 INFORMATION SERVICES INTERNATIONAL - DENTSU, LTD. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.ui.importwizard;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.iplass.mtp.eclipse.Activator;
import org.iplass.mtp.eclipse.service.ServiceRegistryThinWrapper;
import org.iplass.mtp.eclipse.service.porting.MetaDataPortingRuntimeException;
import org.iplass.mtp.eclipse.service.porting.MetaDataPortingService;
import org.iplass.mtp.impl.core.config.ServiceRegistryInitializer;
import org.iplass.mtp.impl.metadata.AbstractXmlMetaDataStore;
import org.iplass.mtp.impl.metadata.MetaDataEntry;
import org.iplass.mtp.impl.metadata.MetaDataRepository;
import org.iplass.mtp.impl.metadata.MetaDataStore;
import org.iplass.mtp.impl.metadata.composite.CompositeMetaDataStore;
import org.iplass.mtp.impl.metadata.xmlfile.DomXmlExternalRefHandler;
import org.iplass.mtp.impl.metadata.xmlfile.VersioningXmlFileMetaDataStore;
import org.iplass.mtp.impl.metadata.xmlfile.XmlFileMetaDataStore;
import org.iplass.mtp.impl.metadata.xmlfile.dom.XsiTypeDomHandlerFactory;
import org.iplass.mtp.spi.ServiceRegistry;

public class MetaDataImporter {
	private IProject pj;
	private IProgressMonitor monitor;

	public MetaDataImporter(IProject pj, String configPath, IProgressMonitor monitor) {
		ServiceRegistryInitializer.setConfigFileName(configPath);
		ServiceRegistry.getRegistry().reInit();
		this.pj = pj;
		this.monitor = monitor;
	}

	public void doImport(File metadataFilePath) throws CoreException {
		CompositeMetaDataStore dest = setupLocalStore(pj.getLocation().toFile());
		Map<String, MetaDataEntry> src = readMetaDataFile(metadataFilePath);
		writeLocalStore(src, dest);
	}

	/**
	 * 絶対パスを入れる.
	 * <p>
	 * 相対パス方式だとEclipse実行フォルダに出力されるため.
	 * 
	 * @param projectPath
	 * @return
	 */
	private CompositeMetaDataStore setupLocalStore(File projectPath) {
		monitor.beginTask("Setup import tool...", IProgressMonitor.UNKNOWN);

		MetaDataStore ds = ServiceRegistryThinWrapper.getRegistry().getService(MetaDataRepository.class).getTenantLocalStore();
		AbstractXmlMetaDataStore store = null;
		if (ds instanceof CompositeMetaDataStore) {
			store = ((CompositeMetaDataStore) ds).getStore(XmlFileMetaDataStore.class);
			if(store == null) {
				store = ((CompositeMetaDataStore) ds).getStore(VersioningXmlFileMetaDataStore.class);
			}
//		} else if (ds instanceof XmlFileMetaDataStore) {
//			xmlStore = (XmlFileMetaDataStore) ds;
		} else {
			throw new MetaDataPortingRuntimeException(MetaDataRepository.class.getName() + " must be set "
					+ CompositeMetaDataStore.class.getName());
		}
		
		String projectDir = projectPath.getAbsolutePath() + "/";
		if (store instanceof XmlFileMetaDataStore) {
			XmlFileMetaDataStore xmlStore = (XmlFileMetaDataStore) store;
			if (!isAbsolute(xmlStore.getFileStorePath())) {
				xmlStore.setRootPath(projectDir);
				XsiTypeDomHandlerFactory factory = (XsiTypeDomHandlerFactory) ((DomXmlExternalRefHandler) xmlStore
						.getXmlExternalRefHandler()).getDomHandlerFactory();

				factory.setFileStorePath(projectDir + factory.getFileStorePath());
				factory.setGroovySourceStorePath(projectDir + factory.getGroovySourceStorePath());
			}
		} else if (store instanceof VersioningXmlFileMetaDataStore) {
			VersioningXmlFileMetaDataStore versionStore = (VersioningXmlFileMetaDataStore) store;
			if (!isAbsolute(versionStore.getFileStorePath())) {
				versionStore.setFileStorePath(projectDir + versionStore.getFileStorePath());
			}
		} else {
			throw new MetaDataPortingRuntimeException(CompositeMetaDataStore.class.getName() + " must be set "
					+ XmlFileMetaDataStore.class.getName() + " or " + VersioningXmlFileMetaDataStore.class.getName());
		}
		return (CompositeMetaDataStore) ds;
	}

	private boolean isAbsolute(String currentSettingPath) {
		return ':' == currentSettingPath.charAt(1) || currentSettingPath.startsWith("/");
	}
	
	private Map<String, MetaDataEntry> readMetaDataFile(File file) throws CoreException {
		monitor.beginTask("Reading MetaData from:" + file.getAbsolutePath(), IProgressMonitor.UNKNOWN);

		Map<String, MetaDataEntry> m = new HashMap<String, MetaDataEntry>();
		try (InputStream istrm = new BufferedInputStream(new FileInputStream(file))) {
			MetaDataPortingService sv = MetaDataPortingService.getInstance();
			m = sv.getXMLMetaDataEntryInfo(istrm).getPathEntryMap();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 
					"error occurred while reading metadata file:" + file.getAbsolutePath(), e));
		}
		return m;
	}

	private void writeLocalStore(Map<String, MetaDataEntry> src, CompositeMetaDataStore dest) {
		monitor.beginTask("Importing to " + pj.getName(), src.keySet().size());
		int tenantId = getLocalTenantId();
		for (Map.Entry<String, MetaDataEntry> e : src.entrySet()) {
			MetaDataEntry metaDataEntry = e.getValue();
			if (metaDataEntry != null) {
				MetaDataStore store = dest.resolveStore(metaDataEntry.getPath());
				if (store instanceof XmlFileMetaDataStore) {
					((XmlFileMetaDataStore) store).store(tenantId, metaDataEntry, metaDataEntry.getVersion());	
				} else {
					store.store(tenantId, metaDataEntry);
				}
			}
			monitor.worked(1);
			
			if(monitor.isCanceled()) {
                monitor.done();
                return;
            }
		}
	}
	
	private int getLocalTenantId() {		
		MetaDataStore ds = ServiceRegistryThinWrapper.getRegistry().getService(MetaDataRepository.class).getTenantLocalStore();
		XmlFileMetaDataStore xmlStore = ((CompositeMetaDataStore) ds).getStore(XmlFileMetaDataStore.class);
		if (xmlStore != null) {
			return xmlStore.getLocalTenantId();	
		} else {
			return 1;
		}
	}
}