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

package frysk.sys.termios;

import gnu.gcj.RawDataManaged;
import frysk.sys.FileDescriptor;

/**
 * Manipulates a terminal bound to FileDescriptor.
 */
public final class Termios
{
    private native RawDataManaged malloc();
    final RawDataManaged termios = malloc (); // package readable.

    public Termios ()
    {
    }

    public Termios (FileDescriptor fd)
    {
	this ();
	get (fd);
    }

    /**
     * Refresh this Termios with the terminal settings from FD.
     */
    public native Termios get (FileDescriptor fd);
    /**
     * Set FD's terminal settings.
     */
    public native Termios set (FileDescriptor fd, Action action);
    /**
     * Set FD's terminal settings.
     */
    public Termios set (FileDescriptor fd)
    {
	return set (fd, Action.NOW);
    }

    /**
     * Enable or disable a mode.
     */
    public Termios set (Mode mode, boolean on)
    {
	return mode.set (this, on);
    }
    /**
     * Get a mode.
     */
    public boolean get (Mode mode)
    {
	return mode.get (this);
    }

    /**
     * Set the speed of input and output in baud.
     */
    public Termios set (Speed speed)
    {
	return speed.set (this);
    }
    /**
     * Get the input speed in baud.
     */
    public Speed getInputSpeed ()
    {
	return Speed.getInput (this);
    }
    /**
     * Get the output speed in baud.
     */
    public Speed getOutputSpeed ()
    {
	return Speed.getOutput (this);
    }

    /**
     * Set special character field to val.
     */
    public Termios set (Special special, char val)
    {
	return special.set (this, val);
    }
    /**
     * Get special character field.
     */
    public char get (Special special)
    {
	return special.get (this);
    }

    /**
     * Adjust Termios so that it is "raw".
     */
    public native Termios setRaw ();

    /**
     * Sends a continuous stream of zero-valued bits - the BREAK.
     */
    public static native void sendBreak (FileDescriptor fd, int duration);
    /**
     * Waits until all output has been sent.
     */
    public static native void drain (FileDescriptor fd);
}
