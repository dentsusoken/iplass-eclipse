/*
 * Copyright 2017 INFORMATION SERVICES INTERNATIONAL - DENTSU, LTD. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.IPreferenceStore;

import org.iplass.mtp.eclipse.Activator;

public class ProjectPropertyUtil {

	public static boolean getShowDialogCheckProperty(IProject project) {
		boolean ret = false;
		try {
			QualifiedName key = new QualifiedName(Activator.PLUGIN_ID, ProjectPropertyPage.KEY_PJ_CANNOT_SHOW_ENTITY_METADATA_IMPORT_PROMPT_DIALOG);
			ret = Boolean.valueOf(project.getPersistentProperty(key));
		} catch (CoreException e) {
			Activator.log("cannot get show dialog check property.", e);
		}
		return ret;
	}
	
	public static boolean getSkipReInitCheckProperty(IProject project) {
		boolean ret = false;
		try {
			QualifiedName key = new QualifiedName(Activator.PLUGIN_ID, ProjectPropertyPage.KEY_PJ_SKIP_REINIT_SERVICE_CONFIG);
			ret = Boolean.valueOf(project.getPersistentProperty(key));
		} catch (CoreException e) {
			Activator.log("cannot get skip reInit service config check property.", e);
		}
		return ret;
	}
	
	public static String getTenantLocalDirProperty(IProject project) {
		String ret = null;
		try {
			IPreferenceStore store = Activator.getDefault().getPreferenceStore();
			ret = store.getString(PreferenceInitializer.KEY_TENANT_LOCAL_DIR);
			
			QualifiedName key = new QualifiedName(Activator.PLUGIN_ID, ProjectPropertyPage.KEY_PJ_TENANT_LOCAL_DIR);
			String pjProp = project.getPersistentProperty(key);
			if (pjProp != null) {
				ret = pjProp;
			}
		} catch (CoreException ce) {
			Activator.log("cannot get tenant local directory.", ce);
		}
		return ret;
	}
	
	public static String getConfigPathProperty(IProject project) {
		String ret = null;
		try {
			IPreferenceStore store = Activator.getDefault().getPreferenceStore();
			ret = store.getString(PreferenceInitializer.KEY_SERVICE_CONFIG_PATH);
			
			QualifiedName key = new QualifiedName(Activator.PLUGIN_ID, ProjectPropertyPage.KEY_PJ_SERVICE_CONFIG_PATH);
			String pjProp = project.getPersistentProperty(key);
			if (pjProp != null) {
				ret = pjProp;
			}			
		} catch (CoreException ce) {
			Activator.log("cannot get service config path.", ce);
		}
		return ret;
	}
}
