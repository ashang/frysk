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

import frysk.dom.DOMSource;

/**
 * The StackLevel class represents a frame on the program execution stack (i.e. a
 * function call).
 * 
 * @author ajocksch
 *
 */
public class StackLevel {
	// Constant signifying the end of the line (length independantly)
	public static int EOL = -1;
	
	// current line information
	private CurrentLineSection currentLine;
	
	// Depth in the stack
	private int depth;
	
	// Next and previous scopes in the stack
	protected StackLevel nextScope;
	protected StackLevel prevScope;
	
	// Actual data for this stack
	private DOMSource data;

	/**
	 * Creates a new StackLevel with the given current line. This constructor
	 * allows the 'current line' to be a statement within a line. Eg:
	 * <pre>i = 1; j = i+1;</pre>, the current line can be set to 'i = 1;' 
	 * @param data The DOMSource representing the current file
	 * @param line The line the PC is currently on (in the source code)
	 * @param endLine The line that the current instruction ends on
	 * @param colStart The column of the current instruction
	 * @param colEnd The column of the end of the current instruction
	 */
	public StackLevel(DOMSource data, CurrentLineSection currentLine){
		this.currentLine = currentLine;
		
		this.depth = 0;
		this.data = data;
	}

	/**
	 * Creates a new stack level. This constructor assumes that the instruction at
	 * the current PC takes up an entire line of code
	 * @param data
	 * @param line
	 */
	public StackLevel(DOMSource data, int line){
		this(data, new CurrentLineSection(line, line, 0, StackLevel.EOL));
	}
	
	/**
	 * @return The next scope in the stack
	 */
	public StackLevel getNextScope() {
		return nextScope;
	}

	/**
	 * @return The previous scope in the stack
	 */
	public StackLevel getPrevScope() {
		return prevScope;
	}

	/**
	 * Attaches another StackLevel as the next scope in the list. Any previously
	 * attached scope is removed.
	 * @param next The scope to follow this one in the stack frame
	 */
	public void addNextScope(StackLevel next){
		if(this.nextScope != null){
			next.nextScope = this.nextScope;
			this.nextScope.depth++;
			this.nextScope.prevScope = next;
		}
		
		this.nextScope = next;
		next.depth = this.depth + 1;
		next.prevScope = this;
	}
	
	/**
	 * @return The depth of this scope in the stack
	 */
	public int getDepth() {
		return depth;
	}

	/**
	 * @return The DOM object representing this stack
	 */
	public DOMSource getData() {
		return data;
	}

	/**
	 *  @return true iff this scope has been previously parsed for static data
	 */
	public boolean isParsed() {
		return this.data.isParsed();
	}

	/**
	 * @param parsed Whether or not this scope has been previously parsed for 
	 * static data.
	 */
	public void setParsed(boolean parsed) {
		this.data.setParsed(parsed);
	}

	public CurrentLineSection getCurrentLine() {
		return currentLine;
	}

	public void setCurrentLine(CurrentLineSection currentLine) {
		this.currentLine = currentLine;
	}
}
