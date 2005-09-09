package frysk.gui.srcwin;

/**
 * This class represents a specific instance of a variable in a source file. It
 * records the name, line number, and column span of the variable. VariableLocations 
 * act as a linked list, so that multiple variables on the same line will be
 * connected
 * 
 * @author ajocksch
 *
 */

public class Variable extends CodeItem {
	private boolean global = false;
	
	private Variable prev = null;
	private Variable next = null;

	/**
	 * Create a new, empty variable
	 */
	public Variable(){
		super("", -1, -1);
	}
	
	/**
	 * Create a new VariableLocation with the given parameters
	 * @param name Name of the variable
	 * @param lineNum Line number where it occurs
	 * @param startCol Start of the column span (wrt the line it's on)
	 * @param endCol End of the column span (wrt the line it's on)
	 */
	public Variable(String name, int lineNum, int startCol, boolean global){
		super(name, lineNum, startCol);
		this.global = global;
	}

	/**
	 * @return The next VariableLocation on the line
	 */
	public Variable getNext() {
		return next;
	}

	/**
	 * Sets the next variable on the line
	 * @param next
	 */
	public void setNext(Variable next) {
		this.next = next;
	}

	/**
	 * @return The previous variable on the line
	 */
	public Variable getPrev() {
		return prev;
	}

	/**
	 * Sets the previous variable on the line
	 * @param prev
	 */
	public void setPrev(Variable prev) {
		this.prev = prev;
	}

	
	public String toString(){
		return this.name+":("+this.line+"-"+this.col+")";
	}

	public boolean isGlobal() {
		return global;
	}
}
