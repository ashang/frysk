package frysk.gui.srcwin;

public abstract class CodeItem {
	String name;
	int line;
	int col;
	
	public CodeItem(String name, int line, int col){
		this.name = name;
		this.line = line;
		this.col = col;
	}
	public int getCol() {
		return col;
	}
	public int getLine() {
		return line;
	}
	public String getName() {
		return name;
	}
	
	/**
	 * Returns true if col is contained within the column range of this VariableLocation,
	 * false otherwise
	 * 
	 * @param col The column to test
	 * @return
	 */
	public boolean isInRange(int col){
		if(col >= this.col && col <= this.col+this.name.length())
			return true;
		
		return false;
	}
	public void setCol(int col) {
		this.col = col;
	}
}
