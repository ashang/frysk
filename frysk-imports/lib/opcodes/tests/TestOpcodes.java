package lib.opcodes.tests;

import inua.eio.ByteBuffer;

import java.util.LinkedList;

import junit.framework.TestCase;
import lib.opcodes.Disassembler;
import lib.opcodes.Instruction;
import lib.opcodes.OpcodesException;

public class TestOpcodes extends TestCase {

	/*
	 * Note: this test is expected to fail on anything but i386 for the time being.
	 * TODO: come up with a way of doing the correct assertEquals for other archs
	 */
	public void testDisassembler(){
		ByteBuffer buffer = new DummyByteBuffer();
		final int numInstructions = 16;
		
		Disassembler disAsm = new Disassembler(buffer);
		LinkedList list = null;
		try{
			list = disAsm.disassembleInstructions(0, numInstructions);
		}
		catch(OpcodesException e){
			e.printStackTrace(System.err);
			fail("Exception thrown during disassembly");
		}
		
		assertNotNull(list);
		assertEquals(list.size(), numInstructions);
		
		String[] addrs = {"0", "2", "4", "6", "7", "8", "a", "c", "e", "f",
				"10", "12", "14", "16", "17", "18"};
		String[] insts = {"DWORD PTR [ecx]", "BYTE PTR [ebx]", "0x5", "es", "es", "al",
				"BYTE PTR [ebx]", "0x5", "es", "es", "al",
				"BYTE PTR [ebx]", "0x5", "es", "es", "al"};
		
		for(int i = 0; i < list.size(); i++){
			Instruction inst = (Instruction) list.get(i);
			assertNotNull(inst);
			
			assertEquals(addrs[i], Long.toHexString(inst.address));
			assertEquals(insts[i], inst.instruction);
		}
	}
}
