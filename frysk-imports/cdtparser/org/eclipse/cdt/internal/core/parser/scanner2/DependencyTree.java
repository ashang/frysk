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
package org.eclipse.cdt.internal.core.parser.scanner2;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.parser.util.ArrayUtil;

public class DependencyTree implements IASTTranslationUnit.IDependencyTree, IDependencyNodeHost {

    private final String tu_path;
    
    public DependencyTree( String path )
    {
        tu_path = path;
    }

    public String getTranslationUnitPath() {
        return tu_path;
    }

    private IASTTranslationUnit.IDependencyTree.IASTInclusionNode [] incs = new IASTTranslationUnit.IDependencyTree.IASTInclusionNode[2];
    private int incsPos=-1;
    
    public IASTTranslationUnit.IDependencyTree.IASTInclusionNode[] getInclusions() {
        incs = (IASTTranslationUnit.IDependencyTree.IASTInclusionNode[]) ArrayUtil.removeNullsAfter( IASTTranslationUnit.IDependencyTree.IASTInclusionNode.class, incs, incsPos );
        return incs;
    }

    public void addInclusionNode(IASTTranslationUnit.IDependencyTree.IASTInclusionNode node) {
    	if (node != null) {
    		incsPos++;
    		incs = (IASTTranslationUnit.IDependencyTree.IASTInclusionNode[]) ArrayUtil.append( IASTTranslationUnit.IDependencyTree.IASTInclusionNode.class, incs, node );
    	}
    }

}
