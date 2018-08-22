/*
 * Copyright 2017 INFORMATION SERVICES INTERNATIONAL - DENTSU, LTD. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.ui;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.iplass.mtp.eclipse.Activator;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	private static final String DEFAULT_SERVICE_CONFIG_PATH = "/mtp-service-config.xml";
	private static final String DEFAULT_TENANT_LOCAL_DIR = "src/main/tenantLocalStore";
	
	public static final String KEY_TENANT_LOCAL_DIR = "tenant_local_dir";
	public static final String KEY_SERVICE_CONFIG_PATH = "service_config_path";
	public static final String KEY_CLASSPATH_PROJECT = "classpath_project";
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(KEY_SERVICE_CONFIG_PATH, DEFAULT_SERVICE_CONFIG_PATH);
		store.setDefault(KEY_TENANT_LOCAL_DIR, DEFAULT_TENANT_LOCAL_DIR);
	}
}
