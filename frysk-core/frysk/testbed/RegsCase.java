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
import frysk.isa.ISA;
import frysk.isa.ISAMap;
import frysk.proc.Task;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.math.BigInteger;
import inua.eio.ByteOrder;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A base class for test-cases that need to check all register values.
 *
 * For instance, both frysk.proc.Task.getRegister* and
 * frysk.stack.Frame.getRegister.
 */

public abstract class RegsCase extends TestLib {

    private static final Logger logger = Logger.getLogger("frysk");

    private Object taskObject;
    private ValueMap values;
    private ByteOrder order;
    private ISA isa;
    public void setUp() {
	super.setUp();
	Task task = new DaemonBlockedAtSignal("funit-regs").getMainTask();
	taskObject = taskObject(task);
	isa = task.getISA();
	order = isa.order();
	if (isaValues.containsKey(isa))
	    values = (ValueMap)isaValues.get(isa);
    }
    public void tearDown() {
	taskObject = null;
	values = null;
	order = null;
	super.tearDown();
    }

    // Package private.
    LinkedHashMap values() {
	return values;
    }

    // Package private.
    ISA isa() {
	return isa;
    }

    protected abstract Object taskObject(Task task);
    protected abstract void accessRegister(Object task,
					   Register reg, int offset,
					   int length, byte[] bytes,
					   int start, boolean write);
    protected abstract long getRegister(Object task, Register reg);

    private void checkRegisterRead(Register register, Object value) {
	if (value == null)
	    return;
	String name = register.getName();
	// When the compare fails, BigInteteger gives a more
	// meaningful message.
	BigInteger correct;
	if (value instanceof BigInteger) {
	    correct = (BigInteger)value;
	} else {
	    correct = toBigInteger((byte[])value);
	}
	logger.log(Level.FINE, "checking register {0} expected {1}",
		   new Object[] { name, correct });
	byte[] bytes = new byte[register.getType().getSize()];
	accessRegister(taskObject, register, 0, bytes.length,
		       bytes, 0, false);
	BigInteger check = toBigInteger(bytes);
	assertEquals(name, correct, check);
    }

    public void testAccessRegisterRead() {
	if (unresolved(5107))
	    return;
	if (values == null && unresolved(0))
	    return;
	for (Iterator i = values.entrySet().iterator(); i.hasNext(); ) {
	    Entry entry = (Entry)i.next();
	    checkRegisterRead((Register)entry.getKey(), entry.getValue());
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

    private static class ValueMap extends LinkedHashMap {
	static final long serialVersionUID = 0;
	// Add a chaining put method.
	ValueMap put(Register r, Object o) {
	    put((Object)r, o);
	    return this;
	}
    }

    /**
     * The registers should all be compared against non-zero ramdom
     * byte[] arrays, and not simple constants.
     *
     * For exceptions, such as PC or STATUS, which can't be set,
     * specify either NULL (skip check) or BigInteger (weaker check).
     */

    private ValueMap IA32 = new ValueMap()
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
        .put(IA32Registers.EFLAGS, BigInteger.valueOf(0x10246L))
        .put(IA32Registers.ESP, // 0x93d4a6ed
             new byte[] { (byte)0xed, (byte)0xa6, (byte)0xd4, (byte)0x93 })
 	.put(IA32Registers.EIP, null)
	;

    private final ISAMap isaValues = new ISAMap("RegsCase")
	.put(ISA.IA32, IA32)
	;
}
