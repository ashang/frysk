// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
// Copyright 2007, (C) IBM
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

package frysk.testbed;

import frysk.isa.registers.RegistersFactory;
import frysk.isa.registers.Registers;
import frysk.isa.registers.RegisterGroup;
import frysk.isa.registers.Register;
import frysk.isa.registers.IA32Registers;
import frysk.isa.registers.X8664Registers;
import frysk.isa.registers.PPC32Registers;
import frysk.isa.registers.PPC64Registers;
import frysk.isa.registers.X87Registers;
import frysk.isa.ISA;
import frysk.isa.ISAMap;
import frysk.proc.Task;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.math.BigInteger;
import inua.eio.ByteOrder;
import frysk.symtab.Symbol;
import frysk.symtab.SymbolFactory;

/**
 * A base class for test-cases that need to check all register values.
 *
 * For instance, both frysk.proc.Task.getRegister* and
 * frysk.stack.Frame.getRegister.
 */

public abstract class RegsCase extends TestLib {

    private Task task;
    private Values values;
    private ByteOrder order;
    private ISA isa;
    private Registers registers;

    /**
     * Set things up for testing against the specified task.
     *
     * Initially things are set up for testing against the live
     * "funit-regs" process; this can be overwritten by re-calling
     * with a new task (for instance from a core file created from the
     * live funit-regs.
     */
    protected void setTask(Task task) {
	this.task = task;
	isa = task.getISA();
	order = isa.order();
	registers = RegistersFactory.getRegisters(isa);
	if (isaValues.containsKey(isa))
	    values = (Values)isaValues.get(isa);
    }

    public void setUp() {
	super.setUp();
	setTask(new DaemonBlockedAtSignal("funit-regs").getMainTask());
    }

    public void tearDown() {
	task = null;
	values = null;
	registers = null;
	isa = null;
	super.tearDown();
    }

    protected Task task() {
	return task;
    }

    // Package-private
    Values values() {
	return values;
    }
    protected ISA isa() {
	return isa;
    }

    protected abstract void access(Register register,
				   int offset, int length, byte[] bytes,
				   int start, boolean write);
    protected abstract long getRegister(Object task, Register reg);

    private void checkRegisterGroup(RegisterGroup registerGroup) {
	Register[] registers = registerGroup.getRegisters();
	for (int i = 0; i < registers.length; i++) {
	    Register register = registers[i];
	    Value value = (Value)values.get(register);
	    value.checkRegister(this, register);
	}
    }

    private void checkRegisterGroup(String what) {
	RegisterGroup registerGroup = registers.getGroup(what);
	if (unsupported("no " + what + " registers", registerGroup == null))
	    return;
	checkRegisterGroup(registerGroup);
    }
   
    public void testGeneralRegisters() {
	checkRegisterGroup(registers.getGeneralRegisterGroup());
    }

    public void testFloatRegisters() {
	checkRegisterGroup("float");
    }

    public void testVectorRegisters() {
	checkRegisterGroup("vector");
    }

    /**
     * Convert the ByteOrdered byte array to a BigInteger.
     */
    BigInteger toBigInteger(byte[] bytes) {
	byte[] ordered;
	if (order == ByteOrder.LITTLE_ENDIAN) {
	    // Reverse for integer output.
	    ordered = new byte[bytes.length];
	    for (int j = 0; j < bytes.length; j++) {
		ordered[j] = bytes[bytes.length - j - 1];
	    }
	} else {
	    ordered = bytes;
	}
	return new BigInteger(1, ordered);
    }

    /**
     * Possible register values;; package private.
     */
    static abstract class Value {
	abstract void checkValue(Register register);
	abstract void checkRegister(RegsCase c, Register register);
	BigInteger registerValue(RegsCase c, Register register) {
	    byte[] bytes = new byte[register.getType().getSize()];
	    c.access(register, 0, bytes.length, bytes, 0, false);
	    BigInteger check = c.toBigInteger(bytes);
	    return check;
	}
    }

    /**
     * Don't check the register; not yet used.
     */
//     private static class NoValue extends Value {
// 	void checkValue(Register register) { }
// 	void checkRegister(RegsCase c, Register register) { }
//     }

    /**
     * Compare the register against the BigInteger (the register is
     * converted to an unsigned big integer using the native byte
     * order).
     *
     * The BigInteger must have a non-zero value for all register
     * bytes.
     */
    private static class BigIntegerValue extends Value {
	private final BigInteger correct;
	BigIntegerValue(Register register, BigInteger correct) {
	    this.correct = correct;
	}
	// Check that the least significant bytes are all non-zero.
	void checkValue(Register register) {
	    byte[] bytes = correct.toByteArray();
	    for (int i = bytes.length - register.getType().getSize();
		 i < bytes.length; i++) {
		RegsCase.assertTrue(register.getName() + "[" + i + "] != 0",
				    bytes[i] != 0);
	    }
	}
	void checkRegister(RegsCase c, Register register) {
	    RegsCase.assertEquals(register.getName(), correct,
				  registerValue(c, register));
	}
    }

    private static class ByteValue extends Value {
	private final byte[] bytes;
	ByteValue(Register register, byte[] bytes) {
	    this.bytes = bytes;
	}
	void checkValue(Register register) {
	    RegsCase.assertEquals(register.getName() + " size",
				  bytes.length, register.getType().getSize());
	    for (int i = 0; i < bytes.length; i++) {
		RegsCase.assertTrue(register.getName() + "[" + 
				    i + "] != 0 (found " + bytes[i] + " there)",
				    bytes[i] != 0);
	    }
	}
	void checkRegister(RegsCase c, Register register) {
	    BigInteger correct = c.toBigInteger(bytes);
	    RegsCase.assertEquals(register.getName(), correct,
				  registerValue(c, register));
	}
    }

    private static class SymbolValue extends Value {
	private final String correct;
	SymbolValue(Register register, String correct) {
	    this.correct = correct;
	}
	void checkValue(Register register) { }
	void checkRegister(RegsCase c, Register register) {
	    BigInteger value = registerValue(c, register);
	    Symbol check = SymbolFactory.getSymbol(c.task(),
						   value.longValue());
	    RegsCase.assertEquals(register.getName(), correct,
				  check.getName());
	}
    }

    private static class MaskedValue extends Value {
	private final BigInteger mask;
	private final BigInteger correct;
	MaskedValue(Register register, BigInteger correct, BigInteger mask) {
	    this.correct = correct;
	    this.mask = mask;
	}
	void checkValue(Register register) {
	    // can't verify this one
	}
	void checkRegister(RegsCase c, Register register) {
	    BigInteger check = registerValue(c, register).andNot(mask);
	    RegsCase.assertEquals(register.getName(), correct, check);
	}
    }

    /**
     * Possible register values; package private.
     */
    static class Values {
	// XXX: Do not extend LinkedHashMap as want to restrict the
	// "add" methods available.
	private final LinkedHashMap map = new LinkedHashMap();
	Iterator iterator() {
	    return map.entrySet().iterator();
	}
	boolean containsKey(Register key) {
	    return map.containsKey(key);
	}
	Value get(Register register) {
	    return (Value)map.get(register);
	}
	Values put(Register register, byte[] bytes) {
	    map.put(register, new ByteValue(register, bytes));
	    return this;
	}
	Values put(Register register, BigInteger correct) {
	    map.put(register, new BigIntegerValue(register, correct));
	    return this;
	}
	Values put(Register register, long correct) {
	    map.put(register, new BigIntegerValue(register,
					BigInteger.valueOf(correct)));
	    return this;
	}
	Values put(Register register, long correct, long mask) {
	    map.put(register, new MaskedValue(register,
				    BigInteger.valueOf(correct),
				    BigInteger.valueOf(mask)));
	    return this;
	}
	Values put(Register register, String correct) {
	    map.put(register, new SymbolValue(register, correct));
	    return this;
	}
    }

