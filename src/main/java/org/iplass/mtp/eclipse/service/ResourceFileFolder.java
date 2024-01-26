/*
 * Copyright 2016 DENTSU SOKEN INC. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.iplass.mtp.eclipse.Activator;

public class ResourceFileFolder {
	private IJavaProject javaProject;
	private String sourceFolder;
	
	public ResourceFileFolder(IJavaProject javaProject) {
		this.javaProject = javaProject; 
		this.sourceFolder = "src/main/resources";
	}
	
	public ResourceFileFolder(IJavaProject javaProject, String sourceFoleder) {
		this.javaProject = javaProject;
		this.sourceFolder = sourceFoleder;
	}
	
	public Path getFolderPath() throws JavaModelException {
		IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
		if(classpathEntries == null) {
			return null;
		}
		
		String pjName = javaProject.getProject().getName();
		String pjRoot = javaProject.getProject().getLocation().toString();
		String sourceFolder = null;
		
		for (IClasspathEntry cpe : classpathEntries) {
			IClasspathEntry resoleved = JavaCore.getResolvedClasspathEntry(cpe);
			sourceFolder = cpe.getPath().toString().replaceFirst("/" + pjName, "");

			if (IClasspathEntry.CPE_SOURCE == resoleved.getEntryKind() && sourceFolder.endsWith(this.sourceFolder)) {
				break;
			}
		}
		
		return new File(pjRoot + sourceFolder).toPath();
	}
	
	public List<String> getResourceFilesAsClasspath() throws CoreException {
		Path resourceFileDirectory = getFolderPath();
		
		List<String> list = new ArrayList<String>();
		try {
			Files.walkFileTree(resourceFileDirectory, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
						throws IOException {
					list.add("/" + resourceFileDirectory.relativize(file).toString().replaceAll("\\\\", "/"));
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new CoreException(
					new Status(IStatus.ERROR, Activator.PLUGIN_ID, "error occurred while seek classpath entry.", e));
		}
		return list;
	}
	
	public List<Path> getResourceFiles() throws CoreException {
		Path resourceFileDirectory = getFolderPath();
		
		List<Path> list = new ArrayList<Path>();
		try {
			Files.walkFileTree(resourceFileDirectory, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
						throws IOException {
					list.add(file.toAbsolutePath());
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new CoreException(
					new Status(IStatus.ERROR, Activator.PLUGIN_ID, "error occurred while seek classpath entry.", e));
		}
		return list;
	}
}
