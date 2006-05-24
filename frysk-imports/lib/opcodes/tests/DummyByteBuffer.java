package lib.opcodes.tests;

import inua.eio.ByteBuffer;

public class DummyByteBuffer extends ByteBuffer {

	private byte[] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3, 4, 5, 6, 7};
	
	public DummyByteBuffer(){
		super(0, 8);
	}
	
	protected int peek(long caret) {
		if(caret < bytes.length && caret > 0)
			return bytes[(int) caret];
		
		return -1;
	}

	protected void poke(long caret, int val) {
		if(caret < bytes.length && caret > 0)
			bytes[(int) caret] = (byte) val;
	}

}
