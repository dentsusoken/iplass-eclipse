/*
 * Copyright 2016 DENTSU SOKEN INC. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.ui.importwizard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.iplass.mtp.eclipse.Activator;
import org.iplass.mtp.eclipse.ImageRegistryInitializer;
import org.iplass.mtp.eclipse.service.ResourceFileFolder;

public class MetaDataImportWizardPage extends WizardPage {
	private static final int BUTTON_WIDTH = 200;
	private static final int BUTTON_HEIGHT = 28;
	
	private Text metaDataPath;
	
	private SelectedProject selectedProject;
	
	private Combo configPath;

	private ISelection selection;
	
	public MetaDataImportWizardPage(ISelection selection) {
		super("iPLAss MetaData Import Wizard");
		setTitle("Import File");
		setDescription("This wizard creates a new tenant local metadata files.");
		this.selection = selection;
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		{
			GridLayout layout = new GridLayout();
			container.setLayout(layout);
			layout.numColumns = 1;
			layout.verticalSpacing = 15;
		}
		
		// metadata xml
		{
			Composite metaCompo = createMetaDataComposite(container);
			
			Label imgLabel = new Label(metaCompo, SWT.NULL);
			imgLabel.setImage(Activator.getDefault().getImageRegistry().getDescriptor(ImageRegistryInitializer.IMPORT_WIZARD_METADATA).createImage());
			
			Label label = new Label(metaCompo, SWT.NULL);
			label.setText("&Source MetaData File:");

			metaDataPath = new Text(metaCompo, SWT.BORDER | SWT.SINGLE);
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			metaDataPath.setLayoutData(gd);
			metaDataPath.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					if (metaDataPath == null || metaDataPath.toString().isEmpty()) {
						updateStatus("Metadata Pash must be specified.");
						return;
					}		
					updateStatus(null);
				}
			});
			
			Button button = new Button(metaCompo, SWT.PUSH);
			button.setText("Browse...");
			GridData buttonGd = new GridData();
			buttonGd.widthHint = BUTTON_WIDTH;
			buttonGd.heightHint = BUTTON_HEIGHT;
			button.setLayoutData(buttonGd);
			button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
					String[] exts = {"*.xml"};
					dialog.setFilterExtensions(exts);
					String openFile = dialog.open();
					if (openFile != null) {
						metaDataPath.setText(openFile);	
					}
				}
			});
		}
		
		// Project
		Composite groupCompo = createProjectServiceConfigGroupComposite(container);
		{
			Label imgLabel = new Label(groupCompo, SWT.NULL);
			imgLabel.setImage(Activator.getDefault().getImageRegistry().getDescriptor(ImageRegistryInitializer.IMPORT_WIZARD_PROJECT).createImage());

			Label label = new Label(groupCompo, SWT.NULL);
			label.setText("Destination &Project:");
			
			Combo project = new Combo(groupCompo, SWT.DROP_DOWN | SWT.READ_ONLY);
			project.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			project.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					selectedProject.comboChanged();
					refreshConfigPath();
					updateStatus(null);
				}
			});
			selectedProject = new SelectedProject(project);
		}
		
		// ServiceConfig
		{						
			Label imgLabel = new Label(groupCompo, SWT.NULL);
			imgLabel.setImage(JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKFRAG_ROOT));
			//imgLabel.setImage(Activator.getDefault().getImageRegistry().getDescriptor(ImageRegistryInitializer.IMPORT_WIZARD_SERVICE_CONFIG).createImage());

			Label label = new Label(groupCompo, SWT.NULL);
			label.setText("&Service Config Path:");
			
			configPath = new Combo(groupCompo, SWT.DROP_DOWN | SWT.READ_ONLY);
			configPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			configPath.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					updateStatus(null);
				}
			});
		}
		
		initialize();
		setControl(container);
	}

	private void initialize() {
		if (selection != null && selection.isEmpty() == false
				&& selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() > 1){
				return;
			}
			Object obj = ssel.getFirstElement();
			if (obj instanceof IResource) {
				IContainer container;
				if (obj instanceof IContainer) {
					container = (IContainer) obj;
				} else {
					container = ((IResource) obj).getParent();
				}
				
				for (IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
					if (p.exists() && p.isOpen() && p.getName().equals(container.getName())) {
						selectedProject.initSelect(JavaCore.create(p));
						refreshConfigPath();	
					}
				}
			}
		}
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		boolean isValidSourceMetaDataPath = metaDataPath.getText().length() != 0;
		boolean isSelectedjavaProject = selectedProject.getJavaProject() != null;
		boolean isValidServiceConfigPath = configPath.getText().length() != 0; 
		setPageComplete(isSelectedjavaProject && isValidSourceMetaDataPath && isValidServiceConfigPath);
	}
	
	private List<String> getServiceConfigFilesAsClassPath(IJavaProject javaProject) throws CoreException {
		ResourceFileFolder r = new ResourceFileFolder(javaProject);
		List<String> resources = r.getResourceFilesAsClasspath();
		Iterator<String> ite  = resources.iterator();
		while (ite.hasNext()) {
			String e = ite.next();
			if (!e.endsWith(".xml")) {
				ite.remove();
			}
		}
		return resources;
	}
	
	private void refreshConfigPath() {
		IJavaProject javaProject = selectedProject.getJavaProject();
		if (javaProject == null) {
			return;
		}

		List<String> resources = null;
		try {
			resources = getServiceConfigFilesAsClassPath(javaProject);
		} catch (CoreException ce) {
		}

		if (resources != null) {
			configPath.setItems(resources.toArray(new String[resources.size()]));
			configPath.setEnabled(true);
		} else {
			configPath.setItems(new String[0]);
			configPath.setEnabled(false);
		}
	}
	
	private Composite createMetaDataComposite(Composite container) {
		Composite metaCompo = new Composite(container, SWT.NONE);
		int numColumns = 4;
		metaCompo.setLayout(new GridLayout(numColumns, false));
		metaCompo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return  metaCompo;
	}
	
	private Composite createProjectServiceConfigGroupComposite(Composite container) {
		Composite groupCompo = new Composite(container, SWT.BORDER);
		int numColumns = 3;
		GridLayout layout = new GridLayout(numColumns, false);
		groupCompo.setLayout(layout);
		groupCompo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return  groupCompo;
	}
		
	private class SelectedProject {
		private Combo combo;

		private List<IJavaProject> itemsAsJavaProjects;
		
		private IJavaProject currentJavaProject;
		
		public SelectedProject(Combo combo) {
			this.combo = combo;
			this.itemsAsJavaProjects = getProjects();
			
			for(IJavaProject p : itemsAsJavaProjects) {
				this.combo.add(p.getProject().getName());
			}
			
			if (!this.itemsAsJavaProjects.isEmpty()) {
				combo.setEnabled(true);
			} else {
				combo.setEnabled(false);
			}
		}

		public void initSelect(IJavaProject javaProject) {
			String pjName = javaProject.getProject().getName();
			doSet(pjName);
			for (int i = 0; i < combo.getItemCount(); i++) {
				if (combo.getItem(i).equals(pjName)) {
					combo.select(i);
				}
			}
		}
		
		public void comboChanged() {
			doSet(combo.getText());
		}
		
		private void doSet(String target) {
			for(IJavaProject jp : itemsAsJavaProjects) {
				if(jp.getElementName().equals(target)) {
					this.currentJavaProject = jp;
					break;
				}
			}
		}
		
		public IJavaProject getJavaProject() {
			return currentJavaProject;
		}
		
		private List<IJavaProject> getProjects() {
			List<IJavaProject> projects = new ArrayList<IJavaProject>();
			for (IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
				if (p.exists() && p.isOpen()) {
					projects.add(JavaCore.create(p));
				}
					
			}
			return projects;
		}
	}

	public String getMetaDataPath() {
		return metaDataPath.getText();
	}

	public String getConfigPath() {
		return configPath.getText();
	}
	
	public IJavaProject getJavaProject() {
		return selectedProject.getJavaProject();
	}
}