    /**
     * The registers should all be compared against non-zero ramdom
     * byte[] arrays, and not simple constants.
     */

    private Values IA32 = new Values()
    // general registers
        .put(IA32Registers.EAX, // 0x7eb03efc
             new byte[] { (byte)0xfc, 0x3e, (byte)0xb0, 0x7e })
        .put(IA32Registers.EBX, // 0x35a322a0
             new byte[] { (byte)0xa0, 0x22, (byte)0xa3, 0x35 })
        .put(IA32Registers.ECX, // 0x7f198cab
             new byte[] { (byte)0xab, (byte)0x8c, 0x19, 0x7f })
        .put(IA32Registers.EDX, // 0x35b374c3
             new byte[] { (byte)0xc3, 0x74, (byte)0xb3, 0x35 })
        .put(IA32Registers.ESI, // 0x1bc5daed
             new byte[] { (byte)0xed, (byte)0xda, (byte)0xc5, 0x1b })
        .put(IA32Registers.EDI, // 0x457319f3
             new byte[] { (byte)0xf3, 0x19, 0x73, 0x45 })
        .put(IA32Registers.EBP, // 0xcbfed73c
             new byte[] { 0x3c, (byte)0xd7, (byte)0xfe, (byte)0xcb })
        .put(IA32Registers.EFLAGS, 0x10246,
	     1 << 21) // Mask CPUID
        .put(IA32Registers.ESP, // 0x93d4a6ed
             new byte[] { (byte)0xed, (byte)0xa6, (byte)0xd4, (byte)0x93 })
 	.put(IA32Registers.EIP, "crash")
    // floating-point registers
        .put(X87Registers.FCW, // 0x1e71
             new byte[] { 0x71,0x1e })
        .put(X87Registers.FSW, // 0xc7e4
             new byte[] { (byte)0xe4,(byte)0xc7 })
        .put(X87Registers.FTW, // 0xc9
             new byte[] { (byte)0xc9 })
        .put(X87Registers.FOP, // 0x068f
             new byte[] { (byte)0x8f,0x06 })
        .put(X87Registers.EIP, // 0x79d5eeff
             new byte[] { (byte)0xff,(byte)0xee,(byte)0xd5,(byte)0x79 })
	.put(X87Registers.CS, // Can't reliably access CS
	     0, 0)
        .put(X87Registers.DP, // 0xd0acd7b0
             new byte[] { (byte)0xb0,(byte)0xd7,(byte)0xac,(byte)0xd0 })
        .put(X87Registers.DS, // Can't reliably access CS
	     0, 0)
	.put(X87Registers.ST0, // 0xa7367289dc779dba0bd9
             new byte[] { (byte)0xd9,0xb,(byte)0xba,(byte)0x9d,
                          0x77,(byte)0xdc,(byte)0x89,0x72,
                          0x36,(byte)0xa7 })
        .put(X87Registers.ST1, // 0x64abfe452c2a5b8d0eb1
             new byte[] { (byte)0xb1,0xe,(byte)0x8d,0x5b,
                          0x2a,0x2c,0x45,(byte)0xfe,
                          (byte)0xab,0x64 })
        .put(X87Registers.ST2, // 0xb829e094740ce9d53a04
             new byte[] { 0x4,0x3a,(byte)0xd5,(byte)0xe9,
                          0xc,0x74,(byte)0x94,(byte)0xe0,
                          0x29,(byte)0xb8 })
        .put(X87Registers.ST3, // 0x4bd27ebf86294a4a48f8
             new byte[] { (byte)0xf8,0x48,0x4a,0x4a,
                          0x29,(byte)0x86,(byte)0xbf,0x7e,
                          (byte)0xd2,0x4b })
        .put(X87Registers.ST4, // 0xb96a6b1dabba9af1fa66
             new byte[] { 0x66,(byte)0xfa,(byte)0xf1,(byte)0x9a,
                          (byte)0xba,(byte)0xab,0x1d,0x6b,
                          0x6a,(byte)0xb9 })
        .put(X87Registers.ST5, // 0x87d05c0a1e9c9bb98ebc
             new byte[] { (byte)0xbc,(byte)0x8e,(byte)0xb9,(byte)0x9b,
                          (byte)0x9c,0x1e,0xa,0x5c,
                          (byte)0xd0,(byte)0x87 })
        .put(X87Registers.ST6, // 0x5a0f14dcf87c56690b5f
             new byte[] { 0x5f,0xb,0x69,0x56,
                          0x7c,(byte)0xf8,(byte)0xdc,0x14,
                          0xf,0x5a })
        .put(X87Registers.ST7, // 0x1f12ae5ec49479a8cb19
             new byte[] { 0x19,(byte)0xcb,(byte)0xa8,0x79,
                          (byte)0x94,(byte)0xc4,0x5e,(byte)0xae,
                          0x12,0x1f })
    // vector registers
        .put(X87Registers.XMM0, // 0x47beb912e3bfa457d6af5267b3fec23b
             new byte[] { 0x3b,(byte)0xc2,(byte)0xfe,(byte)0xb3,
                          0x67,0x52,(byte)0xaf,(byte)0xd6,
                          0x57,(byte)0xa4,(byte)0xbf,(byte)0xe3,
                          0x12,(byte)0xb9,(byte)0xbe,0x47 })
        .put(X87Registers.XMM1, // 0x7ce95f1c2fe254e2cac9b22bf43f73c5
             new byte[] { (byte)0xc5,0x73,0x3f,(byte)0xf4,
                          0x2b,(byte)0xb2,(byte)0xc9,(byte)0xca,
                          (byte)0xe2,0x54,(byte)0xe2,0x2f,
                          0x1c,0x5f,(byte)0xe9,0x7c })
        .put(X87Registers.XMM2, // 0x566b1b326d658a3365678d130362a6b5
             new byte[] { (byte)0xb5,(byte)0xa6,0x62,0x3,
                          0x13,(byte)0x8d,0x67,0x65,
                          0x33,(byte)0x8a,0x65,0x6d,
                          0x32,0x1b,0x6b,0x56 })
        .put(X87Registers.XMM3, // 0x20801ada9126df05d6927e0847fa8f07
             new byte[] { 0x7,(byte)0x8f,(byte)0xfa,0x47,
                          0x8,0x7e,(byte)0x92,(byte)0xd6,
                          0x5,(byte)0xdf,0x26,(byte)0x91,
                          (byte)0xda,0x1a,(byte)0x80,0x20 })
        .put(X87Registers.XMM4, // 0x2304fff624579bbddc74a7df4d34cfd9
             new byte[] { (byte)0xd9,(byte)0xcf,0x34,0x4d,
                          (byte)0xdf,(byte)0xa7,0x74,(byte)0xdc,
                          (byte)0xbd,(byte)0x9b,0x57,0x24,
                          (byte)0xf6,(byte)0xff,0x4,0x23 })
        .put(X87Registers.XMM5, // 0x58afb31bf2d2b4a33512eefc0d1f5fc4
             new byte[] { (byte)0xc4,0x5f,0x1f,0xd,
                          (byte)0xfc,(byte)0xee,0x12,0x35,
                          (byte)0xa3,(byte)0xb4,(byte)0xd2,(byte)0xf2,
                          0x1b,(byte)0xb3,(byte)0xaf,0x58 })
        .put(X87Registers.XMM6, // 0x5caa454b2a0c2975df1df97f8d180e93
             new byte[] { (byte)0x93,0xe,0x18,(byte)0x8d,
                          0x7f,(byte)0xf9,0x1d,(byte)0xdf,
                          0x75,0x29,0xc,0x2a,
                          0x4b,0x45,(byte)0xaa,0x5c })
        .put(X87Registers.XMM7, // 0x2147b54a26ac605b98ef6a2f2da97f57
             new byte[] { 0x57,0x7f,(byte)0xa9,0x2d,
                          0x2f,0x6a,(byte)0xef,(byte)0x98,
                          0x5b,0x60,(byte)0xac,0x26,
                          0x4a,(byte)0xb5,0x47,0x21 })
	;

