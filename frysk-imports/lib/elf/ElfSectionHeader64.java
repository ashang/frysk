package lib.elf;

public class ElfSectionHeader64 extends ElfSectionHeader {

	public ElfSectionHeader64(long ptr) {
		super(ptr);
		// TODO Auto-generated constructor stub
	}

	protected native long get_sh_name();
	protected native long get_sh_type();
	protected native long get_sh_flags();
	protected native long get_sh_addr();
	protected native long get_sh_offset();
	protected native long get_sh_size();
	protected native long get_sh_link();
	protected native long get_sh_info();
	protected native long get_sh_addralign();
	protected native long get_sh_entsize();
}
