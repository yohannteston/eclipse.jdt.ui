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
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.net.URL;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.StatusDialog;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.preferences.IndexConfigurationBlock;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

/**
 * Dialog to configure a Index location
 */
public class IndexLocationDialog extends StatusDialog {

	private IndexConfigurationBlock fIndexConfigurationBlock;

	/**
	 * Shows the UI for configuring an index location. Use {@link org.eclipse.jdt.ui.JavaUI} to
	 * access and configure index locations.
	 * 
	 * @param parent The parent shell for the dialog.
	 * @param libraryName Name of of the library to which configured index location belongs.
	 * @param initialURL The initial URL or <code>null</code>.
	 */
	public IndexLocationDialog(Shell parent, String libraryName, URL initialURL) {
		super(parent);

		IStatusChangeListener listener= new IStatusChangeListener() {
			public void statusChanged(IStatus status) {
				updateStatus(status);
			}
		};

		setTitle(Messages.format(NewWizardMessages.LibrariesWorkbookPage_JavadocPropertyDialog_title, libraryName));
		fIndexConfigurationBlock= new IndexConfigurationBlock(parent, listener, initialURL, false);
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 * @since 3.4
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);
		Control inner= fIndexConfigurationBlock.createContents(composite);
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
		applyDialogFont(composite);
		return composite;
	}

	/**
	 * Returns the configured index location. The result is only valid after the dialog has been
	 * opened and has not been cancelled by the user.
	 * 
	 * @return The configured index location
	 */
	public URL getResult() {
		return fIndexConfigurationBlock.getIndexLocation();
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.JAVADOC_PROPERTY_DIALOG);
	}
}
