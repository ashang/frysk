/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.internal.core.dom.parser.cpp;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
import org.eclipse.cdt.core.parser.util.ArrayUtil;

/**
 * @author jcamelon
 */
public class CPPASTEnumerationSpecifier extends org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTBaseDeclSpecifier
		implements IASTEnumerationSpecifier, ICPPASTDeclSpecifier {

	private IASTName name;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier#addEnumerator(org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator)
	 */
	public void addEnumerator(IASTEnumerationSpecifier.IASTEnumerator enumerator) {
		if (enumerator != null) {
			enumeratorsPos++;
			enumerators = (IASTEnumerationSpecifier.IASTEnumerator[]) ArrayUtil.append( IASTEnumerationSpecifier.IASTEnumerator.class, enumerators, enumerator );
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier#getEnumerators()
	 */
	public IASTEnumerationSpecifier.IASTEnumerator[] getEnumerators() {
		if (enumerators == null)
			return IASTEnumerationSpecifier.IASTEnumerator.EMPTY_ENUMERATOR_ARRAY;
		enumerators = (IASTEnumerationSpecifier.IASTEnumerator[]) ArrayUtil.removeNullsAfter( IASTEnumerationSpecifier.IASTEnumerator.class, enumerators, enumeratorsPos );
		return enumerators;
	}


	private IASTEnumerationSpecifier.IASTEnumerator[] enumerators = null;
	private int enumeratorsPos=-1;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier#setName(org.eclipse.cdt.core.dom.ast.IASTName)
	 */
	public void setName(IASTName name) {
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier#getName()
	 */
	public IASTName getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier#getRawSignature()
	 */
	public String getRawSignature() {
		return getName().toString() == null ? "" : getName().toString(); //$NON-NLS-1$
	}

	public boolean accept(ASTVisitor action) {
		if (action.shouldVisitDeclSpecifiers) {
			switch (action.visit(this)) {
			case ASTVisitor.PROCESS_ABORT:
				return false;
			case ASTVisitor.PROCESS_SKIP:
				return true;
			default:
				break;
			}
		}
		if (name != null)
			if (!name.accept(action))
				return false;
		IASTEnumerationSpecifier.IASTEnumerator[] enums = getEnumerators();
		for (int i = 0; i < enums.length; i++)
			if (!enums[i].accept(action))
				return false;

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.core.dom.ast.IASTNameOwner#getRoleForName(org.eclipse.cdt.core.dom.ast.IASTName)
	 */
	public int getRoleForName(IASTName n) {
		if (name == n)
			return r_definition;
		return r_unclear;
	}
}
