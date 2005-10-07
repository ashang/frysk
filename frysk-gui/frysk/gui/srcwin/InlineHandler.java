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
	
    private static InlineViewer firstVisible;
	private static InlineViewer lastVisible;
	
	private static SourceViewWidget parent;
	
    private static int numLevels;
    
	private static Preferences myPrefs;
	
	public static boolean init(PCLocation data, Preferences prefs, SourceViewWidget parentWin){
		// If there's only one scope, we have no nead of inline functions
		if(data.nextScope == null)
			return false;
		
		top = data;
		
		// Start with nothing expanded
        firstVisible = null;
		lastVisible = null;
		
		// Store the preferences to create the inline viewers with
		myPrefs = prefs;
		parent = parentWin;
	
        // get the number of levels of inlined code to display
        numLevels = myPrefs.getInt("inline_levels", 2);
        
        System.out.println(numLevels);
        
		initialized = true;
		return true;
	}
	
	public static boolean moveDown(){
		if(!initialized)
			return false;
        
        int scopeDiff;
        if(lastVisible != null)
            scopeDiff = lastVisible.getScope().getDepth()-firstVisible.getScope().getDepth()+1;
        else 
            scopeDiff = 0;
        
        System.out.println(scopeDiff);
        
		if(verboseMode){
			// Code here to create a new instance of InlineViewer and
			// append it to the lowest child
		}
		else{
			/*
             * Base case #1: No levels of inline code are being shown 
			 */
			if(lastVisible == null){
                System.out.println("making first scope");
				lastVisible = new InlineViewer(myPrefs);
				lastVisible.load(top.nextScope);
                firstVisible = lastVisible;
				parent.setSubscopeAtCurrentLine(lastVisible);
			}
			
			/*
             * Base case #2: We haven't reached the max number of levels:
             * keep appending.
			 */
			else if(scopeDiff < numLevels){
                System.out.println("adding scope");
                // Do a quick check to make sure there are more levels to show...
                if(lastVisible.getScope().nextScope == null)
                    return false;
                
				lastVisible.nextLevel = new InlineViewer(myPrefs);
				lastVisible.nextLevel.load(lastVisible.getScope().nextScope);
				lastVisible.setSubscopeAtCurrentLine(lastVisible.nextLevel);
				lastVisible = lastVisible.nextLevel;
			}
			/*
             * Usual case: The max number of scopes is being displayed, so all the levels
             * need to move down one
			 */
			else{
                // Check to see that we _can_ move down
                if(lastVisible.getScope().nextScope == null)
                    return false;
                
				InlineViewer current = firstVisible;
                
                System.out.println(current.prevLevel);
                
                while(current != lastVisible){
                    current.moveDown();
                    // For all but the top level we want to shift down
                    if(current != firstVisible){
                        current.prevLevel.setSubscopeAtCurrentLine(current);
                    }
                    
                    current = current.nextLevel;
                }
                
                // load the new scope in the last frame
                current.load(current.getScope().nextScope);
                current.prevLevel.setSubscopeAtCurrentLine(current);
                
                // we want to return false here so the last level doesn't think it's expanded
                return false;
			}
		}
		
		return true;
	}
	
	public static boolean moveUp(InlineViewer clicked){
		if(!initialized)
			return false; 
        
		if(verboseMode){
			// Code here to collapse the lower viewers until we reach the one that was clicked
		}
		else{
		    /*
             * Case 1: We do not have any hidden scopes. All we need to do is 
             * collapse the scopes below it
		     */
			if(firstVisible.getScope().getDepth() == 1){
                clicked.clearSubscopeAtCurrentLine();
                clicked.nextLevel = null;
                lastVisible = clicked;
            }
            /*
             * Case 2: After collapsing to the clicked level, we don't have to hide
             * anything anymore 
             */
            if(clicked.getScope().getDepth() <= numLevels){
                int depth = clicked.getScope().getDepth();
                
                /*
                 * TODO: This is a really ugly implementation. We're dropping everything
                 * on the floor and then rebuilding from the ground up. Make more efficient
                 */
                parent.clearSubscopeAtCurrentLine();
                firstVisible = null;
                lastVisible = null;
                
                InlineViewer prev = null;
                InlineViewer current = firstVisible;
                PCLocation currentLocation = top.nextScope;
                
                for(int i = 1; i <= depth; i++){
                    current = new InlineViewer(myPrefs);
                    current.load(currentLocation);
                    
                    // If this is the first one, add it to the parent
                    if(i == 1){
                        parent.setSubscopeAtCurrentLine(current);
                        firstVisible = current;
                    }
                    else
                        prev.setSubscopeAtCurrentLine(current);
                    
                    prev = current;
                    currentLocation = currentLocation.nextScope;
                }
                
                lastVisible = current;
                parent.showAll();                
            }
            /*
             * Case 3: We still need to hide levels
             */
            else{
                int depth = clicked.getScope().getDepth();
                int startDepth = depth - numLevels + 1;
                
                /*
                 * TODO: Once again, this is an ugly implementation. We'll worry about
                 * speed later
                 */
                parent.clearSubscopeAtCurrentLine();
                firstVisible = null;
                lastVisible = null;
                
                InlineViewer prev = null;
                InlineViewer current = firstVisible;
                PCLocation currentLocation = top.nextScope;
                
                for(int i = 1; i < startDepth; i++)
                    currentLocation = currentLocation.nextScope;
                
                for(int i = startDepth; i <= depth; i++){
                    current = new InlineViewer(myPrefs, i == startDepth);
                    current.load(currentLocation);
                    
                    // If this is the first one, add it to the parent
                    if(i == startDepth){
                        parent.setSubscopeAtCurrentLine(current);
                        firstVisible = current;
                    }
                    else
                        prev.setSubscopeAtCurrentLine(current);
                    
                    prev = current;
                    currentLocation = currentLocation.nextScope;
                }
                
                lastVisible = current;
                parent.showAll(); 
            }
		}
		
		return true;
	}
	
	public static boolean moveUp(SourceViewWidget clicked){
		if(!initialized)
			return false;
		
		parent.clearSubscopeAtCurrentLine();
		
		parent.refresh();
        
		lastVisible = null;
        firstVisible = null;
		
		return true;
	}

}