    private Values X8664 = new Values()
        .put(X8664Registers.RAX, // 0x837bb4e2d8209ca3
             new byte[] { (byte)0xa3,(byte)0x9c,0x20,(byte)0xd8,
                          (byte)0xe2,(byte)0xb4,0x7b,(byte)0x83 })
        .put(X8664Registers.RDX, // 0x16d196be91fb2b92
             new byte[] { (byte)0x92,0x2b,(byte)0xfb,(byte)0x91,
                          (byte)0xbe,(byte)0x96,(byte)0xd1,0x16 })
        .put(X8664Registers.RCX, // 0x2b9f5c8f2d8cc8f9
             new byte[] { (byte)0xf9,(byte)0xc8,(byte)0x8c,0x2d,
                          (byte)0x8f,0x5c,(byte)0x9f,0x2b })
        .put(X8664Registers.RBX, // 0x96c38e833b3f5b12
             new byte[] { 0x12,0x5b,0x3f,0x3b,
                          (byte)0x83,(byte)0x8e,(byte)0xc3,(byte)0x96 })
        .put(X8664Registers.RSI, // 0x79a4d3db85249938
             new byte[] { 0x38,(byte)0x99,0x24,(byte)0x85,
                          (byte)0xdb,(byte)0xd3,(byte)0xa4,0x79 })
        .put(X8664Registers.RDI, // 0x20f76067e815c3b3
             new byte[] { (byte)0xb3,(byte)0xc3,0x15,(byte)0xe8,
                          0x67,0x60,(byte)0xf7,0x20 })
        .put(X8664Registers.RBP, // 0x187298d02605a3d
             new byte[] { 0x3d,0x5a,0x60,0x2,
                          (byte)0x8d,0x29,(byte)0x87,0x1 })
        .put(X8664Registers.RSP, // 0x284732dfa61c4a30
             new byte[] { 0x30,0x4a,0x1c,(byte)0xa6,
                          (byte)0xdf,0x32,0x47,0x28 })
        .put(X8664Registers.R8, // 0xb3323ac248cb3a9
             new byte[] { (byte)0xa9,(byte)0xb3,(byte)0x8c,0x24,
                          (byte)0xac,0x23,0x33,0xb })
        .put(X8664Registers.R9, // 0x4bbb808dda0214ec
             new byte[] { (byte)0xec,0x14,0x2,(byte)0xda,
                          (byte)0x8d,(byte)0x80,(byte)0xbb,0x4b })
        .put(X8664Registers.R10, // 0xb21dd4b61174f4b2
             new byte[] { (byte)0xb2,(byte)0xf4,0x74,0x11,
                          (byte)0xb6,(byte)0xd4,0x1d,(byte)0xb2 })
        .put(X8664Registers.R11, // 0x8c5582abde79fd44
             new byte[] { 0x44,(byte)0xfd,0x79,(byte)0xde,
                          (byte)0xab,(byte)0x82,0x55,(byte)0x8c })
        .put(X8664Registers.R12, // 0xad1a8867c8bd5a68
             new byte[] { 0x68,0x5a,(byte)0xbd,(byte)0xc8,
                          0x67,(byte)0x88,0x1a,(byte)0xad })
        .put(X8664Registers.R13, // 0xe0fb8a51946f37a0
             new byte[] { (byte)0xa0,0x37,0x6f,(byte)0x94,
                          0x51,(byte)0x8a,(byte)0xfb,(byte)0xe0 })
        .put(X8664Registers.R14, // 0x3f22bc816a35f02d
             new byte[] { 0x2d,(byte)0xf0,0x35,0x6a,
                          (byte)0x81,(byte)0xbc,0x22,0x3f })
        .put(X8664Registers.R15, // 0x46bf65d4d966290
             new byte[] { (byte)0x90,0x62,(byte)0x96,0x4d,
                          0x5d,(byte)0xf6,0x6b,0x4 })
        .put(X8664Registers.RIP, "crash")
    // floating-point registers
        .put(X87Registers.FCW, // 0x1e71
             new byte[] { 0x71,0x1e })
        .put(X87Registers.FSW, // 0xc7e4
             new byte[] { (byte)0xe4,(byte)0xc7 })
        .put(X87Registers.FTW, // 0xc9
             new byte[] { (byte)0xc9 })
        .put(X87Registers.FOP, // 0x068f
             new byte[] { (byte)0x8f,0x06 })
        .put(X87Registers.RIP, // 48-bit
	     0x0000236679d5eeffL, 0)
        .put(X87Registers.RDP, // 48-bit
	     0x00007565d0acd7b0L, 0)
	.put(X87Registers.ST0, // 0xa7367289dc779dba0bd9
             new byte[] { (byte)0xd9,0xb,(byte)0xba,(byte)0x9d,
                          0x77,(byte)0xdc,(byte)0x89,0x72,
                          0x36,(byte)0xa7 })
        .put(X87Registers.ST1, // 0x64abfe452c2a5b8d0eb1
             new byte[] { (byte)0xb1,0xe,(byte)0x8d,0x5b,
                          0x2a,0x2c,0x45,(byte)0xfe,
                          (byte)0xab,0x64 })
        .put(X87Registers.ST2, // 0xb829e094740ce9d53a04
             new byte[] { 0x4,0x3a,(byte)0xd5,(byte)0xe9,
                          0xc,0x74,(byte)0x94,(byte)0xe0,
                          0x29,(byte)0xb8 })
        .put(X87Registers.ST3, // 0x4bd27ebf86294a4a48f8
             new byte[] { (byte)0xf8,0x48,0x4a,0x4a,
                          0x29,(byte)0x86,(byte)0xbf,0x7e,
                          (byte)0xd2,0x4b })
        .put(X87Registers.ST4, // 0xb96a6b1dabba9af1fa66
             new byte[] { 0x66,(byte)0xfa,(byte)0xf1,(byte)0x9a,
                          (byte)0xba,(byte)0xab,0x1d,0x6b,
                          0x6a,(byte)0xb9 })
        .put(X87Registers.ST5, // 0x87d05c0a1e9c9bb98ebc
             new byte[] { (byte)0xbc,(byte)0x8e,(byte)0xb9,(byte)0x9b,
                          (byte)0x9c,0x1e,0xa,0x5c,
                          (byte)0xd0,(byte)0x87 })
        .put(X87Registers.ST6, // 0x5a0f14dcf87c56690b5f
             new byte[] { 0x5f,0xb,0x69,0x56,
                          0x7c,(byte)0xf8,(byte)0xdc,0x14,
                          0xf,0x5a })
        .put(X87Registers.ST7, // 0x1f12ae5ec49479a8cb19
             new byte[] { 0x19,(byte)0xcb,(byte)0xa8,0x79,
                          (byte)0x94,(byte)0xc4,0x5e,(byte)0xae,
                          0x12,0x1f })
    // vector registers
        .put(X87Registers.XMM0, // 0x47beb912e3bfa457d6af5267b3fec23b
             new byte[] { 0x3b,(byte)0xc2,(byte)0xfe,(byte)0xb3,
                          0x67,0x52,(byte)0xaf,(byte)0xd6,
                          0x57,(byte)0xa4,(byte)0xbf,(byte)0xe3,
                          0x12,(byte)0xb9,(byte)0xbe,0x47 })
        .put(X87Registers.XMM1, // 0x7ce95f1c2fe254e2cac9b22bf43f73c5
             new byte[] { (byte)0xc5,0x73,0x3f,(byte)0xf4,
                          0x2b,(byte)0xb2,(byte)0xc9,(byte)0xca,
                          (byte)0xe2,0x54,(byte)0xe2,0x2f,
                          0x1c,0x5f,(byte)0xe9,0x7c })
        .put(X87Registers.XMM2, // 0x566b1b326d658a3365678d130362a6b5
             new byte[] { (byte)0xb5,(byte)0xa6,0x62,0x3,
                          0x13,(byte)0x8d,0x67,0x65,
                          0x33,(byte)0x8a,0x65,0x6d,
                          0x32,0x1b,0x6b,0x56 })
        .put(X87Registers.XMM3, // 0x20801ada9126df05d6927e0847fa8f07
             new byte[] { 0x7,(byte)0x8f,(byte)0xfa,0x47,
                          0x8,0x7e,(byte)0x92,(byte)0xd6,
                          0x5,(byte)0xdf,0x26,(byte)0x91,
                          (byte)0xda,0x1a,(byte)0x80,0x20 })
        .put(X87Registers.XMM4, // 0x2304fff624579bbddc74a7df4d34cfd9
             new byte[] { (byte)0xd9,(byte)0xcf,0x34,0x4d,
                          (byte)0xdf,(byte)0xa7,0x74,(byte)0xdc,
                          (byte)0xbd,(byte)0x9b,0x57,0x24,
                          (byte)0xf6,(byte)0xff,0x4,0x23 })
        .put(X87Registers.XMM5, // 0x58afb31bf2d2b4a33512eefc0d1f5fc4
             new byte[] { (byte)0xc4,0x5f,0x1f,0xd,
                          (byte)0xfc,(byte)0xee,0x12,0x35,
                          (byte)0xa3,(byte)0xb4,(byte)0xd2,(byte)0xf2,
                          0x1b,(byte)0xb3,(byte)0xaf,0x58 })
        .put(X87Registers.XMM6, // 0x5caa454b2a0c2975df1df97f8d180e93
             new byte[] { (byte)0x93,0xe,0x18,(byte)0x8d,
                          0x7f,(byte)0xf9,0x1d,(byte)0xdf,
                          0x75,0x29,0xc,0x2a,
                          0x4b,0x45,(byte)0xaa,0x5c })
        .put(X87Registers.XMM7, // 0x2147b54a26ac605b98ef6a2f2da97f57
             new byte[] { 0x57,0x7f,(byte)0xa9,0x2d,
                          0x2f,0x6a,(byte)0xef,(byte)0x98,
                          0x5b,0x60,(byte)0xac,0x26,
                          0x4a,(byte)0xb5,0x47,0x21 })
        .put(X87Registers.XMM8, // 0x59f29cf0c8c06a32cfbcf982d29b2622
             new byte[] { 0x22,0x26,(byte)0x9b,(byte)0xd2,
                          (byte)0x82,(byte)0xf9,(byte)0xbc,(byte)0xcf,
                          0x32,0x6a,(byte)0xc0,(byte)0xc8,
                          (byte)0xf0,(byte)0x9c,(byte)0xf2,0x59 })
        .put(X87Registers.XMM9, // 0x20105410d22c027c7ff7fd949673caad
             new byte[] { (byte)0xad,(byte)0xca,0x73,(byte)0x96,
                          (byte)0x94,(byte)0xfd,(byte)0xf7,0x7f,
                          0x7c,0x2,0x2c,(byte)0xd2,
                          0x10,0x54,0x10,0x20 })
        .put(X87Registers.XMM10, // 0xf618933912ad69c1b184ef8159ce5708
             new byte[] { 0x8,0x57,(byte)0xce,0x59,
                          (byte)0x81,(byte)0xef,(byte)0x84,(byte)0xb1,
                          (byte)0xc1,0x69,(byte)0xad,0x12,
                          0x39,(byte)0x93,0x18,(byte)0xf6 })
        .put(X87Registers.XMM11, // 0x856c8d779d6ed06a4bdadc488a3e1989
             new byte[] { (byte)0x89,0x19,0x3e,(byte)0x8a,
                          0x48,(byte)0xdc,(byte)0xda,0x4b,
                          0x6a,(byte)0xd0,0x6e,(byte)0x9d,
                          0x77,(byte)0x8d,0x6c,(byte)0x85 })
        .put(X87Registers.XMM12, // 0xf232c4e489f81468c534d0627b8f373
             new byte[] { 0x73,(byte)0xf3,(byte)0xb8,0x27,
                          0x6,0x4d,0x53,(byte)0x8c,
                          0x46,(byte)0x81,(byte)0x9f,0x48,
                          0x4e,0x2c,0x23,0xf })
        .put(X87Registers.XMM13, // 0x117c5a8df5f87761027e19798ad84b0
             new byte[] { (byte)0xb0,(byte)0x84,(byte)0xad,(byte)0x98,
                          (byte)0x97,(byte)0xe1,0x27,0x10,
                          0x76,(byte)0x87,0x5f,(byte)0xdf,
                          (byte)0xa8,(byte)0xc5,0x17,0x1 })
        .put(X87Registers.XMM15, // 0xea03fbb7498f45fc918621ffe21d2f53
             new byte[] { 0x53,0x2f,0x1d,(byte)0xe2,
                          (byte)0xff,0x21,(byte)0x86,(byte)0x91,
                          (byte)0xfc,0x45,(byte)0x8f,0x49,
                          (byte)0xb7,(byte)0xfb,0x3,(byte)0xea })
        .put(X87Registers.XMM15, // 0xfc6683d958a95180c6226f27a78b2c5a
             new byte[] { 0x5a,0x2c,(byte)0x8b,(byte)0xa7,
                          0x27,0x6f,0x22,(byte)0xc6,
                          (byte)0x80,0x51,(byte)0xa9,0x58,
                          (byte)0xd9,(byte)0x83,0x66,(byte)0xfc })
	;

