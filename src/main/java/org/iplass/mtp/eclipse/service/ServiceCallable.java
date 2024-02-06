/*
 * Copyright 2016 DENTSU SOKEN INC. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.service;

import org.eclipse.core.runtime.CoreException;

public interface ServiceCallable {
	void call() throws CoreException;
}
