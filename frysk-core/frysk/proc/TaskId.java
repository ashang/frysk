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
 * Light weight identifier for a task.
 *
 * This implements comparable so can be used for searching and the
 * like.  Anything wanting to search for a task should use this.
 */

class TaskId
    extends Id
{
    TaskId ()
    {
	super ();
    }
    public TaskId (int id)
    {
	super (id);
    }
    public String toString ()
    {
	return ("{TaskId," + super.toString () + "}");
    }
}
