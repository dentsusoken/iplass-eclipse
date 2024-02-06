/*
 * Copyright 2017 DENTSU SOKEN INC. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.ui;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.dialogs.PropertyPage;
import org.iplass.mtp.eclipse.Activator;
import org.iplass.mtp.eclipse.service.ResourceFileFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ProjectPropertyPage extends PropertyPage {

	public static final String KEY_PJ_TENANT_LOCAL_DIR = "tenant_local_dir";
	public static final String KEY_PJ_SERVICE_CONFIG_PATH = "service_config_path";
	public static final String KEY_PJ_CANNOT_SHOW_ENTITY_METADATA_IMPORT_PROMPT_DIALOG = "cannot_show_entity_metadata_import_prompt_dialog";
	public static final String KEY_PJ_SKIP_REINIT_SERVICE_CONFIG = "skip_reinit_service_config";
	
	private Combo serviceConfigPathCombo;
	private Text tenantLocalDirText;
	private Button showDialogCheck;
	private Button skipReInitCheck;

	@Override
	protected Control createContents(Composite parent) {
		IProject project = (IProject) getElement();

		Composite container = new Composite(parent, SWT.NULL);
		{
			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			layout.verticalSpacing = 15;
			container.setLayout(layout);
			container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}
		
		Composite composite = createGroupComposite(container);
		{
			Label label = new Label(composite, SWT.NONE);
			label.setText("service config:");

			serviceConfigPathCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
			serviceConfigPathCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			
			List<String> resources = getServiceConfigFilesAsClassPath();
			serviceConfigPathCombo.setItems(resources.toArray(new String[resources.size()]));			

			String persistentValue = getValue(project, KEY_PJ_SERVICE_CONFIG_PATH);
			if (persistentValue != null) {
				if (!persistentValue.isEmpty() && !persistentValue.startsWith("/")) {
					persistentValue = "/" + persistentValue;
				}
				serviceConfigPathCombo.setText(persistentValue);
			}			
		}

		{
			Label label = new Label(composite, SWT.NONE);
			label.setText("tenant local dir:");

			tenantLocalDirText = new Text(composite, SWT.SINGLE | SWT.BORDER);
			tenantLocalDirText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			String persistentValue = getValue(project, KEY_PJ_TENANT_LOCAL_DIR);
			if (persistentValue != null) {
				tenantLocalDirText.setText(persistentValue);
			}
		}
		
		showDialogCheck = new Button(composite, SWT.CHECK);
		showDialogCheck.setText("&Do not show prompt dialog");
		String canShowDialog = getValue(project, KEY_PJ_CANNOT_SHOW_ENTITY_METADATA_IMPORT_PROMPT_DIALOG);
		if (canShowDialog != null) {
			this.showDialogCheck.setSelection(Boolean.valueOf(canShowDialog));
		}

		skipReInitCheck = new Button(composite, SWT.CHECK);
		skipReInitCheck.setText("&Skip reload service config");
		String skipReloadServiceConfig = getValue(project, KEY_PJ_SKIP_REINIT_SERVICE_CONFIG);
		if (skipReloadServiceConfig != null) {
			this.skipReInitCheck.setSelection(Boolean.valueOf(skipReloadServiceConfig));
		}
		
		super.noDefaultAndApplyButton();
		return composite;
	}

	private Composite createGroupComposite(Composite container) {
		Group c = new Group(container, SWT.NULL);
		int numColumns = 2;
		GridLayout layout = new GridLayout(numColumns, false);
		c.setLayout(layout);
		c.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		c.setText("Sync Entity Metadata");		
		return  c;
	}
	
	@Override
	public boolean performOk() {
		IProject project = (IProject) getElement();
		setValue(project, KEY_PJ_TENANT_LOCAL_DIR,   tenantLocalDirText.getText());
		setValue(project, KEY_PJ_SERVICE_CONFIG_PATH, serviceConfigPathCombo.getText());
		setValue(project, KEY_PJ_CANNOT_SHOW_ENTITY_METADATA_IMPORT_PROMPT_DIALOG, String.valueOf(showDialogCheck.getSelection()));
		setValue(project, KEY_PJ_SKIP_REINIT_SERVICE_CONFIG, String.valueOf(skipReInitCheck.getSelection()));
		return true;
	}
	
	private String getValue(IProject project, String key) {
		try {			
			return project.getPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, key));
		} catch (CoreException e) {
			ILog log = Activator.getDefault().getLog();
			log.log(e.getStatus());
			return null;
		}
	}

	private void setValue(IProject project, String key, String value) {
		try {
			project.setPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, key), value);
		} catch (CoreException e) {
			ILog log = Activator.getDefault().getLog();
			log.log(e.getStatus());
		}
	}
	
	private List<String> getServiceConfigFilesAsClassPath() {
		try {
			IJavaProject jp = JavaCore.create((IProject) getElement());
			ResourceFileFolder r = new ResourceFileFolder(jp);
			List<String> resources;
			resources = r.getResourceFilesAsClasspath();
			Iterator<String> ite = resources.iterator();
			while (ite.hasNext()) {
				String e = ite.next();
				if (!e.endsWith(".xml")) {
					ite.remove();
				}
			}
			resources.add("");
			return resources;
		} catch (CoreException e) {
			return Collections.emptyList();
		}
	}
}
