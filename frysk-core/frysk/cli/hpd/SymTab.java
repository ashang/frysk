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

import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;

import java.io.StringReader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;


import lib.dw.DwarfDie;
import lib.dw.Dwfl;
import lib.dw.DwflDieBias;
import lib.dw.DwflLine;

import antlr.CommonAST;
import frysk.lang.BaseTypes;
import frysk.lang.DoubleType;
import frysk.lang.FloatType;
import frysk.lang.IntegerType;
import frysk.lang.ShortType;
import frysk.lang.Type;
import frysk.lang.Variable;
import frysk.expr.CppParser;
import frysk.expr.CppLexer;
import frysk.expr.CppTreeParser;

import frysk.expr.CppSymTab;
import frysk.proc.MachineType;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TaskException;
import frysk.sys.Ptrace;
import frysk.sys.PtraceByteBuffer;

class symTab implements CppSymTab
{
  static Map symTab = new HashMap();
  private DwarfDie getDie (String s)
  {
    Dwfl dwfl;
    DwarfDie[] allDies;
    long address;
    long pc;
    try
    {
      pc = SymTab.task.getIsa().pc(SymTab.task) - 1;
    }
    catch (TaskException tte)
    {
      throw new RuntimeException(tte);
    }
  
    dwfl = new Dwfl(SymTab.pid);
    DwflLine line = null;
    DwflDieBias bias = dwfl.getDie(pc);
    DwarfDie die = bias.die;
    allDies = die.getScopes(die.getLowPC() - bias.bias);

    DwarfDie varDie = die.getScopeVar(allDies, s);
    if (varDie == null)
      return null;
    return varDie;
  }
  
  private long getBufferAddr(DwarfDie varDie)
  {
    long pc;
    try
    {
      pc = SymTab.task.getIsa().pc(SymTab.task) - 1;
    }
    catch (TaskException tte)
    {
      throw new RuntimeException(tte);
    }
    long addr = varDie.getAddr();
    if (varDie.fbregVariable())
    {
      long regval = 0;
      try
      {
        if (MachineType.getMachineType() == MachineType.X8664)
          regval = SymTab.task.getIsa().getRegisterByName("rbp").get (SymTab.task);
        else if (MachineType.getMachineType() == MachineType.IA32)
          regval = SymTab.task.getIsa().getRegisterByName("ebp").get (SymTab.task);
      }
      catch (TaskException tte)
      {
        throw new RuntimeException(tte);
      }
      addr += varDie.getFrameBase (varDie.getScope(), pc);
      addr += regval;
    }
    return addr;
  }
  
  public void put (String s, Variable v)
  {
    DwarfDie varDie = getDie(s);
    if (varDie == null)
      return;

    long addr = getBufferAddr(varDie);

    ByteBuffer buffer;
    buffer = new PtraceByteBuffer(SymTab.pid, PtraceByteBuffer.Area.DATA,
                                  0xffffffffl);
    try
    {
      buffer = buffer.order(SymTab.task.getIsa().getByteOrder());
    }
    catch (TaskException tte)
    {
      throw new RuntimeException(tte);
    }

    if (varDie.getType().compareTo("int") == 0)
      {
        buffer.putInt(addr, v.getInt());
      }
    
    else if (varDie.getType().compareTo("short int") == 0)
      {
        buffer.putShort(addr, v.getShort());
      }
    else if (varDie.getType().compareTo("char") == 0)
      {
        buffer.putByte(addr, (byte)v.getChar());
      }
    else if (varDie.getType().compareTo("float") == 0)
      {
        buffer.putFloat(addr, v.getFloat());
      }
    else if (varDie.getType().compareTo("double") == 0)
      {
        buffer.putDouble(addr, v.getDouble());
      }
  }
  
  public Variable get (String s)
  {
    ByteOrder byteorder;
    DwarfDie varDie = getDie(s);
    if (varDie == null)
      return (null);

    long addr = getBufferAddr(varDie);

    ByteBuffer buffer;
    buffer = new PtraceByteBuffer(SymTab.pid, PtraceByteBuffer.Area.DATA,
                                  0xffffffffl);
    
    try 
    {
      byteorder = SymTab.task.getIsa().getByteOrder();
    }
    catch (TaskException tte)
    {
      throw new RuntimeException(tte);
    }
    buffer = buffer.order(byteorder);

    Variable v;
    if (varDie.getType().compareTo("int") == 0)
      {
        int intVal;
        intVal = buffer.getInt(addr);
        IntegerType intType = new IntegerType(4, byteorder);
        v = IntegerType.newIntegerVariable(intType, s, intVal); 
        return v; 
      }
    
    else if (varDie.getType().compareTo("short int") == 0)
      {
        short shortVal;
        shortVal = buffer.getShort(addr);
        ShortType shortType = new ShortType(2, byteorder);
        v = ShortType.newShortVariable(shortType, s, shortVal); 
        return v; 
      }
//    else if (varDie.getType().compareTo("char") == 0)
//      {
//        byte byteVal;
//        byteVal = buffer.getByte(addr);
//        ByteType byteType = new ByteType(2, byteorder);
//        v = ByteType.newByteVariable(byteType, s, byteVal); 
//        return v; 
//      }
    else if (varDie.getType().compareTo("float") == 0)
      {
        float floatVal;
        floatVal = buffer.getFloat(addr);
        FloatType floatType = new FloatType(4, byteorder);
        v = FloatType.newFloatVariable(floatType, s, floatVal);
        return v; 
      }    
    else if (varDie.getType().compareTo("double") == 0)
      {
        double doubleVal;
        doubleVal = buffer.getDouble(addr);
        DoubleType doubleType = new DoubleType(8, byteorder);
        v = DoubleType.newDoubleVariable(doubleType, s, doubleVal); 
        return v; 
      }    
    return null;
  }
}

