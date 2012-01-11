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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

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

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
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
			fIsValidElement= false;
			if (elem instanceof IPackageFragmentRoot && ((IPackageFragmentRoot) elem).getKind() == IPackageFragmentRoot.K_BINARY) {
				IPackageFragmentRoot root= (IPackageFragmentRoot) elem;

				IClasspathEntry entry= JavaModelUtil.getClasspathEntry(root);
				if (entry != null && entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					fContainerPath= null;
					fEntry= entry;
					fIsValidElement= true;
					setDescription(PreferencesMessages.IndexConfigurationPropertyPage_IsPackageFragmentRoot_description);
				}
			}
		} catch (JavaModelException e) {
			// do nothing
		}
		if (!fIsValidElement) {
			setDescription(PreferencesMessages.IndexConfigurationPropertyPage_IsIncorrectElement_description);
		}
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.JAVADOC_CONFIGURATION_PROPERTY_PAGE); //TODO
	}

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		if (!fIsValidElement) {
			return new Composite(parent, SWT.NONE);
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

		fIndexConfigurationBlock= new IndexConfigurationBlock(getShell(), this, fInitalLocation);
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
		if (kind != IClasspathEntry.CPE_LIBRARY && kind != IClasspathEntry.CPE_VARIABLE) { //TODO: CPE_VARIABLE?
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
			URL indexLocation= getIndexLocation(entry);
			if (indexLocation != null) {
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
				String title= PreferencesMessages.SourceAttachmentPropertyPage_error_title; //TODO
				String message= PreferencesMessages.SourceAttachmentPropertyPage_error_message; //TODO
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
