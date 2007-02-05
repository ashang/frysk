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

package frysk.gui.test;

import frysk.Config;
import frysk.junit.TestCase;

import frysk.proc.Task;
import frysk.gui.common.IconManager;
import frysk.gui.memory.MemoryWindow;
import frysk.gui.register.RegisterWindow;
import frysk.proc.Manager;
import frysk.proc.DummyProc;
import frysk.proc.DummyTask;
import org.gnu.glade.LibGlade;
import org.gnu.gtk.Gtk;

//import org.gnu.glib.CustomEvents;

public class TestWindowFactorization
    extends TestCase
{
  private Task theTask = null;

  private LibGlade gladem = null;

  private LibGlade glader = null;

  private MemoryWindow mw = null;

  private RegisterWindow rw = null;

  public void setUp ()
  {
    Gtk.init(new String[] {});
    IconManager.setImageDir(new String[] { Config.getImageDir () });
    IconManager.loadIcons();
    IconManager.useSmallIcons();
  }

  public void testWindowFactorization ()
      throws org.gnu.glade.GladeXMLException,
	     java.io.FileNotFoundException,
	     java.io.IOException
  {
    DummyProc dp = new DummyProc();
    DummyTask dt = new DummyTask(dp);
    this.theTask = (Task) dt;

    Manager.eventLoop.start();

    for (int j = 0; j < 15; j++)
      {
        //System.out.println("#" + j);
        initGlades();

        mw = new MemoryWindow(gladem);
        rw = new RegisterWindow(glader);

        setTasks();

        System.gc();
//        mw.showAll();
//        mw.hideAll();
        mw = null;

        System.gc();
//        rw.showAll();
//        rw.hideAll();
        rw = null;

        // theTask = null;
      }
  }

  public void initGlades ()
      throws org.gnu.glade.GladeXMLException,
	     java.io.FileNotFoundException,
	     java.io.IOException
  {
      gladem = new LibGlade (Config.getGladeDir () + "memorywindow.glade", null);
      glader = new LibGlade (Config.getGladeDir () + "registerwindow.glade", null);
      MemoryWindow mw = new MemoryWindow(gladem);
      mw.getClass();
      RegisterWindow rw = new RegisterWindow(glader);
      rw.getClass();
  }

  public void setTasks ()
  {
    if (theTask.getMemory() != null)
      {
        mw.setTask(theTask);
        //System.out.println("Memory Set!");
      }
//    else
//      System.out.println("Memory is null");

    try
      {
        if (theTask.getIsa() != null)
          rw.setTask(theTask);
//        else
//          System.out.println("ISA is null");
      }
    catch (Exception e)
      {
        System.exit(1);
      }

  }
}
