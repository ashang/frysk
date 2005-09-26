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
	private int lineNum;
	
	private int depth;
	
	protected PCLocation nextScope;
	protected PCLocation prevScope;
	
	public PCLocation(String filename, int line){
		this.filename = filename;
		this.lineNum = line;
		this.depth = 0;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public PCLocation getInlineData() {
		return nextScope;
	}

	public void setNextScope(PCLocation inlineData) {
		this.nextScope = inlineData;
		inlineData.depth = this.depth + 1;
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

	public void setPrevScope(PCLocation prevScope) {
		this.prevScope = prevScope;
	}
	
	public void link(PCLocation next){
		if(this.nextScope != null){
			next.setNextScope(this.nextScope);
			this.nextScope.setPrevScope(next);
		}
		
		this.setNextScope(next);
		next.setPrevScope(this);
	}
}
