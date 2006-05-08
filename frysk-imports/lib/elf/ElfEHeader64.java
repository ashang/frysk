package lib.elf;

public class ElfEHeader64 extends ElfEHeader {

	public ElfEHeader64(long ptr) {
		super(ptr);
	}

	protected native String get_e_ident();
	protected native int get_e_type();
	protected native int get_e_machine();
	protected native long get_e_version();
	protected native long get_e_entry();
	protected native long get_e_phoff();
	protected native long get_e_shoff();
	protected native long get_e_flags();
	protected native int get_e_ehsize();
	protected native int get_e_phentsize();
	protected native int get_e_phnum();
	protected native int get_e_shentsize();
	protected native int get_e_shnum();
	protected native int get_e_shstrndx();
}
