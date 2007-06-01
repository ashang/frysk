// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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


package frysk.gui.terminal;

import org.gnu.gtk.Window;
import org.gnu.gtk.WindowType;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gdk.Color;
import org.gnu.gnomevte.Terminal;
import frysk.sys.PseudoTerminal;

public class TermWindow
    extends Window
{
  private Terminal term;
  
  private String name;

  public TermWindow ()
  {
    super(WindowType.TOPLEVEL);

    this.setTitle("Frysk Terminal Window");
    this.addListener(new LifeCycleListener()
    {
      public void lifeCycleEvent (LifeCycleEvent event)
      {
      }

      public boolean lifeCycleQuery (LifeCycleEvent event)
      {
        if (event.isOfType(LifeCycleEvent.Type.DESTROY)
            || event.isOfType(LifeCycleEvent.Type.DELETE))
          TermWindow.this.shutDown();

        return true;
      }
    });

    PseudoTerminal pty = new PseudoTerminal ();

    int master = pty.getFd();
    System.out.println("TermWindow: master = " + master);
    this.name = pty.getFile ().getPath ();

    this.term = new Terminal();
    this.term.setPty(master);

    this.term.setDefaultColors();
    this.term.setBackgroudColor(Color.WHITE);
    this.term.setForegroundColor(Color.BLACK);
    this.term.setSize(80, 25);

    this.add(term);
    this.showAll();
  }
  
  /**
   * getPts returns the pts string for this terminal
   *
   */
  public String getPts()
  {
    return this.name;
  }
  
  /**
   * setWindowTitle sets the title of the window
   *
   *@param path is the full path to the process
   */
  public void setWindowTitle(String path)
  {
    this.setTitle("Frysk Terminal Window for: " + path);
  }
  

  private void shutDown ()
  {
    this.destroy();
  }
}
