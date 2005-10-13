/**
 * 
 */
package frysk.gui.srcwin;

/**
 * @author ajocksch
 *
 */
public class PCLocation {
	private String filename;
	private String function;
	private int lineNum;
	
	private int depth;
	
	protected PCLocation nextScope;
	protected PCLocation prevScope;
	
	protected PCLocation parentScope;	
	protected PCLocation inlineScope;
	
	public PCLocation(String filename, String function, int line){
		this.filename = filename;
		this.lineNum = line;
		this.depth = 0;
		this.function = function;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public PCLocation getNextScope() {
		return nextScope;
	}

	public int getLineNum() {
		return lineNum;
	}

	public void setLineNum(int lineNum) {
		this.lineNum = lineNum;
	}

	public PCLocation getPrevScope() {
		return prevScope;
	}

	public void addNextScope(PCLocation next){
		if(this.nextScope != null){
			next.nextScope = this.nextScope;
			this.nextScope.depth++;
			this.nextScope.prevScope = next;
		}
		
		this.nextScope = next;
		next.depth = this.depth + 1;
		next.prevScope = this;
	}
	
	public void addInlineScope(PCLocation child){
		if(this.inlineScope != null){
			child.inlineScope = this.inlineScope;
			this.inlineScope.depth++;
			this.inlineScope.parentScope = child;
		}
		
		this.inlineScope = child;
		child.depth = this.depth + 1;
		child.parentScope = this;
	}
	
	public int getDepth() {
		return depth;
	}

	public String getFunction() {
		return function;
	}
}
