package frysk.gui.srcwin;

/**
 * This class represents a line of source code. Specifically it knows about 
 * whether the line is breakable, and whether there is a breakpoint set on this
 * line. It also has a string representation of the source in this line.
 * This class was created to keep track of source code in the SourceViewWidget,
 * while there is a lack of a similar class in the backend. Once enough information
 * can be ascertained from debug-symbols we probably won't need this. 
 * @author ifoox
 *
 */
public class SourceCodeLine {
	private boolean breakable;
	private boolean breakpointSet;
	private String source;
	
	
	public SourceCodeLine(){
		this("", false);
	}
	public SourceCodeLine(String code){
		this(code, true);	
	}
	
	public SourceCodeLine(String code, boolean breakable){
		this.setSource(code);
		this.setBreakable(breakable);
		this.setBreakpoint(false);
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String getSource() {
		return source;
	}
	public void setBreakpoint(boolean breakpointSet) {
		this.breakpointSet = breakpointSet;
	}
	public boolean isBreakpointSet() {
		return breakpointSet;
	}
	public void setBreakable(boolean breakable) {
		this.breakable = breakable;
	}
	public boolean isExecutable() {
		return breakable;
	}
	
	
}
