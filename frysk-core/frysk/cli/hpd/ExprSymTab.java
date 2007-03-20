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
import java.util.Iterator;
import java.util.List;
import java.lang.Integer;

import lib.dw.BaseTypes;
import lib.dw.DwarfDie;
import lib.dw.Dwfl;
import lib.dw.DwflDieBias;
import lib.dw.DwOpEncodings;
import lib.dw.DwTagEncodings;
import frysk.expr.CppSymTab;
import frysk.value.ArrayType;
import frysk.value.ByteType;
import frysk.value.ClassType;
import frysk.value.DoubleType;
import frysk.value.FloatType;
import frysk.value.LongType;
import frysk.value.IntegerType;
import frysk.value.ShortType;
import frysk.value.Variable;
import frysk.proc.Isa;
import frysk.proc.Task;
import frysk.rt.LexicalBlock;
import frysk.rt.StackFactory;
import frysk.rt.StackFrame;
import frysk.rt.Subprogram;
import frysk.sys.Errno;
import frysk.sys.PtraceByteBuffer;

class ExprSymTab
    implements CppSymTab
{
  private Task task;

  private int pid;

  private StackFrame currentFrame;
  
  private Subprogram subprogram;

  private ByteBuffer buffer;

  private ByteType byteType;
  private ShortType shortType;
  private IntegerType intType;
  private LongType longType;
  private FloatType floatType;
  private DoubleType doubleType;
  
  public void setSubprogram (Subprogram subprogram)
  {
    this.subprogram = subprogram;
  }

  public boolean putUndefined ()
  {
    return false;
  }

  /**
   * Create an ExprSymTab object which is the interface between SymTab and
   * CppTreeParser, the expression parser.
   * 
   * @param task_p Task
   * @param pid_p Pid
   * @param frame StackFrame
   */
  ExprSymTab (Task task, int pid, StackFrame frame)
  {
    this.task = task;
    this.pid = pid;
    // ??? 0x7fffffffffffffff
    buffer = new PtraceByteBuffer(task.getTid(), PtraceByteBuffer.Area.DATA,
                                  0x7fffffffffffffffl);
    ByteOrder byteorder = task.getIsa().getByteOrder();
    buffer = buffer.order(byteorder);

    if (frame == null)
      {
        currentFrame = StackFactory.createStackFrame(task);
      }

    else
      {
        while (frame.getInner() != null)
          frame = frame.getInner();

        /* currentFrame is now the innermost StackFrame */
        currentFrame = frame;
      }

    byteType = new ByteType(1, byteorder);
    shortType = new ShortType(2, byteorder);
    intType = new IntegerType(4, byteorder);
    longType = new LongType(8, byteorder);
    floatType = new FloatType(4, byteorder);
    doubleType = new DoubleType(8, byteorder);
  }

  /**
   * Refresh the current frame.
   */
    
  void refreshCurrentFrame()
  {
    currentFrame = StackFactory.createStackFrame(task);
  }

  interface VariableAccessor
  {
    boolean isSuccessful ();

    void setSuccessful (boolean b);

    DwarfDie varDie = null;

    long getAddr (String s);

    long getLong (DwarfDie varDieP, long offset);

    void putLong (DwarfDie varDieP, long offset, Variable v);

    int getInt (DwarfDie varDieP, long offset);

    void putInt (DwarfDie varDieP, long offset, Variable v);

    short getShort (DwarfDie varDieP, long offset);

    void putShort (DwarfDie varDieP, long offset, Variable v);

    byte getByte (DwarfDie varDieP, long offset);

    void putByte (DwarfDie varDieP, long offset, Variable v);

    float getFloat (DwarfDie varDieP, long offset);

    void putFloat (DwarfDie varDieP, long offset, Variable v);

    double getDouble (DwarfDie varDieP, long offset);

    void putDouble (DwarfDie varDieP, long offset, Variable v);
  }

  /**
   * Access by DW_FORM_block. Typically this is a static address or ptr+disp.
   */
  class AccessDW_FORM_block
      implements VariableAccessor
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
     * 
     * @param varDieP
     * @return
     */
    protected long getBufferAddr (DwarfDie varDieP)
    {
      long pc;
      // ??? Need an isa specific way to get x86 reg names and numbers
      String[][] x86regnames = { { "eax", "rax" }, { "ecx", "rdx" },
                                { "edx", "rcx" }, { "ebx", "rbx" },
                                { "esp", "rsi" }, { "ebp", "rdi" },
                                { "esi", "rbp" }, { "edi", "rsp" }, 
                                { "", "r8" }, { "", "r9" }, { "", "r10"},
                                { "", "r11"}, { "", "r12"}};
      int[] x86regnumbers = { 0, 2, 1, 3, 7, 6, 4, 5 };

      pc = currentFrame.getAdjustedAddress();

      List ops = varDieP.getAddr();
      if (ops.size() == 0 ||
          ((DwarfDie.DwarfOp)ops.get(0)).operator == -1)
        return 0;
      if (((DwarfDie.DwarfOp)ops.get(0)).operator == DwOpEncodings.DW_OP_addr_)
        {
          setSuccessful(true);
          return ((DwarfDie.DwarfOp)ops.get(0)).operand1;
        }
      long addr = ((DwarfDie.DwarfOp)ops.get(0)).operand1;
      
      ops = varDieP.getFrameBase(pc);
      if (ops.size() == 0 ||
          ((DwarfDie.DwarfOp)ops.get(0)).operator == -1)
        return 0;
      int reg = ((DwarfDie.DwarfOp)ops.get(0)).operator;
      reg = (reg >= 0x70) ? reg - 0x70 : reg - 0x50;
      long regval = 0;
      
      // DW_OP_fbreg
      setSuccessful(true);
      if (currentFrame.getInner() == null)
        {
          Isa isa = task.getIsa();

          if (isa instanceof frysk.proc.IsaIA32)
            regval = isa.getRegisterByName(x86regnames[reg][0]).get(task);
          else if (isa instanceof frysk.proc.IsaX8664)
            regval = isa.getRegisterByName(x86regnames[reg][1]).get(task);
        }
      else
        {
          Isa isa = currentFrame.getTask().getIsa();

          if (isa instanceof frysk.proc.IsaIA32)
            regval = currentFrame.getReg(x86regnumbers[reg]);
          else if (isa instanceof frysk.proc.IsaX8664)
            regval = currentFrame.getReg(reg);
        }

      addr += ((DwarfDie.DwarfOp)ops.get(0)).operand1;
      addr += regval;
      return addr;
    }

    public long getAddr (String s)
    {
      DwarfDie die = getDie(s);
      return getBufferAddr(die);
    }

    public long getLong (DwarfDie varDieP, long offset)
    {
      long addr = getBufferAddr(varDieP);
      return buffer.getLong(addr + offset);
    }

    public void putLong (DwarfDie varDieP, long offset, Variable v)
    {
      long addr = getBufferAddr(varDieP);
      buffer.putLong(addr + offset, v.getLong());
    }

    public int getInt (DwarfDie varDieP, long offset)
    {
      long addr = getBufferAddr(varDieP);
      return buffer.getInt(addr + offset);
    }

    public void putInt (DwarfDie varDieP, long offset, Variable v)
    {
      long addr = getBufferAddr(varDieP);
      buffer.putInt(addr + offset, v.getInt());
    }

    public short getShort (DwarfDie varDieP, long offset)
    {
      long addr = getBufferAddr(varDieP);
      return buffer.getShort(addr + offset);
    }

    public void putShort (DwarfDie varDieP, long offset, Variable v)
    {
      long addr = getBufferAddr(varDieP);
      buffer.putShort(addr + offset, v.getShort());
    }

    public byte getByte (DwarfDie varDieP, long offset)
    {
      long addr = getBufferAddr(varDieP);
      return buffer.getByte(addr + offset);
    }

    public void putByte (DwarfDie varDieP, long offset, Variable v)
    {
      long addr = getBufferAddr(varDieP);
      buffer.putByte(addr + offset, v.getByte());
    }

    public float getFloat (DwarfDie varDieP, long offset)
    {
      long addr = getBufferAddr(varDieP);
      return buffer.getFloat(addr + offset);
    }

    public void putFloat (DwarfDie varDieP, long offset, Variable v)
    {
      long addr = getBufferAddr(varDieP);
      buffer.putFloat(addr + offset, v.getFloat());
    }

    public double getDouble (DwarfDie varDieP, long offset)
    {
      long addr = getBufferAddr(varDieP);
      return buffer.getDouble(addr + offset);
    }

    public void putDouble (DwarfDie varDieP, long offset, Variable v)
    {
      long addr = getBufferAddr(varDieP);
      buffer.putDouble(addr + offset, v.getDouble());
    }
  }

  /**
   * Access by DW_FORM_data. Typically this is a location list.
   */
  class AccessDW_FORM_data
      implements VariableAccessor
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

    private long getReg (DwarfDie varDieP)
    {
      long pc;
      int[] x86regnumbers = { 0, 2, 1, 3, 7, 6, 4, 5 };
      long reg = 0;
      Isa isa;

      if (currentFrame.getInner() == null)
        isa = task.getIsa();
      else
        isa = currentFrame.getTask().getIsa();

      pc = currentFrame.getAdjustedAddress();
      List ops = varDieP.getFormData(pc);
      
      if (ops.size() == 0 ||
          ((DwarfDie.DwarfOp)ops.get(0)).operator == -1)
        return 0;
      
      reg = ((DwarfDie.DwarfOp)ops.get(0)).operator;
      reg = (reg >= 0x70) ? reg - 0x70 : reg - 0x50;
      setSuccessful(true);
      if (isa instanceof frysk.proc.IsaIA32)
          reg = x86regnumbers[(int)reg];

      return reg;
    }

    public long getLong (DwarfDie varDieP, long offset)
    {
      long val = currentFrame.getReg(getReg(varDieP));
      return val;
    }

    public void putLong (DwarfDie varDieP, long offset, Variable v)
    {
      long reg = getReg(varDieP);
      currentFrame.setReg(reg, v.getLong());
    }

    public int getInt (DwarfDie varDieP, long offset)
    {
      long val = currentFrame.getReg(getReg(varDieP));
      return (int) val;
    }

    public void putInt (DwarfDie varDieP, long offset, Variable v)
    {
      long reg = getReg(varDieP);
      currentFrame.setReg(reg, v.getInt());
    }

    public short getShort (DwarfDie varDieP, long offset)
    {
      long val = currentFrame.getReg(getReg(varDieP));
      return (short) val;
    }

    public void putShort (DwarfDie varDieP, long offset, Variable v)
    {
      long reg = getReg(varDieP);
      currentFrame.setReg(reg, v.getShort());
    }

    public byte getByte (DwarfDie varDieP, long offset)
    {
      long val = currentFrame.getReg(getReg(varDieP));
      return (byte) val;
    }

    public void putByte (DwarfDie varDieP, long offset, Variable v)
    {
      long reg = getReg(varDieP);
      currentFrame.setReg(reg, v.getByte());
    }

    public float getFloat (DwarfDie varDieP, long offset)
    {
      long val = currentFrame.getReg(getReg(varDieP));
      float fval = Float.intBitsToFloat((int)val);
      return fval;
    }

    public void putFloat (DwarfDie varDieP, long offset, Variable v)
    {
      long reg = getReg(varDieP);
      currentFrame.setReg(reg, (long) v.getFloat());
    }

    public double getDouble (DwarfDie varDieP, long offset)
    {
      long val = currentFrame.getReg(getReg(varDieP));
      double dval = Double.longBitsToDouble(val);
      return dval;
    }

    public void putDouble (DwarfDie varDieP, long offset, Variable v)
    {
      long reg = getReg(varDieP);
      currentFrame.setReg(reg, (long) v.getDouble());
    }
  }

  /**
   * Given a variable, return its Die.
   * 
   * @param s
   * @return DwarfDie
   */
  private DwarfDie getDie (String s)
  {
    Dwfl dwfl;
    DwarfDie[] allDies;
    long pc = this.currentFrame.getAdjustedAddress();

    dwfl = new Dwfl(pid);
    DwflDieBias bias = dwfl.getDie(pc);
    if (bias == null)
      return null;
    DwarfDie die = bias.die;

    LexicalBlock b = subprogram.getBlock();
    Variable vars[] = b.getVariables();
    DwarfDie varDies[] = b.getVariableDies();
    DwarfDie varDie;
    for (int j = 0; j < vars.length; j++)
      if (vars[j] != null && vars[j].getText().compareTo(s) == 0)
        {
          allDies = die.getScopes(pc - bias.bias);
          varDies[j].setScopes(allDies);
          return varDies[j];
        }
          
    allDies = die.getScopes(pc - bias.bias);
    varDie = die.getScopeVar(allDies, s);
    if (varDie == null)
      return null;
    return varDie;
  }

  /*
   * (non-Javadoc)
   * 
   * @see frysk.expr.CppSymTab#put(java.lang.String, frysk.lang.Variable)
   */
  public void put (String s, Variable v)
  {
    VariableAccessor[] variableAccessor = { new AccessDW_FORM_block()
    // new AccessDwOpData()
    };
    DwarfDie varDie = getDie(s);
    if (varDie == null)
      return;

    try
      {
        DwarfDie type = varDie.getType();
        if (type == null)
          return;
        for (int i = 0; i < variableAccessor.length; i++)
          {
            if (type.getBaseType() == BaseTypes.baseTypeLong)
              {
                variableAccessor[i].putLong(varDie, 0, v);
              }
            else if (type.getBaseType() == BaseTypes.baseTypeInteger)
              {
                variableAccessor[i].putInt(varDie, 0, v);
              }
            else if (type.getBaseType() == BaseTypes.baseTypeShort)
              {
                variableAccessor[i].putShort(varDie, 0, v);
              }
            else if (type.getBaseType() == BaseTypes.baseTypeChar)
              {
                variableAccessor[i].putByte(varDie, 0, v);
              }
            else if (type.getBaseType() == BaseTypes.baseTypeFloat)
              {
                variableAccessor[i].putFloat(varDie, 0, v);
              }
            else if (type.getBaseType() == BaseTypes.baseTypeDouble)
              {
                variableAccessor[i].putDouble(varDie, 0, v);
              }
          }
      }
    catch (Errno ignore)
      {
      }
  }

  /*
   * (non-Javadoc)
   * 
   * @see frysk.expr.CppSymTab#get(java.lang.String)
   */
  public Variable get (String s)
  {
    VariableAccessor[] variableAccessor = { new AccessDW_FORM_block(),
                                           new AccessDW_FORM_data() };
    ByteOrder byteorder = task.getIsa().getByteOrder();

    DwarfDie varDie = getDie(s);
    if (varDie == null)
      return (null);

    for (int i = 0; i < variableAccessor.length; i++)
      {
        try
          {
            DwarfDie type = varDie.getType();
            int tag = type.getTag();
            if (type == null)
              return null;
            switch (type.getBaseType())
              {
              case BaseTypes.baseTypeLong:
              {
                long longVal = variableAccessor[i].getLong(varDie, 0);
                if (variableAccessor[i].isSuccessful() == false)
                  continue;
                return LongType.newLongVariable(longType, s, longVal);
              }
              case BaseTypes.baseTypeInteger:
              {
                int intVal = variableAccessor[i].getInt(varDie, 0);
                if (variableAccessor[i].isSuccessful() == false)
                  continue;
                return IntegerType.newIntegerVariable(intType, s, intVal);
              }
              case BaseTypes.baseTypeShort:
              {
                short shortVal = variableAccessor[i].getShort(varDie, 0);
                if (variableAccessor[i].isSuccessful() == false)
                  continue;
                return ShortType.newShortVariable(shortType, s, shortVal);
              }
              case BaseTypes.baseTypeChar:
              {
                byte byteVal = variableAccessor[i].getByte(varDie, 0);
                if (variableAccessor[i].isSuccessful() == false)
                  continue;
                return ByteType.newByteVariable(byteType, s, byteVal);
              }
              case BaseTypes.baseTypeFloat:
              {
                float floatVal = variableAccessor[i].getFloat(varDie, 0);
                if (variableAccessor[i].isSuccessful() == false)
                  continue;
                return FloatType.newFloatVariable(floatType, s, floatVal);
              }
              case BaseTypes.baseTypeDouble:
              {
                double doubleVal = variableAccessor[i].getDouble(varDie, 0);
                if (variableAccessor[i].isSuccessful() == false)
                  continue;
                return DoubleType.newDoubleVariable(doubleType, s, doubleVal);
              }
            }
            if (tag == DwTagEncodings.DW_TAG_array_type_)
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
                int typeSize = BaseTypes.getTypeSize(type.getType().getBaseType());
                switch (type.getType().getBaseType())
                  {
                  case BaseTypes.baseTypeChar:
                    arrayType = new ArrayType(byteType, dims);
                    break;
                  case BaseTypes.baseTypeShort:
                    arrayType = new ArrayType(shortType, dims);
                    break;
                  case BaseTypes.baseTypeInteger:
                    arrayType = new ArrayType(intType, dims);
                    break;
                  case BaseTypes.baseTypeLong:
                    arrayType = new ArrayType(longType, dims);
                    break;
                  case BaseTypes.baseTypeFloat:
                    arrayType = new ArrayType(floatType, dims);
                    break;
                  case BaseTypes.baseTypeDouble:
                    arrayType = new ArrayType(doubleType, dims);
                    break;
                  default:
                    return null;
                  }

                byte buf[] = new byte[bufSize * typeSize];
                for (int j = 0; j < bufSize; j++)
                  buffer.get(addr + j * typeSize, buf, j * typeSize, typeSize);
                ArrayByteBuffer abb = new ArrayByteBuffer(buf, 0, bufSize);

                abb.order(byteorder);
                Variable arrVar = new Variable(arrayType, s, abb);
                return arrVar;
              }
            else if (tag == DwTagEncodings.DW_TAG_structure_type_)
              {
                DwarfDie subrange;
                long addr = variableAccessor[0].getAddr(s);
                if (addr == 0)
                  continue;
                subrange = type.getChild();
                int bufSize = 1;
                ClassType classType = new ClassType(byteorder);
                long typeSize = 0;
                while (subrange != null)
                  {
                    long offset = subrange.getDataMemberLocation();
                    typeSize = offset + BaseTypes.getTypeSize(subrange.getType().getBaseType());
                    switch (subrange.getType().getBaseType())
                      {
                      case BaseTypes.baseTypeChar:
                        classType.addMember(byteType, subrange.getName(), offset);
                        break;
                      case BaseTypes.baseTypeShort:
                        classType.addMember(shortType, subrange.getName(), offset);
                        break;
                      case BaseTypes.baseTypeInteger:
                        classType.addMember(intType, subrange.getName(), offset);
                        break;
                      case BaseTypes.baseTypeLong:
                        classType.addMember(longType, subrange.getName(), offset);
                        break;
                      case BaseTypes.baseTypeFloat:
                        classType.addMember(floatType, subrange.getName(), offset);
                        break;
                      case BaseTypes.baseTypeDouble:
                        classType.addMember(doubleType, subrange.getName(), offset);
                        break;
                      default:
                        return null;
                      }
                    subrange = subrange.getSibling();
                  }

                byte buf[] = new byte[(int)typeSize];
                for (int j = 0; j < typeSize; j++)
                  buffer.get(addr + j, buf, j, 1);
                ArrayByteBuffer abb = new ArrayByteBuffer(buf, 0, bufSize);

                abb.order(byteorder);
                Variable classVar = new Variable(classType, s, abb);
                return classVar;
              }
          }
        catch (Errno ignore)
          {
          }
      }
    return null;
  }

  public Variable get (String s, ArrayList components)
  {
    VariableAccessor[] variableAccessor = { new AccessDW_FORM_block(),
                                            new AccessDW_FORM_data() };

     DwarfDie varDie = getDie(s);
     if (varDie == null)
       return (null);

     for (int i = 0; i < variableAccessor.length; i++)
       {
         try
         {
           DwarfDie type = varDie.getType();
           if (type == null)
             return null;
           ArrayList dims = new ArrayList();
           
           int typeSize = BaseTypes.getTypeSize(type.getType().getBaseType());
           
           for (DwarfDie subrange = type.getChild();
                subrange != null;
                subrange = subrange.getSibling())
             {
               int arrDim = subrange.getUpperBound();
               dims.add(new Integer(arrDim));
             }
           int stride [] = new int [dims.size()];
           stride[stride.length - 1] = typeSize;
           for (int d = stride.length - 2;
                d >= 0;
                d -= 1)
             {
               stride[d] = (((Integer)dims.get(d + 1)).intValue() + 1) * stride[d + 1];
             }

           Iterator ci = components.iterator();
           int offset = 0;
           for (int d = 0;
                ci.hasNext();
                d += 1)
             {
               String component = (String)ci.next();
               offset += stride[d] * (Integer.parseInt(component));
             }
           
           switch (type.getType().getBaseType())
             {
             case BaseTypes.baseTypeChar:
               byte byteVal = variableAccessor[i].getByte(varDie, offset);
               if (variableAccessor[i].isSuccessful() == false)
                 continue;
               return ByteType.newByteVariable(byteType, s, byteVal);
             case BaseTypes.baseTypeShort:
               short shortVal = variableAccessor[i].getShort(varDie, offset);
               if (variableAccessor[i].isSuccessful() == false)
                 continue;
               return ShortType.newShortVariable(shortType, s, shortVal);
             case BaseTypes.baseTypeInteger:
               int intVal = variableAccessor[i].getInt(varDie, offset);
               if (variableAccessor[i].isSuccessful() == false)
                 continue;
               return IntegerType.newIntegerVariable(intType, s, intVal);
             case BaseTypes.baseTypeLong:
               long longVal = variableAccessor[i].getLong(varDie, offset);
               if (variableAccessor[i].isSuccessful() == false)
                 continue;
               return LongType.newLongVariable(longType, s, longVal);
             case BaseTypes.baseTypeFloat:
               float floatVal = variableAccessor[i].getFloat(varDie, offset);
               if (variableAccessor[i].isSuccessful() == false)
                 continue;
               return FloatType.newFloatVariable(floatType, s, floatVal);
             case BaseTypes.baseTypeDouble:
               double doubleVal = variableAccessor[i].getDouble(varDie, offset);
               if (variableAccessor[i].isSuccessful() == false)
                 continue;
               return DoubleType.newDoubleVariable(doubleType, s, doubleVal);
             default:
               return null;
             }
         }
         catch (Errno ignore)
         {
         }
       }
     return null;
  }
  
  public Variable get (ArrayList components)
  {
    VariableAccessor[] variableAccessor = { new AccessDW_FORM_block(),
                                            new AccessDW_FORM_data() };

    String s = (String)components.get(0);
    DwarfDie varDie = getDie(s);
    
    if (varDie == null)
      return (null);

    DwarfDie type = varDie.getType();
    Iterator ci = components.iterator();
    String component;
    if (ci.hasNext())
      component = (String)ci.next();           

    try
    {
      DwarfDie subrange;
      long addr = variableAccessor[0].getAddr(s);
      if (addr == 0)
        return null;
      if (ci.hasNext())
        component = (String)ci.next();
      else
        return null;
      
      subrange = type.getChild();
      while (subrange != null)
        {
          for (int i = 0; i < variableAccessor.length; i++)
            {
              long offset = subrange.getDataMemberLocation();
              if (subrange.getName().equals(component))
                switch (subrange.getType().getBaseType())
                {
                case BaseTypes.baseTypeLong:
                {
                  long longVal = variableAccessor[i].getLong(varDie, offset);
                  if (variableAccessor[i].isSuccessful() == false)
                    continue;
                  return LongType.newLongVariable(longType, s, longVal);
                }
                case BaseTypes.baseTypeInteger:
                {
                  int intVal = variableAccessor[i].getInt(varDie, offset);
                  if (variableAccessor[i].isSuccessful() == false)
                    continue;
                  return IntegerType.newIntegerVariable(intType, s, intVal);
                }
                case BaseTypes.baseTypeShort:
                {
                  short shortVal = variableAccessor[i].getShort(varDie, offset);
                  if (variableAccessor[i].isSuccessful() == false)
                    continue;
                  return ShortType.newShortVariable(shortType, s, shortVal);
                }
                case BaseTypes.baseTypeChar:
                {
                  byte byteVal = variableAccessor[i].getByte(varDie, offset);
                  if (variableAccessor[i].isSuccessful() == false)
                    continue;
                  return ByteType.newByteVariable(byteType, s, byteVal);
                }
                case BaseTypes.baseTypeFloat:
                {
                  float floatVal = variableAccessor[i].getFloat(varDie, offset);
                  if (variableAccessor[i].isSuccessful() == false)
                    continue;
                  return FloatType.newFloatVariable(floatType, s, floatVal);
                }
                case BaseTypes.baseTypeDouble:
                {
                  double doubleVal = variableAccessor[i].getDouble(varDie, offset);
                  if (variableAccessor[i].isSuccessful() == false)
                    continue;
                  return DoubleType.newDoubleVariable(doubleType, s, doubleVal);
                }                 
                default:
                }
            }
          subrange = subrange.getSibling();
        }
    }
    catch (Errno ignore)
    {
    }
    return null;
  }
  
  public Variable getVariable (DwarfDie varDie)
  {
      if (varDie == null)
      return (null);

    DwarfDie type = varDie.getType();
    if (type == null)
      return null;
    switch (type.getBaseType())
    {
    case BaseTypes.baseTypeLong:
      return LongType.newLongVariable(longType, varDie.getName(), 0);
    case BaseTypes.baseTypeInteger:
      return IntegerType.newIntegerVariable(intType, varDie.getName(), 0);
    case BaseTypes.baseTypeShort:
      return ShortType.newShortVariable(shortType, varDie.getName(), (short)0);
    case BaseTypes.baseTypeChar:
      return ByteType.newByteVariable(byteType, varDie.getName(), (byte)0);
    case BaseTypes.baseTypeFloat:
      return FloatType.newFloatVariable(floatType, varDie.getName(), 0);
    case BaseTypes.baseTypeDouble:
      return DoubleType.newDoubleVariable(doubleType, varDie.getName(), 0); 
    }
    return null;
  }

  
  /**
   * Get the current stack frame.
   * 
   * @return StackFrame
   */
  StackFrame getCurrentFrame ()
  {
    return currentFrame;
  }

  /**
   * Set the current stack frame.
   * 
   * @param sf_p
   */
  void setCurrentFrame (StackFrame sf_p)
  {
    currentFrame = sf_p;
  }

  /**
   * Get the most recent stack frame.
   * 
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
