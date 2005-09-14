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
package frysk.gui.srcwin;

/**
 * This class represents a specific instance of a variable in a source file. It
 * records the name, line number, and column span of the variable. VariableLocations 
 * act as a linked list, so that multiple variables on the same line will be
 * connected
 * 
 * @author ajocksch
 *
 */

public class Variable extends CodeItem {
	private boolean isMember = false;
	
	private Variable prev = null;
	private Variable next = null;

	/**
	 * Create a new, empty variable
	 */
	public Variable(){
		super("", -1, -1);
	}
	
	/**
	 * Create a new VariableLocation with the given parameters
	 * @param name Name of the variable
	 * @param lineNum Line number where it occurs
	 * @param startCol Start of the column span (wrt the line it's on)
	 * @param endCol End of the column span (wrt the line it's on)
	 */
	public Variable(String name, int lineNum, int startCol, boolean member){
		super(name, lineNum, startCol);
		this.isMember = member;
	}

	/**
	 * @return The next VariableLocation on the line
	 */
	public Variable getNext() {
		return next;
	}

	/**
	 * Sets the next variable on the line
	 * @param next
	 */
	public void setNext(Variable next) {
		this.next = next;
	}

	/**
	 * @return The previous variable on the line
	 */
	public Variable getPrev() {
		return prev;
	}

	/**
	 * Sets the previous variable on the line
	 * @param prev
	 */
	public void setPrev(Variable prev) {
		this.prev = prev;
	}

	
	public String toString(){
		return this.name+":("+this.line+"-"+this.col+")";
	}

	public boolean isMember() {
		return isMember;
	}
}
