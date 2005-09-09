// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

package frysk.proc;

/**
 * Light weight identifier.
 *
 * This implements comparable so can be used for searching and the
 * like.  Anything wanting to search for a task should use this.
 */

class Id
    implements Comparable
{
    int id;
    protected Id ()
    {
	id = -1;
    }
    protected Id (int id)
    {
	this.id = id;
    }
    public boolean equals (Object o)
    {
	// if (o instanceof Id)
	if (o.getClass ().isInstance (this))
	    return ((Id)o).id == id;
	else
	    return false;
    }
    public int hashCode ()
    {
	return id;
    }
    public int compareTo (Object o)
    {
	if (o.getClass ().isInstance (this))
	    return ((Id)o).id - id;
	return
	    -1;
    }
    public String toString ()
    {
	return "" + id;
    }
}
