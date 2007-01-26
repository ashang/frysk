// This file is part of the program FRYSK.
//
// Copyright 2006, 2007 Red Hat Inc.
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
import java.util.Iterator;
import java.util.List;

import javax.naming.NameNotFoundException;

import lib.dw.Dwarf;
import lib.dw.DwarfCommand;
import lib.dw.DwarfDie;
import lib.dw.Dwfl;
import lib.dw.DwflDieBias;
import lib.dw.DwflLine;
import lib.elf.Elf;
import lib.elf.ElfCommand;
import antlr.CommonAST;
import frysk.value.Variable;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.rt.StackFrame;
import frysk.expr.CppParser;
import frysk.expr.CppLexer;
import frysk.expr.CppSymTab;
import frysk.expr.CppTreeParser;


public class SymTab
{
  Proc proc;
  Task task;
  int pid;
  Elf elf;
  Dwarf dwarf;
  static ExprSymTab exprSymTab;


    public SymTab (int pid_p, Proc proc_p, Task task_p)
    {
      this(pid_p, proc_p, task_p, null);
    }
    /**
     * Create a symbol table object.
     * @param pid_p
     * @param proc_p
     * @param task_p
     */
    public SymTab (int pid_p, Proc proc_p, Task task_p, StackFrame frame)
    {
      pid = pid_p;
      proc = proc_p;
      task = task_p;
      try 
      {
        elf = new Elf(proc.getExe(), ElfCommand.ELF_C_READ);
        dwarf = new Dwarf(elf, DwarfCommand.READ, null);
      }
      catch (lib.elf.ElfException ee)
      {}
      exprSymTab = new ExprSymTab (task, pid, frame);
    }
    
    /**
     * Handle ConsoleReader Completor
     * @param buffer Input buffer.
     * @param cursor Position of TAB in buffer.
     * @param candidates List that may complete token.
     * @return cursor position in buffer
     */
    public int complete (String buffer, int cursor, List candidates)
    {
      long pc;
      Dwfl dwfl;
      
      StackFrame currentFrame = getCurrentFrame();
      pc = currentFrame.getAdjustedAddress();

      dwfl = new Dwfl(pid);
      DwflDieBias bias = dwfl.getDie(pc);
      DwarfDie die = bias.die;
      String token = "";

      String sInput = buffer.substring(0, cursor) + '\t' + (cursor < buffer.length() 
          ? buffer.substring(cursor) : "");

      sInput += (char)3;
      CppLexer lexer = new CppLexer(new StringReader(sInput));
      CppParser parser = new CppParser(lexer);
      try {
        parser.start();
      }
      catch (antlr.RecognitionException r)
      {}
      catch (antlr.TokenStreamException t)
      {}
      catch (frysk.expr.TabException t)
      {
        token = t.getTabExpression().trim();
      }

      DwarfDie[] allDies = die.getScopes(pc - bias.bias);
      List candidates_p = die.getScopeVarNames(allDies, token /*buffer.substring(buffer.indexOf(' ')+1)*/);
      for (Iterator i = candidates_p.iterator(); i.hasNext();)
        {
            String sNext = (String) i.next();
            candidates.add(sNext);
        }

      return buffer.indexOf(token) + 1;
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
      
      StackFrame currentFrame = getCurrentFrame();
      pc = currentFrame.getAdjustedAddress();

      dwfl = new Dwfl(pid);
      DwflDieBias bias = dwfl.getDie(pc);
      DwarfDie die = bias.die;
      StringBuffer result = new StringBuffer();

      DwarfDie[] allDies = die.getScopes(pc - bias.bias);
      DwarfDie varDie = die.getScopeVar(allDies, sInput);
      if (varDie == null)
        {
          varDie = DwarfDie.getDecl(dwarf, sInput);
	  if (varDie == null)
            throw new NameNotFoundException(sInput + " not found in scope.");
          if (varDie.isExternal())
            result.append("extern ");
          result.append(varDie + " " + varDie.getName());
          DwarfDie parm = varDie.getChild();
          boolean first = true;
          while (parm != null && parm.isFormalParameter())
            {
              if (parm.isArtificial() == false)
                {
                  if (first)
                    {
                      result.append(" (");
                      first = false;
                    }
                  else
                    result.append(",");
                  result.append(parm.getType().getName());
                }
              parm = parm.getSibling();
            }
          if (first == false)
            result.append(")");

          if (varDie == null)
            throw new NameNotFoundException(sInput + " not found in scope.");
        }
      else
        {
          if (varDie.isExternal())
            result.append("extern ");
          result.append(varDie);
        }
      if (varDie != null)
        {
          result.append(" at " + varDie.getDeclFile()
                        + "#" + varDie.getDeclLine());
        }
      return result.toString();
    }
    
     /**
     * Implement the cli print request.
     * @param sInput
     * @return Variable
     * @throws ParseException
     */
    static public Variable print(String sInput) throws ParseException,NameNotFoundException
    {
      final class TmpSymTab
      implements CppSymTab
      {
        public void put (String s, Variable v) throws NameNotFoundException    
        {
          throw new NameNotFoundException("No symbol table is available.");
        }
        public Variable get(String s) throws NameNotFoundException 
        {
          throw new NameNotFoundException("No symbol table is available.");
        }
        public boolean putUndefined() {return false;}
      }
      TmpSymTab tmpSymTab = new TmpSymTab();
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
      CppTreeParser treeParser;
      if (exprSymTab == null)
        treeParser = new CppTreeParser(4, 2, tmpSymTab);
      else
        treeParser = new CppTreeParser(4, 2, exprSymTab);

      try {
        result = treeParser.expr(t);
      }
      catch (ArithmeticException ae)  {
        throw ae;
      }
      catch (antlr.RecognitionException r)
      {}
      catch (frysk.value.InvalidOperatorException i)
      {}
      catch (frysk.value.OperationNotDefinedException o)
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
       if (tmpFrame != null)
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
