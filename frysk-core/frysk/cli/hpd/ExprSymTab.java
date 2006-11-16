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

import inua.eio.ArrayByteBuffer;
import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.lang.Integer;
import java.util.Map;

import lib.dw.BaseTypes;
import lib.dw.DwarfDie;
import lib.dw.Dwfl;
import lib.dw.DwflDieBias;
import frysk.expr.CppSymTab;
import frysk.value.ArrayType;
import frysk.value.ClassType;
import frysk.value.DoubleType;
import frysk.value.FloatType;
import frysk.value.IntegerType;
import frysk.value.ShortType;
import frysk.value.Variable;
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
  private StackFrame currentFrame;  

  Map symTab;
  ByteBuffer buffer;
  public boolean putUndefined ()
  {
      return false;
  }

  /**
   * Create an ExprSymTab object which is the interface between
   * SymTab and CppTreeParser, the expression parser.
   * @param task_p Task
   * @param pid_p Pid
   * @param frame StackFrame
   */
  ExprSymTab (Task task_p, int pid_p, StackFrame frame)
  {
    task = task_p;
    pid = pid_p;
    // ??? 0x7fffffffffffffff
    buffer = new PtraceByteBuffer(task.getTid(), PtraceByteBuffer.Area.DATA, 0x7fffffffffffffffl);
    ByteOrder byteorder;
    try 
    {
      byteorder = task.getIsa().getByteOrder();
    }
    catch (TaskException tte)
    {
      throw new RuntimeException(tte);
    }
    buffer = buffer.order(byteorder);
    
    if (frame == null)
      {
        try
          {
            currentFrame = StackFactory.createStackFrame(task);
          }
        catch (TaskException tte)
          {
            throw new RuntimeException(tte);
          }
      }
    
    else
      {
        while (frame.getInner() != null)
          frame = frame.getInner();

        /* currentFrame is now the innermost StackFrame */
        currentFrame = frame;
      }
    
    symTab = new HashMap();
  }
  

  interface VariableAccessor {
    int DW_OP_addr = 0x03;
    int DW_OP_fbreg = 0x91;
    boolean isSuccessful();
    void setSuccessful(boolean b);
    DwarfDie varDie = null;
    long getAddr (String s);
    int getInt (String s);
    void putInt (String s, Variable v);
    short getShort (String s);
    void putShort (String s, Variable v);
    float getFloat (String s);
    void putFloat (String s, Variable v);
    double getDouble (String s);
    void putDouble (String s, Variable v);
  }
  
  /**
   * Access by DW_FORM_block.  Typically this is a static address or ptr+disp.
   */
  class AccessDW_FORM_block implements VariableAccessor
  {
    private boolean succeeded = false;
    public boolean isSuccessful ()
    {
      return succeeded;
    }
    public void setSuccessful (boolean b)
    {
      succeeded = b;
    }

    /**
     * Given a variable's Die return its address.
     * @param varDie
     * @return
     */
    protected long getBufferAddr(DwarfDie varDie)
    {
      long pc;
      // ??? Need an isa specific way to get x86 reg names and numbers
      String[][] x86regnames = {
                    {"eax", "rax"}, {"ecx", "rdx"}, {"edx", "rcx"}, {"ebx", "rbx"},
                    {"esp", "rsi"}, {"ebp", "rdi"}, {"esi", "rbp"}, {"edi", "rsp"}
                  };
      int[] x86regnumbers = {0, 2, 1, 3, 7, 6, 4, 5};
      
      try
      {
      if (currentFrame.getInner() == null)
          pc = task.getIsa().pc(task) - 1;
        else
          pc = currentFrame.getAddress();
      }
      catch (TaskException tte)
      {
        throw new RuntimeException(tte);
      }
      long fbreg_and_disp [] = new long[2];
      varDie.getAddr (fbreg_and_disp);
      if (fbreg_and_disp[0] == DW_OP_addr)
        {
          setSuccessful(true);
          return fbreg_and_disp[1];
        }
      long addr = fbreg_and_disp[1];
      varDie.getFrameBase (fbreg_and_disp, varDie.getScope(), pc);
      if (fbreg_and_disp[0] != -1) // DW_OP_fbreg
      {
        long regval = 0;
        try
        {
          setSuccessful(true);
          if (currentFrame.getInner() == null)
            {
              if (MachineType.getMachineType() == MachineType.IA32)
                regval = task.getIsa().getRegisterByName(x86regnames[(int)fbreg_and_disp[0]][0]).get (task);
              else if (MachineType.getMachineType() == MachineType.X8664)
                regval = task.getIsa().getRegisterByName(x86regnames[(int)fbreg_and_disp[0]][1]).get (task);
            }
          else
            {
              if (MachineType.getMachineType() == MachineType.IA32)
                regval = currentFrame.getReg(x86regnumbers[(int)fbreg_and_disp[0]]);
              else if (MachineType.getMachineType() == MachineType.X8664)
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
 
    public long getAddr (String s)
    {
      DwarfDie varDie = getDie(s);
      return getBufferAddr(varDie);
    }
    
    public int getInt (String s)
    {
      DwarfDie varDie = getDie(s);
      long addr = getBufferAddr(varDie);
      return buffer.getInt(addr);
    }
    public void putInt (String s, Variable v) 
    {
      DwarfDie varDie = getDie(s);
      long addr = getBufferAddr(varDie);
      buffer.putInt(addr, v.getInt());
    }
    public short getShort (String s)
    {
      DwarfDie varDie = getDie(s);
      long addr = getBufferAddr(varDie);
      return buffer.getShort(addr);      
    }
    public void putShort (String s, Variable v)
    {
      DwarfDie varDie = getDie(s);
      long addr = getBufferAddr(varDie);
      buffer.putShort(addr, v.getShort());
    }
    public float getFloat (String s)
    {
      DwarfDie varDie = getDie(s);
      long addr = getBufferAddr(varDie);
      return buffer.getFloat(addr);      
    }
    public void putFloat (String s, Variable v)
    {
      DwarfDie varDie = getDie(s);
      long addr = getBufferAddr(varDie);
      buffer.putFloat(addr, v.getFloat());
    }
    public double getDouble (String s)
    {
      DwarfDie varDie = getDie(s);
      long addr = getBufferAddr(varDie);
      return buffer.getDouble(addr);      
    }
    public void putDouble (String s, Variable v)
    {
      DwarfDie varDie = getDie(s);
      long addr = getBufferAddr(varDie);
      buffer.putDouble(addr, v.getDouble());
    }
  }
  
  /**
   * Access by DW_FORM_data.  Typically this is a location list.
   */
  class AccessDW_FORM_data implements VariableAccessor
  {
    private boolean succeeded = false;
    public boolean isSuccessful ()
    {
      return succeeded;
    }
    public void setSuccessful (boolean b)
    {
      succeeded = b;
    }
    
    public long getAddr (String s)
    {
      return 0;
    }
    
    private long getReg(DwarfDie varDie)
    {
      long pc;
      int[] x86regnumbers = {0, 2, 1, 3, 7, 6, 4, 5};
      long reg = 0;
      
      try
      {
	if (currentFrame.getInner() == null)
          pc = task.getIsa().pc(task) - 1;
        else
          pc = currentFrame.getAddress();
      }
      catch (TaskException tte)
      {
        throw new RuntimeException(tte);
      }
      long fbreg_and_disp [] = new long[2];
      varDie.getFormData (fbreg_and_disp, varDie.getScope(), pc);
      if (fbreg_and_disp[0] != -1)
      {
        setSuccessful(true);
        if (MachineType.getMachineType() == MachineType.IA32)
          reg = x86regnumbers[(int)fbreg_and_disp[0]];
        else if (MachineType.getMachineType() == MachineType.X8664)
          reg = fbreg_and_disp[0];
      }
   
      return reg;
    }    

      
    
    public int getInt (String s)
    {
      DwarfDie varDie = getDie(s);
      long val = currentFrame.getReg(getReg(varDie));
      return (int)val;
    }
    public void putInt (String s, Variable v)
    {
      DwarfDie varDie = getDie(s);
      long reg = getReg(varDie);
      currentFrame.setReg(reg, (long)v.getInt());
    }
    public short getShort (String s)
    {
      DwarfDie varDie = getDie(s);
      long val = currentFrame.getReg(getReg(varDie));
      return (short)val;
    }
    public void putShort (String s, Variable v)
    {
      DwarfDie varDie = getDie(s);
      long reg = getReg(varDie);
      currentFrame.setReg(reg, (long)v.getShort());
    }
    public float getFloat (String s)
    {
      DwarfDie varDie = getDie(s);
      long val = currentFrame.getReg(getReg(varDie));
      return (float)val;
    }
    public void putFloat (String s, Variable v)
    {
      DwarfDie varDie = getDie(s);
      long reg = getReg(varDie);
      currentFrame.setReg(reg, (long)v.getFloat());
    }
    public double getDouble (String s)
    {
      DwarfDie varDie = getDie(s);
      long val = currentFrame.getReg(getReg(varDie));
      return (double)val;
    }
    public void putDouble (String s, Variable v)
    {
      DwarfDie varDie = getDie(s);
      long reg = getReg(varDie);
      currentFrame.setReg(reg, (long)v.getDouble());
    }
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
      if (currentFrame.getInner() == null)
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
    if (bias == null)
      return null;
    DwarfDie die = bias.die;
    
    allDies = die.getScopes(pc - bias.bias);
    DwarfDie varDie = die.getScopeVar(allDies, s);
    if (varDie == null)
      return null;
    return varDie;
  }
  
 
  /* (non-Javadoc)
   * @see frysk.expr.CppSymTab#put(java.lang.String, frysk.lang.Variable)
   */
  public void put (String s, Variable v)
  {
    VariableAccessor[] variableAccessor = 
    {
     new AccessDW_FORM_block()
 //    new AccessDwOpData()
    };
    DwarfDie varDie = getDie(s);
    if (varDie == null)
      return;

    try
    {
      DwarfDie type = varDie.getType();
      if (type == null)
        return;
      for(int i = 0; i < variableAccessor.length; i++) 
        {
          if (type.getBaseType() == BaseTypes.baseTypeInteger)
            {
              variableAccessor[i].putInt(s, v);
            }
          else if (type.getBaseType() == BaseTypes.baseTypeShort)
            {
              variableAccessor[i].putShort(s, v);
            }
          else if (type.getBaseType() == BaseTypes.baseTypeFloat)
            {
              variableAccessor[i].putFloat(s, v);
            }
          else if (type.getBaseType() == BaseTypes.baseTypeDouble)
            {
              variableAccessor[i].putDouble(s, v);
            }
        }
    }    
    catch (Errno e) {} 
  }
  
  /* (non-Javadoc)
   * @see frysk.expr.CppSymTab#get(java.lang.String)
   */
  public Variable get (String s)
  {
    VariableAccessor[] variableAccessor = 
      {
       new AccessDW_FORM_block(),
       new AccessDW_FORM_data()
      };
    ByteOrder byteorder;
    try 
    {
      byteorder = task.getIsa().getByteOrder();
    }
    catch (TaskException tte)
    {
      throw new RuntimeException(tte);
    }
    
    DwarfDie varDie = getDie(s);
    if (varDie == null)
      return (null);

    Variable v;

    for(int i = 0; i < variableAccessor.length; i++)
      {
        try
        {
          DwarfDie type = varDie.getType();
          if (type == null)
            return null;      
          if (type.getBaseType() == BaseTypes.baseTypeInteger)
            {
              int intVal = variableAccessor[i].getInt(s);
              if (variableAccessor[i].isSuccessful() == false)
                continue;
              IntegerType intType = new IntegerType(4, byteorder);
              v = IntegerType.newIntegerVariable(intType, s, intVal); 
              return v; 
            }
          else if (type.getBaseType() == BaseTypes.baseTypeShort)
            {
              short shortVal = variableAccessor[i].getShort(s);
              if (variableAccessor[i].isSuccessful() == false)
                continue;
              ShortType shortType = new ShortType(2, byteorder);
              v = ShortType.newShortVariable(shortType, s, shortVal); 
              return v; 
            }
          else if (type.getBaseType() == BaseTypes.baseTypeFloat)
            {
              float floatVal = variableAccessor[i].getFloat(s);
              if (variableAccessor[i].isSuccessful() == false)
                continue;
              FloatType floatType = new FloatType(4, byteorder);
              v = FloatType.newFloatVariable(floatType, s, floatVal); 
              return v; 
            }
          else if (type.getBaseType() == BaseTypes.baseTypeDouble)
            {
              double doubleVal = variableAccessor[i].getDouble(s);
              if (variableAccessor[i].isSuccessful() == false)
                continue;
              DoubleType doubleType = new DoubleType(8, byteorder);
              v = DoubleType.newDoubleVariable(doubleType, s, doubleVal); 
              return v; 
            }    
          else if (type.isArrayType())
              {
                DwarfDie subrange;
                long addr = variableAccessor[0].getAddr(s);
                if (addr == 0)
                  continue;
                ArrayList dims = new ArrayList();
                subrange = type.getChild();
                int bufSize = 1;
                while (subrange != null)
                  {
                    int arrDim = subrange.getUpperBound();
                    dims.add(new Integer(arrDim));
                    subrange = subrange.getSibling();
                    bufSize *= arrDim + 1;
                  }

                ArrayType arrayType = null;
                int typeSize = 0;
                if (type.getType().getBaseType() == BaseTypes.baseTypeShort)
                  {
                    typeSize = 2;
                    ShortType shortType = new ShortType(2, byteorder);
                    arrayType = new ArrayType (shortType, dims);
                  }
                else if (type.getType().getBaseType() == BaseTypes.baseTypeInteger)
                  {
                    typeSize = 4;
                    IntegerType intType = new IntegerType(4, byteorder);
                    arrayType = new ArrayType (intType, dims);
                  }
                else if (type.getType().getBaseType() == BaseTypes.baseTypeLong)
                  {
                    typeSize = 8;
                    IntegerType longType = new IntegerType(8, byteorder);
                    arrayType = new ArrayType (longType, dims);
                  }
                else if (type.getType().getBaseType() == BaseTypes.baseTypeFloat)
                  {
                    typeSize = 4;
                    FloatType floatType = new FloatType(4, byteorder);
                    arrayType = new ArrayType (floatType, dims);
                  }
                else if (type.getType().getBaseType() == BaseTypes.baseTypeDouble)
                  {
                    typeSize = 8;
                    DoubleType doubleType = new DoubleType(8, byteorder);
                    arrayType = new ArrayType (doubleType, dims);
                  }
                
                byte buf []= new byte[bufSize * typeSize];
                for (int j = 0 ; j < bufSize; j++)
                  buffer.get(addr + j * typeSize, buf, j * typeSize, typeSize);  
                ArrayByteBuffer abb = new ArrayByteBuffer(buf, 0, bufSize);

                abb.order(byteorder);
                Variable arrVar = new Variable (arrayType, "", abb);
                return arrVar;
              }
          else if (type.isClassType())
            {
                DwarfDie subrange;
                long addr = variableAccessor[0].getAddr(s);
                if (addr == 0)
                  continue;
                subrange = type.getChild();
                int bufSize = 1;
                ClassType classType = new ClassType (byteorder);
                int typeSize = 0;
                while (subrange != null)
                  {
                    if (subrange.getType().getBaseType() == BaseTypes.baseTypeShort)
                      {
                        typeSize += 2;
                        ShortType shortType = new ShortType(2, byteorder);
                        classType.addMember (shortType, subrange.getName());
                      }
                    else if (subrange.getType().getBaseType() == BaseTypes.baseTypeInteger)
                      {
                        typeSize += 4;
                        IntegerType intType = new IntegerType(4, byteorder);
                        classType.addMember (intType, subrange.getName());
                      }
                    else if (subrange.getType().getBaseType() == BaseTypes.baseTypeLong)
                      {
                        typeSize += 8;
                        IntegerType longType = new IntegerType(8, byteorder);
                        classType.addMember (longType, subrange.getName());
                      }
                    else if (subrange.getType().getBaseType() == BaseTypes.baseTypeFloat)
                      {
                        typeSize += 4;
                        FloatType floatType = new FloatType(4, byteorder);
                        classType.addMember (floatType, subrange.getName());
                      }
                    else if (subrange.getType().getBaseType() == BaseTypes.baseTypeDouble)
                      {
                        typeSize += 8;
                        DoubleType doubleType = new DoubleType(8, byteorder);
                        classType.addMember (doubleType, subrange.getName());
                      }
                    subrange = subrange.getSibling();
                  }
                
                byte buf []= new byte[typeSize];
                for (int j = 0 ; j < typeSize; j++)
                  buffer.get(addr + j, buf, j, 1);  
                ArrayByteBuffer abb = new ArrayByteBuffer(buf, 0, bufSize);

                abb.order(byteorder);
                Variable classVar = new Variable (classType, "", abb);
                return classVar;
            }
        }
        catch (Errno e) {}
    }
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
    StackFrame curr = currentFrame;
    
    while (curr.getInner() != null)
      curr = curr.getInner();
    
    return curr;
  }
}
