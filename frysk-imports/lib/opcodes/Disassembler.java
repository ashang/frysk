package lib.opcodes;

import java.util.LinkedList;

import inua.eio.ByteBuffer;

public final class Disassembler {
	
	protected ByteBuffer buffer;
	
	private LinkedList isnList;
	private Instruction current;
	
	public Disassembler(ByteBuffer buffer){
		this.buffer = buffer;
	}
	
	public LinkedList disassembleWords(long address, long words) throws OpcodesException{
		this.isnList = new LinkedList();
		disassemble(address, words);
		return isnList;
	}
	
	// THIS SHOULD NOT BE CALLED FROM JAVA!
	protected void setCurrentAddress(long address){
		current.address = address;
	}
	
	// THIS SHOULD NOT BE CALLED FROM JAVA!
	protected void setCurrentInstruction(String inst){
		current.instruction = inst;
	}
	
	// THIS SHOULD NOT BE CALLED FROM JAVA!
	protected void moveToNext(){
		this.isnList.add(this.current);
		this.current = new Instruction();
	}
	
	private native void disassemble(long address, long count);
}
