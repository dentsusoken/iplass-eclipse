/*
 * Copyright 2017 DENTSU SOKEN INC. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.ui;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.iplass.mtp.eclipse.Activator;
import org.iplass.mtp.eclipse.service.ServiceUtil;

public class PreferencePage extends org.eclipse.jface.preference.PreferencePage implements IWorkbenchPreferencePage {

	private Text serviceConfigPathText;
	private Text tenantLocalDirText;

	private Text projectText;
	
	@Override
	protected Control createContents(Composite parent) {
		setTitle("iPLAss Settings:");

		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		Composite container = new Composite(parent, SWT.NULL);
		{
			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			layout.verticalSpacing = 15;
			container.setLayout(layout);
			container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}
		
		Composite compPj = createProjectClasspathComposite(container);
		{
			Label label = new Label(compPj, SWT.NONE);
			label.setText("project:");

			projectText = new Text(compPj, SWT.SINGLE | SWT.BORDER);
			projectText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			projectText.setText(store.getString(PreferenceInitializer.KEY_CLASSPATH_PROJECT));
			
			String loaded = ServiceUtil.getLoadedPjName();
			if (loaded != null && !"".equals(loaded)) {
				projectText.setEditable(false);
				if (projectText.getText() == null || "".equals(projectText.getText())) {
					projectText.setText(loaded);
				}
			}
		}
		
		Composite compSyncmeta = createSyncMetadataComposite(container);
		{
			Label label = new Label(compSyncmeta, SWT.NONE);
			label.setText("service config:");
			
			serviceConfigPathText = new Text(compSyncmeta, SWT.SINGLE | SWT.BORDER);
			serviceConfigPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			serviceConfigPathText.setText(store.getString(PreferenceInitializer.KEY_SERVICE_CONFIG_PATH));
		}

		{
			Label label = new Label(compSyncmeta, SWT.NONE);
			label.setText("tenant local dir:");

			tenantLocalDirText = new Text(compSyncmeta, SWT.SINGLE | SWT.BORDER);
			tenantLocalDirText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			tenantLocalDirText.setText(store.getString(PreferenceInitializer.KEY_TENANT_LOCAL_DIR));
		}
		
		return compSyncmeta;
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	/**
	 * restore default button.
	 */
	@Override
	protected void performDefaults() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		serviceConfigPathText.setText(store.getDefaultString(PreferenceInitializer.KEY_SERVICE_CONFIG_PATH));
		tenantLocalDirText.setText(store.getDefaultString(PreferenceInitializer.KEY_TENANT_LOCAL_DIR));
	}

	/**
	 * OK button.
	 */
	@Override
	public boolean performOk() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setValue(PreferenceInitializer.KEY_SERVICE_CONFIG_PATH, serviceConfigPathText.getText());
		store.setValue(PreferenceInitializer.KEY_TENANT_LOCAL_DIR, tenantLocalDirText.getText());
		store.setValue(PreferenceInitializer.KEY_CLASSPATH_PROJECT, projectText.getText());
		return true;
	}
	
	private Composite createProjectClasspathComposite(Composite container) {
		Group c = new Group(container, SWT.NULL);
		int numColumns = 2;
		GridLayout layout = new GridLayout(numColumns, false);
		c.setLayout(layout);
		c.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		c.setText("Project in which plugin uses iplass jars");
		return  c;
	}
	
	private Composite createSyncMetadataComposite(Composite container) {
		Group c = new Group(container, SWT.NULL);
		int numColumns = 2;
		GridLayout layout = new GridLayout(numColumns, false);
		c.setLayout(layout);
		c.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		c.setText("Sync Entity Metadata");
		return  c;
	}
}
