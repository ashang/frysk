package lib.elf;

public abstract class ElfPHeader {

	private long pointer;
	
	protected ElfPHeader(long ptr){
		this.pointer = ptr;
	}
	
	public long getType(){
		return get_p_type();
	}
	
	public long getOffset(){
		return get_p_offset();
	}
	
	public long getVirtualAddress(){
		return get_p_vaddr();
	}
	
	public long getPhysicalAddress(){
		return get_p_paddr();
	}
	
	public long getSegmentSizeInFile(){
		return get_p_filesz();
	}
	
	public long getSegmentSizeInMem(){
		return get_p_memsz();
	}
	
	public long getFlags(){
		return get_p_flags();
	}
	
	public long getAlignment(){
		return get_p_align();
	}
	
	protected long getPointer(){
		return this.pointer;
	}
	
	protected abstract long get_p_type();
	protected abstract long get_p_offset();
	protected abstract long get_p_vaddr();
	protected abstract long get_p_paddr();
	protected abstract long get_p_filesz();
	protected abstract long get_p_memsz();
	protected abstract long get_p_align();
	protected abstract long get_p_flags();
}
