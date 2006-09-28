// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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

import java.io.StringReader;
import java.text.ParseException;

import javax.naming.NameNotFoundException;

import lib.dw.DwarfDie;
import lib.dw.Dwfl;
import lib.dw.DwflDieBias;
import lib.dw.DwflLine;
import antlr.CommonAST;
import frysk.lang.Variable;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TaskException;
import frysk.rt.StackFactory;
import frysk.rt.StackFrame;
import frysk.expr.CppParser;
import frysk.expr.CppLexer;
import frysk.expr.CppTreeParser;


public class SymTab
{
  Proc proc;
  Task task;
  int pid;
  static ExprSymTab exprSymTab;


    /**
     * Create a symbol table object.
     * @param pid_p
     * @param proc_p
     * @param task_p
     */
    public SymTab (int pid_p, Proc proc_p, Task task_p)
    {
      pid = pid_p;
      proc = proc_p;
      task = task_p;
      exprSymTab = new ExprSymTab (task, pid);
    }
    /**
     * Implement the cli what request
     * @param sInput
     * @return String
     * @throws ParseException
     * @throws NameNotFoundException
     */
    public String what(String sInput) throws ParseException,NameNotFoundException    

    {
      long pc;
      Dwfl dwfl;
      
      if (proc == null)
        throw new NameNotFoundException("No symbol table is available.");
      
      try
      {
        pc = task.getIsa().pc(task) - 1;
      }
      catch (TaskException tte)
      {
        throw new RuntimeException(tte);
      }

      dwfl = new Dwfl(pid);
      DwflLine line = null;
      DwflDieBias bias = dwfl.getDie(pc);
      DwarfDie die = bias.die;
      DwarfDie[] allDies = die.getScopes(die.getLowPC() - bias.bias);

      DwarfDie varDie = die.getScopeVar(allDies, sInput);
      if (varDie == null)
        throw new NameNotFoundException(sInput + " not found in scope.");
      return varDie.getType() + " " + varDie.getName()
                         + " declared on line " + varDie.getDeclLine()
                         + " of " + varDie.getDeclFile();
    }
    
     /**
     * Implement the cli print request.
     * @param sInput
     * @return Variable
     * @throws ParseException
     */
    static public Variable print(String sInput) throws ParseException
    {
      Variable result = null;

      sInput += (char)3;
      CppLexer lexer = new CppLexer(new StringReader (sInput));
      CppParser parser = new CppParser(lexer);
      try {
        parser.start();
      }
      catch (antlr.RecognitionException r)
      {}
      catch (antlr.TokenStreamException t)
      {}
      catch (frysk.expr.TabException t)
      {}

      CommonAST t = (CommonAST)parser.getAST();
      CppTreeParser treeParser = new CppTreeParser(4, 2, exprSymTab);

      try {
        Integer intResult;
        result = treeParser.expr(t);
      }
      catch (ArithmeticException ae)  {
        throw ae;
      }
      catch (antlr.RecognitionException r)
      {}
      catch (frysk.lang.InvalidOperatorException i)
      {}
      catch (frysk.lang.OperationNotDefinedException o)
      {}
      return result;
    }
    
    /**
     * Implement the cli up/down requests.
     * @param level
     * @return StackFrame
     */
     public StackFrame setCurrentFrame(int level)
     {
       boolean down;
       StackFrame tmpFrame = exprSymTab.getCurrentFrame();
       if (level < 0)
         {
           down = true;
           level = -level;
         }
       else
         down = false;
       
       while (tmpFrame != null && level != 0)
         {
           if (! down)
             tmpFrame = tmpFrame.getOuter();
           else
             tmpFrame = tmpFrame.getInner();
           level -= 1;;
         }
       exprSymTab.setCurrentFrame(tmpFrame);
       return exprSymTab.getCurrentFrame();
     }
     
     /**
      * Get the current stack frame.
      * @return
      */
     public StackFrame getCurrentFrame ()
     {
       return exprSymTab.getCurrentFrame();
     }
     /**
      * Get the most recent stack frame.
      * @return StackFrame
      */
     public StackFrame getInnerMostFrame ()
     {
       return exprSymTab.getInnerMostFrame();
     }
}
