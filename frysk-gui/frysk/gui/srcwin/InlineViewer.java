/**
 * 
 */
package frysk.gui.srcwin;

import java.util.prefs.Preferences;

/**
 * @author ajocksch
 *
 */
public class InlineViewer extends SourceViewWidget {

	protected InlineViewer nextLevel;
	protected InlineViewer prevLevel;
	
	private PCLocation scope;
	
	/**
	 * @param parentPrefs
	 */
	public InlineViewer(Preferences parentPrefs) {
		super(parentPrefs);
		this.setBorderWidth(1);
	}
	
	public void toggleChild(){
		/*
		 * For right now we're running under the assumptions that
		 *  a) The structure of the inline code is a linked-list, not a tree
		 *  b) We're only viewing inline code at the current PC address
		 */
		
		if(this.nextLevel == null)
			this.expanded = InlineHandler.moveDown();
		else
			this.expanded = !InlineHandler.moveUp(this);
	}
	
	public void load(PCLocation current){
		try {
			this.buf.loadFile(current.getFilename());
		} catch (Exception e){
			e.printStackTrace();
		}
		
		this.setCurrentLine(current.getLineNum());
		this.scope = current;
	}

	public PCLocation getScope() {
		return scope;
	}

	public void setScope(PCLocation scope) {
		this.scope = scope;
	}

}
