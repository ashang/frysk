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
 * VariableList is intended to serve as a record of what variables are in a file
 * and the lines and columns where they occur.
 * 
 * @author ajocksch
 *
 */

public class VariableList {
	private Variable[] vars;
	
	/**
	 * Craetes a new VariableList of the given size
	 * @param size The number of lines in the file represented
	 */
	public VariableList(int size){
		vars = new Variable[size];
	}
	
	/**
	 * Adds a new variable to the VariableList
	 * 
	 * @param name The name of the variable
	 * @param lineNum The line number the variable's on
	 * @param startCol The column the variable starts on
	 * @param endCol The column the variable ends on
	 */
	public void addVariable(String name, int lineNum, int startCol, int endCol, boolean global){
		Variable toPut = new Variable(name, lineNum, startCol, global);
		this.addVariable(toPut);
	}
	
	/**
	 * Adds a new variable to the VariableList
	 * @param toPut The variable to add
	 */
	public void addVariable(Variable toPut){
		int lineNum = toPut.getLine();
		Variable var = this.vars[lineNum];
		
		if(var == null){
			this.vars[lineNum] = toPut;
			return;
		}
		
		while(var.getNext() != null){
			if(var.getCol() >= toPut.getCol())
				break;
			
			var = var.getNext();
		}
		
//		 no duplicates allowed
		if(var.getCol() == toPut.getCol())
			return;
		
		// reached the end of the list
		if(var.getNext() == null){
			var.setNext(toPut);
			toPut.setPrev(var);
			
			return;
		}
		
		// still at the start
		if(var.getPrev() == null){
			toPut.setNext(var);
			var.setPrev(toPut);
			this.vars[lineNum] = toPut;
			
			return;
		}
		
		// ordinary situation
		toPut.setNext(var);
		toPut.setPrev(var.getPrev());
		var.getPrev().setNext(var);
		var.setPrev(toPut);
	}
	
	/**
	 * Searches for a variable at the given line number and column. If no variable
	 * is found returns null
	 * 
	 * @param lineNum Line to search on
	 * @param col Column to search on
	 * @return The variable if found, null otherwise
	 */
	public Variable getVariable(int lineNum, int col){
		Variable var = this.vars[lineNum];
		
		while(var != null){
			if(var.isInRange(col))
				break;
			
			var = var.getNext();
		}
		
		return var;
	}
	
	public String toString(){
		String s = "";
		
		for(int i = 0;i < this.vars.length; i++){
			s += "["+i+"]";
			
			Variable var = this.vars[i];
			
			while(var != null){
				s += " -->"+var.toString();
				var = var.getNext();
			}
			
			s += "\n";
		}
		
		return s;
	}
}
