// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.
package frysk.gui.srcwin;

import java.util.prefs.Preferences;


/**
 * @author ajocksch
 *
 */
public class InlineHandler{
	
//	private static boolean verboseMode = false;
	private static boolean initialized = false;
	
//	private static StackLevel top;
//	
//    private static InlineViewer firstVisible;
//	private static InlineViewer lastVisible;
	
//	private static SourceViewWidget parent;
	
    private static int numLevels;
    
	private static Preferences myPrefs;
	
	/**
	 * Initializes the data necessary for the handling of inline code
	 * @param data The top level of the PCLocation tree
	 * @param prefs The Preference model to use
	 * @param parentWin The SourceViewWidget in which to place the inline code
	 * @return Whether or not the initialization was successful
	 */
	public static boolean init(StackLevel data, Preferences prefs, SourceViewWidget parentWin){
		// If there's only one scope, we have no nead of inline functions
		if(data.nextScope == null)
			return false;
		
//		top = data;
//		
//		// Start with nothing expanded
//        firstVisible = null;
//		lastVisible = null;
		
		// Store the preferences to create the inline viewers with
		myPrefs = prefs;
//		parent = parentWin;
	
        // get the number of levels of inlined code to display
        numLevels = myPrefs.getInt("inline_levels", 2);
        
        System.out.println(numLevels);
        
		initialized = true;
		return true;
	}
	
	/**
	 * Moves the level of inline code being viewed down one level. This generalizes to a 
	 * number of different cases:
	 * <ol>
	 * <li>No inline code is being displayed, in which case the first inline
	 * block is shown</li>
	 * <li>Fewer than n inline levels are shown, where n is the max number of
	 * levels of inline code that can be displayed (gotten from the preference
	 * model). In this case another level of code is nested</li>
	 * <li>n Levels of inline code are show, in which case each level is shifted
	 * down one level and a notification is shown to the user saying that levels
	 * of inline code have been hidden</li>
	 * </ol> 
	 */
	public static void moveDown(){
		if(!initialized)
			return;

        // FIXME: This needs to be re-implemented within the scope of the data that the DOM provides
		
//        int scopeDiff;
//        if(lastVisible != null)
//            scopeDiff = lastVisible.getScope().getDepth()-firstVisible.getScope().getDepth()+1;
//        else 
//            scopeDiff = 0;
        

        
//		if(verboseMode){
//			// Code here to create a new instance of InlineViewer and
//			// append it to the lowest child
//		}
//		else{
//			/*
//             * Base case #1: No levels of inline code are being shown 
//			 */
//			if(lastVisible == null){
//				if(!top.hasInlineScope())
//					return;
//				
//				lastVisible = new InlineViewer(myPrefs);
//				lastVisible.load(top.getInlineScope());
//                firstVisible = lastVisible;
//				parent.setSubscopeAtCurrentLine(lastVisible);
//			}
//			
//			/*
//             * Base case #2: We haven't reached the max number of levels:
//             * keep appending.
//			 */
//			else if(scopeDiff < numLevels){
//				
//                // Do a quick check to make sure there are more levels to show...
//                if(!lastVisible.getScope().hasInlineScope())
//                    return;
//                
//				lastVisible.nextLevel = new InlineViewer(myPrefs);
//				lastVisible.nextLevel.load(lastVisible.getScope().getInlineScope());
//				lastVisible.setSubscopeAtCurrentLine(lastVisible.nextLevel);
//				lastVisible = lastVisible.nextLevel;
//			}
//			/*
//             * Usual case: The max number of scopes is being displayed, so all the levels
//             * need to move down one
//			 */
//			else{
//                // Check to see that we _can_ move down
//                if(!lastVisible.getScope().hasInlineScope())
//                    return;
//                
//				InlineViewer current = firstVisible;
//                
//                while(current != lastVisible){
//                    current.moveDown();
//                    // For all but the top level we want to shift down
//                    if(current != firstVisible){
//                        current.prevLevel.setSubscopeAtCurrentLine(current);
//                    }
//                    
//                    current = current.nextLevel;
//                }
//                
//                // load the new scope in the last frame
//                current.load(current.getScope().getInlineScope());
//                current.prevLevel.setSubscopeAtCurrentLine(current);
//                
//                // we want to return false here so the last level doesn't think it's expanded
//                return;
//			}
//		}
	}
	
