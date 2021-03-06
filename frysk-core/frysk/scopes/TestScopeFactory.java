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

package frysk.scopes;

import lib.dwfl.DwarfDie;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflDie;
import frysk.debuginfo.TypeFactory;
import frysk.dwfl.DwflCache;
import frysk.proc.Task;
import frysk.stack.Frame;
import frysk.stack.StackFactory;
import frysk.testbed.DaemonBlockedAtSignal;
import frysk.testbed.TestLib;

public class TestScopeFactory
    extends TestLib
{
    public void testFrameScopes ()
    {
      Task task = (new DaemonBlockedAtSignal("funit-scopes")).getMainTask();
      Frame frame = StackFactory.createFrame(task);
      
      Dwfl dwfl = DwflCache.getDwfl(task);
      DwflDie bias = dwfl.getCompilationUnit(frame.getAdjustedAddress());
      DwarfDie[] scopes = bias.getScopes(frame.getAdjustedAddress());

      TypeFactory typeFactory = new TypeFactory(frame.getTask().getISA());
      
      Scope scope1 = ScopeFactory.theFactory.getScope(scopes[0], typeFactory);
      Scope scope2 = ScopeFactory.theFactory.getScope(scopes[1], typeFactory);
      Scope scope3 = ScopeFactory.theFactory.getScope(scopes[2], typeFactory);

      Scope scope4 = ScopeFactory.theFactory.getScope(scopes[0], typeFactory);
      Scope scope5 = ScopeFactory.theFactory.getScope(scopes[1], typeFactory);
      Scope scope6 = ScopeFactory.theFactory.getScope(scopes[2], typeFactory);

      // abstract inlined function
      Scope scope9 = ScopeFactory.theFactory.getScope(scopes[1].getOriginalDie(), typeFactory);
//      Scope scope10 =  ScopeFactory.theFactory.getScope(scopes[1].getOriginalDie(), typeFactory);

      // test scopes from outer frame
      frame = frame.getOuter();
      scopes = bias.getScopes(frame.getAdjustedAddress());

      Scope scope7 = ScopeFactory.theFactory.getScope(scopes[0], typeFactory);
      Scope scope8 = ScopeFactory.theFactory.getScope(scopes[0], typeFactory);
      
      
      
      assertTrue("lexical block scope" , scope1 instanceof LexicalBlock);
      assertTrue("ConcreteInlinedFunction scope" , scope2 instanceof ConcreteInlinedFunction);
      assertTrue("Abstract Inlinable funciton" , scope9 instanceof InlinedFunction);
      assertTrue("Subprogram scope" , scope7 instanceof Function && !((Function)scope7).isInlined());
      assertTrue("File scope" , scope3 instanceof Scope);
      
      assertTrue("same object" , scope1 == scope4);
      assertTrue("same object" , scope2 == scope5);
      assertTrue("same object" , scope3 == scope6);
      assertTrue("same object" , scope7 == scope8);
//      assertTrue("same object" , scope9 == scope10);
      
    }    
}
