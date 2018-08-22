/*
 * Copyright 2017 INFORMATION SERVICES INTERNATIONAL - DENTSU, LTD. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.ui.menu;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.iplass.mtp.eclipse.Activator;
import org.iplass.mtp.eclipse.service.ServiceRegistryThinWrapper;
import org.iplass.mtp.eclipse.service.porting.MetaDataPortingRuntimeException;
import org.iplass.mtp.eclipse.service.porting.MetaDataPortingService;
import org.iplass.mtp.impl.core.ExecuteContext;
import org.iplass.mtp.impl.core.TenantContext;
import org.iplass.mtp.impl.core.config.ServiceRegistryInitializer;
import org.iplass.mtp.impl.metadata.AbstractXmlMetaDataStore;
import org.iplass.mtp.impl.metadata.MetaDataEntry;
import org.iplass.mtp.impl.metadata.MetaDataEntryInfo;
import org.iplass.mtp.impl.metadata.MetaDataRepository;
import org.iplass.mtp.impl.metadata.MetaDataStore;
import org.iplass.mtp.impl.metadata.composite.CompositeMetaDataStore;
import org.iplass.mtp.impl.metadata.xmlfile.VersioningXmlFileMetaDataStore;
import org.iplass.mtp.impl.metadata.xmlfile.XmlFileMetaDataStore;
import org.iplass.mtp.spi.ServiceRegistry;

public class EntityMetaDataImporter {
	private static final String ENTITY_PREFIX= "/entity/";
	private IProject pj;
	private String configPath;
	private String destPath;
	private boolean canSkipReInit;
	private CompositeMetaDataStore cs;
	private AbstractXmlMetaDataStore xs;
	private SubMonitor progress;

	public EntityMetaDataImporter(IProject pj, String configPath, String destPath, boolean canSkipReInit, SubMonitor progress) {
		this.pj = pj;
		this.configPath = configPath;
		this.destPath = destPath;
		this.canSkipReInit = canSkipReInit;
		this.progress = progress;
	}

	public void doImport() throws CoreException {
        progress.setWorkRemaining(100);
        progress.worked(10);
        
        progress.setTaskName("setup...");
        setupLocalStore();
		if(progress.isCanceled()) {
	        throw new OperationCanceledException();
		}
		progress.worked(20);
		
		progress.setTaskName("load definition...");
		List<String> paths = getDefinitions();
		if(progress.isCanceled()) {
	        throw new OperationCanceledException();
		}
		progress.worked(10);
		
		progress.setTaskName("download from database...");
		Map<String, MetaDataEntry> src = getEntityMetaData(paths);
		if(progress.isCanceled()) {
	        throw new OperationCanceledException();
		}
		progress.worked(50);
		
		progress.setTaskName("write local store...");
		writeLocalStore(src);
		if(progress.isCanceled()) {
	        throw new OperationCanceledException();
		}
		progress.worked(10);
	}

	private void setupLocalStore() {
		ServiceRegistryInitializer.setConfigFileName(configPath);
		if (!canSkipReInit) {
			ServiceRegistry.getRegistry().reInit();	
		}
		MetaDataRepository md = ServiceRegistryThinWrapper.getRegistry().getService(MetaDataRepository.class);
		cs = (CompositeMetaDataStore) md.getTenantLocalStore();
		if (cs instanceof CompositeMetaDataStore) {
			xs = ((CompositeMetaDataStore) cs).getStore(XmlFileMetaDataStore.class);
			if(xs == null) {
				xs = ((CompositeMetaDataStore) cs).getStore(VersioningXmlFileMetaDataStore.class);
			}
		} else {
			throw new MetaDataPortingRuntimeException(
					MetaDataRepository.class.getName() + " must be set " + CompositeMetaDataStore.class.getName());
		}

		String projectDir = pj.getLocation().toFile().getAbsolutePath() + "/";
		if (!projectDir.endsWith("/")) {
			projectDir = projectDir + "/";
		}
		if (isAbsoluteDest()) {
			throw new MetaDataPortingRuntimeException("Destination folder must be set as Project relative path.");
		}
		
		if (xs instanceof XmlFileMetaDataStore) {
			XmlFileMetaDataStore xmlStore = (XmlFileMetaDataStore) xs;
			xmlStore.setRootPath(projectDir);
			xmlStore.setFileStorePath(destPath);
		} else if (xs instanceof VersioningXmlFileMetaDataStore) {
			VersioningXmlFileMetaDataStore verStore = (VersioningXmlFileMetaDataStore) xs;
			verStore.setFileStorePath(projectDir + destPath);
		} else {
			throw new MetaDataPortingRuntimeException(CompositeMetaDataStore.class.getName() + " must be set "
					+ XmlFileMetaDataStore.class.getName() + " or " + VersioningXmlFileMetaDataStore.class.getName());
		}
	}

	private boolean isAbsoluteDest() {
		return ':' == destPath.charAt(1) || destPath.startsWith("/");
	}
	
	private List<String> getDefinitions() {
		List<String> paths = new ArrayList<String>();
		for(MetaDataEntryInfo i : cs.definitionList(this.getLocalTenantId(), ENTITY_PREFIX)) {
			paths.add(i.getPath());
		}
		return paths;
	}
	
	private Map<String, MetaDataEntry> getEntityMetaData(List<String> paths) throws CoreException {		
		File tmp = getTempFile();
		MetaDataPortingService sv = MetaDataPortingService.getInstance();		
		try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmp), "UTF-8")))) {
			// ExecuteContextでテナントを指定しておかないと空のメタデータリストが返る.
			initContext();
			sv.write(writer, paths);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"error occurred while reading metadata.", e));
		}
		Map<String, MetaDataEntry> ret = new HashMap<String, MetaDataEntry>();
		try (InputStream is = new BufferedInputStream(new FileInputStream(tmp))) {
			ret = sv.getXMLMetaDataEntryInfo(is).getPathEntryMap();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"error occurred while writing metadata file:" + tmp.getAbsolutePath(), e));
		}
		
		return ret;
	}

	private void writeLocalStore(Map<String, MetaDataEntry> src) {
		int tenantId = getLocalTenantId();
		for (Map.Entry<String, MetaDataEntry> e : src.entrySet()) {
			MetaDataEntry metaDataEntry = e.getValue();
			if (metaDataEntry != null) {
				if (metaDataEntry.getPath().startsWith(ENTITY_PREFIX)) {
					if (xs instanceof XmlFileMetaDataStore) {
						((XmlFileMetaDataStore) xs).store(tenantId, metaDataEntry, metaDataEntry.getVersion());
					} else {
						xs.store(tenantId, metaDataEntry);
					}
				}
			}
		}
	}

	private int getLocalTenantId() {
		MetaDataStore ds = ServiceRegistryThinWrapper.getRegistry().getService(MetaDataRepository.class)
				.getTenantLocalStore();
		XmlFileMetaDataStore xmlStore = ((CompositeMetaDataStore) ds).getStore(XmlFileMetaDataStore.class);
		if (xmlStore != null) {
			return xmlStore.getLocalTenantId();	
		} else {
			return 1;
		}	
	}
	
	private void initContext() {
		TenantContext t = new TenantContext(getLocalTenantId(), "___dummy", "___dummy", false);
		//ExecuteContext econtext = new ExecuteContext(t, TenantAuthType.ID);
		ExecuteContext econtext = new ExecuteContext(t);
		econtext.setClientId("0000");
		ExecuteContext.initContext(econtext);		
	}
	
	private File getTempFile() {
		SimpleDateFormat df = new SimpleDateFormat("yyMMdd-HHmmss");
		String tmpdir = System.getProperty("java.io.tmpdir");
		if(tmpdir.endsWith(File.separator)) {
			tmpdir += File.separator;
		}
		return new File(tmpdir + "tmp-" + getLocalTenantId() + "-" + df.format(new Date())+ ".xml");
	}
}