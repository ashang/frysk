/*
 * Java-Gnome Bindings Library
 *
 * Copyright 1998-2004 the Java-Gnome Team, all rights reserved.
 *
 * The Java-Gnome bindings library is free software distributed under
 * the terms of the GNU Library General Public License version 2.
 */


package com.redhat.ftk;

public class SimultaneousEvent
{
  int trace;

  int marker;

  String string;

  public SimultaneousEvent (int t, int m, String s)
  {
    this.trace = t;
    this.marker = m;
    this.string = s;
  }
}
