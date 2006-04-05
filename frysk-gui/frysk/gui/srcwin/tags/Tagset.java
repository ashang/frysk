package frysk.gui.srcwin.tags;

/**
 * A Tagset contains a collection of tags that are applicable to a process.
 * 
 * TODO: Tags not implemented yet
 * 
 * @author ajocksch
 *
 */
public class Tagset {

	private String name;
	private String desc;
	private String command;
	
	/**
	 * Creates a new tagset
	 * @param name The name of the tagset
	 * @param desc A brief description of the tagset
	 */
	public Tagset(String name, String desc, String command){
		this.name = name;
		this.desc = desc;
		this.command = command;
	}

	/**
	 * 
	 * @return The description of this tagset
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * 
	 * @return The name of this tagset
	 */
	public String getName() {
		return name;
	}

	/**
	 * 
	 * @return The command that this tagset associates with
	 */
	public String getCommand() {
		return command;
	}
}
