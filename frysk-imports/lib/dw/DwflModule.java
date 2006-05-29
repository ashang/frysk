package lib.dw;

public class DwflModule {
	
	private long pointer;
	
	protected DwflModule(long val){
		this.pointer = val;
	}
	
//	public String getName(){
//		return dwfl_module_info_getname();
//	}
	
	protected long getPointer(){
		return pointer;
	}
	
//	protected native String dwfl_module_info_getname();
}
