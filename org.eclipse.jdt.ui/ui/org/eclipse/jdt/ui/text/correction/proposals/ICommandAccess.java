/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.text.correction.proposals;


/**
 * Correction proposals implement this interface to be invokable by a command. (e.g. keyboard
 * shortcut)
 * 
 * @since 3.8
 */
public interface ICommandAccess {

	/**
	 * All correction commands must start with the following prefix.
	 */
	public static final String COMMAND_PREFIX= "org.eclipse.jdt.ui.correction."; //$NON-NLS-1$
	/**
	 * Commands for quick assist must have the following suffix.
	 */
	public static final String ASSIST_SUFFIX= ".assist"; //$NON-NLS-1$

	/**
	 * Returns the id of the command that should invoke this correction proposal
	 * 
	 * @return the id of the command. This id must start with {@link ICommandAccess#COMMAND_PREFIX}
	 *         to be recognizes as correction command. In addition the id must end with
	 *         {@link ICommandAccess#ASSIST_SUFFIX} to be recognized as an assist command.
	 */
	String getCommandId();

}
