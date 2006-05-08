package lib.elf;

public class ElfPHeader32 extends ElfPHeader {

	protected ElfPHeader32(long ptr) {
		super(ptr);
	}

	protected native long get_p_type();
	protected native long get_p_offset();
	protected native long get_p_vaddr();
	protected native long get_p_paddr();
	protected native long get_p_filesz();
	protected native long get_p_memsz();
	protected native long get_p_align();
	protected native long get_p_flags();
}
