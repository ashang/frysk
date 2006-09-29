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

import java.util.HashMap;
import java.util.Map;

import lib.dw.DwarfDie;
import lib.dw.Dwfl;
import lib.dw.DwflDieBias;
import frysk.expr.CppSymTab;
import frysk.lang.DoubleType;
import frysk.lang.FloatType;
import frysk.lang.IntegerType;
import frysk.lang.ShortType;
import frysk.lang.Variable;
import frysk.proc.MachineType;
import frysk.proc.Task;
import frysk.proc.TaskException;
import frysk.rt.StackFactory;
import frysk.rt.StackFrame;
import frysk.sys.Errno;
import frysk.sys.PtraceByteBuffer;


class ExprSymTab implements CppSymTab
{
  private Task task;
  private int pid;
  private StackFrame innerMostFrame;
  private StackFrame currentFrame;  
  Map symTab;

  /**
   * Create an ExprSymTab object which is the interface between
   * SymTab and CppTreeParser, the expression parser.
   * @param task_p
   * @param pid_p
   */
  ExprSymTab (Task task_p, int pid_p)
  {
    task = task_p;
    pid = pid_p;
    try
    {
      innerMostFrame = StackFactory.createStackFrame(task);
    }
    catch (TaskException tte)
    {
      throw new RuntimeException(tte);
    }
    currentFrame = innerMostFrame;    
    symTab = new HashMap();
  }
  
  /**
   * Given a variable, return its Die.
   * @param s
   * @return DwarfDie
   */
  private DwarfDie getDie (String s)
  {
    Dwfl dwfl;
    DwarfDie[] allDies;
    long pc;
    try
    {
      if (currentFrame == innerMostFrame)
        pc = task.getIsa().pc(task) - 1;
      else
        pc = currentFrame.getAddress();
    }
    catch (TaskException tte)
    {
      throw new RuntimeException(tte);
    }
  
    dwfl = new Dwfl(pid);
    DwflDieBias bias = dwfl.getDie(pc);
    DwarfDie die = bias.die;
    
    allDies = die.getScopes(pc - bias.bias);
    DwarfDie varDie = die.getScopeVar(allDies, s);
    if (varDie == null)
      return null;
    return varDie;
  }
  
  /**
   * Given a variable's Die return its address.
   * @param varDie
   * @return
   */
  private long getBufferAddr(DwarfDie varDie)
  {
    long pc;
    try
    {
      if (currentFrame == innerMostFrame)
        pc = task.getIsa().pc(task) - 1;
      else
        pc = currentFrame.getAddress();
    }
    catch (TaskException tte)
    {
      throw new RuntimeException(tte);
    }
    long addr = varDie.getAddr();
    long fbreg_and_disp [] = new long[2];
    varDie.getFrameBase (fbreg_and_disp, varDie.getScope(), pc);
    if (fbreg_and_disp[0] != -1)
    {
      long regval = 0;
      try
      {
        if (currentFrame == innerMostFrame)
          {
            regval = task.getIsa().getRegisterByName
            (task.getIsa().getRegisterNameByUnwindRegnum(fbreg_and_disp[0])).get(task);
          }
        else
          {
            regval = currentFrame.getReg(fbreg_and_disp[0]);
          }
      }
      catch (TaskException tte)
      {
        throw new RuntimeException(tte);
      }
 
      addr += fbreg_and_disp[1];
      addr += regval;
    }
    return addr;
  }
  
  /* (non-Javadoc)
   * @see frysk.expr.CppSymTab#put(java.lang.String, frysk.lang.Variable)
   */
  public void put (String s, Variable v)
  {
    DwarfDie varDie = getDie(s);
    if (varDie == null)
      return;

    long addr = getBufferAddr(varDie);

    ByteBuffer buffer;
    buffer = new PtraceByteBuffer(pid, PtraceByteBuffer.Area.DATA,
                                  0xffffffffl);
    try
    {
      buffer = buffer.order(task.getIsa().getByteOrder());
    }
    catch (TaskException tte)
    {
      throw new RuntimeException(tte);
    }

    try
    {
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
    catch (Errno e) {} 
  }
  
  /* (non-Javadoc)
   * @see frysk.expr.CppSymTab#get(java.lang.String)
   */
  public Variable get (String s)
  {
    ByteOrder byteorder;
    DwarfDie varDie = getDie(s);
    if (varDie == null)
      return (null);

    long addr = getBufferAddr(varDie);

    ByteBuffer buffer;
    buffer = new PtraceByteBuffer(pid, PtraceByteBuffer.Area.DATA,
                                  0xffffffffl);
    
    try 
    {
      byteorder = task.getIsa().getByteOrder();
    }
    catch (TaskException tte)
    {
      throw new RuntimeException(tte);
    }
    buffer = buffer.order(byteorder);

    Variable v;
    try
    {
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
    }
    catch (Errno e) {}
    return null;
  }
  
  /**
   * Get the current stack frame.
   * @return StackFrame
   */
  StackFrame getCurrentFrame ()
  {
    return currentFrame;
  }
  
  /**
   * Set the current stack frame.
   * @param sf_p
   */
  void setCurrentFrame (StackFrame sf_p)
  {
    currentFrame = sf_p;
  }
  
  /**
   * Get the most recent stack frame.
   * @return StackFrame
   */
  StackFrame getInnerMostFrame ()
  {
    return innerMostFrame;
  }
}
