/*
 * Copyright 2016 DENTSU SOKEN INC. All Rights Reserved.
 */
package org.iplass.mtp.eclipse;

import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;

public class ImageRegistryInitializer {
	public static final String IMPORT_WIZARD_METADATA = "import.wizard.metadata";
	public static final String IMPORT_WIZARD_PROJECT = "import.wizard.project";
	public static final String IMPORT_WIZARD_SERVICE_CONFIG = "import.wizard.serviceConfig";
	public static final String MENU_ENTITY_REFRESH = "menu.entity.refresh";
//	public static final String IMPORT_WIZARD_TENANT_ID = "import.wizard.tenantId";
	
	public static void initializeImageRegistry(ImageRegistry registry) {
		registerImage(registry, ImageRegistryInitializer.IMPORT_WIZARD_METADATA, "icons/database.png");
		registerImage(registry, ImageRegistryInitializer.IMPORT_WIZARD_PROJECT, "icons/application_get.png");
		registerImage(registry, ImageRegistryInitializer.IMPORT_WIZARD_SERVICE_CONFIG, "icons/config.png");
		registerImage(registry, ImageRegistryInitializer.MENU_ENTITY_REFRESH, "icons/refresh.png");
//		registerImage(registry, ImageRegistryInitializer.IMPORT_WIZARD_TENANT_ID, "icons/house.png");
	}
	
	private static void registerImage(ImageRegistry registry, String key, String filePath) {
		try {
			URL baseUrl = Activator.getDefault().getBundle().getEntry("/");
			ImageDescriptor desc = ImageDescriptor.createFromURL(new URL(baseUrl, filePath));
			registry.put(key, desc);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}