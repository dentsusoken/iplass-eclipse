/*
 * Copyright 2017 INFORMATION SERVICES INTERNATIONAL - DENTSU, LTD. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.ui.menu;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import org.iplass.mtp.eclipse.Activator;
import org.iplass.mtp.eclipse.ui.ProjectPropertyPage;
import org.iplass.mtp.eclipse.ui.ProjectPropertyUtil;

public class EntityMetaDataImportHandler extends AbstractHandler {
	
	public EntityMetaDataImportHandler() {
		super();
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		if (window != null) {
			IStructuredSelection selection = (IStructuredSelection) window.getSelectionService().getSelection();
			Object targetElement = selection.getFirstElement();
			

			IJavaProject selectedJavaProject = null;
			if (targetElement instanceof IProject) {
				selectedJavaProject = JavaCore.create((IProject) targetElement);
			} else if (targetElement instanceof IJavaProject) {
				selectedJavaProject = (IJavaProject) targetElement;
			} else if (targetElement instanceof IFolder) {
				selectedJavaProject = JavaCore.create(((IFolder) targetElement).getProject());
			} else if (targetElement instanceof IFile) {
				selectedJavaProject = JavaCore.create(((IFile) targetElement).getProject());
			} else {
				throw new ExecutionException("invalid selection:" + targetElement.getClass());
			}
			doExecute(window.getShell(), selectedJavaProject);
	    }

		return null;
	}

	private void doExecute(Shell shell, IJavaProject selectedJavaProject) {
		if (selectedJavaProject != null) {
			IProject pj = selectedJavaProject.getProject();
			if (!cannotShowPromptDialog(pj)) {
				EntityMetaDataImportPromptDialog prompt = new EntityMetaDataImportPromptDialog(shell, selectedJavaProject);
				prompt.open();
			} else {
				EntityMetaDataImportJob job = new EntityMetaDataImportJob(shell, getConfigPathProperty(pj),
						getTenantLocalDirProperty(pj), getSkipReInitCheckProperty(pj), selectedJavaProject);
				job.setUser(true);
				job.schedule();
			}
		}
	}

	private boolean cannotShowPromptDialog(IProject pj) {
		boolean ret = true;
		try {
			QualifiedName key = new QualifiedName(Activator.PLUGIN_ID,
					ProjectPropertyPage.KEY_PJ_CANNOT_SHOW_ENTITY_METADATA_IMPORT_PROMPT_DIALOG);
			ret = Boolean.valueOf(pj.getPersistentProperty(key));
		} catch (CoreException e) {
			Activator.log("cannot get show dialog check property", e);
		}
		return ret;
	}

	private String getConfigPathProperty(IProject pj) {
		return ProjectPropertyUtil.getConfigPathProperty(pj);
	}

	private String getTenantLocalDirProperty(IProject pj) {
		return ProjectPropertyUtil.getTenantLocalDirProperty(pj);
	}
	
	private boolean getSkipReInitCheckProperty(IProject pj) {
		return ProjectPropertyUtil.getSkipReInitCheckProperty(pj);
	}
}