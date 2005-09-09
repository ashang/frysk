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

/**
 * Host Errors, thrown by this directory.
 *
 */

package frysk.sys;

public class Errno
    extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    static public class Ebadf
	extends Errno
    {
        private static final long serialVersionUID = 1L;

    protected Ebadf (String message)
	{
	    super (message);
	}
    }
    static public class Enomem
	extends Errno
    {
        private static final long serialVersionUID = 1L;

    protected Enomem (String message)
	{
	    super (message);
	}
    }
    static public class Efault
	extends Errno
    {
        private static final long serialVersionUID = 1L;

    protected Efault (String message)
	{
	    super (message);
	}
    }
    static public class Einval
	extends Errno
    {
        private static final long serialVersionUID = 1L;

    protected Einval (String message)
	{
	    super (message);
	}
    }
    static public class Esrch
	extends Errno
    {
        private static final long serialVersionUID = 1L;

    protected Esrch (String message)
	{
	    super (message);
	}
    }
    static public class Echild
	extends Errno
    {
        private static final long serialVersionUID = 1L;
	protected Echild (String message)
	{
	    super (message);
	}
    }

    private String message;
    public String toString ()
    {
	return message;
    }
    protected Errno (String message)
    {
	this.message = message;
    }

    protected Errno ()
    {
	this.message = "internal error";
    }
}
