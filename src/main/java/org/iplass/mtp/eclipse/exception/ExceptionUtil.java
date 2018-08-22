/*
 * Copyright 2016 INFORMATION SERVICES INTERNATIONAL - DENTSU, LTD. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.iplass.mtp.eclipse.Activator;

public class ExceptionUtil {
	
	public static MultiStatus createMultiStatus(Throwable t) {
		List<Status> childStatuses = new ArrayList<>();
		for(String ste: ExceptionUtils.getRootCauseStackTrace(t)) {
			childStatuses.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, ste));
		}
		
		MultiStatus multiStatus = new MultiStatus(Activator.PLUGIN_ID,
		        IStatus.ERROR, childStatuses.toArray(new Status[childStatuses.size()]),
		        ExceptionUtils.getRootCauseMessage(t), t);
		
		return multiStatus;
	}
	
	public static String getStackTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		boolean autoFlush = true;
		PrintWriter pw = new PrintWriter(sw, autoFlush);
		t.printStackTrace(pw);
		return sw.getBuffer().toString();
	}
}
