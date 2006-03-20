/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Refactoring wizard for the extract supertype refactoring.
 * 
 * @since 3.2
 */
public final class ExtractSupertypeWizard extends RefactoringWizard {

	/**
	 * Creates a new extract supertype wizard.
	 * 
	 * @param refactoring
	 *            the refactoring
	 */
	public ExtractSupertypeWizard(final Refactoring refactoring) {
		super(refactoring, WIZARD_BASED_USER_INTERFACE);
		setDefaultPageTitle(RefactoringMessages.ExtractSupertypeWizard_defaultPageTitle);
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_EXTRACT_SUPERTYPE);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void addUserInputPages() {
		final PullUpMethodPage page= new PullUpMethodPage();
		addPage(new ExtractSupertypeMemberPage(page));
		addPage(page);
	}
}