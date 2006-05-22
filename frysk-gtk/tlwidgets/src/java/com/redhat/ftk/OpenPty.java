/*
 * Java-Gnome Bindings Library
 *
 * Copyright 1998-2004 the Java-Gnome Team, all rights reserved.
 *
 * The Java-Gnome bindings library is free software distributed under
 * the terms of the GNU Library General Public License version 2.
 */

//package org.gnu.gtk;
package com.redhat.ftk;

/**
 * The OpenPty class opens a pty and provides a pty string for libvte-java
 */
public class OpenPty
{
    private int master;
    private String name;

    static {
	System.loadLibrary ("ftk");
	System.loadLibrary ("ftkjni");
    }

    public OpenPty() {
        master = openpty_frysk();
	name   = ptsname_frysk(master);
    }

    public int getMaster() {
        return master;
    }

    public String getName() {
        return name;
    }
	
    native static final protected int
	openpty_frysk ();

    native static final protected String
	ptsname_frysk ();
}

