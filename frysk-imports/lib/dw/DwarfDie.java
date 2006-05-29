package lib.dw;

public class DwarfDie {

	private long pointer;
	
	protected DwarfDie(long pointer){
		this.pointer = pointer;
	}
	
//	public DwarfDie getContainingCompilationUnit(){
//		long val = dwarf_diecu();
//		if(val == 0)
//			return null;
//		
//		return new DwarfDie(val);
//	}
	
	protected long getPointer(){
		return this.pointer;
	}
	
//	protected native long dwarf_diecu();
	
}
