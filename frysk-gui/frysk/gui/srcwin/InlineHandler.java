/**
 * 
 */
package frysk.gui.srcwin;

import java.util.prefs.Preferences;

import org.gnu.gtk.TextChildAnchor;
import org.gnu.gtk.TextIter;


/**
 * @author ajocksch
 *
 */
public class InlineHandler{
	
	private static boolean verboseMode = false;
	private static boolean initialized = false;
	
	private static PCLocation top;
	private static PCLocation currentBottom = null;
	
	private static InlineViewer bottom;
	
	private static SourceViewWidget parent;
	
	private static Preferences myPrefs;
	
	public static boolean init(PCLocation data, Preferences prefs, SourceViewWidget parentWin){
		// If there's only one scope, we have no nead of inline functions
		if(data.nextScope == null)
			return false;
		
		top = data;
		
		// Start with nothing expanded
		bottom = null;
		currentBottom = top;
		
		// Store the preferences to create the inline viewers with
		myPrefs = prefs;
		parent = parentWin;
		
		initialized = true;
		return true;
	}
	
	public static boolean moveDown(){
		if(!initialized || currentBottom.nextScope == null)
			return false;
		
		currentBottom = currentBottom.nextScope;
		
		if(verboseMode){
			// Code here to create a new instance of InlineViewer and
			// append it to the lowest child
		}
		else{
			// Code here to move the contents of the lowest into the
			// level above, and load the new data in the lower one
			if(bottom == null){
				bottom = new InlineViewer(myPrefs);
				bottom.load(currentBottom);
				
				SourceBuffer buf = (SourceBuffer) parent.getBuffer();
				buf.insertText(buf.getLineIter(buf.getCurrentLine()+1), "\n");
				TextChildAnchor anchor = buf.createChildAnchor(buf.getLineIter(buf.getCurrentLine()+1));
				
				parent.addChild(bottom, anchor);
				bottom.showAll();
			}
			
			// bottom exists, now do the case where it doesn't have a next
			else if(bottom.getScope().prevScope == top){
				bottom.nextLevel = new InlineViewer(myPrefs);
				bottom.nextLevel.load(currentBottom);
				
				SourceBuffer buf = (SourceBuffer) bottom.getBuffer();
				buf.insertText(buf.getLineIter(buf.getCurrentLine()+1), "\n");
				TextChildAnchor anchor = buf.createChildAnchor(buf.getLineIter(buf.getCurrentLine()+1));
				
				bottom.addChild(bottom.nextLevel, anchor);
				bottom.nextLevel.showAll();
				
				bottom.nextLevel.prevLevel = bottom;
				bottom = bottom.nextLevel;
			}
			// general case - move bottom to bottom.prevLevel
			else{
				InlineViewer tmp = bottom.prevLevel;
				
				tmp.load(bottom.getScope());
				
				bottom = new InlineViewer(myPrefs);
				bottom.load(currentBottom);
				
				SourceBuffer buf = (SourceBuffer) tmp.getBuffer();
				buf.insertText(buf.getLineIter(buf.getCurrentLine()+1), "\n");
				TextChildAnchor anchor = buf.createChildAnchor(buf.getLineIter(buf.getCurrentLine()+1));
				
				tmp.addChild(bottom, anchor);
				bottom.showAll();
				
				bottom.prevLevel = tmp;
				tmp.nextLevel = bottom;
			}
		}
		
		return true;
	}
	
	public static boolean moveUp(InlineViewer clicked){
		if(!initialized || currentBottom.prevScope == null)
			return false;
		
		if(verboseMode){
			// Code here to collapse the lower viewers until we reach the one that was clicked
		}
		else{
			// Code here to move the contents of the top viewer into the lower
			// viewer and add the top viewer's parent content into the top viewer
			if(top.nextScope == clicked.getScope()){
				SourceBuffer buf = (SourceBuffer) clicked.getBuffer();
				TextIter line = buf.getLineIter(buf.getCurrentLine()+1);
				buf.deleteText(line, buf.getIter(line.getOffset()+2));
					
				bottom = clicked;
				currentBottom = clicked.getScope(); 
				clicked.nextLevel = null;
			}
			else{
				InlineViewer tmp = clicked;
				tmp.load(tmp.getScope().prevScope);
				
				bottom = new InlineViewer(myPrefs);
				bottom.load(tmp.getScope().nextScope);
				
				SourceBuffer buf = (SourceBuffer) tmp.getBuffer();
				buf.insertText(buf.getLineIter(buf.getCurrentLine()+1), "\n");
				TextChildAnchor anchor = buf.createChildAnchor(buf.getLineIter(buf.getCurrentLine()+1));
				
				tmp.addChild(bottom, anchor);
				bottom.showAll();
				
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean moveUp(SourceViewWidget clicked){
		if(!initialized)
			return false;
		
		SourceBuffer buf = (SourceBuffer) parent.getBuffer();
		TextIter line = buf.getLineIter(buf.getCurrentLine()+1);
		buf.deleteText(line, buf.getIter(line.getOffset()+2));
		
		parent.refresh();
		currentBottom = top;
		bottom = null;
		
		return true;
	}

}
