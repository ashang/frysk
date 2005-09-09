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

package frysk.sys;

public final class Fork
{
    /**
     * Create a child process running ARGV[0] with arguments
     * ARGV[1..].
     */
    public static native int exec (String in, String out, String err,
				    String[] argv);
    public static final int exec (String[] argv)
    {
	return exec (null, null, null, argv);
    }

    /**
     * Create a child process marked for tracing.
     */
    public static native int ptrace (String in, String out, String err,
				     String[] argv);
    public static final int ptrace (String[] argv)
    {
	return exec (null, null, null, argv);
    }

    /**
     * Create a "daemon" process (A daemon has process ID 1 as its
     * parent).
     */
    public static native int daemon (String in, String out, String err,
				     String[] argv);
    public static final int daemon (String[] argv)
    {
	return daemon (null, null, null, argv);
    }
}
