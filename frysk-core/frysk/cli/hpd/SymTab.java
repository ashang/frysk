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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.naming.NameNotFoundException;

import lib.dw.Dwarf;
import lib.dw.DwarfCommand;
import lib.dw.DwarfDie;
import lib.dw.Dwfl;
import lib.dw.DwflDieBias;
import lib.dw.DwTagEncodings;
import lib.dw.DwAtEncodings;
import lib.elf.Elf;
import lib.elf.ElfCommand;
import antlr.CommonAST;
import frysk.value.Variable;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.rt.LexicalBlock;
import frysk.rt.StackFrame;
import frysk.rt.Subprogram;
import frysk.expr.CppParser;
import frysk.expr.CppLexer;
import frysk.expr.CppSymTab;
import frysk.expr.CppTreeParser;


public class SymTab
{
  Proc proc;
  int pid;
  Elf elf;
  Dwarf dwarf;
  
  static ExprSymTab[] exprSymTab;
  
  static Subprogram[] subprogram;

  public SymTab (int tid, Proc proc, Task task)
  {
    this(tid, proc, task, null);
  }
  
  /**
   * Create a symbol table object.
   * 
   * @param pid
   * @param proc
   * @param task
   */
  public SymTab (int tid, Proc proc, Task task, StackFrame f)
    {
      this.pid = tid;
      this.proc = proc;
      try 
      {
        elf = new Elf(proc.getExe(), ElfCommand.ELF_C_READ);
        dwarf = new Dwarf(elf, DwarfCommand.READ, null);
      }
      catch (lib.elf.ElfException ignore)
      {}
      exprSymTab = new ExprSymTab[1];
      subprogram = new Subprogram[1];
      exprSymTab[0] = new ExprSymTab (task, pid, f);
    }

   /**
     * Synchronize the symbol table with the current state of the task.
     */
   public void refresh()
   {
     for (int i = 0; i < exprSymTab.length; i++)
       {
         exprSymTab[i].refreshCurrentFrame();
         subprogram[i] = setSubprogram(exprSymTab[i].getCurrentFrame());
         exprSymTab[i].setSubprogram(subprogram[i]);
       }
   }
  
    /**
     * Handle ConsoleReader Completor
     * 
     * @param buffer Input buffer.
     * @param cursor Position of TAB in buffer.
     * @param candidates List that may complete token.
     * @return cursor position in buffer
     */
    public int complete (String buffer, int cursor, List candidates)
    {
      long pc;
      Dwfl dwfl;
      
      pc = getCurrentFrame().getAdjustedAddress();

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
      catch (antlr.RecognitionException ignore)
      {}
      catch (antlr.TokenStreamException ignore)
      {}
      catch (frysk.expr.TabException t)
      {
        token = t.getTabExpression().trim();
      }

      DwarfDie[] allDies = die.getScopes(pc - bias.bias);
      List candidates_p = die.getScopeVarNames(allDies, token);
      for (Iterator i = candidates_p.iterator(); i.hasNext();)
        {
            String sNext = (String) i.next();
            candidates.add(sNext);
        }

      return buffer.indexOf(token) + 1;
    }
    
