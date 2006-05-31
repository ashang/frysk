package lib.dw;

public class DwflModule {
	
	private long pointer;
	private Dwfl parent;
	
	protected DwflModule(long val, Dwfl parent){
		this.pointer = val;
		this.parent = parent;
	}
	
//	public String getName(){
//		return dwfl_module_info_getname();
//	}
	
	protected long getPointer(){
		return pointer;
	}
	
	protected Dwfl getParent(){
		return this.parent;
	}
	
//	protected native String dwfl_module_info_getname();
}
