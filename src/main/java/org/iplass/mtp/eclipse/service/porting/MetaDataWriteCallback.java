/*
 * Copyright 2018 DENTSU SOKEN INC. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.service.porting;

public interface MetaDataWriteCallback {
	void onStarted();
	void onWrited(String path, String version);
	void onFinished();
	boolean onWarning(String path, String message, String version);
	boolean onErrored(String path, String message, String version);
}