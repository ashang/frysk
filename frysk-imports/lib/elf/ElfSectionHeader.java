package lib.elf;

public abstract class ElfSectionHeader {

	long pointer;
	
	protected ElfSectionHeader(long ptr){
		this.pointer = ptr;
	}
	
	protected long getPointer(){
		return this.pointer;
	}
	
	public long getNameIndex(){
		return get_sh_name();
	}
	
	public long getType(){
		return get_sh_type();
	}
	
	public long getFlags(){
		return get_sh_flags();
	}
	
	public long getAddress(){
		return get_sh_addr();
	}
	
	public long getOffset(){
		return get_sh_offset();
	}
	
	public long getSize(){
		return get_sh_size();
	}
	
	// TODO: does this point to another Section Header?
	public long getLink(){
		return get_sh_link();
	}
	
	public long getAdditionalInfo(){
		return get_sh_info();
	}
	
	public long getAlignment(){
		return get_sh_addralign();
	}
	
	public long getEntrySize(){
		return get_sh_entsize();
	}
	
	protected abstract long get_sh_name();
	protected abstract long get_sh_type();
	protected abstract long get_sh_flags();
	protected abstract long get_sh_addr();
	protected abstract long get_sh_offset();
	protected abstract long get_sh_size();
	protected abstract long get_sh_link();
	protected abstract long get_sh_info();
	protected abstract long get_sh_addralign();
	protected abstract long get_sh_entsize();
	
}
