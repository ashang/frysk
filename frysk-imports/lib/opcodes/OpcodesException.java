package lib.opcodes;

public class OpcodesException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2287257007740954049L;

	public OpcodesException(String s, int status, long address) {
		super(s + " Status #" + status + ", at address 0x" + Long.toHexString(address));
		// TODO Auto-generated constructor stub
	}
	
	public OpcodesException(String s){
		super(s);
	}


}
