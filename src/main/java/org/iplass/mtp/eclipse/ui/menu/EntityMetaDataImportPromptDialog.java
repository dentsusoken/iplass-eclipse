/*
 * Copyright 2017 DENTSU SOKEN INC. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.ui.menu;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.iplass.mtp.eclipse.Activator;
import org.iplass.mtp.eclipse.ui.ProjectPropertyPage;
import org.iplass.mtp.eclipse.ui.ProjectPropertyUtil;

public class EntityMetaDataImportPromptDialog extends TitleAreaDialog {

	private static final String USAGE_MSG = "Following properties can be set using project property or preference page.";
	private static final String INFO_MSG = "Download entity metadata as tenant local xml from database. " + USAGE_MSG;
	private static final String WARN_MSG = "Service Config Path and Destination folder must be set. " + USAGE_MSG;
	private Text destPath;
	private Text configPath;
	private Button showDialogCheck;
	private Button skipReInitCheck;
	private IJavaProject selectedJavaProject;

	public EntityMetaDataImportPromptDialog(Shell parentShell, IJavaProject selectedJavaProject) {
		super(parentShell);
		setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE);
		setBlockOnOpen(false);
		this.selectedJavaProject = selectedJavaProject;
	}

	@Override
	public void create() {
		super.create();
		setTitle("iPLAss refresh entity metadata");
		if (check()) {
			setMessage(INFO_MSG, IMessageProvider.INFORMATION);
		} else {
			setMessage(WARN_MSG, IMessageProvider.WARNING);
		}
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			if (check()) {
				saveProjectProperty();
				doOkButton();
				close();
			} else {
				setMessage("You cannot refresh. " + WARN_MSG, IMessageProvider.ERROR);
			}
		} else {
			this.close();
		}
	}

	private void doOkButton() {
		EntityMetaDataImportJob job = new EntityMetaDataImportJob(getShell(), configPath.getText(), destPath.getText(), skipReInitCheck.getSelection(), selectedJavaProject);
		job.setUser(true);
		job.schedule();
	}

	@Override
	protected Control createDialogArea(Composite parent) {

		Composite container = (Composite) super.createDialogArea(parent);
		{
			GridLayout layout = new GridLayout();
			container.setLayout(layout);
			layout.numColumns = 1;
			layout.verticalSpacing = 15;
		}

		Composite groupCompo = createGroupComposite(container);
		{
			// ServiceConfig
			Label imgLabel = new Label(groupCompo, SWT.NULL);
			imgLabel.setImage(JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKFRAG_ROOT));

			Label label = new Label(groupCompo, SWT.NULL);
			label.setText("&Service Config Path:");

			configPath = new Text(groupCompo, SWT.BORDER | SWT.SINGLE);
			configPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			//configPath.setEditable(false);
			configPath.setText(getConfigPathProperty());
		}

		{
			// destination folder
			Label imgLabel = new Label(groupCompo, SWT.NULL);
			imgLabel.setImage(
					PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJ_FOLDER));

			Label label = new Label(groupCompo, SWT.NULL);
			label.setText("&Destination folder:");

			destPath = new Text(groupCompo, SWT.BORDER | SWT.SINGLE);
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			destPath.setLayoutData(gd);
			//destPath.setEditable(false);
			destPath.setText(getTenantLocalDirProperty());
		}
		
		// show　dialog check box
		Composite c = new Composite(container, SWT.NONE);
		c.setLayout(new GridLayout(3, false));
		c.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		showDialogCheck = new Button(c, SWT.CHECK);
		showDialogCheck.setText("&Do not show this dialog again.");
		showDialogCheck.setSelection(getShowDialogCheckProperty());
		
		// skip service registry reInit check box	
		skipReInitCheck = new Button(c, SWT.CHECK);
		skipReInitCheck.setText("&Skip reload service config as it was not modified.");
		skipReInitCheck.setSelection(getSkipReInitCheckProperty());
		
		return container;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 300);
	}

	private Composite createGroupComposite(Composite container) {
		Composite c = new Composite(container, SWT.BORDER);
		int numColumns = 3;
		GridLayout layout = new GridLayout(numColumns, false);
		c.setLayout(layout);
		c.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return c;
	}

	private void saveProjectProperty() {
		try {
			QualifiedName showDialogCheckKey = new QualifiedName(Activator.PLUGIN_ID, ProjectPropertyPage.KEY_PJ_CANNOT_SHOW_ENTITY_METADATA_IMPORT_PROMPT_DIALOG);
			selectedJavaProject.getProject().setPersistentProperty(showDialogCheckKey, String.valueOf(showDialogCheck.getSelection()));
			
			QualifiedName skipReInitCheckKey = new QualifiedName(Activator.PLUGIN_ID, ProjectPropertyPage.KEY_PJ_SKIP_REINIT_SERVICE_CONFIG);
			selectedJavaProject.getProject().setPersistentProperty(skipReInitCheckKey, String.valueOf(skipReInitCheck.getSelection()));
			
			QualifiedName configPathKey = new QualifiedName(Activator.PLUGIN_ID, ProjectPropertyPage.KEY_PJ_SERVICE_CONFIG_PATH);
			selectedJavaProject.getProject().setPersistentProperty(configPathKey, String.valueOf(configPath.getText()));

			QualifiedName tenantDirKey = new QualifiedName(Activator.PLUGIN_ID, ProjectPropertyPage.KEY_PJ_TENANT_LOCAL_DIR);
			selectedJavaProject.getProject().setPersistentProperty(tenantDirKey, String.valueOf(destPath.getText()));
		} catch (CoreException e) {
			Activator.log("cannot save show dialog check property", e);
		}
	}
	
	private String getConfigPathProperty() {
		return ProjectPropertyUtil.getConfigPathProperty(selectedJavaProject.getProject());
	}

	private String getTenantLocalDirProperty() {
		return ProjectPropertyUtil.getTenantLocalDirProperty(selectedJavaProject.getProject());
	}
	
	private boolean getShowDialogCheckProperty() {
		return ProjectPropertyUtil.getShowDialogCheckProperty(selectedJavaProject.getProject());
	}

	private boolean getSkipReInitCheckProperty() {
		return ProjectPropertyUtil.getSkipReInitCheckProperty(selectedJavaProject.getProject());
	}
	
	private boolean check() {
		String scpath = getConfigPathProperty();
		String tldpath = getTenantLocalDirProperty();
		return !(scpath == null || scpath.isEmpty() || tldpath == null || tldpath.isEmpty());
	}
}
