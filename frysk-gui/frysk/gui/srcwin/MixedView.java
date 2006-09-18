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

import java.util.Vector;

import org.gnu.gtk.HPaned;
import org.gnu.gtk.ScrolledWindow;

import frysk.rt.StackFrame;

public class MixedView extends HPaned implements View {

	private SourceView sourceWidget;
	private SourceView assemblyWidget;
	
	public MixedView(StackFrame scope, SourceWindow parent){
		super();
		
		this.sourceWidget = new SourceView(scope, parent);
		this.assemblyWidget = new SourceView(scope, parent, SourceBuffer.ASM_MODE);

		ScrolledWindow sw1 = new ScrolledWindow();
		sw1.add(this.sourceWidget);
		this.sourceWidget.showAll();
		this.add1(sw1);
		
		ScrolledWindow sw2 = new ScrolledWindow();
		sw2.add(this.assemblyWidget);
		this.add2(sw2);
		
		this.showAll();
	}

	public boolean findNext(String toFind, boolean caseSensitive) {
		boolean result = this.sourceWidget.findNext(toFind, caseSensitive);
		if(!result)
			result = this.assemblyWidget.findNext(toFind, caseSensitive);
		
		return result;
	}

	public boolean findPrevious(String toFind, boolean caseSensitive) {
		// TODO: How do we tell where we're searching back from?
		return false;
	}

	public boolean highlightAll(String toFind, boolean caseSensitive) {
		return this.sourceWidget.highlightAll(toFind, caseSensitive) ||
			this.assemblyWidget.highlightAll(toFind, caseSensitive);
	}

	public void scrollToFound() {
		// TODO: same problem as findPrevious
	}

	public void load(StackFrame data) {
		this.sourceWidget.load(data);
		this.assemblyWidget.load(data);
		this.assemblyWidget.setMode(SourceBuffer.ASM_MODE);
	}

	public void setSubscopeAtCurrentLine(InlineSourceView child) {
		// TODO Inlined code for mixed view? How do we do this?
	}

	public void clearSubscopeAtCurrentLine() {
		// TODO Inlined code for mixed view? How do we do this?
	}

	public void toggleChild() {
		// TODO Inlined code for mixed view? How do we do this?
	}

	public void scrollToFunction(String markName) {
		
	}

	public void scrollToLine(int line) {
		
	}

	public Vector getFunctions() {
		return null;
	}

	public StackFrame getScope() {
		return this.sourceWidget.getScope();
	}

}
