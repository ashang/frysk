// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

package frysk.hpd;

import java.lang.IllegalArgumentException;

class Message
{
	public static int TYPE_DBG_ERROR = 0;
	public static int TYPE_ERROR = 1;
	public static int TYPE_WARNING = 2;
	public static int TYPE_NORMAL = 3;
	public static int TYPE_VERBOSE = 4;

	private final String msg;
	private final int type;
        private final Throwable exc;

        /**
	 * Creates a new Message with the given message and type
	 * and no exception.
	 */
        public Message (String msg, int type)
        {
	    this(msg, type, null);
	}  
        /**
	 * Creates a new Message with the given message and type.
	 * The message cannot be null or empty. The exception is optional
	 * and may be null.
	 */
        public Message (String msg, int type, Throwable exc)
	{
	        if (msg == null)
		    throw new NullPointerException("null msg");

	        if (msg.equals(""))
		    throw new IllegalArgumentException("empty msg");

		this.msg = msg;

		if (type < TYPE_DBG_ERROR || type > TYPE_VERBOSE)
			throw new IllegalArgumentException("Debugger message created with illegal type.");
		else
			this.type = type;

		this.exc = exc;
	}

	public String getMessage()
	{
		return msg;
	}

	public int getType()
	{
		return type;
	}

        public Throwable getException()
        {
	        return exc;
        }
}
