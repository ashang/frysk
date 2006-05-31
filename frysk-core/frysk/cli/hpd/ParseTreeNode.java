// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.
package frysk.cli.hpd;

/**
 * A node of a tree representation of a process/thread set in hpdf notation.
 * On creation sets takes a parent reference, but when you add it to a real parent, the refence get repointed to that parent,
 * just in case, might change depending on implementation.
 */
public class ParseTreeNode
{
	public final static int TYPE_RANGE = 0;
	public final static int TYPE_REG = 1;

	ParseTreeNode parent;
	ParseTreeNode leftChild;
	ParseTreeNode rightChild;
	int id;
	int type;

	public ParseTreeNode(ParseTreeNode parent, int type)
	{
		this.parent = parent;
		this.id = -1;
		this.type = type;
	}

	public ParseTreeNode(ParseTreeNode parent, int id, int type)
	{
		this.parent = parent;
		this.id = id;
		this.type = type;
	}

	public ParseTreeNode(int type)
	{
		this.parent = null;
		this.id = -1;
		this.type = type;
	}

	public ParseTreeNode(int id, int type)
	{
		this.parent = null;
		this.id = id;
		this.type = type;
	}

	public boolean isLeaf()
	{
		boolean result = false;

		if (rightChild == null && leftChild == null)
			result = true;

		return result;
	}

	public void setParent(ParseTreeNode parent)
	{
		this.parent = parent;
	}

	public void setLeft(ParseTreeNode leftChild)
	{
		if (leftChild != null)
			leftChild.setParent(this);

		this.leftChild = leftChild;
	}

	public void setRight(ParseTreeNode rightChild)
	{
		if (rightChild != null)
			rightChild.setParent(this);

		this.rightChild = rightChild;
	}

	public ParseTreeNode getParent()
	{
		return parent;
	}

	public ParseTreeNode getLeft()
	{
		return leftChild;
	}

	public ParseTreeNode getRight()
	{
		return rightChild;
	}

	public int getID()
	{
		return id;
	}

	public int getType()
	{
		return type;
	}
}
