/*
 * Copyright 2016 INFORMATION SERVICES INTERNATIONAL - DENTSU, LTD. All Rights Reserved.
 */
package org.iplass.mtp.eclipse;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {
	public static final String PLUGIN_ID = "jp.co.isid.mtp.eclipse"; //$NON-NLS-1$

	private static Activator plugin;

	private Path stateLocationOutPath;

	public Path getStateLocationOutPath() {
		return stateLocationOutPath;
	}
	
	public Activator() {
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		stateLocationOutPath = new File(Activator.getDefault().getStateLocation().toFile(), "out").toPath();
		stateLocationOutPath.toFile().mkdirs();
		addServiceConfigClassPath();
	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static Activator getDefault() {
		return plugin;
	}

	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	public static void log(String message, Throwable exception) {
		if (message == null) {
			message = "";
		}
		IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, IStatus.OK, message, exception);
		getDefault().getLog().log(status);
	}

	public static void log(Throwable exception) {
		if (exception instanceof CoreException) {
			IStatus st = ((CoreException) exception).getStatus();
			log(st.getMessage(), st.getException());
		} else {
			log("iplass plugin internal error.", exception);
		}
	}

	protected void initializeImageRegistry(ImageRegistry registry) {
		ImageRegistryInitializer.initializeImageRegistry(registry);
	}
	
	private void addServiceConfigClassPath() throws CoreException {
		try {
			URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
			URL stateLocation = Activator.getDefault().getStateLocationOutPath().toUri().toURL();
			Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
			method.setAccessible(true);
			method.invoke(systemClassLoader, stateLocation);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | IOException e) {
			IStatus st = new Status(IStatus.ERROR, PLUGIN_ID, "adding classpath failed.", e);
			throw new CoreException(st);
		}
	}
}
