/*
 * Copyright 2016 DENTSU SOKEN INC. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.service;

import java.io.IOException;
import java.io.InputStream;

import org.iplass.mtp.eclipse.service.ServiceUtil;

import org.iplass.mtp.spi.ServiceConfigrationException;
import org.iplass.mtp.spi.ServiceRegistry;
import org.iplass.mtp.impl.core.config.ServiceRegistryInitializer;

public class ServiceRegistryThinWrapper {

	/**
	 * 事前にパスをチェックしてからサービスレジストリを返す.
	 * <p>
	 * Service Configがない状態でサービスレジストリを使ってExceptionInInitializerErrorが投げられると、
	 * 以降はアンロードされてプラグインのクラスローダから見えなくなってしまうため.
	 * 
	 * @return
	 */
	public static ServiceRegistry getRegistry() {
		String configPath = ServiceRegistryInitializer.getConfigFileName();
		try {
			InputStream is = ServiceUtil.class.getResourceAsStream(configPath);
			if (is == null) {
				throw new ServiceConfigrationException(configPath + " is not found.");
			}
			is.close();
		} catch (IOException e) {
			throw new ServiceConfigrationException(e);
		}
		return ServiceRegistry.getRegistry();
	}
}