/*
 * Copyright 2016 INFORMATION SERVICES INTERNATIONAL - DENTSU, LTD. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.ui.importwizard;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class MetaDataImportWizard extends Wizard implements IImportWizard {
	private MetaDataImportWizardPage page;
	private ISelection selection;

	public MetaDataImportWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * ウィザードにページ追加.
	 */
	public void addPages() {
		page = new MetaDataImportWizardPage(selection);
		addPage(page);
	}

	/**
	 * Finishボタン.
	 */
	@Override
	public boolean performFinish() {
		MetaDataImportJob job = new MetaDataImportJob(getShell(), page.getMetaDataPath(), page.getJavaProject(), page.getConfigPath());
		job.setUser(true);
		job.schedule();
		return true;
	}
	
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}