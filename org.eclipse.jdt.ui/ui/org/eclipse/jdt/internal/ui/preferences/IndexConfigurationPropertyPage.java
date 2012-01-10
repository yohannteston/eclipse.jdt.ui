/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;

import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ArchiveFileFilter;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathSupport;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;

/**
 * Property page used to set the jar's index location
 */
public class IndexConfigurationPropertyPage extends PropertyPage implements IStatusChangeListener {

	public static final String PROP_ID= "org.eclipse.jdt.ui.propertyPages.IndexConfigurationPropertyPage"; //$NON-NLS-1$

	private IndexConfigurationBlock fIndexConfigurationBlock;
	private boolean fIsValidElement;
	private boolean fIsReadOnly;

	private IPath fContainerPath;
	private IClasspathEntry fEntry;
	private URL fInitalLocation;

	public IndexConfigurationPropertyPage() {
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		IJavaElement elem= getJavaElement();
		try {
			if (elem instanceof IPackageFragmentRoot && ((IPackageFragmentRoot) elem).getKind() == IPackageFragmentRoot.K_BINARY) {
				IPackageFragmentRoot root= (IPackageFragmentRoot) elem;

				IClasspathEntry entry= JavaModelUtil.getClasspathEntry(root);
				if (entry == null) {
					fIsValidElement= false;
					setDescription(PreferencesMessages.JavadocConfigurationPropertyPage_IsIncorrectElement_description);
				} else {
					if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
						fContainerPath= entry.getPath();
						fEntry= handleContainerEntry(fContainerPath, elem.getJavaProject(), root.getPath());
						fIsValidElement= fEntry != null;
					} else {
						fContainerPath= null;
						fEntry= entry;
						fIsValidElement= true;
						setDescription(PreferencesMessages.JavadocConfigurationPropertyPage_IsPackageFragmentRoot_description);
					}
				}
			} else {
				fIsValidElement= false;
				setDescription(PreferencesMessages.JavadocConfigurationPropertyPage_IsIncorrectElement_description);
			}
		} catch (JavaModelException e) {
			fIsValidElement= false;
			setDescription(PreferencesMessages.JavadocConfigurationPropertyPage_IsIncorrectElement_description);
		}
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.JAVADOC_CONFIGURATION_PROPERTY_PAGE);
	}

	private IClasspathEntry handleContainerEntry(IPath containerPath, IJavaProject jproject, IPath jarPath) throws JavaModelException {
		ClasspathContainerInitializer initializer= JavaCore.getClasspathContainerInitializer(containerPath.segment(0));
		IClasspathContainer container= JavaCore.getClasspathContainer(containerPath, jproject);
		if (initializer == null || container == null) {
			setDescription(Messages.format(PreferencesMessages.JavadocConfigurationPropertyPage_invalid_container, BasicElementLabels.getPathLabel(containerPath, false)));
			return null;
		}
		String containerName= container.getDescription();
		IStatus status= initializer.getAttributeStatus(containerPath, jproject, IClasspathAttribute.INDEX_LOCATION_ATTRIBUTE_NAME);
		if (status.getCode() == ClasspathContainerInitializer.ATTRIBUTE_NOT_SUPPORTED) {
			setDescription(Messages.format(PreferencesMessages.JavadocConfigurationPropertyPage_not_supported, containerName));
			return null;
		}
		IClasspathEntry entry= JavaModelUtil.findEntryInContainer(container, jarPath);
		if (status.getCode() == ClasspathContainerInitializer.ATTRIBUTE_READ_ONLY) {
			setDescription(Messages.format(PreferencesMessages.JavadocConfigurationPropertyPage_read_only, containerName));
			fIsReadOnly= true;
			return entry;
		}
		Assert.isNotNull(entry);
		setDescription(PreferencesMessages.JavadocConfigurationPropertyPage_IsPackageFragmentRoot_description);
		return entry;
	}

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		if (!fIsValidElement || fIsReadOnly) {
			Composite inner= new Composite(parent, SWT.NONE);
			
			if (fIsReadOnly) {
				GridLayout layout= new GridLayout();
				layout.marginWidth= 0;
				inner.setLayout(layout);

				Label label= new Label(inner, SWT.WRAP);
				label.setText(PreferencesMessages.JavadocConfigurationPropertyPage_location_path);
				
				Text location= new Text(inner, SWT.READ_ONLY | SWT.WRAP);
				GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
				gd.widthHint= convertWidthInCharsToPixels(80);
				location.setLayoutData(gd);
				String locationPath= PreferencesMessages.JavadocConfigurationPropertyPage_locationPath_none;
				if (fEntry != null) {
					URL javadocUrl= JavaDocLocations.getLibraryJavadocLocation(fEntry);
					if (javadocUrl != null) {
						locationPath= javadocUrl.toExternalForm();
					}
				}
				location.setText(locationPath);
				Dialog.applyDialogFont(inner);
			}
			return inner;
		}

		IJavaElement elem= getJavaElement();
		fInitalLocation= null;
		if (elem != null) {
			try {
				fInitalLocation= getIndexLocation(elem);
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}

		boolean isProject= (elem instanceof IJavaProject);
		fIndexConfigurationBlock= new IndexConfigurationBlock(getShell(), this, fInitalLocation, isProject);
		Control control= fIndexConfigurationBlock.createContents(parent);
		control.setVisible(elem != null);

		Dialog.applyDialogFont(control);
		return control;
	}

	private static URL getIndexLocation(IClasspathEntry entry) {
		if (entry == null) {
			throw new IllegalArgumentException("Entry must not be null"); //$NON-NLS-1$
		}

		int kind= entry.getEntryKind();
		if (kind != IClasspathEntry.CPE_LIBRARY && kind != IClasspathEntry.CPE_VARIABLE) {
			throw new IllegalArgumentException("Entry must be of kind CPE_LIBRARY or CPE_VARIABLE"); //$NON-NLS-1$
		}

		IClasspathAttribute[] extraAttributes= entry.getExtraAttributes();
		for (int i= 0; i < extraAttributes.length; i++) {
			IClasspathAttribute attrib= extraAttributes[i];
			if (IClasspathAttribute.INDEX_LOCATION_ATTRIBUTE_NAME.equals(attrib.getName())) {
				try {
					return new URL(attrib.getValue());
				} catch (MalformedURLException e) {
					return null;
				}
			}
		}
		return null;
	}

	private static URL getIndexLocation(IJavaElement element) throws JavaModelException {
		IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(element);
		if (root == null) {
			return null;
		}

		if (root.getKind() == IPackageFragmentRoot.K_BINARY) {
			IClasspathEntry entry= root.getResolvedClasspathEntry();
			URL javadocLocation= getIndexLocation(entry);
			if (javadocLocation != null) {
				return getIndexLocation(entry);
			}
			entry= root.getRawClasspathEntry();
			switch (entry.getEntryKind()) {
				case IClasspathEntry.CPE_LIBRARY:
				case IClasspathEntry.CPE_VARIABLE:
					return getIndexLocation(entry);
				default:
					return null;
			}
		}
		return null;
	}

	private IJavaElement getJavaElement() {
		IAdaptable adaptable= getElement();
		IJavaElement elem= (IJavaElement) adaptable.getAdapter(IJavaElement.class);
		if (elem == null) {

			IResource resource= (IResource) adaptable.getAdapter(IResource.class);
			//special case when the .jar is a file
			try {
				if (resource instanceof IFile && ArchiveFileFilter.isArchivePath(resource.getFullPath(), true)) {
					IProject proj= resource.getProject();
					if (proj.hasNature(JavaCore.NATURE_ID)) {
						IJavaProject jproject= JavaCore.create(proj);
						elem= jproject.getPackageFragmentRoot(resource); // create a handle
					}
				}
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
		return elem;
	}

	/*
	 * @see PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		if (fIndexConfigurationBlock != null) {
			fIndexConfigurationBlock.performDefaults();
		}
		super.performDefaults();
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		if (fIndexConfigurationBlock != null) {
			URL indexLocation= fIndexConfigurationBlock.getIndexLocation();
			if (indexLocation == null && fInitalLocation == null
					|| indexLocation != null && fInitalLocation != null && indexLocation.toExternalForm().equals(fInitalLocation.toExternalForm())) {
				return true; // no change
			}


			IJavaElement elem= getJavaElement();
			try {
				IRunnableWithProgress runnable= getRunnable(getShell(), elem, indexLocation, fEntry, fContainerPath);
				PlatformUI.getWorkbench().getProgressService().run(true, true, runnable);
			} catch (InvocationTargetException e) {
				String title= PreferencesMessages.SourceAttachmentPropertyPage_error_title;
				String message= PreferencesMessages.SourceAttachmentPropertyPage_error_message;
				ExceptionHandler.handle(e, getShell(), title, message);
				return false;
			} catch (InterruptedException e) {
				// cancelled
				return false;
			}
		}
		return true;
	}


	private static IRunnableWithProgress getRunnable(final Shell shell, final IJavaElement elem, final URL indexLocation, final IClasspathEntry entry, final IPath containerPath) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					if (elem instanceof IPackageFragmentRoot) {
						CPListElement cpElem= CPListElement.createFromExisting(entry, elem.getJavaProject());
						String loc= indexLocation != null ? indexLocation.toExternalForm() : null;
						cpElem.setAttribute(CPListElement.INDEX, loc);
						IClasspathEntry newEntry= cpElem.getClasspathEntry();
						String[] changedAttributes= { CPListElement.INDEX };
						BuildPathSupport.modifyClasspathEntry(shell, newEntry, changedAttributes, elem.getJavaProject(), containerPath, entry.getReferencingEntry() != null, monitor);
					}
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
	}


	/**
	 * @see IStatusChangeListener#statusChanged(IStatus)
	 */
	public void statusChanged(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}

}
