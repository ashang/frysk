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
 * @author ajocksch
 *
 */
public class PCLocation {
	private String filename;
	private String function;
	private int lineNum;
	
	private int depth;
	
	protected PCLocation nextScope;
	protected PCLocation prevScope;
	
	protected PCLocation parentScope;	
	protected PCLocation inlineScope;

	public PCLocation(String filename, String function, int line){
		this.filename = filename;
		this.lineNum = line;
		this.depth = 0;
		this.function = function;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public PCLocation getNextScope() {
		return nextScope;
	}

	public int getLineNum() {
		return lineNum;
	}

	public void setLineNum(int lineNum) {
		this.lineNum = lineNum;
	}

	public PCLocation getPrevScope() {
		return prevScope;
	}

	public void addNextScope(PCLocation next){
		if(this.nextScope != null){
			next.nextScope = this.nextScope;
			this.nextScope.depth++;
			this.nextScope.prevScope = next;
		}
		
		this.nextScope = next;
		next.depth = this.depth + 1;
		next.prevScope = this;
	}
	
	public void addInlineScope(PCLocation child){
		if(this.inlineScope != null){
			child.inlineScope = this.inlineScope;
			this.inlineScope.depth++;
			this.inlineScope.parentScope = child;
		}
		
		this.inlineScope = child;
		child.depth = this.depth + 1;
		child.parentScope = this;
	}
	
	public int getDepth() {
		return depth;
	}

	public String getFunction() {
		return function;
	}
}