public class SymTab
{
    static Proc proc;
    static Task task;
    static int pid;
    static symTab hpdsymTab = new symTab();

    public SymTab (int pid_p, Proc proc_p, Task task_p)
    {
      pid = pid_p;
      proc = proc_p;
      task = task_p;
    }
    static public void what(Command cmd) throws ParseException
    {
      long pc;
      Dwfl dwfl;
      
      if (proc == null)
        {
          cmd.getOut().println("No symbol table is available");
          return;
        }
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

      String sInput = cmd.getFullCommand().substring(4).trim();
      DwarfDie varDie = die.getScopeVar(allDies, sInput);
      if (varDie == null)
        {
          cmd.getOut().println(sInput + " not found in scope.");
          return;
        }
      cmd.getOut().println(varDie.getType() + " " + varDie.getName()
                         + " declared on line " + varDie.getDeclLine()
                         + " of " + varDie.getDeclFile());
    }
    
    private static final int DECIMAL = 10;
    private static final int HEX = 16;
    private static final int OCTAL = 8;
    
    static public void print(Command cmd) throws ParseException
    {
      if (proc == null)
        {
          cmd.getOut().println("No symbol table is available");
          return;
        }
      
      Vector params = cmd.getParameters();
      Variable result;
      boolean haveFormat = false;
      int outputFormat = DECIMAL;
      String sInput = cmd.getFullCommand().substring(cmd.getAction().length()).trim();

      for (int i = 0; i < params.size(); i++)
        {
          if (((String)params.elementAt(i)).equals("-format"))
            {
              haveFormat = true;
              i += 1;
              String arg = ((String)params.elementAt(i));
              if (arg.compareTo("d") == 0)
                outputFormat = DECIMAL;
              else if (arg.compareTo("o") == 0)
                outputFormat = OCTAL;
              else if (arg.compareTo("x") == 0)
                outputFormat = HEX;
            }
        }
      if (haveFormat)
        sInput = sInput.substring(0,sInput.indexOf("-format"));

      if (sInput.length() == 0) {
        cmd.getOut().println ("Usage " + cmd.getAction() + " Expression [-format d|x|o]");
        return;
      }
      if (cmd.getAction().compareTo("assign") == 0) {
        int i = sInput.indexOf(' ');
        if (i == -1) {
          cmd.getOut().println ("Usage: assign Lhs Expression");
          return;
        }
        sInput = sInput.substring(0, i) + "=" + sInput.substring(i);
      }
      sInput += (char)3;
      CppLexer lexer = new CppLexer(new StringReader (sInput));
      CppParser parser = new CppParser(lexer);
      try {
        parser.start();
      }
      catch (antlr.RecognitionException r)
      {}
      catch (antlr.TokenStreamException t)
      {cmd.getOut().println ("Token");}
      catch (frysk.expr.TabException t)
      {cmd.getOut().println ("Tab");}

      CommonAST t = (CommonAST)parser.getAST();
      CppTreeParser treeParser = new CppTreeParser(4, 2, hpdsymTab);

      try {
        Integer intResult;
        result = treeParser.expr(t);
        switch (outputFormat)
        {
        case HEX: 
          cmd.getOut().print("0x");
          break;
        case OCTAL: 
          cmd.getOut().print("0");
          break;
        }
        if (result.getType().getTypeId() == BaseTypes.baseTypeFloat)
          cmd.getOut().println(String.valueOf(result.getFloat()));
        else if (result.getType().getTypeId() == BaseTypes.baseTypeDouble)
          cmd.getOut().println(String.valueOf(result.getDouble()));
        else
          cmd.getOut().println(Integer.toString((int)result.getType().longValue(result),outputFormat));
      }   catch (ArithmeticException ae)  {
        cmd.getOut().println("Arithmetic Exception occurred:  " + ae);
      }
      catch (antlr.RecognitionException r)
      {}
      catch (frysk.lang.InvalidOperatorException i)
      {}
      catch (frysk.lang.OperationNotDefinedException o)
      {}
    }
}
