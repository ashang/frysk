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
package frysk.debuginfo;

import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.naming.NameNotFoundException;

import lib.dw.Dwarf;
import lib.dw.DwarfCommand;
import lib.dw.DwarfDie;
import lib.dw.DwarfException;
import lib.dw.Dwfl;
import lib.dw.DwflDieBias;
import lib.dw.DwTagEncodings;
import lib.dw.DwAtEncodings;
import lib.elf.Elf;
import lib.elf.ElfCommand;
import antlr.CommonAST;
import frysk.value.Value;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.rt.Frame;
import frysk.rt.LexicalBlock;
import frysk.rt.Subprogram;
import frysk.expr.CppParser;
import frysk.expr.CppLexer;
import frysk.expr.CppSymTab;
import frysk.expr.CppTreeParser;


public class DebugInfo
{
  Proc proc;
  int pid;
  Elf elf;
  Dwarf dwarf;
  
  static DebugInfoEvaluator[] debugInfoEvaluator;
  
  static Subprogram[] subprogram;

  /**
   * Create a symbol table object.  There should be one SymTab per process.
   * 
   * @param pid
   * @param proc
   * @param task
   */
  public DebugInfo (int tid, Proc proc, Task task, Frame f)
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
      debugInfoEvaluator = new DebugInfoEvaluator[1];
      subprogram = new Subprogram[1];
      debugInfoEvaluator[0] = new DebugInfoEvaluator (task, pid, f);
    }

   /**
     * Synchronize the symbol table with the current state of the task.
     */
   public void refresh()
   {
     for (int i = 0; i < debugInfoEvaluator.length; i++)
       {
         debugInfoEvaluator[i].refreshCurrentFrame();
         subprogram[i] = setSubprogram(debugInfoEvaluator[i].getCurrentFrame());
         debugInfoEvaluator[i].setSubprogram(subprogram[i]);
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
      boolean haveStruct = false;
      if (token.endsWith("."))
	haveStruct = true;
	
      for (Iterator i = candidates_p.iterator(); i.hasNext();)
        {
            String sNext = (haveStruct ? "." : "") + (String) i.next();
            candidates.add(sNext);
        }

      if (haveStruct)
      	token = ".";
      return buffer.indexOf(token) + 1;
    }

  /**
   * Get the DwarfDie for a function symbol
   */
  public DwarfDie getSymbolDie(String input)
    throws NameNotFoundException
  {
    DwarfDie result = DwarfDie.getDecl(dwarf, input);
    if (result == null)
      throw new NameNotFoundException("symbol " + input + " not found.");
    else
      return result;
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
      if (bias == null)
        throw new NameNotFoundException("No symbol table is available.");
      DwarfDie die = bias.die;
      StringBuffer result = new StringBuffer();

      DwarfDie[] allDies = die.getScopes(pc - bias.bias);
      DwarfDie varDie = die.getScopeVar(allDies, sInput);
      if (varDie == null)
        {
          varDie = DwarfDie.getDecl(dwarf, sInput);
          if (varDie == null)
            throw new NameNotFoundException(sInput + " not found in scope.");
          if (varDie.getAttrBoolean(DwAtEncodings.DW_AT_external_))
            result.append("extern ");
          result.append(varDie + " " + varDie.getName());
          DwarfDie parm = varDie.getChild();
          boolean first = true;
          while (parm != null && parm.getTag() == DwTagEncodings.DW_TAG_formal_parameter_)
            {
              if (parm.getAttrBoolean(DwAtEncodings.DW_AT_artificial_) == false)
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
          if (varDie.getAttrBoolean(DwAtEncodings.DW_AT_external_))
            result.append("extern ");
          result.append(varDie);
          if (varDie.getType().getTag() == DwTagEncodings.DW_TAG_array_type_
              || varDie.getType().getTag() == DwTagEncodings.DW_TAG_structure_type_
              || varDie.getType().getTag() == DwTagEncodings.DW_TAG_enumeration_type_)
            {
              Value v = DebugInfo.print(sInput);
	      if (v != null)
		result.append(v.getType().getName());
            }
        }
      if (varDie != null)
        {
	  try
	    {
	      result.append(" at " + varDie.getDeclFile()
			    + "#" + varDie.getDeclLine());
	    }
	  catch (DwarfException de)
	    {
	      result.append(" at <unknown>");
	    }
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
    static public Value print (String sInput) throws ParseException,
      NameNotFoundException
  {
    Value result = null;
    sInput += (char) 3;

    final class TmpSymTab
        implements CppSymTab
    {
      public void put (String s, Value v) throws NameNotFoundException
      {
        throw new NameNotFoundException("No symbol table is available.");
      }

      public Value get (String s) throws NameNotFoundException
      {
        throw new NameNotFoundException("No symbol table is available.");
      }

      public Value get (ArrayList v) throws NameNotFoundException
      {
        throw new NameNotFoundException("No symbol table is available.");
      }
      
      public Value getAddress (String s) throws NameNotFoundException
      {
        throw new NameNotFoundException("No symbol table is available.");
      }
      public Value getMemory (String s) throws NameNotFoundException
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
    if (debugInfoEvaluator == null)
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
        while (result == null && j < debugInfoEvaluator.length)
          {
            treeParser = new CppTreeParser(4, 2, debugInfoEvaluator[j]);

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
     public Frame setCurrentFrame(int level)
     {
       boolean down;
       Frame tmpFrame = debugInfoEvaluator[0].getCurrentFrame();
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
           debugInfoEvaluator[0].setCurrentFrame(tmpFrame);
           subprogram[0] = setSubprogram(tmpFrame);
           debugInfoEvaluator[0].setSubprogram(subprogram[0]);
         }
       return debugInfoEvaluator[0].getCurrentFrame();
     }
     
     /**
       * Get the current stack frame.
       * 
       * @return
       */
     public Frame getCurrentFrame ()
     {
       return debugInfoEvaluator[0].getCurrentFrame();
     }
     /**
       * Get the most recent stack frame.
       * 
       * @return StackFrame
       */
     public Frame getInnerMostFrame ()
     {
       return debugInfoEvaluator[0].getInnerMostFrame();
     }
     
     private Subprogram setSubprogram(Frame sf)
     {
       DwarfDie varDie = DwarfDie.getDecl(dwarf, sf.getSymbol().getName());
       if (varDie == null)
         return null;
       Subprogram subPr = new Subprogram();
       LexicalBlock block = new LexicalBlock();
       subPr.setBlock(block);
       DwarfDie parm = varDie.getChild();
       int nParms = 0;
 
       while (parm != null && parm.getTag() == DwTagEncodings.DW_TAG_formal_parameter_)
         {
           nParms += 1;
           parm = parm.getSibling();
         }
       parm = varDie.getChild();
       subPr.setParameters(nParms);
       Value parms[] = subPr.getParameters();
       nParms = 0;
       while (parm != null && parm.getTag() == DwTagEncodings.DW_TAG_formal_parameter_)
         {
           if (parm.getAttrBoolean((DwAtEncodings.DW_AT_artificial_)) == false)
             parms[nParms] = debugInfoEvaluator[0].getVariable(parm);
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
       Value vars[] = block.getVariables();
       block.setVariableDies(nParms);
       DwarfDie dies[] = block.getVariableDies();
       block.setTypeDies(nParms);
       DwarfDie types[] = block.getTypeDies();
       parm = firstVar;
       nParms = 0;
       while (parm != null)
         {
           vars[nParms] = debugInfoEvaluator[0].getVariable(parm);
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
           Value p[] = subPr.getParameters ();
           System.out.println("Parameters");
           for (int j = 0; j < p.length; j++)
             System.out.println(p[j].getText());
           LexicalBlock b = subPr.getBlock();
           Value v[] = b.getVariables();
           System.out.println("Variables");
           for (int j = 0; j < v.length; j++)
             if (v[j] != null)
               System.out.println(v[j].getText());
           DwarfDie d[] = b.getVariableDies();
           for (int j = 0; j < d.length; j++)
             if (d[j] != null)
               System.out.println(d[j].getName());
           DwarfDie t[] = b.getTypeDies();
           System.out.println("Types");
           for (int j = 0; j < t.length; j++)
             if (t[j] != null)
               System.out.println(t[j].getName());
         }
       
       return subPr;
     }
     
     public void setFrames (Frame newFrames[])
     {
       debugInfoEvaluator = new DebugInfoEvaluator[newFrames.length];
       subprogram = new Subprogram[newFrames.length];
       for (int i = 0; i < newFrames.length; i++)
         {
           debugInfoEvaluator[i] = new DebugInfoEvaluator (newFrames[i].getTask(), 
                                           newFrames[i].getTask().getTid(), 
                                           newFrames[i]);
           subprogram[i] = setSubprogram(newFrames[i]);
	   debugInfoEvaluator[i].setSubprogram(subprogram[i]);
         }
     }
     
     
     public Value getVariable (DwarfDie die)
     {
       return debugInfoEvaluator[0].getVariable(die);
     } 

}
