// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

import frysk.sys.FileDescriptor;

/**
 * Manipulates a terminal bound to FileDescriptor.
 */
public final class Termios {
    private native long malloc();
    private native void free(long termios);
    private long termios = malloc();
    protected void finalize() {
	if (termios != 0) {
	    free(termios);
	    termios = 0;
	}
    }

    public Termios() {
    }

    public Termios(FileDescriptor fd) {
	this();
	get(fd);
    }

    /**
     * Refresh this Termios with the terminal settings from FD.
     */
    public Termios get(FileDescriptor fd) {
	get(termios, fd.getFd());
	return this;
    }
    private static native void get(long termios, int fd);
    /**
     * Set FD's terminal settings.
     */
    public Termios set(FileDescriptor fd, Action action) {
	set(termios, fd.getFd(), action);
	return this;
    }
    private static native void set(long termios, int fd, Action action);
    /**
     * Set FD's terminal settings.
     */
    public Termios set(FileDescriptor fd) {
	return set(fd, Action.NOW);
    }

    /**
     * Enable or disable a mode.
     */
    public Termios set(Mode mode, boolean on) {
	mode.set(termios, on);
	return this;
    }
    /**
     * Get a mode.
     */
    public boolean get(Mode mode) {
	return mode.get(termios);
    }

    /**
     * Set the speed of input and output in baud.
     */
    public Termios set(Speed speed) {
	speed.set(termios);
	return this;
    }
    /**
     * Get the input speed in baud.
     */
    public Speed getInputSpeed() {
	return Speed.getInput(termios);
    }
    /**
     * Get the output speed in baud.
     */
    public Speed getOutputSpeed() {
	return Speed.getOutput(termios);
    }

    /**
     * Set special character field to val.
     */
    public Termios set(Special special, char val) {
	special.set(termios, val);
	return this;
    }
    /**
     * Get special character field.
     */
    public char get(Special special) {
	return special.get(termios);
    }

    /**
     * Adjust Termios so that it is "raw".
     */
    public Termios setRaw() {
	setRaw(termios);
	return this;
    }
    private native void setRaw(long termios);

    /**
     * Sends a continuous stream of zero-valued bits - the BREAK.
     */
    public static void sendBreak(FileDescriptor fd, int duration) {
	sendBreak(fd.getFd(), duration);
    }
    private static native void sendBreak(int fd, int duration);
    /**
     * Waits until all output has been sent.
     */
    public static void drain(FileDescriptor fd) {
	drain(fd.getFd());
    }
    private static native void drain(int fd);
}
