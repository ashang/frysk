// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

package frysk.gui.sessions;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jdom.Element;

import frysk.gui.monitor.SaveableXXX;
import frysk.rt.DisplayValueObserver;
import frysk.rt.UpdatingDisplayValue;

/**
 * The WatchList class represents a list of variables that are being watched
 * from a process. All of the variables in a WatchList should come from the same
 * process. A WatchList can also save the variables to ~/.frysk/path/to/proc
 * when the source window closes so that the next time the program is debugged
 * the watched can be automatically retrieved.
 * 
 */
public class WatchList implements SaveableXXX {
    private boolean shouldSave = true; // Save by default

    private List listeners;

    private List vars;

    private List descriptions; // Holds the descriptions of the Variable

    // Watches loaded from disk

    /**
     * Creates a new empty list of watched variables
     * 
     */
    public WatchList() {
	vars = new LinkedList();
	listeners = new LinkedList();
	descriptions = new LinkedList();
    }

    /**
     * Create a new WatchList containing the same variables as the provided
     * list. The new watch list will not have any listeners attached to it.
     * 
     * @param other
     *                WatchList to populate variables from
     */
    public WatchList(WatchList other) {
	vars = new LinkedList(other.vars);
	listeners = new LinkedList();
	descriptions = new LinkedList();
    }

    /**
     * Adds a display to the list of watched variables
     * 
     * @param disp
     *                The new display to watch
     */
    public void addVariable(UpdatingDisplayValue disp) {
	// When the observer updates, notify our observers
	disp.addObserver(new DisplayValueObserver() {
	    public void updateValueChanged(UpdatingDisplayValue value) {
		notifyListenersUpdated(value);
	    }
	
	    public void updateUnavailbeResumedExecution(UpdatingDisplayValue value) {
		notifyListenersUpdated(value);
	    }
	
	    public void updateUnavailableOutOfScope(UpdatingDisplayValue value) {
		notifyListenersUpdated(value);
	    }
	
	    public void updateDisabled(UpdatingDisplayValue value) {
		notifyListenersUpdated(value);
	    }
	
	    public void updateAvailableTaskStopped(UpdatingDisplayValue value) {}
	});
	vars.add(disp);
	notifyListenersAdded(disp);
    }

    /**
     * Removes the given variable from the watched list
     * 
     * @param disp
     *                The variable to remove
     * @return True if the variable was removed, false otherwise
     */
    public boolean removeVariable(UpdatingDisplayValue disp) {
	Iterator iter = vars.iterator();
	while (iter.hasNext()) {
	    UpdatingDisplayValue uDisp = (UpdatingDisplayValue) iter.next();
	    // TODO: Do we need a better way of identifying whether two
	    // variables are the same?
	    if (uDisp.getName().equals(disp.getValue().getTextFIXME())
		    && uDisp.getValue().getType().getName().equals(
			    disp.getValue().getType().getName())) {
		iter.remove();
		notifyListenersRemoved(disp);
		return true;
	    }
	}

	return false;
    }

    /**
     * Clears all of the variables currently being watched
     * 
     */
    public void clearVariables() {
	Iterator iter = vars.iterator();
	while (iter.hasNext())
	    notifyListenersRemoved((UpdatingDisplayValue) iter.next());
	vars.clear();
    }

    /**
     * 
     * @return An iterator to the variables currently being watched
     */
    public Iterator getVariableIterator() {
	return vars.iterator();
    }

    /**
     * Retrieves an iterator to the descriptions that contain the data to
     * recreate the variable watches.
     * 
     * @return An iterator that iterates through the watch descriptions
     */
    public Iterator getDescriptionIterator() {
	return descriptions.iterator();
    }

    /**
     * Adds a listener to be notified when the list of watched variables
     * changes.
     * 
     * @param obj
     *                The object to be notified
     */
    public void addListener(WatchListListener obj) {
	listeners.add(obj);
    }

    /**
     * Get the number of variables currently being watched
     * 
     * @return The number of variables currently being watched
     */
    public int getWatchSize() {
	return vars.size();
    }

    /**
     * Get the number of descriptions that are contained in this watch list.
     * 
     * @return The number of variable descriptions being stored.
     */
    public int getDescriptionSize() {
	return descriptions.size();
    }

    /**
     * Removes a listener from the objects to be notified
     * 
     * @param obj
     *                The listener to remove
     * @return True if the listener was removed, false otherwise
     */
    public boolean removeListener(WatchListListener obj) {
	return listeners.remove(obj);
    }

    public void doSaveObject() {
	shouldSave = true;
    }

    public void dontSaveObject() {
	shouldSave = false;
    }

    public void load(Element node) {
	/*
         * Load the descriptions into a WatchDescription, and store it until
         * needed.
         */
	Iterator iter = node.getChildren("variable").iterator();
	while (iter.hasNext()) {
	    Element elem = (Element) iter.next();
	    WatchDescription desc = new WatchDescription(elem
		    .getAttributeValue("type"), elem.getAttributeValue("text"),
		    "file",// elem.getAttributeValue("filePath"),
		    0,// Integer.parseInt(elem.getAttributeValue("line")),
		    0// Integer.parseInt(elem.getAttributeValue("col"))
	    );
	    descriptions.add(desc);
	}
    }

    public void save(Element node) {
	if (!shouldSave)
	    return;

	Iterator iter = vars.iterator();
	while (iter.hasNext()) {
	    UpdatingDisplayValue disp = (UpdatingDisplayValue) iter.next();
	    Element varNode = new Element("variable");

	    // A variable may not necessarially have a file/line/colum.
	    // For instance a "register" can be represented as a variable.
	    // For instance, is this the line that the variable was
	    // declared, or the line at which the variable is being
	    // examined, or the most recent line at which the program
	    // stopped and the variable was live.
	    //
	    // A better separation of abstraction is needed. That way,
	    // when it comes to saving, storing, or manipulating what is
	    // being watched, these lower-level variables are not
	    // involved. For instance, the text of what was requested, or
	    // the expression of what was requested, could be saved.
	    // Either of which can then be possibly mapped onto a
	    // Variable.
	    //
	    // Further, underlying all this there appears to be a more
	    // fundamental problem. What happens as a variable moves into
	    // or out of scope? Variable isn't intended to indicate that.
	    // What happens as a variable's location changes? Variable
	    // isn't intended to indicate that. Likely there needs to be
	    // a frysk-core Watch object that worries about all that
	    // stuff; kind of a peer to the Breakpoint object that worries
	    // about code being loaded and un-loaded.

	    varNode.setAttribute("expr", disp.getName());
	    varNode.setAttribute("process", disp.getTask().getProc().getExe());
	    varNode.setAttribute("frame", disp.getFrameIdentifier().toString());

	    node.addContent(varNode);
	}
    }

    public boolean shouldSaveObject() {
	return shouldSave;
    }

    protected void notifyListenersAdded(UpdatingDisplayValue disp) {
	Iterator iter = listeners.iterator();
	while (iter.hasNext())
	    ((WatchListListener) iter.next()).variableWatchAdded(disp);
    }
    
    protected void notifyListenersUpdated(UpdatingDisplayValue disp) {
	Iterator iter = listeners.iterator();
	while (iter.hasNext())
	    ((WatchListListener) iter.next()).variableWatchChanged(disp);
    }

    protected void notifyListenersRemoved(UpdatingDisplayValue disp) {
	Iterator iter = listeners.iterator();
	while (iter.hasNext())
	    ((WatchListListener) iter.next()).variableWatchDeleted(disp);
    }
}
