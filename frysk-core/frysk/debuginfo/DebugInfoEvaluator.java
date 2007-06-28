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


package frysk.debuginfo;

import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.naming.NameNotFoundException;

import lib.dw.BaseTypes;
import lib.dw.DwAtEncodings;
import lib.dw.DwException;
import lib.dw.DwOpEncodings;
import lib.dw.DwTagEncodings;
import lib.dw.DwarfDie;
import lib.dw.Dwfl;
import lib.dw.DwflDieBias;
import frysk.dwfl.DwflFactory;
import frysk.expr.CppSymTab;
import frysk.proc.Isa;
import frysk.proc.Task;
import frysk.rt.Subprogram;
import frysk.rt.Variable;
import frysk.stack.Frame;
import frysk.stack.StackFactory;
import frysk.sys.Errno;
import frysk.value.ArithmeticType;
import frysk.value.ArrayType;
import frysk.value.ClassType;
import frysk.value.EnumType;
import frysk.value.FunctionType;
import frysk.value.PointerType;
import frysk.value.Type;
import frysk.value.Value;

class DebugInfoEvaluator
    implements CppSymTab
{
  private Task task;

  private Frame currentFrame;
  
  // private Subprogram subprogram;

  private ByteBuffer buffer;

  private ArithmeticType byteType;
// private ArithmeticType byteUnsignedType;
  private ArithmeticType shortType;
// private ArithmeticType shortUnsignedType;
  private ArithmeticType intType;
// private ArithmeticType intUnsignedType;
  private ArithmeticType longType;
// private ArithmeticType longUnsignedType;
  private ArithmeticType floatType;
  private ArithmeticType doubleType;
  
  public void setSubprogram (Subprogram subprogram)
  {
    // this.subprogram = subprogram;
  }

  public boolean putUndefined ()
  {
    return false;
  }

  /**
   * Create an DebugInfoEvaluator object which is the interface between
   * DebugInfo and CppTreeParser, the expression parser.
   * 
   * @param frame StackFrame
   * @param task_p Task
   * @param pid_p Pid
   */
  DebugInfoEvaluator (Frame frame)
  {
    this.task = frame.getTask();
    buffer = this.task.getMemory();

    ByteOrder byteorder = this.task.getIsa().getByteOrder();
    buffer.order(byteorder);

    while (frame.getInner() != null)
      frame = frame.getInner();

    /* currentFrame is now the innermost StackFrame */
    currentFrame = frame;

    byteType = new ArithmeticType(1, byteorder, BaseTypes.baseTypeByte, "byte");
// byteUnsignedType = new ArithmeticType(1, byteorder,
// BaseTypes.baseTypeUnsignedByte, "unsigned byte");
    shortType = new ArithmeticType(2, byteorder, BaseTypes.baseTypeShort, "short");
// shortUnsignedType = new ArithmeticType(2, byteorder,
// BaseTypes.baseTypeUnsignedShort, "unsigned short");
    intType = new ArithmeticType(4, byteorder, BaseTypes.baseTypeInteger, "int");
// intUnsignedType = new ArithmeticType(4, byteorder,
// BaseTypes.baseTypeUnsignedInteger, "unsigned int");
    longType = new ArithmeticType(8, byteorder, BaseTypes.baseTypeLong, "long");
// longUnsignedType = new ArithmeticType(8, byteorder,
// BaseTypes.baseTypeUnsignedLong, "unsigned long");
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
  
  void refreshCurrentFrame(Frame scope)
  {
    currentFrame = scope;
  }

  // ??? Give registers a real type and let the type system do the swap.
  private long swapBytes(long val)
    {
	long newVal = 0;
	Isa isa = currentFrame.getTask().getIsa();

	if (isa.getByteOrder() == ByteOrder.BIG_ENDIAN)
	    return val;
	else if (isa.getWordSize() == 4)
	    {
		int ival = (int)val;
		for (int i = 0; i < 4; i++)
		    {
			newVal = newVal | ((ival & 0xff000000) >>> (24 - (i * 8)));
			ival = ival << 8;
		    }
		newVal = newVal & 0x00000000ffffffffL;
	    }
	else if (isa.getWordSize() == 8)
	    for (int i = 0; i < 8; i++)
		{
		    newVal = newVal | ((val & 0xff00000000000000L) >>> (56 - (i * 8)));
		    val = val << 8;
		}
	return newVal;
    }

  interface VariableAccessor
  {
    DwarfDie varDie = null;

    long getAddr (String s) throws NameNotFoundException;

    long getLong (DwarfDie varDieP, long offset) throws NameNotFoundException;

    void putLong (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException;

    int getInt (DwarfDie varDieP, long offset) throws NameNotFoundException;

    void putInt (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException;

    short getShort (DwarfDie varDieP, long offset) throws NameNotFoundException;

    void putShort (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException;

    byte getByte (DwarfDie varDieP, long offset) throws NameNotFoundException;

    void putByte (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException;

    float getFloat (DwarfDie varDieP, long offset) throws NameNotFoundException;

    void putFloat (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException;

    double getDouble (DwarfDie varDieP, long offset) throws NameNotFoundException;

    void putDouble (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException;
  }

  /**
   * Access by DW_FORM_block. Typically this is a static address or ptr+disp.
   */
  class AccessMemory
      implements VariableAccessor
  {
    /**
     * Given a variable's Die return its address.
     * 
     * @param varDieP
     * @return
     */
    protected long getBufferAddr (DwarfDie varDieP) throws NameNotFoundException
    {
      long pc;
      // ??? Do we need an isa specific way to get x86 reg numbers?
      int[] x86regnumbers = { 0, 2, 1, 3, 7, 6, 4, 5 };

      pc = currentFrame.getAdjustedAddress();

      List ops = varDieP.getAddr();      
      if (ops.size() == 0 ||
          ((DwarfDie.DwarfOp)ops.get(0)).operator == -1)
	  throw new NameNotFoundException();
      if (((DwarfDie.DwarfOp)ops.get(0)).operator == DwOpEncodings.DW_OP_addr_)
        {
          return ((DwarfDie.DwarfOp)ops.get(0)).operand1;
        }
      long addr = ((DwarfDie.DwarfOp)ops.get(0)).operand1;
      
      ops = varDieP.getFrameBase(pc);
      if (ops.size() == 0 ||
          ((DwarfDie.DwarfOp)ops.get(0)).operator == -1)
	  throw new NameNotFoundException();
      int reg = ((DwarfDie.DwarfOp)ops.get(0)).operator;
      if (reg >= DwOpEncodings.DW_OP_reg0_ && reg <= DwOpEncodings.DW_OP_reg31_)
	  reg = reg - DwOpEncodings.DW_OP_reg0_;
      else if (reg >= DwOpEncodings.DW_OP_breg0_ && reg <= DwOpEncodings.DW_OP_breg31_)
	  reg = reg - DwOpEncodings.DW_OP_breg0_;
      long regval = 0;
      
      // DW_OP_fbreg
      Isa isa = currentFrame.getTask().getIsa();
      if (isa instanceof frysk.proc.IsaIA32)
        regval = swapBytes(currentFrame.getReg(x86regnumbers[reg]));
      else if (isa instanceof frysk.proc.IsaX8664)
        regval = swapBytes(currentFrame.getReg(reg));

      addr += ((DwarfDie.DwarfOp)ops.get(0)).operand1;
      addr += regval;
      return addr;
    }

    public long getAddr (String s) throws NameNotFoundException
    {
      DwarfDie die = getDie(s);
      return getBufferAddr(die);
    }

    public long getLong (DwarfDie varDieP, long offset) throws NameNotFoundException
    {
      long addr = getBufferAddr(varDieP);
      return buffer.getLong(addr + offset);
    }

    public void putLong (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException
    {
      long addr = getBufferAddr(varDieP);
      buffer.putLong(addr + offset, v.getLong());
    }

    public int getInt (DwarfDie varDieP, long offset) throws NameNotFoundException
    {
      long addr = getBufferAddr(varDieP);
      return buffer.getInt(addr + offset);
    }

    public void putInt (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException
    {
      long addr = getBufferAddr(varDieP);
      buffer.putInt(addr + offset, v.getInt());
    }

    public short getShort (DwarfDie varDieP, long offset) throws NameNotFoundException
    {
      long addr = getBufferAddr(varDieP);
      return buffer.getShort(addr + offset);
    }

    public void putShort (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException
    {
      long addr = getBufferAddr(varDieP);
      buffer.putShort(addr + offset, v.getShort());
    }

    public byte getByte (DwarfDie varDieP, long offset) throws NameNotFoundException
    {
      long addr = getBufferAddr(varDieP);
      return buffer.getByte(addr + offset);
    }

    public void putByte (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException
    {
      long addr = getBufferAddr(varDieP);
      buffer.putByte(addr + offset, v.getByte());
    }

    public float getFloat (DwarfDie varDieP, long offset) throws NameNotFoundException
    {
      long addr = getBufferAddr(varDieP);
      return buffer.getFloat(addr + offset);
    }

    public void putFloat (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException
    {
      long addr = getBufferAddr(varDieP);
      buffer.putFloat(addr + offset, v.getFloat());
    }

    public double getDouble (DwarfDie varDieP, long offset) throws NameNotFoundException
    {
      long addr = getBufferAddr(varDieP);
      return buffer.getDouble(addr + offset);
    }

    public void putDouble (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException
    {
      long addr = getBufferAddr(varDieP);
      buffer.putDouble(addr + offset, v.getDouble());
    }
  }

  /**
   * Access by DW_FORM_data. Typically this is a location list.
   */
  class AccessRegisters
      implements VariableAccessor
  {
    public long getAddr (String s) throws NameNotFoundException
    {
      return 0;
    }

    private long getReg (DwarfDie varDieP) throws NameNotFoundException
    {
      long pc;
      int[] x86regnumbers = { 0, 2, 1, 3, 7, 6, 4, 5 };
      Isa isa;

      if (currentFrame.getInner() == null)
        isa = task.getIsa();
      else
        isa = currentFrame.getTask().getIsa();

      pc = currentFrame.getAdjustedAddress();
      List ops = varDieP.getFormData(pc);
      int op = -1;

      if (ops.size() != 0)
	  op = ((DwarfDie.DwarfOp)ops.get(0)).operator;
	      
      if (op == -1 
	  || (op < DwOpEncodings.DW_OP_reg0_ || op > DwOpEncodings.DW_OP_breg31_))
	  throw new NameNotFoundException();

      int reg = 0;
      if (op >= DwOpEncodings.DW_OP_reg0_ && op <= DwOpEncodings.DW_OP_reg31_)
	  reg = op - DwOpEncodings.DW_OP_reg0_;
      else if (reg >= DwOpEncodings.DW_OP_breg0_ && reg <= DwOpEncodings.DW_OP_breg31_)
	  reg = op - DwOpEncodings.DW_OP_breg0_;
      else
	  throw new NameNotFoundException();
      if (isa instanceof frysk.proc.IsaIA32)
          reg = x86regnumbers[(int)reg];
      
      return reg;
    }

    public long getLong (DwarfDie varDieP, long offset) throws NameNotFoundException
    {
      long val = swapBytes(currentFrame.getReg(getReg(varDieP)));
      return val;
    }

    public void putLong (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException
    {
      long reg = getReg(varDieP);
      currentFrame.setReg(reg, v.getLong());
    }

    public int getInt (DwarfDie varDieP, long offset) throws NameNotFoundException
    {
      long val = swapBytes(currentFrame.getReg(getReg(varDieP)));
      return (int) val;
    }

    public void putInt (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException
    {
      long reg = getReg(varDieP);
      currentFrame.setReg(reg, v.getInt());
    }

    public short getShort (DwarfDie varDieP, long offset) throws NameNotFoundException
    {
      long val = swapBytes(currentFrame.getReg(getReg(varDieP)));
      return (short) val;
    }

    public void putShort (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException
    {
      long reg = getReg(varDieP);
      currentFrame.setReg(reg, v.getShort());
    }

    public byte getByte (DwarfDie varDieP, long offset) throws NameNotFoundException
    {
      long val = swapBytes(currentFrame.getReg(getReg(varDieP)));
      return (byte) val;
    }

    public void putByte (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException
    {
      long reg = getReg(varDieP);
      currentFrame.setReg(reg, v.getByte());
    }

    public float getFloat (DwarfDie varDieP, long offset) throws NameNotFoundException
    {
      long val = swapBytes(currentFrame.getReg(getReg(varDieP)));
      float fval = Float.intBitsToFloat((int)val);
      return fval;
    }

    public void putFloat (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException
    {
      long reg = getReg(varDieP);
      currentFrame.setReg(reg, (long) v.getFloat());
    }

    public double getDouble (DwarfDie varDieP, long offset) throws NameNotFoundException
    {
      long val = swapBytes(currentFrame.getReg(getReg(varDieP)));
      double dval = Double.longBitsToDouble(val);
      return dval;
    }

    public void putDouble (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException
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
  private DwarfDie getDie (String s) throws NameNotFoundException
  {
    Dwfl dwfl;
    DwarfDie[] allDies;
    DwarfDie varDie;
    long pc = this.currentFrame.getAdjustedAddress();

    dwfl = DwflFactory.createDwfl(task);
    DwflDieBias bias = dwfl.getDie(pc);
    if (bias == null)
      return null;
    DwarfDie die = bias.die;

     Subprogram b = currentFrame.getSubprogram();
     LinkedList vars = b.getVariables();
     
     Iterator iterator = vars.iterator();
     while (iterator.hasNext()) {
	Variable variable = (Variable) iterator.next();
	if (variable.getVariable() != null && variable.getVariable().getText().compareTo(s) == 0)
	{
	    allDies = die.getScopes(pc - bias.bias);
	    variable.getVariableDie().setScopes(allDies);
	    return variable.getVariableDie();
	}
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
      int typeSize = BaseTypes.getTypeSize(type.getUltimateType().getBaseType());
      switch (type.getUltimateType().getBaseType())
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

      type = type.getUltimateType();
      if (type.getTag() == DwTagEncodings.DW_TAG_structure_type_)
	{
	  ClassType classType = getClassType(type, null);
	  typeSize = classType.getSize();
	  arrayType = new ArrayType(classType, elementCount * typeSize, dims);
	}
      return arrayType;
    }
  
  private ArithmeticType fetchType (boolean haveTypeDef, ArithmeticType type,
                                    String name)
  {
    if (haveTypeDef == false) 
      return type;
    switch (type.getTypeId())
    {
      case BaseTypes.baseTypeLong:
      case BaseTypes.baseTypeUnsignedLong:
        return new ArithmeticType(longType.getSize(), longType.getEndian(), BaseTypes.baseTypeLong, name, true);
      case BaseTypes.baseTypeInteger:
      case BaseTypes.baseTypeUnsignedInteger:
        return new ArithmeticType(intType.getSize(), intType.getEndian(), BaseTypes.baseTypeInteger, name, true);
      case BaseTypes.baseTypeShort:
      case BaseTypes.baseTypeUnsignedShort:
        return new ArithmeticType(shortType.getSize(), shortType.getEndian(), BaseTypes.baseTypeShort, name, true);
      case BaseTypes.baseTypeByte:
      case BaseTypes.baseTypeUnsignedByte:
        return new ArithmeticType(byteType.getSize(), byteType.getEndian(), BaseTypes.baseTypeByte, name, true);
      case BaseTypes.baseTypeFloat:
        return new ArithmeticType(floatType.getSize(), floatType.getEndian(), BaseTypes.baseTypeFloat, name, true);
      case BaseTypes.baseTypeDouble:
        return new ArithmeticType(doubleType.getSize(), doubleType.getEndian(), BaseTypes.baseTypeDouble, name, true);
      default:
        return null;
    }
  }

  private ClassType getClassType (DwarfDie classDie, String name)
  {
    int typeSize = 0;
    ClassType classType = new ClassType(task.getIsa().getByteOrder(), name);
    for (DwarfDie member = classDie.getChild();
    	 member != null;
    	 member = member.getSibling())
      {
        long offset;
        boolean haveTypeDef;
        try
        {
          offset = member.getDataMemberLocation();
        }
        catch (DwException de)
        {
          offset = 0;                           // union
        }
        
        DwarfDie dieType = member.getType();
        DwarfDie memberType = member.getUltimateType();
        Type type;
        
        if (dieType != memberType)
          haveTypeDef = true;
        else
          haveTypeDef = false;
        
        if (memberType != null)
          typeSize = (int)offset + BaseTypes.getTypeSize(memberType.getBaseType());
        switch (memberType.getBaseType())
        {
        case BaseTypes.baseTypeByte:
        case BaseTypes.baseTypeUnsignedByte:
          classType.addMember(fetchType(haveTypeDef, byteType, dieType.getName()),
                              member.getName(), offset, 0);
          continue;
        case BaseTypes.baseTypeShort:
        case BaseTypes.baseTypeUnsignedShort:
          classType.addMember(fetchType(haveTypeDef, shortType, dieType.getName()),
                              member.getName(), offset, 0);
          continue;
        case BaseTypes.baseTypeInteger:
        case BaseTypes.baseTypeUnsignedInteger:
          type = fetchType(haveTypeDef, intType, dieType.getName());
          // System V ABI Supplements discuss bit field layout
          int bitSize = member.getAttrConstant(DwAtEncodings.DW_AT_bit_size_);
          int bitOffset = 0;
          int byteSize = 0;
          int mask = 0;
          if (bitSize != -1)
            {
              byteSize = member.getAttrConstant(DwAtEncodings.DW_AT_byte_size_);
              bitOffset = member.getAttrConstant(DwAtEncodings.DW_AT_bit_offset_);
              mask = (0xffffffff >>> (byteSize * 8 - bitSize) << (4 * 8 - bitOffset - bitSize));
            }
          classType.addMember(type, member.getName(), offset, mask);
          continue;
        case BaseTypes.baseTypeLong:
        case BaseTypes.baseTypeUnsignedLong:
          classType.addMember(fetchType(haveTypeDef, longType, dieType.getName()),
                              member.getName(), offset, 0);
          continue;
        case BaseTypes.baseTypeFloat:
          classType.addMember(fetchType(haveTypeDef, floatType, dieType.getName()),
                              member.getName(), offset, 0);
          continue;
        case BaseTypes.baseTypeDouble:
          classType.addMember(fetchType(haveTypeDef, doubleType, dieType.getName()),
                              member.getName(), offset, 0);
          continue;
        }
        
	switch (memberType.getTag())
	{
	case DwTagEncodings.DW_TAG_structure_type_:
	{
          ClassType memberClassType = getClassType(memberType, dieType.getName());
          memberClassType.setTypedef(haveTypeDef);
          typeSize += memberClassType.getSize();
          typeSize += 4 - (typeSize % 4);             // round up to mod 4
          classType.addMember(memberClassType, member.getName(), offset, 0);
          continue;
	}
        
	case DwTagEncodings.DW_TAG_inheritance_:
        {
          ClassType memberClassType = getClassType(memberType, dieType.getName());
          typeSize += memberClassType.getSize();
          typeSize += 4 - (typeSize % 4);             // round up to mod 4
          classType.addMember(memberClassType, member.getName(), offset, 0);
          classType.setBaseClass();
          continue;
        }

        case DwTagEncodings.DW_TAG_array_type_:
        {
          ArrayType memberArrayType = getArrayType(memberType, memberType.getChild());
          typeSize += memberArrayType.getSize();
          classType.addMember(memberArrayType, member.getName(), offset, 0);
          continue;
        }
	}
      }

    typeSize += 4 - (typeSize % 4);             // round up to mod 4
    classType.setSize(typeSize);
    return classType;
  }
  
  
  /*
   * (non-Javadoc)
   * 
   * @see frysk.expr.CppSymTab#put(java.lang.String, frysk.lang.Value)
   */
  public void put (String s, Value v) throws NameNotFoundException
  {
    VariableAccessor[] variableAccessor = { new AccessMemory()
    // new AccessDwOpData()
    };
    DwarfDie varDie = getDie(s);
    if (varDie == null)
      return;

    try
      {
        DwarfDie type = varDie.getUltimateType();
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


  public Value get (String s) throws NameNotFoundException
  {
    DwarfDie varDie = getDie(s);
    if (varDie == null)
      return (null);
    
    return get(varDie);
  }
  
  /**
   * Returns the Value associated with the given DwarfDie.
   * @see frysk.expr.CppSymTab#get(java.lang.String)
   */
  public Value get (DwarfDie varDie) throws NameNotFoundException
  {
    VariableAccessor[] variableAccessor = { new AccessMemory(),
                                           new AccessRegisters() };
    ByteOrder byteorder = task.getIsa().getByteOrder();

    if (varDie == null)
      return (null);

    String s = varDie.getName();
    
    for (int i = 0; i < variableAccessor.length; i++)
      {
	try
          {
            DwarfDie type = varDie.getUltimateType();
            // if there is no type then setup a sentinel
            int baseType = type != null ? type.getBaseType() : 0;
            switch (baseType)
            {
            case BaseTypes.baseTypeLong:
            case BaseTypes.baseTypeUnsignedLong:
            {
              long longVal = variableAccessor[i].getLong(varDie, 0);
              return ArithmeticType.newLongValue(longType, s, longVal);
            }
            case BaseTypes.baseTypeInteger:
            case BaseTypes.baseTypeUnsignedInteger:
            {
        	
              int intVal = variableAccessor[i].getInt(varDie, 0);
              return ArithmeticType.newIntegerValue(intType, s, intVal);
            }
            case BaseTypes.baseTypeShort:
            case BaseTypes.baseTypeUnsignedShort:
            {
              short shortVal = variableAccessor[i].getShort(varDie, 0);
              return ArithmeticType.newShortValue(shortType, s, shortVal);
            }
            case BaseTypes.baseTypeByte:
            case BaseTypes.baseTypeUnsignedByte:
            {
              byte byteVal = variableAccessor[i].getByte(varDie, 0);
              return ArithmeticType.newByteValue(byteType, s, byteVal);
            }
            case BaseTypes.baseTypeFloat:
            {
              float floatVal = variableAccessor[i].getFloat(varDie, 0);
              return ArithmeticType.newFloatValue(floatType, s, floatVal);
            }
            case BaseTypes.baseTypeDouble:
            {
              double doubleVal = variableAccessor[i].getDouble(varDie, 0);
              return ArithmeticType.newDoubleValue(doubleType, s, doubleVal);
            }
            }
            // if there is no type then use this die's tag
            int tag = type != null ? type.getTag() : varDie.getTag();
            switch (tag)
            {
            case DwTagEncodings.DW_TAG_array_type_:
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
              ByteBuffer  abb = buffer.slice (addr, typeSize);
              abb.order(byteorder);
              return new Value(arrayType, s, abb);
            }
            case DwTagEncodings.DW_TAG_union_type_:
            case DwTagEncodings.DW_TAG_structure_type_:
            {
              long addr = variableAccessor[0].getAddr(s);
              if (addr == 0)
                continue;
              ClassType classType = getClassType(type, null);

              ByteBuffer  abb = buffer.slice (addr, classType.getSize());
              abb.order(byteorder);
              return new Value(classType, s, abb);
            }
            case DwTagEncodings.DW_TAG_pointer_type_:
            {
              long addr = variableAccessor[i].getAddr(s);
              return ArithmeticType.newLongValue(longType, addr);
            }
            case DwTagEncodings.DW_TAG_enumeration_type_:
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
              return EnumType.newEnumValue(enumType, s);
            }
            // special case members of an enumeration
            case DwTagEncodings.DW_TAG_enumerator_:
            {
              return ArithmeticType.newLongValue(longType, varDie.getAttrConstant(DwAtEncodings.DW_AT_const_value_));
            }
            }
          }
	catch (NameNotFoundException ignore)
	{
	}
        catch (Errno ignore)
          {
          }
      }
    return null;
  }
    
  public Value get (ArrayList components) throws NameNotFoundException
  {
    String s = (String)components.get(0);
    DwarfDie varDie = getDie(s);
    if (varDie == null)
      return (null);

    Value v = get(s);
    if (v.getType() instanceof ArrayType)
      return ((ArrayType)v.getType()).get(v, 1, components);
    else if (v.getType() instanceof ClassType)
      return ((ClassType)v.getType()).get(v, 0, components);
    else
      return null;
  }
  
  public Value getAddress (String s) throws NameNotFoundException
  {
    AccessMemory access = new AccessMemory();
    return ArithmeticType.newLongValue(longType, access.getAddr(s)); 
  }
  
  public Value getMemory (String s) throws NameNotFoundException
  {     
    ByteOrder byteorder = task.getIsa().getByteOrder();
    DwarfDie varDie = getDie(s);
    
    if (varDie == null)
      return (null);
    
    DwarfDie type = varDie.getUltimateType();
    AccessMemory access = new AccessMemory();
    long addr = access.getAddr(s); 
    long addrIndirect = buffer.getLong(addr);
    
    switch (type.getUltimateType().getBaseType())
      {
      case BaseTypes.baseTypeByte:
      case BaseTypes.baseTypeUnsignedByte:
        return ArithmeticType.newByteValue(byteType, buffer.getByte(addrIndirect));
      case BaseTypes.baseTypeShort:
      case BaseTypes.baseTypeUnsignedShort:
        return ArithmeticType.newShortValue(shortType, buffer.getShort(addrIndirect));
      case BaseTypes.baseTypeInteger:
      case BaseTypes.baseTypeUnsignedInteger:
        return ArithmeticType.newIntegerValue(intType, buffer.getInt(addrIndirect));
      case BaseTypes.baseTypeLong:
      case BaseTypes.baseTypeUnsignedLong:
        return ArithmeticType.newLongValue(longType, buffer.getLong(addrIndirect));
      case BaseTypes.baseTypeFloat:
        return ArithmeticType.newFloatValue(floatType, buffer.getFloat(addrIndirect));
      case BaseTypes.baseTypeDouble:
        return ArithmeticType.newDoubleValue(doubleType, buffer.getDouble(addrIndirect));
      }
    int tag = type != null ? type.getTag() : varDie.getTag();
    switch (tag)
    {
    case DwTagEncodings.DW_TAG_array_type_:
    {
      DwarfDie subrange;
      subrange = type.getChild();
      ArrayType arrayType = getArrayType(type, subrange);

      if (arrayType == null)
        return null;
      int typeSize = arrayType.getSize();
      ByteBuffer  abb = buffer.slice (addrIndirect, typeSize);
      abb.order(byteorder);
      return new Value(arrayType, s, abb);
    }
    case DwTagEncodings.DW_TAG_union_type_:
    case DwTagEncodings.DW_TAG_structure_type_:
    {
      ClassType classType = getClassType(type, null);

      if (classType == null)
        return null;
      ByteBuffer  abb = buffer.slice (addrIndirect, classType.getSize());
      abb.order(byteorder);
      return new Value(classType, s, abb);
    }
    }
    return null;
  }
  
  private Type getPointerTarget (DwarfDie type)
  {
    ByteOrder byteorder = task.getIsa().getByteOrder();
    switch (type.getBaseType())
    {
    case BaseTypes.baseTypeByte:
    case BaseTypes.baseTypeUnsignedByte:
      return byteType;
    case BaseTypes.baseTypeShort:
    case BaseTypes.baseTypeUnsignedShort:
      return shortType;
    case BaseTypes.baseTypeInteger:
    case BaseTypes.baseTypeUnsignedInteger:
      return intType;
    case BaseTypes.baseTypeLong:
    case BaseTypes.baseTypeUnsignedLong:
      return longType;
    case BaseTypes.baseTypeFloat:
      return floatType;
    case BaseTypes.baseTypeDouble:
      return doubleType;
    }
    switch (type.getTag())
    {
    case DwTagEncodings.DW_TAG_pointer_type_:
    {
      return new PointerType(byteorder, getPointerTarget(type.getUltimateType()), "void*");
    }
    }
    return null;
  }
  
  public Value getSubprogramValue (DwarfDie varDie)
  {
    ByteOrder byteorder = task.getIsa().getByteOrder();
    
    if (varDie == null)
      return (null);

    switch (varDie.getTag())
    {
    case DwTagEncodings.DW_TAG_subprogram_:
    {
      Value value = null;
      Type type = null;
      if (varDie.getUltimateType() != null)
        {
          value = getValue(varDie);
          if (value != null)
            type = value.getType();
        }
      FunctionType functionType = new FunctionType(byteorder, varDie.getName(), type);
      DwarfDie parm = varDie.getChild();
      while (parm != null && parm.getTag() == DwTagEncodings.DW_TAG_formal_parameter_)
        {
          if (parm.getAttrBoolean((DwAtEncodings.DW_AT_artificial_)) == false)
            {
              value = getValue(parm);
              functionType.addParameter(value.getType(), value.getText());
        }
      parm = parm.getSibling();
        }
      return new Value (functionType, varDie.getName());
    }
    }
    return null;
  }
  
  public Value getValue (DwarfDie varDie)
  {
    ByteOrder byteorder = task.getIsa().getByteOrder();
    
    if (varDie == null)
      return (null);

    DwarfDie type = varDie.getUltimateType();
    if (type == null)
      type = varDie;
    
    switch (type.getTag())
    {
    case DwTagEncodings.DW_TAG_pointer_type_:
    {
      type = type.getUltimateType();
      if (type == null)
        return new Value (new PointerType(byteorder, null, "void*"), varDie.getName());
      return new Value (new PointerType(byteorder, getPointerTarget (type), "*"), varDie.getName());
    }
    case DwTagEncodings.DW_TAG_array_type_:
    {
      DwarfDie subrange = type.getChild();
      return new Value (getArrayType(type, subrange), varDie.getName());
    }
    case DwTagEncodings.DW_TAG_union_type_:
    case DwTagEncodings.DW_TAG_structure_type_:
    {
      boolean noTypeDef = (varDie.getType() == null);
      String name = noTypeDef ? varDie.getName() 
                                               : varDie.getType().getName();
      Value value = new Value (getClassType(type, name), varDie.getName());
      if (type != varDie.getType() && noTypeDef == false)
        value.getType().setTypedef(true);
      return value;
    }
    case DwTagEncodings.DW_TAG_enumeration_type_:
    {
      DwarfDie subrange = type.getChild();
      EnumType enumType = new EnumType(byteorder);
      while (subrange != null)
        {
          enumType.addMember(byteType, subrange.getName(), 
                             subrange.getAttrConstant(DwAtEncodings.DW_AT_const_value_));
          subrange = subrange.getSibling();
        }
      return new Value (enumType, varDie.getName());
    }
    }
    
    type = varDie.getUltimateType();
    if (type == null)
      return null;
    DwarfDie dieType = varDie.getType();
    boolean haveTypeDef;
    if (type != dieType)
      haveTypeDef = true;
    else
      haveTypeDef = false;

    switch (type.getBaseType())
    {
    case BaseTypes.baseTypeLong:
      return ArithmeticType.newLongValue(fetchType(haveTypeDef, longType, dieType.getName()), 
                                         varDie.getName(), 0);
    case BaseTypes.baseTypeInteger:
      return ArithmeticType.newIntegerValue(fetchType(haveTypeDef, intType, dieType.getName()), 
                                            varDie.getName(), 0);
    case BaseTypes.baseTypeShort:
      return ArithmeticType.newShortValue(fetchType(haveTypeDef, shortType, dieType.getName()), 
                                          varDie.getName(), (short)0);
    case BaseTypes.baseTypeByte:
      return ArithmeticType.newByteValue(fetchType(haveTypeDef, byteType, dieType.getName()), 
                                         varDie.getName(), (byte)0);
    case BaseTypes.baseTypeFloat:
      return ArithmeticType.newFloatValue(fetchType(haveTypeDef, floatType, dieType.getName()), 
                                          varDie.getName(), 0);
    case BaseTypes.baseTypeDouble:
      return ArithmeticType.newDoubleValue(fetchType(haveTypeDef, doubleType, dieType.getName()), 
                                          varDie.getName(), 0);
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
