package lib.elf;

public class ElfCommand {

	public static ElfCommand ELF_C_NULL = new ElfCommand(0);
	public static ElfCommand ELF_C_READ = new ElfCommand(1);
	public static ElfCommand ELF_C_RDWR = new ElfCommand(2);
	public static ElfCommand ELF_C_WRITE = new ElfCommand(3);
	public static ElfCommand ELF_C_CLR = new ElfCommand(4);
	public static ElfCommand ELF_C_SET = new ElfCommand(5);
	public static ElfCommand ELF_C_FDDONE = new ElfCommand(6);
	public static ElfCommand ELF_C_FDREAD = new ElfCommand(7);
	public static ElfCommand ELF_C_READ_MMAP = new ElfCommand(8);
	public static ElfCommand ELF_C_RDWR_MMAP = new ElfCommand(9);
	public static ElfCommand ELF_C_READ_MMAP_PRIVATE = new ElfCommand(10);
	public static ElfCommand ELF_C_EMPTY = new ElfCommand(11);
	
	private static ElfCommand[] commands = {ELF_C_NULL, ELF_C_READ, ELF_C_RDWR,
		ELF_C_WRITE, ELF_C_CLR, ELF_C_SET, ELF_C_FDDONE, ELF_C_FDREAD,
		ELF_C_READ_MMAP, ELF_C_RDWR_MMAP, ELF_C_READ_MMAP_PRIVATE, ELF_C_EMPTY
	};
	
	private int value;
	
	private ElfCommand(int value){
		this.value = value;
	}
	
	public boolean equals(Object obj){
		if(!(obj instanceof ElfCommand))
			return false;
		
		return ((ElfCommand)obj).value == this.value;
	}
	
	protected int getValue(){
		return this.value;
	}
	
	protected static ElfCommand intern(int command){
		return commands[command];
	}
}
