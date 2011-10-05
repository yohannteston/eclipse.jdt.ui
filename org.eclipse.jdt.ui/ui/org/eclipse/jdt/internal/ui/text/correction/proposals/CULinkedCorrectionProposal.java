/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.ui.IEditorPart;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalPositionGroup;

import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.viewsupport.LinkedProposalModelPresenter;

/**
 * A proposal for quick fixes and quick assist that work on a single compilation unit.
 * Either a {@link TextChange text change} is directly passed in the constructor or method
 * {@link #addEdits(IDocument, TextEdit)} is overridden to provide the text edits that are
 * applied to the document when the proposal is evaluated.
 * <p>
 * The proposal takes care of the preview of the changes as proposal information.
 * </p>
 * @since 3.2
 */
public class CULinkedCorrectionProposal extends CUCorrectionProposal {

	private LinkedProposalModel fLinkedProposalModel;


	/**
	 * Constructs a correction proposal working on a compilation unit with a given text change
	 *
	 * @param name the name that is displayed in the proposal selection dialog.
	 * @param cu the compilation unit on that the change works.
	 * @param change the change that is executed when the proposal is applied or <code>null</code>
	 * if implementors override {@link #addEdits(IDocument, TextEdit)} to provide
	 * the text edits or {@link #createTextChange()} to provide a text change.
	 * @param relevance the relevance of this proposal.
	 * @param image the image that is displayed for this proposal or <code>null</code> if no
	 * image is desired.
	 */
	public CULinkedCorrectionProposal(String name, ICompilationUnit cu, TextChange change, int relevance, Image image) {
		super(name, cu, change, relevance, image);
		fLinkedProposalModel= null;
	}

	/**
	 * Constructs a correction proposal working on a compilation unit.
	 * <p>Users have to override {@link #addEdits(IDocument, TextEdit)} to provide
	 * the text edits or {@link #createTextChange()} to provide a text change.
	 * </p>
	 *
	 * @param name The name that is displayed in the proposal selection dialog.
	 * @param cu The compilation unit on that the change works.
	 * @param relevance The relevance of this proposal.
	 * @param image The image that is displayed for this proposal or <code>null</code> if no
	 * image is desired.
	 */
	protected CULinkedCorrectionProposal(String name, ICompilationUnit cu, int relevance, Image image) {
		this(name, cu, null, relevance, image);
	}

	protected LinkedProposalModel getLinkedProposalModel() {
		if (fLinkedProposalModel == null) {
			fLinkedProposalModel= new LinkedProposalModel();
		}
		return fLinkedProposalModel;
	}

	public void setLinkedProposalModel(LinkedProposalModel model) {
		fLinkedProposalModel= model;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.proposals.CUCorrectionProposal#performChange(org.eclipse.ui.IEditorPart, org.eclipse.jface.text.IDocument)
	 */
	@Override
	protected void performChange(IEditorPart part, IDocument document) throws CoreException {
		try {
			super.performChange(part, document);
			if (part == null) {
				return;
			}

			if (fLinkedProposalModel != null) {
				if (fLinkedProposalModel.hasLinkedPositions() && part instanceof JavaEditor) {
					// enter linked mode
					ITextViewer viewer= ((JavaEditor) part).getViewer();
					new LinkedProposalModelPresenter().enterLinkedMode(viewer, part, isSwitchedEditor(), fLinkedProposalModel);
				} else if (part instanceof ITextEditor) {
					LinkedProposalPositionGroup.PositionInformation endPosition= fLinkedProposalModel.getEndPosition();
					if (endPosition != null) {
						// select a result
						int pos= endPosition.getOffset() + endPosition.getLength();
						((ITextEditor) part).selectAndReveal(pos, 0);
					}
				}
			}
		} catch (BadLocationException e) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
		}
	}

}
