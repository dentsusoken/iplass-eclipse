/*
 * Copyright 2018 INFORMATION SERVICES INTERNATIONAL - DENTSU, LTD. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.service.porting;

import org.iplass.mtp.ApplicationException;

public class MetaDataPortingRuntimeException  extends ApplicationException {

	private static final long serialVersionUID = -2311690537066906921L;

	public MetaDataPortingRuntimeException() {
		super();
	}

	public MetaDataPortingRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public MetaDataPortingRuntimeException(String message) {
		super(message);
	}

	public MetaDataPortingRuntimeException(Throwable cause) {
		super(cause);
	}
}
