// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, Red Hat Inc.
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
import lib.dw.DwAtEncodings;
import lib.dw.DwTagEncodings;
import frysk.expr.CppSymTab;
import frysk.value.ArithmeticType;
import frysk.value.ArrayType;
import frysk.value.ClassType;
import frysk.value.EnumType;
import frysk.value.Variable;
import frysk.proc.Isa;
import frysk.proc.Task;
import frysk.rt.LexicalBlock;
import frysk.rt.StackFactory;
import frysk.rt.Frame;
import frysk.rt.Subprogram;
import frysk.sys.Errno;

class ExprSymTab
    implements CppSymTab
{
  private Task task;

  private int pid;

  private Frame currentFrame;
  
  private Subprogram subprogram;

  private ByteBuffer buffer;

  private ArithmeticType byteType;
//  private ArithmeticType byteUnsignedType;
  private ArithmeticType shortType;
//  private ArithmeticType shortUnsignedType;
  private ArithmeticType intType;
//  private ArithmeticType intUnsignedType;
  private ArithmeticType longType;
//  private ArithmeticType longUnsignedType;
  private ArithmeticType floatType;
  private ArithmeticType doubleType;
  
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
  ExprSymTab (Task task, int pid, Frame frame)
  {
    this.task = task;
    this.pid = pid;
    buffer = task.getMemory ();
    ByteOrder byteorder = task.getIsa().getByteOrder();

    if (frame == null)
      {
        currentFrame = StackFactory.createFrame(task);
      }

    else
      {
        while (frame.getInner() != null)
          frame = frame.getInner();

        /* currentFrame is now the innermost StackFrame */
        currentFrame = frame;
      }

    byteType = new ArithmeticType(1, byteorder, BaseTypes.baseTypeByte, "byte");
//    byteUnsignedType = new ArithmeticType(1, byteorder, BaseTypes.baseTypeUnsignedByte, "unsigned byte");
    shortType = new ArithmeticType(2, byteorder, BaseTypes.baseTypeShort, "short");
//    shortUnsignedType = new ArithmeticType(2, byteorder, BaseTypes.baseTypeUnsignedShort, "unsigned short");
    intType = new ArithmeticType(4, byteorder, BaseTypes.baseTypeInteger, "int");
//    intUnsignedType = new ArithmeticType(4, byteorder, BaseTypes.baseTypeUnsignedInteger, "unsigned int");
    longType = new ArithmeticType(8, byteorder, BaseTypes.baseTypeLong, "long");
//    longUnsignedType = new ArithmeticType(8, byteorder, BaseTypes.baseTypeUnsignedLong, "unsigned long");
    floatType = new ArithmeticType(4, byteorder, BaseTypes.baseTypeFloat, "float");
    doubleType = new ArithmeticType(8, byteorder, BaseTypes.baseTypeDouble, "double");
  }

  /**
   * Refresh the current frame.
   */
    
  void refreshCurrentFrame()
  {
    currentFrame = StackFactory.createFrame(task);
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
            regval = currentFrame.getRegister(x86regnumbers[reg]);
          else if (isa instanceof frysk.proc.IsaX8664)
            regval = currentFrame.getRegister(reg);
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

    private long getRegister (DwarfDie varDieP)
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
      long val = currentFrame.getRegister(getRegister(varDieP));
      return val;
    }

    public void putLong (DwarfDie varDieP, long offset, Variable v)
    {
      long reg = getRegister(varDieP);
      currentFrame.setRegister(reg, v.getLong());
    }

    public int getInt (DwarfDie varDieP, long offset)
    {
      long val = currentFrame.getRegister(getRegister(varDieP));
      return (int) val;
    }

    public void putInt (DwarfDie varDieP, long offset, Variable v)
    {
      long reg = getRegister(varDieP);
      currentFrame.setRegister(reg, v.getInt());
    }

    public short getShort (DwarfDie varDieP, long offset)
    {
      long val = currentFrame.getRegister(getRegister(varDieP));
      return (short) val;
    }

    public void putShort (DwarfDie varDieP, long offset, Variable v)
    {
      long reg = getRegister(varDieP);
      currentFrame.setRegister(reg, v.getShort());
    }

    public byte getByte (DwarfDie varDieP, long offset)
    {
      long val = currentFrame.getRegister(getRegister(varDieP));
      return (byte) val;
    }

    public void putByte (DwarfDie varDieP, long offset, Variable v)
    {
      long reg = getRegister(varDieP);
      currentFrame.setRegister(reg, v.getByte());
    }

    public float getFloat (DwarfDie varDieP, long offset)
    {
      long val = currentFrame.getRegister(getRegister(varDieP));
      float fval = Float.intBitsToFloat((int)val);
      return fval;
    }

    public void putFloat (DwarfDie varDieP, long offset, Variable v)
    {
      long reg = getRegister(varDieP);
      currentFrame.setRegister(reg, (long) v.getFloat());
    }

    public double getDouble (DwarfDie varDieP, long offset)
    {
      long val = currentFrame.getRegister(getRegister(varDieP));
      double dval = Double.longBitsToDouble(val);
      return dval;
    }

    public void putDouble (DwarfDie varDieP, long offset, Variable v)
    {
      long reg = getRegister(varDieP);
      currentFrame.setRegister(reg, (long) v.getDouble());
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
    // Do we have something above didn't find, e.g. DW_TAG_enumerator?
    if (varDie == null)
      varDie = DwarfDie.getDeclCU(allDies, s);
    if (varDie == null)
      return null;
    return varDie;
  }

  private ArrayType getArrayType (DwarfDie type, DwarfDie subrange)
    {
      int elementCount = 1;
      ArrayList dims = new ArrayList();
      while (subrange != null)
	{
	  int arrDim = subrange.getAttrConstant(DwAtEncodings.DW_AT_upper_bound_);
	  dims.add(new Integer(arrDim));
	  subrange = subrange.getSibling();
	  elementCount *= arrDim + 1;
	}

      ArrayType arrayType = null;
      int typeSize = BaseTypes.getTypeSize(type.getType().getBaseType());
      switch (type.getType().getBaseType())
      {
      case BaseTypes.baseTypeByte:
      case BaseTypes.baseTypeUnsignedByte:
	arrayType = new ArrayType(byteType, elementCount * typeSize, dims);
	break;
      case BaseTypes.baseTypeShort:
      case BaseTypes.baseTypeUnsignedShort:
	arrayType = new ArrayType(shortType, elementCount * typeSize, dims);
	break;
      case BaseTypes.baseTypeInteger:
      case BaseTypes.baseTypeUnsignedInteger:
	arrayType = new ArrayType(intType, elementCount * typeSize, dims);
	break;
      case BaseTypes.baseTypeLong:
      case BaseTypes.baseTypeUnsignedLong:
	arrayType = new ArrayType(longType, elementCount * typeSize, dims);
	break;
      case BaseTypes.baseTypeFloat:
	arrayType = new ArrayType(floatType, elementCount * typeSize, dims);
	break;
      case BaseTypes.baseTypeDouble:
	arrayType = new ArrayType(doubleType, elementCount * typeSize, dims);
	break;
      }

      type = navigateType(type.getType());
      if (type.getTag() == DwTagEncodings.DW_TAG_structure_type_)
	{
	  ClassType classType = getClassType(type.getChild());
	  typeSize = classType.getSize();
	  arrayType = new ArrayType(classType, elementCount * typeSize, dims);
	}
      return arrayType;
    }
  
  
  private ClassType getClassType (DwarfDie subrange)
  {
    int typeSize = 0;
    ClassType classType = new ClassType(task.getIsa().getByteOrder());
    while (subrange != null)
      {
	long offset = subrange.getDataMemberLocation();
	typeSize = (int)offset + BaseTypes.getTypeSize(subrange.getType().getBaseType());
	switch (subrange.getType().getBaseType())
	{
	case BaseTypes.baseTypeByte:
	case BaseTypes.baseTypeUnsignedByte:
	  classType.addMember(byteType, subrange.getName(), offset, 0);
	  break;
	case BaseTypes.baseTypeShort:
	case BaseTypes.baseTypeUnsignedShort:
	  classType.addMember(shortType, subrange.getName(), offset, 0);
	  break;
	case BaseTypes.baseTypeInteger:
	case BaseTypes.baseTypeUnsignedInteger:
	  // System V ABI Supplements discuss bit field layout 
	  int bitSize = subrange.getAttrConstant(DwAtEncodings.DW_AT_bit_size_);
	  int bitOffset = 0;
	  int byteSize = 0;
	  int mask = 0;
	  if (bitSize != -1)
	    {
	      byteSize = subrange.getAttrConstant(DwAtEncodings.DW_AT_byte_size_);
	      bitOffset = subrange.getAttrConstant(DwAtEncodings.DW_AT_bit_offset_);
	      mask = (0xffffffff >>> (byteSize * 8 - bitSize) << (4 * 8 - bitOffset - bitSize));
	    }
	  classType.addMember(intType, subrange.getName(), offset, mask);
	  break;
	case BaseTypes.baseTypeLong:
	case BaseTypes.baseTypeUnsignedLong:
	  classType.addMember(longType, subrange.getName(), offset, 0);
	  break;
	case BaseTypes.baseTypeFloat:
	  classType.addMember(floatType, subrange.getName(), offset, 0);
	  break;
	case BaseTypes.baseTypeDouble:
	  classType.addMember(doubleType, subrange.getName(), offset, 0);
	  break;
	}
	
	DwarfDie classMember = navigateType(subrange.getType());
	if (classMember.getTag() == DwTagEncodings.DW_TAG_structure_type_)
	  {
	    ClassType memberClassType = getClassType(classMember.getChild());
	    typeSize += memberClassType.getSize();
	    typeSize += 4 - (typeSize % 4);		// round up to mod 4
	    classType.addMember(memberClassType, subrange.getName(), offset, 0);
	  }
	
	if (classMember.getTag() == DwTagEncodings.DW_TAG_array_type_)
	{
	  ArrayType memberArrayType = getArrayType(classMember, classMember.getChild());
	  typeSize += memberArrayType.getSize();
	  classType.addMember(memberArrayType, subrange.getName(), offset, 0);
	}
	subrange = subrange.getSibling();
      }

    typeSize += 4 - (typeSize % 4);		// round up to mod 4
    classType.setSize(typeSize);
    return classType;
  }
  
  private DwarfDie navigateType (DwarfDie type)
  {
    while (type.getTag() == DwTagEncodings.DW_TAG_typedef_)
      type = type.getType();
    return type;
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
            switch (type.getBaseType())
            {
              case BaseTypes.baseTypeLong:
              case BaseTypes.baseTypeUnsignedLong:
        	variableAccessor[i].putLong(varDie, 0, v);
        	break;
              case BaseTypes.baseTypeInteger:
              case BaseTypes.baseTypeUnsignedInteger:
                variableAccessor[i].putInt(varDie, 0, v);
                break;
              case BaseTypes.baseTypeShort:
              case BaseTypes.baseTypeUnsignedShort:
                variableAccessor[i].putShort(varDie, 0, v);
                break;
              case BaseTypes.baseTypeByte:
              case BaseTypes.baseTypeUnsignedByte:
                variableAccessor[i].putByte(varDie, 0, v);
                break;
              case BaseTypes.baseTypeFloat:
                variableAccessor[i].putFloat(varDie, 0, v);
                break;
              case BaseTypes.baseTypeDouble:
                variableAccessor[i].putDouble(varDie, 0, v);
                break;
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
            // if there is no type then setup a sentinel
            int baseType = type != null ? type.getBaseType() : 0;
            switch (baseType)
              {
              case BaseTypes.baseTypeLong:
              case BaseTypes.baseTypeUnsignedLong:
              {
                long longVal = variableAccessor[i].getLong(varDie, 0);
                if (variableAccessor[i].isSuccessful() == false)
                  continue;
                return ArithmeticType.newLongVariable(longType, s, longVal);
              }
              case BaseTypes.baseTypeInteger:
              case BaseTypes.baseTypeUnsignedInteger:
              {
                int intVal = variableAccessor[i].getInt(varDie, 0);
                if (variableAccessor[i].isSuccessful() == false)
                  continue;
                return ArithmeticType.newIntegerVariable(intType, s, intVal);
              }
              case BaseTypes.baseTypeShort:
              case BaseTypes.baseTypeUnsignedShort:
              {
                short shortVal = variableAccessor[i].getShort(varDie, 0);
                if (variableAccessor[i].isSuccessful() == false)
                  continue;
                return ArithmeticType.newShortVariable(shortType, s, shortVal);
              }
              case BaseTypes.baseTypeByte:
              case BaseTypes.baseTypeUnsignedByte:
              {
                byte byteVal = variableAccessor[i].getByte(varDie, 0);
                if (variableAccessor[i].isSuccessful() == false)
                  continue;
                return ArithmeticType.newByteVariable(byteType, s, byteVal);
              }
              case BaseTypes.baseTypeFloat:
              {
                float floatVal = variableAccessor[i].getFloat(varDie, 0);
                if (variableAccessor[i].isSuccessful() == false)
                  continue;
                return ArithmeticType.newFloatVariable(floatType, s, floatVal);
              }
              case BaseTypes.baseTypeDouble:
              {
                double doubleVal = variableAccessor[i].getDouble(varDie, 0);
                if (variableAccessor[i].isSuccessful() == false)
                  continue;
                return ArithmeticType.newDoubleVariable(doubleType, s, doubleVal);
              }
            }
            // if there is no type then use this die's tag
            int tag = type != null ? type.getTag() : varDie.getTag();
            if (tag == DwTagEncodings.DW_TAG_array_type_)
              {
                DwarfDie subrange;
                long addr = variableAccessor[0].getAddr(s);
                if (addr == 0)
                  continue;
                subrange = type.getChild();
                ArrayType arrayType = getArrayType(type, subrange);
                
                if (arrayType == null)
                  return null;
                int typeSize = arrayType.getSize();
                byte [] buf = new byte[typeSize];
                buffer.get(addr, buf, 0, typeSize);
                ArrayByteBuffer abb = new ArrayByteBuffer(buf, 0, typeSize);
                abb.order(byteorder);
                return new Variable(arrayType, s, abb);
              }
            else if (tag == DwTagEncodings.DW_TAG_structure_type_)
              {
                DwarfDie subrange;
                long addr = variableAccessor[0].getAddr(s);
                if (addr == 0)
                  continue;
                subrange = type.getChild();
                ClassType classType = getClassType(subrange);
                
                byte [] buf = new byte[classType.getSize()];
                for (int j = 0; j < classType.getSize(); j++)
                  buffer.get(addr + j, buf, j, 1);
                ArrayByteBuffer abb = new ArrayByteBuffer(buf, 0, buf.length);
                abb.order(byteorder);
                return new Variable(classType, s, abb);
              }
            else if (tag == DwTagEncodings.DW_TAG_pointer_type_)
              {
        	long addr = variableAccessor[i].getAddr(s);
        	return ArithmeticType.newLongVariable(longType, s, addr);
              }
            else if (tag == DwTagEncodings.DW_TAG_enumeration_type_)
              {
                DwarfDie subrange;
                long addr = variableAccessor[0].getAddr(s);
                if (addr == 0)
                  continue;
                subrange = type.getChild();
                EnumType enumType = new EnumType(byteorder);
                while (subrange != null)
                  {
                    enumType.addMember(byteType, subrange.getName(), 
                                       subrange.getAttrConstant(DwAtEncodings.DW_AT_const_value_));
                    subrange = subrange.getSibling();
                  }
                return EnumType.newEnumVariable(enumType, s);
              }
            // special case members of an enumeration
            else if (tag == DwTagEncodings.DW_TAG_enumerator_)
              {
        	return ArithmeticType.newLongVariable(longType, "", varDie.getAttrConstant(DwAtEncodings.DW_AT_const_value_));
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
               int arrDim = subrange.getAttrConstant(DwAtEncodings.DW_AT_upper_bound_);
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
             case BaseTypes.baseTypeByte:
             case BaseTypes.baseTypeUnsignedByte:
               byte byteVal = variableAccessor[i].getByte(varDie, offset);
               if (variableAccessor[i].isSuccessful() == false)
                 continue;
               return ArithmeticType.newByteVariable(byteType, s, byteVal);
             case BaseTypes.baseTypeShort:
             case BaseTypes.baseTypeUnsignedShort:
               short shortVal = variableAccessor[i].getShort(varDie, offset);
               if (variableAccessor[i].isSuccessful() == false)
                 continue;
               return ArithmeticType.newShortVariable(shortType, s, shortVal);
             case BaseTypes.baseTypeInteger:
             case BaseTypes.baseTypeUnsignedInteger:
               int intVal = variableAccessor[i].getInt(varDie, offset);
               if (variableAccessor[i].isSuccessful() == false)
                 continue;
               return ArithmeticType.newIntegerVariable(intType, s, intVal);
             case BaseTypes.baseTypeLong:
             case BaseTypes.baseTypeUnsignedLong:
               long longVal = variableAccessor[i].getLong(varDie, offset);
               if (variableAccessor[i].isSuccessful() == false)
                 continue;
               return ArithmeticType.newLongVariable(longType, s, longVal);
             case BaseTypes.baseTypeFloat:
               float floatVal = variableAccessor[i].getFloat(varDie, offset);
               if (variableAccessor[i].isSuccessful() == false)
                 continue;
               return ArithmeticType.newFloatVariable(floatType, s, floatVal);
             case BaseTypes.baseTypeDouble:
               double doubleVal = variableAccessor[i].getDouble(varDie, offset);
               if (variableAccessor[i].isSuccessful() == false)
                 continue;
               return ArithmeticType.newDoubleVariable(doubleType, s, doubleVal);
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
    String s = (String)components.get(0);
    DwarfDie varDie = getDie(s);
    if (varDie == null)
      return (null);

    Variable v = get(s);
    return ((ClassType)v.getType()).get(v, components);
  }
  
  public Variable getAddress (String s)
  {
    AccessDW_FORM_block access = new AccessDW_FORM_block();
    return ArithmeticType.newLongVariable(longType, s, access.getAddr(s)); 
  }
  
  public Variable getMemory (String s)
  {
    DwarfDie varDie = getDie(s);
    
    if (varDie == null)
      return (null);
    
    DwarfDie type = varDie.getType();
    AccessDW_FORM_block access = new AccessDW_FORM_block();
    long addr = access.getAddr(s); 
    long addrIndirect = buffer.getLong(addr);
    
    switch (type.getType().getBaseType())
      {
      case BaseTypes.baseTypeByte:
      case BaseTypes.baseTypeUnsignedByte:
	return ArithmeticType.newByteVariable(byteType, s, buffer.getByte(addrIndirect));
      case BaseTypes.baseTypeShort:
      case BaseTypes.baseTypeUnsignedShort:
	return ArithmeticType.newShortVariable(shortType, s, buffer.getShort(addrIndirect));
      case BaseTypes.baseTypeInteger:
      case BaseTypes.baseTypeUnsignedInteger:
	return ArithmeticType.newIntegerVariable(intType, s, buffer.getInt(addrIndirect));
      case BaseTypes.baseTypeLong:
      case BaseTypes.baseTypeUnsignedLong:
	return ArithmeticType.newLongVariable(longType, s, buffer.getLong(addrIndirect));
      case BaseTypes.baseTypeFloat:
	return ArithmeticType.newFloatVariable(floatType, s, buffer.getFloat(addrIndirect));
      case BaseTypes.baseTypeDouble:
	return ArithmeticType.newDoubleVariable(doubleType, s, buffer.getDouble(addrIndirect));
      default:
        return null;
      }
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
      return ArithmeticType.newLongVariable(longType, varDie.getName(), 0);
    case BaseTypes.baseTypeInteger:
      return ArithmeticType.newIntegerVariable(intType, varDie.getName(), 0);
    case BaseTypes.baseTypeShort:
      return ArithmeticType.newShortVariable(shortType, varDie.getName(), (short)0);
    case BaseTypes.baseTypeByte:
      return ArithmeticType.newByteVariable(byteType, varDie.getName(), (byte)0);
    case BaseTypes.baseTypeFloat:
      return ArithmeticType.newFloatVariable(floatType, varDie.getName(), 0);
    case BaseTypes.baseTypeDouble:
      return ArithmeticType.newDoubleVariable(doubleType, varDie.getName(), 0); 
    }
    return null;
  }

  
  /**
   * Get the current stack frame.
   * 
   * @return StackFrame
   */
  Frame getCurrentFrame ()
  {
    return currentFrame;
  }

  /**
   * Set the current stack frame.
   * 
   * @param sf_p
   */
  void setCurrentFrame (Frame sf_p)
  {
    currentFrame = sf_p;
  }

  /**
   * Get the most recent stack frame.
   * 
   * @return StackFrame
   */
  Frame getInnerMostFrame ()
  {
    Frame curr = currentFrame;

    while (curr.getInner() != null)
      curr = curr.getInner();

    return curr;
  }
}
