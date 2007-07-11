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

package frysk.rt;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import frysk.proc.Task;
import frysk.stack.FrameIdentifier;

/**
 * The DisplayManager is in charge of keeping track of the UpdatingDisplayValues
 * currently in existence
 * 
 */
public class DisplayManager {

    /*
     * We use a map to keep track of a list of dislays for each task
     */
    private static DisplayMap displays;

    private static HashMap displaysByNum;

    static {
	displays = new DisplayMap();
	displaysByNum = new HashMap();
    }

    /**
     * Creates a new UpdatingDisplayValue if no such display exists yet. If
     * one already exists it is returned instead
     * 
     * @param myTask
     *                The task the displayed expression is from
     * @param fIdent
     *                The frame the displayed expression is from
     * @param engine
     *                The stepping engine controlling the task
     * @param text
     *                The expression text to create a display for
     * @return
     */
    public static UpdatingDisplayValue createDisplay(Task myTask,
	    FrameIdentifier fIdent, SteppingEngine engine, String text) {
	UpdatingDisplayValue value = displays.get(myTask, fIdent, text);
	if (value == null) {
	    value = new UpdatingDisplayValue(text, myTask, fIdent, engine,
		    CountManager.getNextId());
	    displays.add(value);
	    displaysByNum.put(new Integer(value.getId()), value);
	}

	return value;
    }

    /**
     * Removes an UpdatingDisplayValue from the manager, allowing it to be
     * garbage collected
     * 
     * @param value
     *                The display to remove
     */
    public static void deleteDisplay(UpdatingDisplayValue value) {
	displays.remove(value);
	displaysByNum.remove(new Integer(value.getId()));
	value.disable();
    }

    /**
     * Disables the display with the given identifier. If no display exists
     * with that identifier nothing is done.
     * 
     * @param num
     *                The unique ID of the display to disable
     * @return true if a display was disabled, false otherwise
     */
    public static boolean disableDisplay(int num) {
	UpdatingDisplayValue val = (UpdatingDisplayValue) displaysByNum
		.get(new Integer(num));
	if (val != null) {
	    if (val.isEnabled())
		val.disable();
	    return true;
	}
	return false;
    }

    /**
     * Enables the display with the given identifier. If no display exists
     * with that number nothing is done
     * 
     * @param num
     *                The unique ID of the identifier to enable
     * @return True if an identifier was enabled, false otherwise
     */
    public static boolean enableDisplay(int num) {
	UpdatingDisplayValue val = (UpdatingDisplayValue) displaysByNum
		.get(new Integer(num));

	if (val != null) {
	    if (!val.isEnabled())
		val.enable();
	    return true;
	}

	return false;
    }

    /**
     * Deletes the display with the given identifier. If no display exists
     * with that identifier nothing is done.
     * 
     * @param num
     *                The unique ID of the display to delete
     * @return true if a display was deleted, false otherwise
     */
    public static boolean deleteDisplay(int num) {
	UpdatingDisplayValue val = (UpdatingDisplayValue) displaysByNum
		.get(new Integer(num));
	if (val != null) {
	    if (val.isEnabled())
		val.disable();
	    displays.remove(val);
	    displaysByNum.remove(new Integer(val.getId()));
	    return true;
	}
	return false;
    }

    /**
     * Allows retrieval of all the displays being tracked by this manager
     * 
     * @return An iterator of all the displays known to the manager
     */
    public static Iterator getDisplayIterator() {
	return displaysByNum.values().iterator();
    }

    /**
     * Retreive the display with the provided identifier if it exists. If no
     * display exists then null is returned.
     * 
     * @param id
     *                The identifier of the display to fetch
     * @return The display with the given id, or null if no such display
     *         exists
     */
    public static UpdatingDisplayValue getDisplay(int id) {
	return (UpdatingDisplayValue) displaysByNum.get(new Integer(id));
    }

}

/*
 * DisplayMap encapsulates a series of nested HashMaps that hash first on Task,
 * then FrameIdentifier, then the expression text.
 */
class DisplayMap {
    private Map taskMap;

    public DisplayMap() {
	taskMap = Collections.synchronizedMap(new HashMap());
    }

    /*
     * Attempts to retrieve the DisplayValue corresponding to the given
     * task, frame identifier, and expression string. Returns null if no
     * such display exists.
     */
    public UpdatingDisplayValue get(Task myTask, FrameIdentifier fIdent,
	    String text) {
	if (!taskMap.containsKey(myTask))
	    return null;

	Map frameMap = (Map) taskMap.get(myTask);
	if (!frameMap.containsKey(fIdent))
	    return null;

	Map exprMap = (Map) frameMap.get(fIdent);
	if (!exprMap.containsKey(text))
	    return null;

	return (UpdatingDisplayValue) exprMap.get(text);
    }

    /*
     * Tries to add the given DisplayValue to the DisplayMap. All nested
     * data structures are created automatically if they do not already
     * exist, and if a record of v already exists this is a no-op.
     */
    public void add(UpdatingDisplayValue v) {
	Map frameMap, exprMap;
	/*
         * Create the frame-level map for the display's task if it doesn't
         * already exist
         */
	if (!taskMap.containsKey(v.getTask())) {
	    frameMap = Collections.synchronizedMap(new HashMap());
	    taskMap.put(v.getTask(), frameMap);
	} else
	    frameMap = (Map) taskMap.get(v.getTask());

	/*
         * Create the expression-level map for the given frame if it doesn't
         * already exist
         */
	if (!frameMap.containsKey(v.getFrameIdentifier())) {
	    exprMap = Collections.synchronizedMap(new HashMap());
	    frameMap.put(v.getFrameIdentifier(), exprMap);
	} else
	    exprMap = (Map) frameMap.get(v.getFrameIdentifier());

	/*
         * Add the DisplayValue to the expression-level map if it does not
         * already exist there
         */
	if (!exprMap.containsKey(v.getName()))
	    exprMap.put(v.getName(), v);
    }

    /*
     * Removes the provided display from the data structure, if it exists.
     */
    public void remove(UpdatingDisplayValue value) {
	if (!taskMap.containsKey(value.getTask()))
	    return;

	Map frameMap = (Map) taskMap.get(value.getTask());
	if (!frameMap.containsKey(value.getFrameIdentifier()))
	    return;

	Map exprMap = (Map) frameMap.get(value.getFrameIdentifier());
	if (!exprMap.containsKey(value.getName()))
	    return;
	exprMap.remove(value);

	/* Clean out empty data structures if they exist */
	if (exprMap.isEmpty())
	    frameMap.remove(exprMap);
	if (frameMap.isEmpty())
	    taskMap.remove(frameMap);
    }
}