    /**
     * Implement the cli what request
     * 
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
      
      pc = getCurrentFrame().getAdjustedAddress();

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
          if (varDie.getAttr(DwAtEncodings.DW_AT_external_))
            result.append("extern ");
          result.append(varDie + " " + varDie.getName());
          DwarfDie parm = varDie.getChild();
          boolean first = true;
          while (parm != null && parm.getTag() == DwTagEncodings.DW_TAG_formal_parameter_)
            {
              if (parm.getAttr(DwAtEncodings.DW_AT_artificial_) == false)
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
          if (varDie.getAttr(DwAtEncodings.DW_AT_external_))
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
       * 
       * @param sInput
       * @return Variable
       * @throws ParseException
       */
    static public Variable print (String sInput) throws ParseException,
      NameNotFoundException
  {
    Variable result = null;
    sInput += (char) 3;

    final class TmpSymTab
        implements CppSymTab
    {
      public void put (String s, Variable v) throws NameNotFoundException
      {
        throw new NameNotFoundException("No symbol table is available.");
      }

      public Variable get (String s) throws NameNotFoundException
      {
        throw new NameNotFoundException("No symbol table is available.");
      }

      public Variable get (String s, ArrayList v) throws NameNotFoundException
      {
        throw new NameNotFoundException("No symbol table is available.");
      }

      public Variable get (ArrayList v) throws NameNotFoundException
      {
        throw new NameNotFoundException("No symbol table is available.");
      }

      public boolean putUndefined ()
      {
        return false;
      }
    }

    CppLexer lexer = new CppLexer(new StringReader(sInput));
    CppParser parser = new CppParser(lexer);
    try
      {
        parser.start();
      }
    catch (antlr.RecognitionException r)
      {
      }
    catch (antlr.TokenStreamException t)
      {
      }
    catch (frysk.expr.TabException t)
      {
      }

    CommonAST t = (CommonAST) parser.getAST();
    CppTreeParser treeParser;
    if (exprSymTab == null)
      {
        TmpSymTab tmpSymTab = new TmpSymTab();
        treeParser = new CppTreeParser(4, 2, tmpSymTab);
        
        try
        {
          result = treeParser.expr(t);
        }
      catch (ArithmeticException ae)
        {
          ae.printStackTrace();
          throw ae;
        }
      catch (antlr.RecognitionException r)
        {
        }
      catch (frysk.value.InvalidOperatorException i)
        {
        }
      catch (frysk.value.OperationNotDefinedException o)
        {
        }
      
      return result;
      }
    else
      {
        /*
         * If this request has come from the SourceWindow, there's no way to
         * know which thread the mouse request came from; if there are multiple
         * innermost frames of multiple threads in the same source file, than
         * all of the threads have to be checked. If there's only one thread;
         * than this loop will run only once anyways.
         */
        int j = 0;
        while (result == null && j < exprSymTab.length)
          {
            treeParser = new CppTreeParser(4, 2, exprSymTab[j]);

            try
              {
                result = treeParser.expr(t);
              }
            catch (ArithmeticException ae)
              {
                ae.printStackTrace();
                throw ae;
              }
            catch (antlr.RecognitionException r)
              {
              }
            catch (frysk.value.InvalidOperatorException i)
              {
              }
            catch (frysk.value.OperationNotDefinedException o)
              {
              }

            ++j;
          }
        
        return result;
      }
  }
   
    
    /**
     * Implement the cli up/down requests.
     * 
     * @param level
     * @return StackFrame
     */
     public StackFrame setCurrentFrame(int level)
     {
       boolean down;
       StackFrame tmpFrame = exprSymTab[0].getCurrentFrame();
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
           level -= 1;
         }
       if (tmpFrame != null)
         {
           exprSymTab[0].setCurrentFrame(tmpFrame);
           subprogram[0] = setSubprogram(tmpFrame);
           exprSymTab[0].setSubprogram(subprogram[0]);
         }
       return exprSymTab[0].getCurrentFrame();
     }
     
     /**
       * Get the current stack frame.
       * 
       * @return
       */
     public StackFrame getCurrentFrame ()
     {
       return exprSymTab[0].getCurrentFrame();
     }
     /**
       * Get the most recent stack frame.
       * 
       * @return StackFrame
       */
     public StackFrame getInnerMostFrame ()
     {
       return exprSymTab[0].getInnerMostFrame();
     }
     
     private Subprogram setSubprogram(StackFrame sf)
     {
       Subprogram subPr = new Subprogram();
       LexicalBlock block = new LexicalBlock();
       subPr.setBlock(block);
       DwarfDie varDie = DwarfDie.getDecl(dwarf, sf.getSymbol().getName());
       DwarfDie parm = varDie.getChild();
       int nParms = 0;
 
       while (parm != null && parm.getTag() == DwTagEncodings.DW_TAG_formal_parameter_)
         {
           nParms += 1;
           parm = parm.getSibling();
         }
       parm = varDie.getChild();
       subPr.setParameters(nParms);
       Variable parms[] = subPr.getParameters();
       nParms = 0;
       while (parm != null && parm.getTag() == DwTagEncodings.DW_TAG_formal_parameter_)
         {
           if (parm.getAttr(DwAtEncodings.DW_AT_artificial_) == false)
             parms[nParms] = exprSymTab[0].getVariable(parm);
           parm = parm.getSibling();
           nParms += 1;
         }
       DwarfDie firstVar = parm;
       nParms = 0;
       while (parm != null)
         {
           nParms += 1;
           parm = parm.getSibling();
         }
       block.setVariables(nParms);
       Variable vars[] = block.getVariables();
       block.setVariableDies(nParms);
       DwarfDie dies[] = block.getVariableDies();
       block.setTypeDies(nParms);
       DwarfDie types[] = block.getTypeDies();
       parm = firstVar;
       nParms = 0;
       while (parm != null)
         {
           vars[nParms] = exprSymTab[0].getVariable(parm);
           if (vars[nParms] == null)
             {
               int tag = parm.getTag();
               switch (tag)
               {
                 case DwTagEncodings.DW_TAG_array_type_:
                 case DwTagEncodings.DW_TAG_base_type_:
                 case DwTagEncodings.DW_TAG_const_type_:
                 case DwTagEncodings.DW_TAG_pointer_type_:
                 case DwTagEncodings.DW_TAG_structure_type_:
                 case DwTagEncodings.DW_TAG_subrange_type_:
                 case DwTagEncodings.DW_TAG_typedef_:
                   types[nParms] = parm;
               }
             }
           else
             dies[nParms] = parm;
           parm = parm.getSibling();
           nParms += 1;
         }
       
       if (false)
         {
           Variable p[] = subPr.getParameters ();
           for (int j = 0; j < p.length; j++)
             System.out.println(p[j].getText());
           LexicalBlock b = subPr.getBlock();
           Variable v[] = b.getVariables();
           for (int j = 0; j < v.length; j++)
             if (v[j] != null)
               System.out.println(v[j].getText());
           DwarfDie d[] = b.getVariableDies();
           for (int j = 0; j < d.length; j++)
             if (d[j] != null)
               System.out.println(d[j].getName());
           DwarfDie t[] = b.getTypeDies();
           for (int j = 0; j < t.length; j++)
             if (t[j] != null)
               System.out.println(t[j].getName());
         }
       
       return subPr;
     }
     
     public void setFrames (StackFrame newFrames[])
     {
       exprSymTab = new ExprSymTab[newFrames.length];
       subprogram = new Subprogram[newFrames.length];
       for (int i = 0; i < newFrames.length; i++)
         {
           exprSymTab[i] = new ExprSymTab (newFrames[i].getTask(), 
                                           newFrames[i].getTask().getTid(), 
                                           newFrames[i]);
           subprogram[i] = setSubprogram(newFrames[i]);
         }
     }
}
