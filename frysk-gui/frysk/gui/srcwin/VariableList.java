package frysk.gui.srcwin;

/**
 * VariableList is intended to serve as a record of what variables are in a file
 * and the lines and columns where they occur.
 * 
 * @author ajocksch
 *
 */

public class VariableList {
	private Variable[] vars;
	
	/**
	 * Craetes a new VariableList of the given size
	 * @param size The number of lines in the file represented
	 */
	public VariableList(int size){
		vars = new Variable[size];
	}
	
	/**
	 * Adds a new variable to the VariableList
	 * 
	 * @param name The name of the variable
	 * @param lineNum The line number the variable's on
	 * @param startCol The column the variable starts on
	 * @param endCol The column the variable ends on
	 */
	public void addVariable(String name, int lineNum, int startCol, int endCol, boolean global){
		Variable toPut = new Variable(name, lineNum, startCol, global);
		this.addVariable(toPut);
	}
	
	/**
	 * Adds a new variable to the VariableList
	 * @param toPut The variable to add
	 */
	public void addVariable(Variable toPut){
		int lineNum = toPut.getLine();
		Variable var = this.vars[lineNum];
		
		if(var == null){
			this.vars[lineNum] = toPut;
			return;
		}
		
		while(var.getNext() != null){
			if(var.getCol() >= toPut.getCol())
				break;
			
			var = var.getNext();
		}
		
//		 no duplicates allowed
		if(var.getCol() == toPut.getCol())
			return;
		
		// reached the end of the list
		if(var.getNext() == null){
			var.setNext(toPut);
			toPut.setPrev(var);
			
			return;
		}
		
		// still at the start
		if(var.getPrev() == null){
			toPut.setNext(var);
			var.setPrev(toPut);
			this.vars[lineNum] = toPut;
			
			return;
		}
		
		// ordinary situation
		toPut.setNext(var);
		toPut.setPrev(var.getPrev());
		var.getPrev().setNext(var);
		var.setPrev(toPut);
	}
	
	/**
	 * Searches for a variable at the given line number and column. If no variable
	 * is found returns null
	 * 
	 * @param lineNum Line to search on
	 * @param col Column to search on
	 * @return The variable if found, null otherwise
	 */
	public Variable getVariable(int lineNum, int col){
		Variable var = this.vars[lineNum];
		
		while(var != null){
			if(var.isInRange(col))
				break;
			
			var = var.getNext();
		}
		
		return var;
	}
	
	public String toString(){
		String s = "";
		
		for(int i = 0;i < this.vars.length; i++){
			s += "["+i+"]";
			
			Variable var = this.vars[i];
			
			while(var != null){
				s += " -->"+var.toString();
				var = var.getNext();
			}
			
			s += "\n";
		}
		
		return s;
	}
}
