package frysk.gui.srcwin.cparser;

class Token{
	public String text;
	public int lineNum;
	public int colNum;
	
	public Token(String text, int line, int col){
		this.text = text;
		this.lineNum = line;
		this.colNum = col;
	}
	
	public String toString(){
		return this.text+"(line "+this.lineNum+", offset "+colNum+")";
	}
}