    private Values PPC32 = new Values()
	//in PowerPC gpr0 is always Zero
        .put(PPC32Registers.GPR0, 0, 0)
	.put(PPC32Registers.GPR1, 
             new byte[] { (byte)0x3a,(byte)0x82,0x27,(byte)0xf1 })
	.put(PPC32Registers.GPR2,
             new byte[] { (byte)0x1b,(byte)0x12,(byte)0xa0,(byte)0xa2 })
	.put(PPC32Registers.GPR3,
             new byte[] { (byte)0xe2,(byte)0xab,(byte)0xff,(byte)0xcc })
        .put(PPC32Registers.GPR4,
             new byte[] { (byte)0xc4,(byte)0x46,(byte)0xeb,(byte)0xf1 })
        .put(PPC32Registers.GPR5,
             new byte[] { (byte)0xa9,(byte)0x94,0x2a,(byte)0x4e })
        .put(PPC32Registers.GPR6,
             new byte[] { (byte)0x55,(byte)0xa2,(byte)0x92,(byte)0x51 })
        .put(PPC32Registers.GPR7,
             new byte[] { (byte)0x4f,(byte)0x61,0x6e,(byte)0xf2 })
        .put(PPC32Registers.GPR8,
             new byte[] { (byte)0xf1,(byte)0x76,(byte)0xef,(byte)0x4d })
        .put(PPC32Registers.GPR9,
             new byte[] { (byte)0xdf,(byte)0xac,0x22,(byte)0x56 })
	.put(PPC32Registers.GPR10,
             new byte[] { (byte)0xad,(byte)0x1a,(byte)0x8a,(byte)0x99 })
        .put(PPC32Registers.GPR11,
             new byte[] { (byte)0x11,(byte)0x2e,(byte)0x88,(byte)0xab })
        .put(PPC32Registers.GPR12,
             new byte[] { (byte)0xf7,(byte)0xfc,(byte)0xd1,(byte)0xf2 })
        .put(PPC32Registers.GPR13,
             new byte[] { (byte)0x88,(byte)0x3e,(byte)0xf1,(byte)0x01 })
        .put(PPC32Registers.GPR14,
             new byte[] { (byte)0xef,(byte)0x2e,0x26,(byte)0x91 })
        .put(PPC32Registers.GPR15,
             new byte[] { (byte)0x9c,(byte)0x1a,(byte)0x6e,(byte)0xe1 })
        .put(PPC32Registers.GPR16,
             new byte[] { (byte)0x20,(byte)0x0d,0x11,(byte)0x34 })
        .put(PPC32Registers.GPR17,
             new byte[] { (byte)0x63,(byte)0x4b,(byte)0x99,(byte)0x11 })
        .put(PPC32Registers.GPR18,
             new byte[] { (byte)0xd8,(byte)0x9b,(byte)0xde,(byte)0x81 })
        .put(PPC32Registers.GPR19,
             new byte[] { (byte)0x6e,(byte)0x6e,(byte)0xf9,(byte)0xba })
	.put(PPC32Registers.GPR20,
             new byte[] { (byte)0x55,(byte)0x51,(byte)0xaa,(byte)0xc3 })
        .put(PPC32Registers.GPR21,
             new byte[] { (byte)0x12,(byte)0x9c,(byte)0x72,(byte)0x3e })
        .put(PPC32Registers.GPR22,
             new byte[] { (byte)0x29,(byte)0x9c,(byte)0x77,(byte)0x33 })
        .put(PPC32Registers.GPR23,
             new byte[] { (byte)0x31,(byte)0x9c,0x20,(byte)0x44 })
        .put(PPC32Registers.GPR24,
             new byte[] { (byte)0xf3,(byte)0x9c,0x45,(byte)0xbb })
        .put(PPC32Registers.GPR25,
             new byte[] { (byte)0x10,(byte)0x9c,(byte)0x92,(byte)0xfa })
        .put(PPC32Registers.GPR26,
             new byte[] { (byte)0xe4,(byte)0x9c,(byte)0xbc,(byte)0xd8 })
        .put(PPC32Registers.GPR27,
             new byte[] { (byte)0x39,(byte)0x9c,(byte)0xe5,(byte)0xc6 })
        .put(PPC32Registers.GPR28,
             new byte[] { (byte)0x43,(byte)0x9c,(byte)0x4a,(byte)0x99 })
        .put(PPC32Registers.GPR29,
             new byte[] { (byte)0xd7,(byte)0x9c,0x44,(byte)0x01 })
	.put(PPC32Registers.GPR30,
             new byte[] { (byte)0x62,(byte)0x9c,0x09,(byte)0xeb })
        .put(PPC32Registers.GPR31,
             new byte[] { (byte)0x42,(byte)0x9c,0x78,(byte)0xa1 })

