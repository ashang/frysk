package lib.elf;

public class ElfData {

	private long pointer;
	private boolean is32bit;
	
	protected ElfData(long ptr, boolean is32bit){
		this.pointer = ptr;
		this.is32bit = is32bit;
	}
	
	public byte getByte(long offset){
		return elf_data_get_byte(offset);
	}
	
	public ElfType getType(){
		return ElfType.intern(elf_data_get_type());
	}
	
	public long getSize(){
		return elf_data_get_size();
	}
	
	public int getOffset(){
		return elf_data_get_off();
	}
	
	public long getAlignment(){
		return elf_data_get_align();
	}
	
	public ElfData translateToMemoryRepresentation(int encoding){
		return new ElfData(elf_xlatetom(encoding), is32bit);
	}
	
	public ElfData translateToELFRepresentation(int encoding){
		return new ElfData(elf_xlatetof(encoding), is32bit);
	}
	
	public int flag(ElfCommand command, int flags){
		return elf_flagdata(command.getValue(), flags);
	}
	
	protected long getPointer(){
		return this.pointer;
	}
	
	protected void finalize() throws Throwable {
		elf_data_finalize();
	}

	native protected void elf_data_finalize();
	native protected byte elf_data_get_byte(long offset);
	native protected int elf_data_get_type();
	native protected int elf_data_get_version();
	native protected long elf_data_get_size();
	native protected int elf_data_get_off();
	native protected long elf_data_get_align();
	native protected int elf_flagdata(int __cmd, int __flags);
	native protected long elf_xlatetom(int __encode);
	native protected long elf_xlatetof(int __encode);
}
