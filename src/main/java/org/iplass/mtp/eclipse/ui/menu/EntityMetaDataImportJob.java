/*
 * Copyright 2017 DENTSU SOKEN INC. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.ui.menu;

import org.eclipse.core.resources.IProject;
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

public class EntityMetaDataImportJob extends Job {
	private String svConf;
	private String dest;
	private boolean canSkipReInit;
	private IJavaProject javaProject;
	private Shell parentShell;
	
	public EntityMetaDataImportJob(Shell parentShell, String svConf, String dest, boolean canSkipReInit, IJavaProject javaProject) {
		super(javaProject.getProject().getName() + " refresh entity metadata from database...");
		this.svConf = svConf;
		this.dest = dest;
		this.canSkipReInit = canSkipReInit;
		this.javaProject = javaProject;
		this.parentShell = parentShell;
	}

	@Override
	protected IStatus run(IProgressMonitor pmon) {
		try {
			SubMonitor progress = SubMonitor.convert(pmon, 100);
			ServiceUtil.invoke(javaProject, new ServiceCallable() {
				@Override
				public void call() throws CoreException {
					//TODO 異なるPJが来たらReInitにチェックを入れていてもSkipさせない
					IProject pj = javaProject.getProject();
					EntityMetaDataImporter emi = new EntityMetaDataImporter(pj, svConf, dest, canSkipReInit, progress.split(90));
					emi.doImport();
				}
			});
			
			progress.setTaskName("refreshing " + dest + "...");
			javaProject.getProject().getFolder(dest).refreshLocal(IResource.DEPTH_INFINITE, progress.split(10));
			
			return Status.OK_STATUS;
		} catch (CoreException ce) {
			String message = "Import Entity Metadata Execution Error";
			Activator.log(message, ce);
			ErrorDialog.openError(parentShell, "Error", message, ExceptionUtil.createMultiStatus(ce));
			return Status.CANCEL_STATUS;
		} finally {
			pmon.done();
		}
	}
}
