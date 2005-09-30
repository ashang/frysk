/**
 * 
 */
package frysk.gui.srcwin;

import java.util.prefs.Preferences;


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
				parent.setSubscopeAtCurrentLine(bottom);
				parent.drawMargin();
			}
			
			// bottom exists, now do the case where it doesn't have a next
			else if(bottom.getScope().prevScope == top){
				bottom.nextLevel = new InlineViewer(myPrefs);
				bottom.nextLevel.load(currentBottom);
				
				bottom.setSubscopeAtCurrentLine(bottom.nextLevel);
				bottom = bottom.nextLevel;
			}
			// general case - move bottom to bottom.prevLevel
			else{
				InlineViewer tmp = new InlineViewer(myPrefs, true); 
				tmp.load(bottom.prevLevel.getScope());
								
				tmp.load(bottom.getScope());
				
				bottom = new InlineViewer(myPrefs);
				bottom.load(currentBottom);
				
				parent.setSubscopeAtCurrentLine(tmp);
				tmp.setSubscopeAtCurrentLine(bottom);
			}
		}
		
		return true;
	}
	
	public static boolean moveUp(InlineViewer clicked){
		if(!initialized || currentBottom.prevScope == null)
			return false;
		
		currentBottom = clicked.getScope();
		
		if(verboseMode){
			// Code here to collapse the lower viewers until we reach the one that was clicked
		}
		else{
			// Code here to move the contents of the top viewer into the lower
			// viewer and add the top viewer's parent content into the top viewer
			if(top.nextScope == clicked.getScope()){
				InlineViewer tmp = new InlineViewer(myPrefs);
				tmp.load(clicked.getScope());
				
				parent.setSubscopeAtCurrentLine(tmp);
		
				bottom = tmp; 
				clicked.nextLevel = null;
			}
			else if(clicked.getScope().prevScope == top.nextScope){
				InlineViewer tmp = new InlineViewer(myPrefs);
				tmp.load(top.nextScope);
				
				bottom = new InlineViewer(myPrefs);
				bottom.load(clicked.getScope());
				
				parent.setSubscopeAtCurrentLine(tmp);
				tmp.setSubscopeAtCurrentLine(bottom);
			}
			else{
				InlineViewer tmp = new InlineViewer(myPrefs, true);
				tmp.load(clicked.getScope().getPrevScope()); 
				tmp.setBorderWidth(0);
				
				bottom = new InlineViewer(myPrefs);
				bottom.load(currentBottom);
				
				parent.setSubscopeAtCurrentLine(tmp);
				tmp.setSubscopeAtCurrentLine(bottom);
					
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean moveUp(SourceViewWidget clicked){
		if(!initialized)
			return false;
		
		parent.clearSubscopeAtCurrentLine();
		
		parent.refresh();
		currentBottom = top;
		bottom = null;
		
		return true;
	}

}
