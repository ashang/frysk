// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

package frysk.debuginfo;

import java.io.PrintWriter;
import java.util.LinkedList;

import lib.dwfl.DwarfDie;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflDieBias;
import frysk.dwfl.DwflCache;
import frysk.rt.LineXXX;
import frysk.scopes.Scope;
import frysk.scopes.ScopeFactory;
import frysk.scopes.SourceLocation;
import frysk.scopes.SourceLocationFactory;
import frysk.scopes.Subprogram;
import frysk.scopes.Subroutine;
import frysk.stack.Frame;
import frysk.stack.FrameDecorator;

public class DebugInfoFrame extends FrameDecorator {

    private Subprogram subprogram;
    private Scope scope;

    private LinkedList inlinedSubprograms;

    private final TypeFactory typeFactory;
    
    protected DebugInfoFrame(FrameDecorator inner, Frame decorated) {
	super(inner, decorated);
	this.typeFactory = new TypeFactory(getTask().getISA());
    }

    public final Subprogram getSubprogram ()
    {
      if (subprogram == null) {
	  this.getScopes();
      }
      return subprogram;
    }

    /**
     * Returns a linked list of scopes starting
     * with the innermost scope containing the current pc.
     * @return
     */
    public final Scope getScopes() {
	
	if (scope == null) {
	    Dwfl dwfl = DwflCache.getDwfl(this.getTask());
	    DwflDieBias bias = dwfl.getCompilationUnit(getAdjustedAddress());

	    if (bias != null) {

		DwarfDie[] scopes = bias.die.getScopes(getAdjustedAddress());
		
		if (scopes.length == 0) {
		    return null;
		}

		scopes = scopes[0].getScopesDie();
		scope = ScopeFactory.theFactory.getScope(scopes[0], typeFactory);
		Scope tempScope = scope;
		
		if (tempScope instanceof Subprogram && !(((Subprogram)tempScope).isInlined()) && subprogram == null) {
		    subprogram = (Subprogram) tempScope;
		}
		    
		Scope outer = null;
		for (int i = 1; i < scopes.length; i++) {
		    outer = ScopeFactory.theFactory.getScope(scopes[i], typeFactory);
		    tempScope.setOuter(outer);
		    tempScope = outer;
		    
		    if (tempScope instanceof Subprogram && !(((Subprogram)tempScope).isInlined()) && subprogram == null) {
			subprogram = (Subprogram) tempScope;
		    }
		}
	    }
	}
	return scope;
    }

    /**
         * returns a LinkedList of all the inlined instances in this frame. The
         * first item in the list is the inner most (narrowest scope) inlined
         * function.
         * 
         * @return
         */
    public final LinkedList getInlinedSubprograms ()
    {
      if (inlinedSubprograms == null) {
	  this.inlinedSubprograms = new LinkedList();
	  
        Dwfl dwfl = DwflCache.getDwfl(this.getTask());
        DwflDieBias bias = dwfl.getCompilationUnit(getAdjustedAddress());

        if (bias != null) {

            DwarfDie[] scopes = bias.die.getScopes(getAdjustedAddress());
            
            if(scopes.length == 0){
        	return inlinedSubprograms;
            }
            
            scopes = scopes[0].getScopesDie();
            
            for (int i = 0; i < scopes.length; i++) {
        	Scope scope = ScopeFactory.theFactory.getScope(scopes[i], typeFactory);
        	if(scope instanceof Subprogram && ((Subprogram)scope).isInlined()){
        	    inlinedSubprograms.add(scope);
        	}
            }
        }
        this.setSubprogram(subprogram);
      }

      return this.inlinedSubprograms;
    }

    /**
     * Return this frame's list of lines as an array; returns an empty
     * array if there is no line number information available. The
     * lack of line-number information can be determined with the
     * test: <<tt>>.getLine() == Line.NULL</tt>.
     */
    public SourceLocation getLine() {
	if (this.line == null) {
	    this.line = SourceLocationFactory.getSourceLocation(getTask(), getAdjustedAddress());
        }
	return this.line;
    }
    private SourceLocation line;

    /**
     * XXX: This function should not be used DOM information (LineXXX)
     * should be moved out of DebugInfoFrame.
     * @return
     */
    public LineXXX getLineXXX(){
	if (this.getLine() == SourceLocation.UNKNOWN)
	    return null;
	if (this.lineXXX == null)
	    this.lineXXX = new LineXXX(this.getLine(), this.getTask().getProc());
	return lineXXX;
    }
    private LineXXX lineXXX;
    
    public void toPrint(PrintWriter writer, boolean printParameters,
		 boolean fullpath){
        Subprogram subprogram = this.getSubprogram();

        if (subprogram != null) {
	    writer.print("0x");
	    String addr = Long.toHexString(this.getAddress());
	    int padding = 2 * this.getTask().getISA().wordSize() - addr.length();
          
	    for (int i = 0; i < padding; ++i)
		writer.print('0');
          
	    writer.print(addr);
	    writer.print(" in " + subprogram.getName() + "(");
	    if (printParameters) {
		subprogram.printParameters(writer, this);
	    }
	    writer.print(") ");
          
	    if (fullpath) {
		SourceLocation line = this.getLine();
		writer.print(line.getFile().getPath());
		writer.print("#");
		writer.print(line.getLine());
	    } else {
		SourceLocation line = this.getLine();
		writer.print(".../"+line.getFile().getName());
		writer.print("#");
		writer.print(line.getLine());
	    }
          
        } else {
            super.toPrint(writer, true, fullpath);
        }
    }
    
    void printScopes(PrintWriter writer) {
	printScope(writer, getSubprogram(), " ");
    }

    private void printScope(PrintWriter writer, Scope scope, String indentString) {
	
	if (scope != null) {
	    writer.print(indentString + "{");
	    scope.toPrint(this, writer, indentString);
	    if(!(scope.getInner() instanceof Subroutine && ((Subroutine)scope.getInner()).isInlined())){
		printScope(writer, scope.getInner(), indentString+" ");
	    }
	    writer.println(indentString+"}");
	}
    }

    public final void setSubprogram (Subprogram subprogram)
      {
        this.subprogram = subprogram;
      }
      
      public DebugInfoFrame getInnerDebugInfoFrame() {
	  return (DebugInfoFrame) getInner();
      }
      
      public DebugInfoFrame getOuterDebugInfoFrame() {
	  return (DebugInfoFrame) getOuter();
      }
      
      public Frame unwind(){
	  Frame undecorated = getUndecoratedFrame().getOuter();
	  if (undecorated == null)
	      // Oops, nothing left.
	      return null;
	  DebugInfoFrame outer = new DebugInfoFrame(this, undecorated);
	  return outer;
      }
	
}
