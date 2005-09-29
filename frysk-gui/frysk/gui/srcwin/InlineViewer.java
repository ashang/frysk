/**
 * 
 */
package frysk.gui.srcwin;

import java.util.prefs.Preferences;

import org.gnu.gtk.EventBox;
import org.gnu.gtk.Justification;
import org.gnu.gtk.Label;
import org.gnu.gtk.ToolTips;

/**
 * @author ajocksch
 *
 */
public class InlineViewer extends SourceViewWidget {

	protected InlineViewer nextLevel;
	protected InlineViewer prevLevel;
	
	private PCLocation scope;
	
	private boolean showEllipsis;
	
	public InlineViewer(Preferences parentPrefs){
		this(parentPrefs, false);
	}
	
	/**
	 * @param parentPrefs
	 */
	public InlineViewer(Preferences parentPrefs, boolean showEllipsis) {
		super(parentPrefs);
		this.setBorderWidth(1);
		this.showEllipsis = showEllipsis;
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
		
		if(showEllipsis){
			Label l = new Label("...");
			l.setJustification(Justification.LEFT);
			EventBox b1 = new EventBox();
			b1.add(l);
			ToolTips t = new ToolTips();
			t.setTip(b1, "Levels of inline code have been hidden. Collapse lower scopes to view these hidden levels", "");
			l = new Label("...");
			l.setJustification(Justification.LEFT);
			EventBox b2 = new EventBox();
			b2.add(l);
			t.setTip(b2, "Levels of inline code have been hidden. Collapse lower scopes to view these hidden levels", "");
			
			buf.insertText(buf.getStartIter(), "\n");
			this.addChild(b1, buf.createChildAnchor(buf.getStartIter()));
			this.addChild(b2, buf.createChildAnchor(buf.getEndIter()));
			
			b1.showAll();
			b2.showAll();
		}
	}

	public PCLocation getScope() {
		return scope;
	}

	public void setScope(PCLocation scope) {
		this.scope = scope;
	}

	public void setSubscopeAtCurrentLine(InlineViewer viewer){
		super.setSubscopeAtCurrentLine(viewer);
		
		viewer.prevLevel = this;
		this.nextLevel = viewer;
	}
}
