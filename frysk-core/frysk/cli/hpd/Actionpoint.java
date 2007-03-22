// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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

package frysk.cli.hpd;

import java.io.PrintWriter;

import frysk.rt.SourceBreakpoint;
/**
 * Hpd actionpoints. Actionpoints include things like breakpoints and
 * watchpoints.
 */
public abstract class Actionpoint
{
  // Possible actionpoint states
  public static class State
  {
    State(String name)
    {
      this.name = name;
    }
    
    private String name;

    public String toString()
    {
      return name;
    }
  }

  /**
   * Possible states of an action point.
   */
  
  /**
   * The action point is enabled and will be "fired" when hit.
   */
  public static final State ENABLED = new State("enabled");

  /**
   * The action point is disabled and should have no effect in the
   * running process.
   */
  public static final State DISABLED = new State("disabled");

  /**
   * The action point is deleted and should not be reenabled.
   */
  public static final State DELETED = new State("deleted");

  private State state;

  /**
   * Get the state of an action point.
   * @return state constant object
   */
  public State getState()
  {
    return state;
  }

  /**
   * Create an action point with a given state.
   * @param state the action point's initial state.
   */
  public Actionpoint(State state)
  {
    this.state = state;
  }

  /**
   * Create an action point in the DISABLED state.
   */
  public Actionpoint()
  {
    this(DISABLED);
  }

  /**
   * Set the action point's state to enabled.
   */
  public void enable()
  {
    state = ENABLED;
  }

  /**
   * Set the action point's state to disabled.
   */
  public void disable()
  {
    state = DISABLED;
  }

  /**
   * Set the action point's state to deleted.
   */
  public void delete()
  {
    state = DELETED;
  }

  /**
   * Print a representation of the actionpoint to the writer.
   * @param writer the output destination
   * @return the writer, for chaining method invocations.
   */
  public abstract PrintWriter output(PrintWriter writer);

  protected SourceBreakpoint rtBreakpoint;

  /**
   * Get the Run Time layer breakpoint object associated with this
   * action point.
   */
  public SourceBreakpoint getRTBreakpoint()
  {
    return rtBreakpoint;
  }

  /**
   * Set the Run Time layer breakpoint object associated with this
   * action point.
   */
  public void setRTBreakpoint(SourceBreakpoint rtBreakpoint)
  {
    this.rtBreakpoint = rtBreakpoint;
  }
  
}
