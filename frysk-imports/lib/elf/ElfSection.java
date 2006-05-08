package lib.elf;

public class ElfSection {

	private long pointer;
	private boolean is32bit;
	
	protected ElfSection(long ptr, boolean is32bit){
		this.pointer = ptr;
		this.is32bit = is32bit;
	}
	
	protected long getPointer(){
		return this.pointer;
	}
	
	public long getIndex(){
		return elf_ndxscn();
	}
	
	public ElfSectionHeader getSectionHeader(){
		if(is32bit)
			return new ElfSectionHeader32(elf_getshdr());
		else
			return new ElfSectionHeader64(elf_getshdr());
	}
	
	public int flag(ElfCommand command, int flags){
		return elf_flagscn(command.getValue(),flags);
	}
	
	public int flagHeader(ElfCommand command, int flags){
		return elf_flagshdr(command.getValue(), flags);
	}
	
	public ElfData getData(){
		return new ElfData(elf_getdata(), is32bit);
	}
	
	public ElfData getRawData(){
		return new ElfData(elf_rawdata(), is32bit);
	}
	
	public ElfData createNewElfData(){
		return new ElfData(elf_newdata(), is32bit);
	}
	
	protected native long elf_ndxscn();
	protected native long elf_getshdr();
	protected native int elf_flagscn(int __cmd, int __flags);
	protected native int elf_flagshdr(int __cmd, int __flags);
	protected native long elf_getdata();
	protected native long elf_rawdata();
	protected native long elf_newdata();
}
