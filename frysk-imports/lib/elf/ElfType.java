package lib.elf;

public class ElfType {
	
	public static ElfType ELF_T_BYTE = new ElfType(0);
	public static ElfType ELF_T_ADDR = new ElfType(1);
	public static ElfType ELF_T_DYN = new ElfType(2);
	public static ElfType ELF_T_EHDR = new ElfType(3);
	public static ElfType ELF_T_HALF = new ElfType(4);
	public static ElfType ELF_T_OFF = new ElfType(5);
	public static ElfType ELF_T_PHDR = new ElfType(6);
	public static ElfType ELF_T_RELA = new ElfType(7);
	public static ElfType ELF_T_REL = new ElfType(8);
	public static ElfType ELF_T_SHDR = new ElfType(9);
	public static ElfType ELF_T_SWORD = new ElfType(10);
	public static ElfType ELF_T_SYM = new ElfType(11);
	public static ElfType ELF_T_WORD = new ElfType(12);
	public static ElfType ELF_T_XWORD = new ElfType(13);
	public static ElfType ELF_T_SXWORD = new ElfType(14);
	public static ElfType ELF_T_VDEF = new ElfType(15);
	public static ElfType ELF_T_VDAUX = new ElfType(16);
	public static ElfType ELF_T_VNEED = new ElfType(17);
	public static ElfType ELF_T_VNAUX = new ElfType(18);
	public static ElfType ELF_T_NHDR = new ElfType(19);
	public static ElfType ELF_T_SYMINFO = new ElfType(20);
	public static ElfType ELF_T_MOVE = new ElfType(21);
	public static ElfType ELF_T_LIB = new ElfType(22);
	
	private static ElfType[] types = {ELF_T_BYTE, ELF_T_ADDR, ELF_T_DYN,
		ELF_T_EHDR, ELF_T_HALF, ELF_T_OFF, ELF_T_PHDR, ELF_T_RELA, ELF_T_REL,
		ELF_T_SHDR, ELF_T_SWORD, ELF_T_SYM, ELF_T_WORD, ELF_T_XWORD, ELF_T_SXWORD,
		ELF_T_VDEF, ELF_T_VDAUX, ELF_T_VNEED, ELF_T_VNAUX, ELF_T_NHDR, 
		ELF_T_SYMINFO, ELF_T_MOVE, ELF_T_LIB
	};
	
	private int value;
	
	private ElfType(int val){
		this.value = val;
	}
	
	public boolean equals(Object obj){
		if(!(obj instanceof ElfType))
			return false;
		
		return ((ElfType)obj).value == this.value;
	}
	
	protected int getValue(){
		return this.value;
	}
	
	protected static ElfType intern(int type){
		return types[type];
	}
}
