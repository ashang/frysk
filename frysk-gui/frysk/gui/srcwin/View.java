package frysk.gui.srcwin;

import java.util.Vector;

import org.gnu.gtk.Widget;


public interface View{
	
	void refresh();
	
	boolean findNext(String toFind, boolean caseSensitive);
	
	boolean findPrevious(String toFind, boolean caseSensitive);
	
	boolean highlightAll(String toFind, boolean caseSensitive);
	void scrollToFunction(String markName);
	
	void scrollToLine(int line);
	
	Vector getFunctions();
	
	void load(StackLevel data);
	
	StackLevel getScope();
	
	void setSubscopeAtCurrentLine(InlineSourceView child);
	
	void clearSubscopeAtCurrentLine();
	
	void toggleChild();
	
	// These are functions from org.gnu.gtk.Widget.. so long as the implementors
	// of this interface extend a widget, they don't need to worry about these
	void showAll();
	Widget getParent();
}
