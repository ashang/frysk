package lib.elf;

public class ElfArchiveSymbol {

	private long pointer;
	
	public ElfArchiveSymbol(){
		elf_as_new();
	}
	
	protected ElfArchiveSymbol(long pointer){
		this.pointer = pointer;
	}
	
	public String getName(){
		return elf_as_get_name();
	}
	
	public int getOffset(){
		return elf_as_get_offset();
	}
	
	public long getHash(){
		return elf_as_get_hash();
	}
	
	protected long getPointer(){
		return this.pointer;
	}
	
	protected void finalize() throws Throwable {
		elf_as_finalize();
	}
	
	protected native void elf_as_new();
	protected native void elf_as_finalize();
	protected native String elf_as_get_name();
	protected native int elf_as_get_offset();
	protected native long elf_as_get_hash();
}
