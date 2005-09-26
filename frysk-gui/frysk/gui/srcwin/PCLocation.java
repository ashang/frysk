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
	
	PCLocation nextScope;
	PCLocation prevScope;
	
	public PCLocation(String filename, int line){
		this.filename = filename;
		this.lineNum = line;
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
