package frysk.gui.srcwin;


public interface View{
	
	void refresh();
	
	boolean findNext(String toFind, boolean caseSensitive);
	
	boolean findPrevious(String toFind, boolean caseSensitive);
	
	boolean highlightAll(String toFind, boolean caseSensitive);
	
	void scrollToFound();
	
	void load(StackLevel data);
	
	void setSubscopeAtCurrentLine(InlineSourceView child);
	
	void clearSubscopeAtCurrentLine();
	
	void toggleChild();
}
