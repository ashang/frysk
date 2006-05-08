package lib.elf;


import java.util.Date;

public class ElfArchiveHeader {

	private long pointer;
	
	public ElfArchiveHeader(){
		elf_ar_new();
	}
	
	protected ElfArchiveHeader(long ptr){
		this.pointer = ptr;
	}
	
	public String getName(){
		return elf_ar_get_name();
	}
	
	public Date getDate(){
		return new Date(elf_ar_get_date());
	}
	
	public int getUid(){
		return elf_ar_get_uid();
	}
	
	public int getGid(){
		return elf_ar_get_gid();
	}
	
	public int getMode(){
		return elf_ar_get_mode();
	}
	
	public int getSize(){
		return elf_ar_get_size();
	}
	
	public String getRawName(){
		return elf_ar_get_raw_name();
	}
	
	protected long getPointer(){
		return this.pointer;
	}

	protected void finalize() throws Throwable {
		elf_ar_finalize();
	}
	
	protected native void elf_ar_new();
	protected native void elf_ar_finalize();
	protected native String elf_ar_get_name();
	protected native long elf_ar_get_date();
	protected native int elf_ar_get_uid();
	protected native int elf_ar_get_gid();
	protected native int elf_ar_get_mode();
	protected native int elf_ar_get_size();
	protected native String elf_ar_get_raw_name();
}
