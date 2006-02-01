package frysk.gui.srcwin;

public class CurrentLineSection {
	private int startLine;
	private int endLine;
	
	private int startOffsset;
	private int endOffset;
	
	private CurrentLineSection nextSection;
	private CurrentLineSection prevSection;
	
	public CurrentLineSection(int lineStart, int lineEnd, int colStart, int colEnd){
		startLine = lineStart;
		endLine = lineEnd;
		startOffsset = colStart;
		endOffset = colEnd;
	}
	
	public int getEndLine() {
		return endLine;
	}
	public void setEndLine(int endLine) {
		this.endLine = endLine;
	}
	public int getEndOffset() {
		return endOffset;
	}
	public void setEndOffset(int endOffset) {
		this.endOffset = endOffset;
	}
	public CurrentLineSection getNextSection() {
		return nextSection;
	}
	public void setNextSection(CurrentLineSection nextSection) {
		this.nextSection = nextSection;
	}
	public CurrentLineSection getPrevSection() {
		return prevSection;
	}
	public void setPrevSection(CurrentLineSection prevSection) {
		this.prevSection = prevSection;
	}
	public int getStartLine() {
		return startLine;
	}
	public void setStartLine(int startLine) {
		this.startLine = startLine;
	}
	public int getStartOffsset() {
		return startOffsset;
	}
	public void setStartOffsset(int startOffsset) {
		this.startOffsset = startOffsset;
	}
}
