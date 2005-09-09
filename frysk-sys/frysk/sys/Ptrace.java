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

public final class Ptrace
{
    public static native void attach (int pid);
    public static native void detach (int pid, int sig);
    public static native void singleStep (int pid, int sig);
    public static native void cont (int pid, int sig);
    public static native void sysCall (int pid, int sig);
    public static native long getEventMsg (int pid);
    // Uses TRACEME.
    public native static int child (String in, String out, String err,
				    String[] args);
    public static native void setOptions (int pid, long options);
    public static native long optionTraceClone ();
    public static native long optionTraceFork ();
    public static native long optionTraceExit ();
    public static native long optionTraceSysgood ();
    public static native long optionTraceExec ();
}