	/**
	 * Moves the level of inline code viewed in the source window up a level. All scopes
	 * below the level that was clicked are collapsed, and if there is need to hide scopes
	 * (i.e. the number of scopes is greater than the maximum in the preference model) then
	 * the excess scopes will be hidden in the same way as {@link #moveDown()}. Note that
	 * this function is called from instances of InlineViewer, wheras 
	 * @param clicked The level of inline code that was clicked.
	 */
	public static void moveUp(InlineViewer clicked){
		if(!initialized)
			return; 
        
		
		// FIXME: This needs to be re-implemented within the scope of the data that the DOM provides
		
//		if(verboseMode){
//			// Code here to collapse the lower viewers until we reach the one that was clicked
//		}
//		else{
//		    /*
//             * Case 1: We do not have any hidden scopes. All we need to do is 
//             * collapse the scopes below it
//		     */
//			if(firstVisible.getScope().getDepth() == 1){
//                clicked.clearSubscopeAtCurrentLine();
//                clicked.nextLevel = null;
//                lastVisible = clicked;
//            }
//            /*
//             * Case 2: After collapsing to the clicked level, we don't have to hide
//             * anything anymore 
//             */
//            if(clicked.getScope().getDepth() <= numLevels){
//                int depth = clicked.getScope().getDepth();
//                
//                /*
//                 * TODO: This is a really ugly implementation. We're dropping everything
//                 * on the floor and then rebuilding from the ground up. Make more efficient
//                 */
//                parent.clearSubscopeAtCurrentLine();
//                firstVisible = null;
//                lastVisible = null;
//                
//                StackLevel currentLocation = top.getInlineScope();
//                 
//                showScopes(1, depth, currentLocation);
//                
//                parent.showAll();                
//            }
//            /*
//             * Case 3: We still need to hide levels
//             */
//            else{
//                int depth = clicked.getScope().getDepth();
//                int startDepth = depth - numLevels + 1;
//                
//                /*
//                 * TODO: Once again, this is an ugly implementation. We'll worry about
//                 * speed later
//                 */
//                parent.clearSubscopeAtCurrentLine();
//                firstVisible = null;
//                lastVisible = null;
//                
//                StackLevel currentLocation = top.inlineScope;
//                
//                showScopes(startDepth, depth, currentLocation);
//                
//                parent.showAll(); 
//            }
//		}
	}
	
	/**
	 * This function hides all the levels of inline code below the
	 * level that was clicked, which in this case happens to be the
	 * top level. {@see #moveUp(InlineViewer)} for more information.
	 * @param clicked The SourceViewWidget at the top of the hierarchy
	 */
	public static void moveUp(SourceViewWidget clicked){
		if(!initialized)
			return;
		
//		parent.clearSubscopeAtCurrentLine();
//		
//		parent.refresh();
//        
//		lastVisible = null;
//        firstVisible = null;
	}

	
//	 FIXME: This needs to be re-implemented within the scope of the data that the DOM provides
	
	/*
	 * Shows all the inline scopes in the tree rooted at topScope, from startDepth until
	 * endDepth.
	 */
//	private static void showScopes(int startDepth, int endDepth, StackLevel topScope){
//		
//		for(int i = 1; i < startDepth; i++)
//            topScope = topScope.inlineScope;
//		
//		InlineViewer current = null;
//		InlineViewer prev = null;
//		
//		for(int i = startDepth; i <= endDepth; i++){
//            current = new InlineViewer(myPrefs, i == startDepth);
//            current.load(topScope);
//            
//            // If this is the first one, add it to the parent
//            if(i == startDepth){
//                parent.setSubscopeAtCurrentLine(current);
//                firstVisible = current;
//            }
//            else
//                prev.setSubscopeAtCurrentLine(current);
//            
//            prev = current;
//            topScope = topScope.inlineScope;
//        }
//		
//		lastVisible = current;
//	}
}
