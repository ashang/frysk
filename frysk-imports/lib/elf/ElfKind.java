package lib.elf;

public class ElfKind {

	public static ElfKind ELF_K_NONE = new ElfKind(0);
	public static ElfKind ELF_K_AR = new ElfKind(1);
	public static ElfKind ELF_K_COFF = new ElfKind(2);
	public static ElfKind ELF_K_ELF = new ElfKind(3);
	
	private static ElfKind[] kinds = {ELF_K_NONE, ELF_K_AR, ELF_K_COFF, ELF_K_ELF};
	
	private int value;

	private ElfKind(int value){
		this.value = value;
	}
	
	public boolean equals(Object obj){
		if(!(obj instanceof ElfKind))
			return false;
		
		return ((ElfKind)obj).value == this.value;
	}
	
	protected int getValue(){
		return this.value;
	}
	
	protected static ElfKind intern(int kind){
		return kinds[kind];
	}
}
