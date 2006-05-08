package lib.elf;

public abstract class ElfEHeader {
	
	private long pointer;
	
	protected ElfEHeader(long ptr){
		this.pointer = ptr;
	}
	
	protected long getPointer(){
		return this.pointer;
	}
	
	public String getItentifier(){
		return get_e_ident();
	}
	
	public int getType(){
		return get_e_type();
	}
	
	public int getArchitecture(){
		return get_e_machine();
	}
	
	public long getVersion(){
		return get_e_version();
	}
	
	public long getEntryPoint(){
		return get_e_entry();
	}
	
	public long getProgramHeaderOffset(){
		return get_e_phoff();
	}
	
	public long getSectionHeaderOffset(){
		return get_e_shoff();
	}
	
	public long getFlags(){
		return get_e_flags();
	}
	
	public int getELFHeaderSize(){
		return get_e_ehsize();
	}
	
	public int getProgramHeaderEntrySize(){
		return get_e_phentsize();
	}
	
	public int getProgramHeaderEntryCount(){
		return get_e_phnum();
	}
	
	public int getSectionHeaderEntrySize(){
		return get_e_shentsize();
	}
	
	public int getSectionHeaderEntryCount(){
		return get_e_shnum();
	}
	
	public int getSectionHeaderStringTableIndex(){
		return get_e_shstrndx();
	}

	protected abstract String get_e_ident();
	protected abstract int get_e_type();
	protected abstract int get_e_machine();
	protected abstract long get_e_version();
	protected abstract long get_e_entry();
	protected abstract long get_e_phoff();
	protected abstract long get_e_shoff();
	protected abstract long get_e_flags();
	protected abstract int get_e_ehsize();
	protected abstract int get_e_phentsize();
	protected abstract int get_e_phnum();
	protected abstract int get_e_shentsize();
	protected abstract int get_e_shnum();
	protected abstract int get_e_shstrndx();
}
