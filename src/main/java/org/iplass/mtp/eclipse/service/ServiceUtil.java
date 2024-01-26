/*
 * Copyright 2016 DENTSU SOKEN INC. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.service;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.iplass.mtp.eclipse.Activator;
import org.iplass.mtp.eclipse.ui.PreferenceInitializer;

public class ServiceUtil {
	private static Set<String> pluginClassPath = new HashSet<String>();
	private static String loadedPjName;
	
	public static void invoke(IJavaProject javaProject, ServiceCallable callable) throws CoreException {
		setupResourcePath(javaProject);
		setupJarPath(javaProject);
		
		try {
			callable.call();
		} catch (Error e) {
			Activator.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "faital error occurred while executing service.", e));
			throw e;
		}
	}
	
	private static void setupResourcePath(IJavaProject javaProject) throws CoreException {
		cleanupStateLocationDir();
		copyProjectOutput(javaProject);
		copyResourceFiles(javaProject);
	}

	private static void setupJarPath(IJavaProject specifiedPj) throws CoreException {
		IJavaProject javaProject = resolveProject(specifiedPj);				
		
		IClasspathEntry[] classpathEntries = javaProject.getResolvedClasspath(true);
		for (IClasspathEntry cpe : classpathEntries) {
			if (cpe.getEntryKind() != IClasspathEntry.CPE_SOURCE) {
				IPath path = cpe.getPath();
				if (path.toString().startsWith("/")) {
					path = javaProject.getProject().getLocation()
							.append(path.toString().replaceFirst("/" + javaProject.getProject().getName(), ""));
				}
				
				if (!pluginClassPath.contains(path.toFile().getName())) {	
					addClasspath(path);
				}
			}
		}
		
		loadedPjName = javaProject.getProject().getName();
	}
	
	private static void addClasspath(IPath jarPath) throws CoreException {
		try {
			URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
			URL jarUrl = jarPath.toFile().getCanonicalFile().toURI().toURL();
			Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
			method.setAccessible(true);
			method.invoke(systemClassLoader, jarUrl);
			
			// 古いjarには対応できない. ロード前ならgradleキャッシュのURLは使わず.metadata/にコピーしておいて、
			//　そのURLを使えばプラグインから削除してロードできなくすることは可能だが、一度ロードされたものには通用しない.
			pluginClassPath.add(jarPath.toFile().getName());			
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | IOException e) {
			IStatus st = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "adding classpath failed.", e);
			throw new CoreException(st);
		}
	}
	
	/**
	 * classpathにjarを追加するプロジェクトを返す.
	 * <p>
	 * 既にロードされていたらそのプロジェクトを利用する.
	 * プリファレンスでプロジェクトを指定されていたら、そのプロジェクトを利用する.
	 * どちらでもなかったら、選択されていたプロジェクトを利用する.
	 * 
	 * @param specifiedPj
	 * @return
	 * @throws CoreException
	 */ 
	private static IJavaProject resolveProject(IJavaProject specifiedPj) throws CoreException {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		String preferencePjName = store.getString(PreferenceInitializer.KEY_CLASSPATH_PROJECT);
		
		if (!(loadedPjName == null || "".equals(loadedPjName))) {
			// ロード済
			return findJavaProject(loadedPjName);
		} else if (!(preferencePjName == null || "".equals(preferencePjName))) {
			// 未ロードでプリファレンス指定あり
			return findJavaProject(preferencePjName);
		} else {
			// 未ロードでプリファレンス指定もなし
			return specifiedPj;
		}
	}
	
	private static IJavaProject findJavaProject(String name) {
		IJavaProject javaPj = null;
		for (IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (p.exists() && p.isOpen() && p.getName().equals(name)) {
				javaPj = JavaCore.create(p);	
			}
		}
		return javaPj;
	}
	
	public static String getLoadedPjName() {
		return loadedPjName;
	}
	
	/**
	 * ユーザのプロジェクトのアウトプットをプラグインのクラスパスに持ってくる.
	 * 
	 * @param javaProject
	 * @throws CoreException
	 */
	private static void copyProjectOutput(IJavaProject javaProject) throws CoreException {
		try {
			String buildDir = javaProject.getOutputLocation().toString()
					.replaceFirst("/" + javaProject.getProject().getName(), "");
			Path pjOutputPath = new File(javaProject.getProject().getLocation() + buildDir).toPath();
			
			Path stateLocationOutPath = Activator.getDefault().getStateLocationOutPath();
			Files.walkFileTree(pjOutputPath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
						throws IOException {
					Files.createDirectories(stateLocationOutPath.resolve(pjOutputPath.relativize(dir)));
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
					Files.copy(file, stateLocationOutPath.resolve(pjOutputPath.relativize(file)),
							StandardCopyOption.REPLACE_EXISTING);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException | SecurityException | IllegalArgumentException | JavaModelException e) {
			throw new CoreException(
					new Status(IStatus.ERROR, Activator.PLUGIN_ID, "copying user project output failed.", e));
		}
	}

	private static void copyResourceFiles(IJavaProject javaProject) throws CoreException {
		ResourceFileFolder r = new ResourceFileFolder(javaProject);
		Path resourceDir = r.getFolderPath();
		Path stateLocationOutPath = Activator.getDefault().getStateLocationOutPath();

		try {
			for (Path file : r.getResourceFiles()) {
				Files.copy(file, stateLocationOutPath.resolve(resourceDir.relativize(file)),
						StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			throw new CoreException(
					new Status(IStatus.ERROR, Activator.PLUGIN_ID, "copying user project resource files failed.", e));
		}
	}
	
	private static void cleanupStateLocationDir() {
		Path stateLocationOut = Activator.getDefault().getStateLocationOutPath();
		if (!stateLocationOut.toFile().exists()) {
			return;
		}

		try {
			Files.walkFileTree(stateLocationOut, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			Activator.log("cleanup state location directory failed.", e);
		}
	}
}