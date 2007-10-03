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

package frysk.expunit;

/**
 * Match the specified input, and remember what matched.  Loosely
 * modeled on the corresponding Java Pattern and Matcher objects.
 */
abstract class Match
{
    /**
     * Find the pattern in the output, normally this is an unanchored
     * match.
     */
    abstract boolean find (String output);
    /**
     * Return the number of matched groups.
     */
    protected int groupCount ()
    {
	return 0;
    }
    /**
     * Return matched group G.
     */
    protected String group (int g)
    {
	// In build, Wall warns about g not being used and Werror
	// turns the warning into a build-killing error.
	// This hack gets around that until such time as ecj/gcj
	// gets less pedantic or this method gets some actual contents.
	// return null;
	return ((g == 0) ? null : null); 
    }
    /**
     * Return the entire matched pattern (group 0).
     */
    protected final String group ()
    {
	return group (0);
    }
    /**
     * Return the start of matched group G.
     */
    protected int start (int g)
    {
	// In build, Wall warns about g not being used and Werror
	// turns the warning into a build-killing error.
	// This hack gets around that until such time as ecj/gcj
	// gets less pedantic or this method gets some actual contents.
	// return -1;
	return (g - g) - 1;
    }
    /**
     * Return the start of the entire matched pattern (group 0).
     */
    protected final int start ()
    {
	return start (0);
    }
    /**
     * Return the end of matched group G.
     */
    protected int end (int g)
    {
	// In build, Wall warns about g not being used and Werror
	// turns the warning into a build-killing error.
	// This hack gets around that until such time as ecj/gcj
	// gets less pedantic or this method gets some actual contents.
	// return 0;
	return g - g;	// temporary hack to make Werror happy
    }
    /**
     * Return the end of the entire matched pattern (group 0) plus 1.
     */
    protected final int end ()
    {
	return end (0);
    }
    public void execute ()
    {
	// pass by default.
    }
}

