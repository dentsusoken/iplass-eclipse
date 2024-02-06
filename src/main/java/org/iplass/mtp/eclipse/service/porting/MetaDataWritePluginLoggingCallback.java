/*
 * Copyright 2018 DENTSU SOKEN INC. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.service.porting;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.iplass.mtp.eclipse.Activator;

public class MetaDataWritePluginLoggingCallback implements MetaDataWriteCallback {

	@Override
	public void onStarted() {
		Activator.log(new Status(IStatus.INFO, Activator.PLUGIN_ID, "start metadata write."));		
	}

	@Override
	public void onWrited(String path, String version) {
		Activator.log(new Status(IStatus.INFO, Activator.PLUGIN_ID, "metadata writed. path = " + path + ". version = " + version));		
	}

	@Override
	public void onFinished() {
		Activator.log(new Status(IStatus.INFO, Activator.PLUGIN_ID, "finish metadata write."));		
	}

	@Override
	public boolean onWarning(String path, String message, String version) {
		Activator.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "warning metadata write proccess. path = " + path + ". version = " + version + ". message = " + message));
		return true;
	}

	@Override
	public boolean onErrored(String path, String message, String version) {
		Activator.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "error metadata write proccess. path = " + path + ". version = " + version + ". message = " + message));
		return true;
	}

}
