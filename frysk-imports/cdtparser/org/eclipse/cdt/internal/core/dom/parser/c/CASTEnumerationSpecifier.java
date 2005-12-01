/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Rational Software - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.internal.core.dom.parser.c;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.c.ICASTEnumerationSpecifier;
import org.eclipse.cdt.core.parser.util.ArrayUtil;

/**
 * @author jcamelon
 */
public class CASTEnumerationSpecifier extends org.eclipse.cdt.internal.core.dom.parser.c.CASTBaseDeclSpecifier implements
	ICASTEnumerationSpecifier {

    private IASTName name;

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier#addEnumerator(org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator)
     */
    public void addEnumerator(org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator enumerator) {
    	if (enumerator != null) {
    		enumeratorsPos++;
    		enumerators = (org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator[]) ArrayUtil.append( org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator.class, enumerators, enumerator );
    	}
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier#getEnumerators()
     */
    public org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator[] getEnumerators() {        
        if( enumerators == null ) return org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator.EMPTY_ENUMERATOR_ARRAY;
        enumerators = (org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator[]) ArrayUtil.removeNullsAfter( org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator.class, enumerators, enumeratorsPos );
        return enumerators;
    }

    private org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator [] enumerators = null;
    private int enumeratorsPos=-1;

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier#setName(org.eclipse.cdt.core.dom.ast.IASTName)
     */
    public void setName(IASTName name) {
        this.name = name;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier#getName()
     */
    public IASTName getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier#getRawSignature()
     */
    public String getRawSignature() {
       return getName().toString() == null ? "" : getName().toString(); //$NON-NLS-1$
    }

    public boolean accept( ASTVisitor action ){
        if( action.shouldVisitDeclSpecifiers ){
		    switch( action.visit( this ) ){
	            case ASTVisitor.PROCESS_ABORT : return false;
	            case ASTVisitor.PROCESS_SKIP  : return true;
	            default : break;
	        }
		}
        if( name != null ) if( !name.accept( action ) ) return false;
        org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator[] etors = getEnumerators();
        for ( int i = 0; i < etors.length; i++ ) {
            if( !etors[i].accept( action ) ) return false;
        }
        return true;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.dom.ast.IASTNameOwner#getRoleForName(org.eclipse.cdt.core.dom.ast.IASTName)
	 */
	public int getRoleForName(IASTName n ) {
		if( this.name == n  )
			return r_definition;
		return r_unclear;
	}

}
