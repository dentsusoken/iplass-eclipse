/*
 * Copyright 2018 INFORMATION SERVICES INTERNATIONAL - DENTSU, LTD. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.ui.importwizard;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Shell;

import org.iplass.mtp.eclipse.Activator;
import org.iplass.mtp.eclipse.exception.ExceptionUtil;
import org.iplass.mtp.eclipse.service.ServiceCallable;
import org.iplass.mtp.eclipse.service.ServiceUtil;

public class MetaDataImportJob extends Job {
	private String configPath;
	private String srcMetadataPath;
	private IJavaProject javaProject;
	private Shell parentShell;
	
	public MetaDataImportJob(Shell parentShell, String srcMetadataPath, IJavaProject javaProject, String configPath) {
		super("import " + new File(srcMetadataPath).getName() + " to " + javaProject.getProject().getName() +"...");
		this.parentShell = parentShell;
		this.srcMetadataPath = srcMetadataPath;
		this.javaProject = javaProject;
		this.configPath = configPath;
	}

	@Override
	protected IStatus run(IProgressMonitor pmon) {
		try {
			SubMonitor monitor = SubMonitor.convert(pmon, 100);
			ServiceUtil.invoke(javaProject, new ServiceCallable() {
				@Override
				public void call() throws CoreException {
					MetaDataImporter md = new MetaDataImporter(javaProject.getProject(), configPath, monitor);
					md.doImport(new File(srcMetadataPath));
				}
			});

			javaProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor.split(10));
			
			return Status.OK_STATUS;
		} catch (CoreException ce) {
			String message = "Error occurred while performing iPLAss MetaData Import.";
			Activator.log(message, ce);
			ErrorDialog.openError(parentShell, "Error", message, ExceptionUtil.createMultiStatus(ce));
			return Status.CANCEL_STATUS;
		} finally {
			pmon.done();
		}
	}
}