        .put(PPC32Registers.FPR0,
             new byte[] { (byte)0xa3,(byte)0x9c,0x20,(byte)0xd8,
                          (byte)0xe2,(byte)0xb4,0x7b,(byte)0x83 })
	;

    private Values PPC64 = new Values()
        //in PowerPC the GPR0 is always Zero
        .put(PPC64Registers.GPR0, 0, 0)
        .put(PPC64Registers.GPR1, // 0x514c159c25c27735
             new byte[] { 0x51,0x4c,0x15,(byte)0x9c,
                          0x25,(byte)0xc2,0x77,0x35 })
        .put(PPC64Registers.GPR2, // 0x674b6064cdf97685
             new byte[] { 0x67,0x4b,0x60,0x64,
                          (byte)0xcd,(byte)0xf9,0x76,(byte)0x85 })
        .put(PPC64Registers.GPR3, // 0x808ac01e8911f56c
             new byte[] { (byte)0x80,(byte)0x8a,(byte)0xc0,0x1e,
                          (byte)0x89,0x11,(byte)0xf5,0x6c })
        .put(PPC64Registers.GPR4, // 0xcf4362db3356a25a
             new byte[] { (byte)0xcf,0x43,0x62,(byte)0xdb,
                          0x33,0x56,(byte)0xa2,0x5a })
        .put(PPC64Registers.GPR5, // 0xe356818815d30ae3
             new byte[] { (byte)0xe3,0x56,(byte)0x81,(byte)0x88,
                          0x15,(byte)0xd3,0xa,(byte)0xe3 })
        .put(PPC64Registers.GPR6, // 0x34a847d84ac039eb
             new byte[] { 0x34,(byte)0xa8,0x47,(byte)0xd8,
                          0x4a,(byte)0xc0,0x39,(byte)0xeb })
        .put(PPC64Registers.GPR7, // 0xa6c244ccfc672fd1
             new byte[] { (byte)0xa6,(byte)0xc2,0x44,(byte)0xcc,
                          (byte)0xfc,0x67,0x2f,(byte)0xd1 })
        .put(PPC64Registers.GPR8, // 0x4e857fa76fae4610
             new byte[] { 0x4e,(byte)0x85,0x7f,(byte)0xa7,
                          0x6f,(byte)0xae,0x46,0x10 })
        .put(PPC64Registers.GPR9, // 0xfa6ecb942e56bdb1
             new byte[] { (byte)0xfa,0x6e,(byte)0xcb,(byte)0x94,
                          0x2e,0x56,(byte)0xbd,(byte)0xb1 })
        .put(PPC64Registers.GPR10, // 0xce40dcae99e51340
             new byte[] { (byte)0xce,0x40,(byte)0xdc,(byte)0xae,
                          (byte)0x99,(byte)0xe5,0x13,0x40 })
        .put(PPC64Registers.GPR11, // 0xd5e20897737372fa
             new byte[] { (byte)0xd5,(byte)0xe2,0x8,(byte)0x97,
                          0x73,0x73,0x72,(byte)0xfa })
	.put(PPC64Registers.GPR12, // 0x44da7341c8169fcb
             new byte[] { 0x44,(byte)0xda,0x73,0x41,
                          (byte)0xc8,0x16,(byte)0x9f,(byte)0xcb })
        .put(PPC64Registers.GPR13, // 0xef908c0f5ceb230f
             new byte[] { (byte)0xef,(byte)0x90,(byte)0x8c,0xf,
                          0x5c,(byte)0xeb,0x23,0xf })
        .put(PPC64Registers.GPR14, // 0xd6f64efaf644ba20
             new byte[] { (byte)0xd6,(byte)0xf6,0x4e,(byte)0xfa,
                          (byte)0xf6,0x44,(byte)0xba,0x20 })
        .put(PPC64Registers.GPR15, // 0xd5b5dd6910287bb3
             new byte[] { (byte)0xd5,(byte)0xb5,(byte)0xdd,0x69,
                          0x10,0x28,0x7b,(byte)0xb3 })
        .put(PPC64Registers.GPR16, // 0xce9381ebf6d51d50
             new byte[] { (byte)0xce,(byte)0x93,(byte)0x81,(byte)0xeb,
                          (byte)0xf6,(byte)0xd5,0x1d,0x50 })
        .put(PPC64Registers.GPR17, // 0xb3d21c30af96757c
             new byte[] { (byte)0xb3,(byte)0xd2,0x1c,0x30,
                          (byte)0xaf,(byte)0x96,0x75,0x7c })
        .put(PPC64Registers.GPR18, // 0x48ce58b2c1242f3
             new byte[] { 0x4,(byte)0x8c,(byte)0xe5,(byte)0x8b,
                          0x2c,0x12,0x42,(byte)0xf3 })
        .put(PPC64Registers.GPR19, // 0x5031020316f4a712
             new byte[] { 0x50,0x31,0x2,0x3,
                          0x16,(byte)0xf4,(byte)0xa7,0x12 })
        .put(PPC64Registers.GPR20, // 0xe5a6446b480c1bcb
             new byte[] { (byte)0xe5,(byte)0xa6,0x44,0x6b,
                          0x48,0xc,0x1b,(byte)0xcb })
        .put(PPC64Registers.GPR21, // 0x39d366cce0933e0c
             new byte[] { 0x39,(byte)0xd3,0x66,(byte)0xcc,
                          (byte)0xe0,(byte)0x93,0x3e,0xc })
        .put(PPC64Registers.GPR22, // 0xf86103d6cd223af7
             new byte[] { (byte)0xf8,0x61,0x3,(byte)0xd6,
                          (byte)0xcd,0x22,0x3a,(byte)0xf7 })
        .put(PPC64Registers.GPR23, // 0xcab98ee45bb9a68f
             new byte[] { (byte)0xca,(byte)0xb9,(byte)0x8e,(byte)0xe4,
                          0x5b,(byte)0xb9,(byte)0xa6,(byte)0x8f })
        .put(PPC64Registers.GPR24, // 0x34195aae274630f9
             new byte[] { 0x34,0x19,0x5a,(byte)0xae,
                          0x27,0x46,0x30,(byte)0xf9 })
	.put(PPC64Registers.GPR25, // 0xd1df3bfceb05da1a
             new byte[] { (byte)0xd1,(byte)0xdf,0x3b,(byte)0xfc,
                          (byte)0xeb,0x5,(byte)0xda,0x1a })
        .put(PPC64Registers.GPR26, // 0xe408d863626b040a
             new byte[] { (byte)0xe4,0x8,(byte)0xd8,0x63,
                          0x62,0x6b,0x4,0xa })
        .put(PPC64Registers.GPR27, // 0xb847f6c562a62676
             new byte[] { (byte)0xb8,0x47,(byte)0xf6,(byte)0xc5,
                          0x62,(byte)0xa6,0x26,0x76 })
        .put(PPC64Registers.GPR28, // 0x44508793aa174c36
             new byte[] { 0x44,0x50,(byte)0x87,(byte)0x93,
                          (byte)0xaa,0x17,0x4c,0x36 })
        .put(PPC64Registers.GPR29, // 0x3c21d5f786149c80
             new byte[] { 0x3c,0x21,(byte)0xd5,(byte)0xf7,
                          (byte)0x86,0x14,(byte)0x9c,(byte)0x80 })
        .put(PPC64Registers.GPR30, // 0x9871b47e31368590
             new byte[] { (byte)0x98,0x71,(byte)0xb4,0x7e,
                          0x31,0x36,(byte)0x85,(byte)0x90 })
        .put(PPC64Registers.GPR31, // 0xd5a767e17d453bef
             new byte[] { (byte)0xd5,(byte)0xa7,0x67,(byte)0xe1,
                          0x7d,0x45,0x3b,(byte)0xef })
        .put(PPC64Registers.FPR0, // 0x75f98ca5ea9d4622
             new byte[] { 0x75,(byte)0xf9,(byte)0x8c,(byte)0xa5,
                          (byte)0xea,(byte)0x9d,0x46,0x22 })
        .put(PPC64Registers.FPR1, // 0x2053c189e7aa9ade
             new byte[] { 0x20,0x53,(byte)0xc1,(byte)0x89,
                          (byte)0xe7,(byte)0xaa,(byte)0x9a,(byte)0xde })
        .put(PPC64Registers.FPR2, // 0x3df09c8307b6fc56
             new byte[] { 0x3d,(byte)0xf0,(byte)0x9c,(byte)0x83,
                          0x7,(byte)0xb6,(byte)0xfc,0x56 })
        .put(PPC64Registers.FPR3, // 0xb3b95a2e6fbe9bf0
             new byte[] { (byte)0xb3,(byte)0xb9,0x5a,0x2e,
                          0x6f,(byte)0xbe,(byte)0x9b,(byte)0xf0 })
        .put(PPC64Registers.FPR4, // 0x29c916902e8c7c07
             new byte[] { 0x29,(byte)0xc9,0x16,(byte)0x90,
                          0x2e,(byte)0x8c,0x7c,0x7 })
	.put(PPC64Registers.FPR5, // 0xe74ecdc530dc7b77
             new byte[] { (byte)0xe7,0x4e,(byte)0xcd,(byte)0xc5,
                          0x30,(byte)0xdc,0x7b,0x77 })
        .put(PPC64Registers.FPR6, // 0x90505e84187b206b
             new byte[] { (byte)0x90,0x50,0x5e,(byte)0x84,
                          0x18,0x7b,0x20,0x6b })
        .put(PPC64Registers.FPR7, // 0x6e5ac9b69dad5852
             new byte[] { 0x6e,0x5a,(byte)0xc9,(byte)0xb6,
                          (byte)0x9d,(byte)0xad,0x58,0x52 })
        .put(PPC64Registers.FPR8, // 0x20ed2a356201d3d1
             new byte[] { 0x20,(byte)0xed,0x2a,0x35,
                          0x62,0x1,(byte)0xd3,(byte)0xd1 })
        .put(PPC64Registers.FPR9, // 0xec7ba6b6d82d2859
             new byte[] { (byte)0xec,0x7b,(byte)0xa6,(byte)0xb6,
                          (byte)0xd8,0x2d,0x28,0x59 })
        .put(PPC64Registers.FPR10, // 0x1908e830b54da771
             new byte[] { 0x19,0x8,(byte)0xe8,0x30,
                          (byte)0xb5,0x4d,(byte)0xa7,0x71 })
        .put(PPC64Registers.FPR11, // 0xba399d517cee2bb3
             new byte[] { (byte)0xba,0x39,(byte)0x9d,0x51,
                          0x7c,(byte)0xee,0x2b,(byte)0xb3 })
        .put(PPC64Registers.FPR12, // 0x601c9bf3dac1541
             new byte[] { 0x6,0x1,(byte)0xc9,(byte)0xbf,
                          0x3d,(byte)0xac,0x15,0x41 })
        .put(PPC64Registers.FPR13, // 0x42ef875526d26ac7
             new byte[] { 0x42,(byte)0xef,(byte)0x87,0x55,
                          0x26,(byte)0xd2,0x6a,(byte)0xc7 })
        .put(PPC64Registers.FPR14, // 0x1dd06c1c9132b4cb
             new byte[] { 0x1d,(byte)0xd0,0x6c,0x1c,
                          (byte)0x91,0x32,(byte)0xb4,(byte)0xcb })
        .put(PPC64Registers.FPR15, // 0xc9b957b3ef59adf5
             new byte[] { (byte)0xc9,(byte)0xb9,0x57,(byte)0xb3,
                          (byte)0xef,0x59,(byte)0xad,(byte)0xf5 })
        .put(PPC64Registers.FPR16, // 0xf3c555504f10ef96
             new byte[] { (byte)0xf3,(byte)0xc5,0x55,0x50,
                          0x4f,0x10,(byte)0xef,(byte)0x96 })
        .put(PPC64Registers.FPR17, // 0x48480daa25666424
             new byte[] { 0x48,0x48,0xd,(byte)0xaa,
                          0x25,0x66,0x64,0x24 })
	.put(PPC64Registers.FPR18, // 0x6008875d4a373061
             new byte[] { 0x60,0x8,(byte)0x87,0x5d,
                          0x4a,0x37,0x30,0x61 })
        .put(PPC64Registers.FPR19, // 0xd11d698728ceab86
             new byte[] { (byte)0xd1,0x1d,0x69,(byte)0x87,
                          0x28,(byte)0xce,(byte)0xab,(byte)0x86 })
        .put(PPC64Registers.FPR20, // 0x84d123c349f2468e
             new byte[] { (byte)0x84,(byte)0xd1,0x23,(byte)0xc3,
                          0x49,(byte)0xf2,0x46,(byte)0x8e })
        .put(PPC64Registers.FPR21, // 0xa45fc9517343163
             new byte[] { 0xa,0x45,(byte)0xfc,(byte)0x95,
                          0x17,0x34,0x31,0x63 })
        .put(PPC64Registers.FPR22, // 0x19a621d6f7d8e7b9
             new byte[] { 0x19,(byte)0xa6,0x21,(byte)0xd6,
                          (byte)0xf7,(byte)0xd8,(byte)0xe7,(byte)0xb9 })
        .put(PPC64Registers.FPR23, // 0xd45b91ceda65de9e
             new byte[] { (byte)0xd4,0x5b,(byte)0x91,(byte)0xce,
                          (byte)0xda,0x65,(byte)0xde,(byte)0x9e })
        .put(PPC64Registers.FPR24, // 0xb02e4044d6802506
             new byte[] { (byte)0xb0,0x2e,0x40,0x44,
                          (byte)0xd6,(byte)0x80,0x25,0x6 })
        .put(PPC64Registers.FPR25, // 0x427411aff7c4766
             new byte[] { 0x4,0x27,0x41,0x1a,
                          (byte)0xff,0x7c,0x47,0x66 })
        .put(PPC64Registers.FPR26, // 0x4f5b955b70217bf5
             new byte[] { 0x4f,0x5b,(byte)0x95,0x5b,
                          0x70,0x21,0x7b,(byte)0xf5 })
        .put(PPC64Registers.FPR27, // 0xeddc0c73bb1e8699
             new byte[] { (byte)0xed,(byte)0xdc,0xc,0x73,
                          (byte)0xbb,0x1e,(byte)0x86,(byte)0x99 })
        .put(PPC64Registers.FPR28, // 0x472c0c5969c0af0a
             new byte[] { 0x47,0x2c,0xc,0x59,
                          0x69,(byte)0xc0,(byte)0xaf,0xa })
        .put(PPC64Registers.FPR29, // 0x19c66012a715212
             new byte[] { 0x1,(byte)0x9c,0x66,0x1,
                          0x2a,0x71,0x52,0x12 })
        .put(PPC64Registers.FPR30, // 0xfaf368738bd3cec4
             new byte[] { (byte)0xfa,(byte)0xf3,0x68,0x73,
                          (byte)0x8b,(byte)0xd3,(byte)0xce,(byte)0xc4 })
	.put(PPC64Registers.FPR31, // 0x5ee6e7418fe61e98
             new byte[] { 0x5e,(byte)0xe6,(byte)0xe7,0x41,
                          (byte)0x8f,(byte)0xe6,0x1e,(byte)0x98 })
        	.put(PPC64Registers.VR0, // 0x44be6c17f1b7a38cfd16c678bd309d01
	     new byte[] { 0x44,(byte)0xbe,0x6c,0x17,
			  (byte)0xf1,(byte)0xb7,(byte)0xa3,(byte)0x8c,
			  (byte)0xfd,0x16,(byte)0xc6,0x78,
			  (byte)0xbd,0x30,(byte)0x9d,0x1 })
	.put(PPC64Registers.VR1, // 0x4b8eacb677c1b42990652f9565244b3c
	     new byte[] { 0x4b,(byte)0x8e,(byte)0xac,(byte)0xb6,
			  0x77,(byte)0xc1,(byte)0xb4,0x29,
			  (byte)0x90,0x65,0x2f,(byte)0x95,
			  0x65,0x24,0x4b,0x3c })
	.put(PPC64Registers.VR2, // 0x7696c4b4a91b0758114622c33bd5fbaa
	     new byte[] { 0x76,(byte)0x96,(byte)0xc4,(byte)0xb4,
			  (byte)0xa9,0x1b,0x7,0x58,
			  0x11,0x46,0x22,(byte)0xc3,
			  0x3b,(byte)0xd5,(byte)0xfb,(byte)0xaa })
	.put(PPC64Registers.VR3, // 0x45a67a61b2ab6d7e51d42597b4582196
	     new byte[] { 0x45,(byte)0xa6,0x7a,0x61,
			  (byte)0xb2,(byte)0xab,0x6d,0x7e,
			  0x51,(byte)0xd4,0x25,(byte)0x97,
			  (byte)0xb4,0x58,0x21,(byte)0x96 })
	.put(PPC64Registers.VR4, // 0x58fc57e626059d0facdf5aa4b0d61f65
	     new byte[] { 0x58,(byte)0xfc,0x57,(byte)0xe6,
			  0x26,0x5,(byte)0x9d,0xf,
			  (byte)0xac,(byte)0xdf,0x5a,(byte)0xa4,
			  (byte)0xb0,(byte)0xd6,0x1f,0x65 })
	.put(PPC64Registers.VR5, // 0xccea5f417e175fa0307ad03de0a7746a
	     new byte[] { (byte)0xcc,(byte)0xea,0x5f,0x41,
			  0x7e,0x17,0x5f,(byte)0xa0,
			  0x30,0x7a,(byte)0xd0,0x3d,
			  (byte)0xe0,(byte)0xa7,0x74,0x6a })
	.put(PPC64Registers.VR6, // 0x98b48dbbd61358bf24d173da255dc92
	     new byte[] { 0x9,(byte)0x8b,0x48,(byte)0xdb,
			  (byte)0xbd,0x61,0x35,(byte)0x8b,
			  (byte)0xf2,0x4d,0x17,0x3d,
			  (byte)0xa2,0x55,(byte)0xdc,(byte)0x92 })
	.put(PPC64Registers.VR7, // 0xfe4405a9d77354f09a0d716f95a5d414
	     new byte[] { (byte)0xfe,0x44,0x5,(byte)0xa9,
			  (byte)0xd7,0x73,0x54,(byte)0xf0,
			  (byte)0x9a,0xd,0x71,0x6f,
			  (byte)0x95,(byte)0xa5,(byte)0xd4,0x14 })
	.put(PPC64Registers.VR8, // 0xea387d2fc70f557bc829a16ed05fffcb
	     new byte[] { (byte)0xea,0x38,0x7d,0x2f,
			  (byte)0xc7,0xf,0x55,0x7b,
			  (byte)0xc8,0x29,(byte)0xa1,0x6e,
			  (byte)0xd0,0x5f,(byte)0xff,(byte)0xcb })
	.put(PPC64Registers.VR9, // 0x4a148db5d37938f316475f6a5987902b
	     new byte[] { 0x4a,0x14,(byte)0x8d,(byte)0xb5,
			  (byte)0xd3,0x79,0x38,(byte)0xf3,
			  0x16,0x47,0x5f,0x6a,
			  0x59,(byte)0x87,(byte)0x90,0x2b })
	.put(PPC64Registers.VR10, // 0xe21f644b9212debe96fa30656aadf11c
	     new byte[] { (byte)0xe2,0x1f,0x64,0x4b,
			  (byte)0x92,0x12,(byte)0xde,(byte)0xbe,
			  (byte)0x96,(byte)0xfa,0x30,0x65,
			  0x6a,(byte)0xad,(byte)0xf1,0x1c })
	.put(PPC64Registers.VR11, // 0x463d137bf4085c18677c0d0871339086
	     new byte[] { 0x46,0x3d,0x13,0x7b,
			  (byte)0xf4,0x8,0x5c,0x18,
			  0x67,0x7c,0xd,0x8,
			  0x71,0x33,(byte)0x90,(byte)0x86 })
	.put(PPC64Registers.VR12, // 0xea0b393faeecfde76286aff0285b4b1d
	     new byte[] { (byte)0xea,0xb,0x39,0x3f,
			  (byte)0xae,(byte)0xec,(byte)0xfd,(byte)0xe7,
			  0x62,(byte)0x86,(byte)0xaf,(byte)0xf0,
			  0x28,0x5b,0x4b,0x1d })
	.put(PPC64Registers.VR13, // 0x8d0248447bcea6b7bff23edc456d26ca
	     new byte[] { (byte)0x8d,0x2,0x48,0x44,
			  0x7b,(byte)0xce,(byte)0xa6,(byte)0xb7,
			  (byte)0xbf,(byte)0xf2,0x3e,(byte)0xdc,
			  0x45,0x6d,0x26,(byte)0xca })
	.put(PPC64Registers.VR14, // 0xb18799b9d5e3a6ff146df3c01257fc40
	     new byte[] { (byte)0xb1,(byte)0x87,(byte)0x99,(byte)0xb9,
			  (byte)0xd5,(byte)0xe3,(byte)0xa6,(byte)0xff,
			  0x14,0x6d,(byte)0xf3,(byte)0xc0,
			  0x12,0x57,(byte)0xfc,0x40 })
	.put(PPC64Registers.VR15, // 0x3797881ded87e87a1e2336fb8fea1f30
	     new byte[] { 0x37,(byte)0x97,(byte)0x88,0x1d,
			  (byte)0xed,(byte)0x87,(byte)0xe8,0x7a,
			  0x1e,0x23,0x36,(byte)0xfb,
			  (byte)0x8f,(byte)0xea,0x1f,0x30 })
	.put(PPC64Registers.VR16, // 0xfb896ec237ed9ed15ee77f2ae5f526d
	     new byte[] { 0xf,(byte)0xb8,(byte)0x96,(byte)0xec,
			  0x23,0x7e,(byte)0xd9,(byte)0xed,
			  0x15,(byte)0xee,0x77,(byte)0xf2,
			  (byte)0xae,0x5f,0x52,0x6d })
	.put(PPC64Registers.VR17, // 0x521681e7987629fd3bbf188b1b79cfd2
	     new byte[] { 0x52,0x16,(byte)0x81,(byte)0xe7,
			  (byte)0x98,0x76,0x29,(byte)0xfd,
			  0x3b,(byte)0xbf,0x18,(byte)0x8b,
			  0x1b,0x79,(byte)0xcf,(byte)0xd2 })
	.put(PPC64Registers.VR18, // 0x5bc0ea0e9657f1d8f766a875232164d
	     new byte[] { 0x5,(byte)0xbc,0xe,(byte)0xa0,
			  (byte)0xe9,0x65,0x7f,0x1d,
			  (byte)0x8f,0x76,0x6a,(byte)0x87,
			  0x52,0x32,0x16,0x4d })
	.put(PPC64Registers.VR19, // 0x214c3b64a0a95653126bb7a400fd6c3
	     new byte[] { 0x2,0x14,(byte)0xc3,(byte)0xb6,
			  0x4a,0xa,(byte)0x95,0x65,
			  0x31,0x26,(byte)0xbb,0x7a,
			  0x40,0xf,(byte)0xd6,(byte)0xc3 })
	.put(PPC64Registers.VR20, // 0x954e02b26679024890485b20e23f9171
	     new byte[] { (byte)0x95,0x4e,0x2,(byte)0xb2,
			  0x66,0x79,0x2,0x48,
			  (byte)0x90,0x48,0x5b,0x20,
			  (byte)0xe2,0x3f,(byte)0x91,0x71 })
	.put(PPC64Registers.VR21, // 0x1226fc3704346199290bb58af16deb1d
	     new byte[] { 0x12,0x26,(byte)0xfc,0x37,
			  0x4,0x34,0x61,(byte)0x99,
			  0x29,0xb,(byte)0xb5,(byte)0x8a,
			  (byte)0xf1,0x6d,(byte)0xeb,0x1d })
	.put(PPC64Registers.VR22, // 0xa0ef40317f5b800cc97f362c3c9a24a0
	     new byte[] { (byte)0xa0,(byte)0xef,0x40,0x31,
			  0x7f,0x5b,(byte)0x80,0xc,
			  (byte)0xc9,0x7f,0x36,0x2c,
			  0x3c,(byte)0x9a,0x24,(byte)0xa0 })
	.put(PPC64Registers.VR23, // 0xbf06116762655604be9ed8790e30d652
	     new byte[] { (byte)0xbf,0x6,0x11,0x67,
			  0x62,0x65,0x56,0x4,
			  (byte)0xbe,(byte)0x9e,(byte)0xd8,0x79,
			  0xe,0x30,(byte)0xd6,0x52 })
	.put(PPC64Registers.VR24, // 0x1f6182f56994c80c34a2505c7ee93f08
	     new byte[] { 0x1f,0x61,(byte)0x82,(byte)0xf5,
			  0x69,(byte)0x94,(byte)0xc8,0xc,
			  0x34,(byte)0xa2,0x50,0x5c,
			  0x7e,(byte)0xe9,0x3f,0x8 })
	.put(PPC64Registers.VR25, // 0x68225bcb834745faeee567033c6f750f
	     new byte[] { 0x68,0x22,0x5b,(byte)0xcb,
			  (byte)0x83,0x47,0x45,(byte)0xfa,
			  (byte)0xee,(byte)0xe5,0x67,0x3,
			  0x3c,0x6f,0x75,0xf })
	.put(PPC64Registers.VR26, // 0x6596dcddd93ddbf55492cf8184147889
	     new byte[] { 0x65,(byte)0x96,(byte)0xdc,(byte)0xdd,
			  (byte)0xd9,0x3d,(byte)0xdb,(byte)0xf5,
			  0x54,(byte)0x92,(byte)0xcf,(byte)0x81,
			  (byte)0x84,0x14,0x78,(byte)0x89 })
	.put(PPC64Registers.VR27, // 0xef4dd08222103314c388b9fd0c466d4c
	     new byte[] { (byte)0xef,0x4d,(byte)0xd0,(byte)0x82,
			  0x22,0x10,0x33,0x14,
			  (byte)0xc3,(byte)0x88,(byte)0xb9,(byte)0xfd,
			  0xc,0x46,0x6d,0x4c })
	.put(PPC64Registers.VR28, // 0xb67426e81b4bc303da89a6b848eb7870
	     new byte[] { (byte)0xb6,0x74,0x26,(byte)0xe8,
			  0x1b,0x4b,(byte)0xc3,0x3,
			  (byte)0xda,(byte)0x89,(byte)0xa6,(byte)0xb8,
			  0x48,(byte)0xeb,0x78,0x70 })
	.put(PPC64Registers.VR29, // 0x195d48ca281de73c7d559ad4a6746d70
	     new byte[] { 0x19,0x5d,0x48,(byte)0xca,
			  0x28,0x1d,(byte)0xe7,0x3c,
			  0x7d,0x55,(byte)0x9a,(byte)0xd4,
			  (byte)0xa6,0x74,0x6d,0x70 })
	.put(PPC64Registers.VR30, // 0xe91cb114b1caf3e4e9f95bfee49b8948
	     new byte[] { (byte)0xe9,0x1c,(byte)0xb1,0x14,
			  (byte)0xb1,(byte)0xca,(byte)0xf3,(byte)0xe4,
			  (byte)0xe9,(byte)0xf9,0x5b,(byte)0xfe,
			  (byte)0xe4,(byte)0x9b,(byte)0x89,0x48 })
	.put(PPC64Registers.VR31, // 0x7ea2a6afa222b34fec61791416397ac
	     new byte[] { 0x7,(byte)0xea,0x2a,0x6a,
			  (byte)0xfa,0x22,0x2b,0x34,
			  (byte)0xfe,(byte)0xc6,0x17,(byte)0x91,
			  0x41,0x63,(byte)0x97,(byte)0xac })
	;
    private final ISAMap isaValues = new ISAMap("RegsCase")
	.put(ISA.IA32, IA32)
	.put(ISA.X8664, X8664)
	.put(ISA.PPC32BE, PPC32)
	.put(ISA.PPC64BE, PPC64)
	;
}
