/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints2;

public interface ITypeConstraint2 {
//	/**
//	 * Returns the resolved representation of the constraint.
//	 * For example, if <code>toString</code> returns "[a] &lt;= [b]" and types of 'a' and 'b' are A and B,
//	 * repespectively, then this method returns "A &lt;= B".
//	 *
//	 * This method is provided for debugging purposes only.
//	 */
//	public abstract String toResolvedString();

	/**
	 * Returns whether this is a simple constraint. If so, it can be safely downcast to 
	 * <code>SimpleTypeConstraint</code>.
	 */
	public boolean isSimpleTypeConstraint();
	
	public ConstraintVariable2[] getContainedConstraintVariables();
	
	public boolean isSameAs(ITypeConstraint2 other);
	
	public int getHash();
}
