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

import java.util.Map.Entry;
import frysk.isa.Register;
import frysk.isa.IA32Registers;
import frysk.isa.X8664Registers;
import frysk.isa.PPC32Registers;
import frysk.isa.PPC64Registers;
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
    public void setUp() {
	super.setUp();
	task = new DaemonBlockedAtSignal("funit-regs").getMainTask();
	isa = task.getISA();
	order = isa.order();
	if (isaValues.containsKey(isa))
	    values = (Values)isaValues.get(isa);
    }
    public void tearDown() {
	task = null;
	values = null;
	super.tearDown();
    }

    protected Task task() {
	return task;
    }

    // Package-private
    Values values() {
	return values;
    }
    ISA isa() {
	return isa;
    }

    protected abstract void access(Register register,
				   int offset, int length, byte[] bytes,
				   int start, boolean write);
    protected abstract long getRegister(Object task, Register reg);

    public void testAccessRegisterRead() {
	if (values == null && unresolved(0))
	    // If there are no values, skip the test, other code will
	    // complain.
	    return;
	for (Iterator i = values.iterator(); i.hasNext(); ) {
	    Entry entry = (Entry)i.next();
	    Register register = (Register)entry.getKey();
	    Value value = (Value)entry.getValue();
	    value.checkRegister(this, register);
	}
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
	     1 << 21 // Mask CPUID
	     )
        .put(IA32Registers.ESP, // 0x93d4a6ed
             new byte[] { (byte)0xed, (byte)0xa6, (byte)0xd4, (byte)0x93 })
 	.put(IA32Registers.EIP, "crash")
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
