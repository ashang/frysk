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
import frysk.value.FunctionType;
import frysk.value.Value;
import frysk.proc.Proc;
import frysk.rt.Frame;
import frysk.rt.Subprogram;
import frysk.dwfl.DwflFactory;
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
  
  DebugInfoEvaluator[] debugInfoEvaluator;
  
  Subprogram[] subprogram;

  /**
   * Create a symbol table object.  There should be one SymTab per process.
   * @param frame
   */
  public DebugInfo (Frame frame)
    {
      this.proc = frame.getTask().getProc();
      this.pid = this.proc.getPid();
      try 
      {
        elf = new Elf(this.proc.getExe(), ElfCommand.ELF_C_READ);
        dwarf = new Dwarf(elf, DwarfCommand.READ, null);
      }
      catch (lib.elf.ElfException ignore)
      {}
      debugInfoEvaluator = new DebugInfoEvaluator[1];
      subprogram = new Subprogram[1];
      debugInfoEvaluator[0] = new DebugInfoEvaluator (frame);
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

      dwfl = DwflFactory.createDwfl(proc);
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

      dwfl = DwflFactory.createDwfl(proc);
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
          if (varDie.getTag() == DwTagEncodings.DW_TAG_subprogram_)
            {
              Value value = debugInfoEvaluator[0].getSubprogramValue(varDie);
              result.append(((FunctionType)value.getType()).getName());
            }
          else
            result.append(varDie + " " + varDie.getName());
        }
      else
        {
          Value value = debugInfoEvaluator[0].getValue(varDie);
          if (varDie.getAttrBoolean(DwAtEncodings.DW_AT_external_))
            result.append("extern ");

          if (varDie.getType().getTag() == DwTagEncodings.DW_TAG_array_type_
              || varDie.getType().getTag() == DwTagEncodings.DW_TAG_structure_type_
              || varDie.getType().getTag() == DwTagEncodings.DW_TAG_enumeration_type_)
            {
              Value v = debugInfoEvaluator[0].get(sInput);
	      if (v != null)
		result.append(v.getType().getName());
            }
          if (value != null)
            result.append(value.getType().getName());
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
  public Value print (String sInput) throws ParseException,
    NameNotFoundException
  {
    Value result = null;
    sInput += (char) 3;
    
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
   
  static public Value printNoSymbolTable (String sInput) throws ParseException,
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
       // ??? Is this needed or is Frame setting up Subprogram?
       Subprogram subPr = new Subprogram(varDie, this);
       subPr.setFunctionType((FunctionType)debugInfoEvaluator[0].getSubprogramValue(varDie).getType());

       return subPr;
     }
     
     public void setFrames (Frame newFrames[])
     {
       debugInfoEvaluator = new DebugInfoEvaluator[newFrames.length];
       subprogram = new Subprogram[newFrames.length];
       for (int i = 0; i < newFrames.length; i++)
         {
           debugInfoEvaluator[i] = new DebugInfoEvaluator (newFrames[i]);
           subprogram[i] = setSubprogram(newFrames[i]);
	   debugInfoEvaluator[i].setSubprogram(subprogram[i]);
         }
     }
     
     
     public Value getValue (DwarfDie die)
     {
       return debugInfoEvaluator[0].getValue(die);
     } 

}
