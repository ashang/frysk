package lib.dw;

public class DwarfCommand {
	
	public static final DwarfCommand READ = new DwarfCommand(0);
	public static final DwarfCommand RDWR = new DwarfCommand(1);
	public static final DwarfCommand WRITE = new DwarfCommand(2);
	
	private static DwarfCommand[] commands = {READ, RDWR, WRITE};
	
	private int val;
	
	private DwarfCommand(int val){
		this.val = val;
	}
	
	protected int getValue(){
		return val;
	}
	
	protected DwarfCommand intern(int val){
		if(val < 0 || val > 2)
			return null;
			
		return commands[val];
	}
}
