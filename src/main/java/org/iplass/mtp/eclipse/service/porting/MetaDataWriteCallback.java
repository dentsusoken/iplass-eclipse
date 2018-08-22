/*
 * Copyright 2018 INFORMATION SERVICES INTERNATIONAL - DENTSU, LTD. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.service.porting;

public interface MetaDataWriteCallback {
	void onStarted();
	void onWrited(String path, String version);
	void onFinished();
	boolean onWarning(String path, String message, String version);
	boolean onErrored(String path, String message, String version);
}