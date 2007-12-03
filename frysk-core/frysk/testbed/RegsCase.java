// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

import frysk.isa.RegistersFactory;
import frysk.isa.Registers;
import frysk.isa.RegisterGroup;
import frysk.isa.Register;
import frysk.isa.IA32Registers;
import frysk.isa.X8664Registers;
import frysk.isa.PPC32Registers;
import frysk.isa.PPC64Registers;
import frysk.isa.X87Registers;
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
		RegsCase.assertTrue(register.getName() + "[" + i + "] != 0",
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
        .put(X87Registers.FSW, // 0x47e4
             new byte[] { (byte)0xe4,0x47 })
        .put(X87Registers.FTW, // 0xc9
             new byte[] { (byte)0xc9 })
        .put(X87Registers.FOP, // 0x1e8f
             new byte[] { (byte)0x8f,0x1e })
        .put(X87Registers.EIP, // 0x2fc38c68
             new byte[] { 0x68,(byte)0x8c,(byte)0xc3,0x2f })
        .put(X87Registers.CS, // 0x7ac9
             new byte[] { (byte)0xc9,0x7a })
        .put(X87Registers.DP, // 0x6d77e6d5
             new byte[] { (byte)0xd5,(byte)0xe6,0x77,0x6d })
        .put(X87Registers.DS, // 0x2a9f
             new byte[] { (byte)0x9f,0x2a })
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
        .put(X87Registers.FSW, // 0x47e4
             new byte[] { (byte)0xe4,0x47 })
        .put(X87Registers.FTW, // 0xc9
             new byte[] { (byte)0xc9 })
        .put(X87Registers.FOP, // 0x1e8f
             new byte[] { (byte)0x8f,0x1e })
        .put(X87Registers.RIP, // 0x99af236679d5eeff
             new byte[] { (byte)0xff,(byte)0xee,(byte)0xd5,0x79,
                          0x66,0x23,(byte)0xaf,(byte)0x99 })
        .put(X87Registers.RDP, // 0x6988a565d0acd7b0
             new byte[] { (byte)0xb0,(byte)0xd7,(byte)0xac,(byte)0xd0,
                          0x65,(byte)0xa5,(byte)0x88,0x69 })
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
        .put(PPC32Registers.GPR0, 
             new byte[] { (byte)0xa3,(byte)0x9c,0x20,(byte)0x08 })
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
        .put(PPC64Registers.GPR0, // 0x837bb4e2d8209ca3
             new byte[] { (byte)0xa3,(byte)0x9c,0x20,(byte)0xd8,
                          (byte)0xe2,(byte)0xb4,0x7b,(byte)0x83 })
	;

    private final ISAMap isaValues = new ISAMap("RegsCase")
	.put(ISA.IA32, IA32)
	.put(ISA.X8664, X8664)
	.put(ISA.PPC32BE, PPC32)
	.put(ISA.PPC64BE, PPC64)
	;
}
