package lib.dw;

import gnu.gcj.RawDataManaged;

public class Dwfl {
	
	private long pointer;
	protected RawDataManaged callbacks;
	
	public Dwfl(int pid){
		dwfl_begin(pid);
	}
	
	protected Dwfl(long pointer){
		this.pointer = pointer;
	}
	
//	public DwflModule[] getModules(){
//		long[] vals = dwfl_get_modules();
//		if(vals == null || vals.length == 0)
//			return new DwflModule[0];
//		
//		DwflModule[] modules = new DwflModule[vals.length];
//		for(int i = 0; i < vals.length; i++){
//			if(vals[i] == 0)
//				modules[i] = null;
//			else
//				modules[i] = new DwflModule(vals[i]);
//		}
//		
//		return modules;
//	}
//	
//	public Dwarf[] getModuleDwarfs(){
//		long[] vals = dwfl_getdwarf();
//		if(vals == null || vals.length == 0)
//			return new Dwarf[0];
//		
//		Dwarf[] dwarfs = new Dwarf[vals.length];
//		for(int i = 0; i < vals.length; i++){
//			if(vals[i] == 0)
//				dwarfs[i] = null;
//			else
//				dwarfs[i] = new Dwarf(vals[i]);
//		}
//		
//		return dwarfs;
//	}
	
	public DwflLine getSourceLine(long addr){
		long val = dwfl_getsrc(addr);
		if(val == 0)
			return null;
		
		return new DwflLine(val, this);
	}
	
	protected long getPointer(){
		return pointer;
	}
	
	protected void finalize(){
		dwfl_end();
	}
	
	protected native void dwfl_begin(int pid);
	protected native void dwfl_end();
//	protected native long[] dwfl_get_modules();
//	protected native long[] dwfl_getdwarf();
	protected native long dwfl_getsrc(long addr);
}
