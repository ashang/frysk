package lib.opcodes.tests;

import inua.eio.ByteBuffer;

import java.util.LinkedList;

import junit.framework.TestCase;
import lib.opcodes.Disassembler;
import lib.opcodes.Instruction;
import lib.opcodes.OpcodesException;

public class TestOpcodes extends TestCase {

	public void testDisassembler(){
		ByteBuffer buffer = new DummyByteBuffer();
		final int numInstructions = 8;
		
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
		
		for(int i = 0; i < list.size(); i++){
			Instruction inst = (Instruction) list.get(i);
			assertNotNull(inst);
		}
	}
}
