// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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


package frysk.proc;

import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;

import frysk.event.RequestStopEvent;

public class TestGetProc
    extends TestLib
{
  class ProcCounter
      implements Observer
  {

    int count = 0;

    public void update (Observable o, Object arg)
    {
      count++;
    }

    public int getCount ()
    {
      return count;
    }
  }

  class MyFinder
      implements Host.FindProc
  {
    ProcId expectedId;

    boolean shouldParent;
    public MyFinder (ProcId pid, boolean shouldHaveParent)
    {
      expectedId = pid;
      shouldParent = shouldHaveParent;
    }

    public void procFound (ProcId procId)
    {
      Proc proc = Manager.host.getProc(procId);
      assertEquals(expectedId, procId);
      if (!shouldParent)
        assertNull("Should not have parent", proc.parent);
      else
        assertNotNull("Should have parent", proc.parent);
        
      Manager.eventLoop.add(new RequestStopEvent(Manager.eventLoop));
    }

    public void procNotFound (ProcId procId, Exception e)
    {
      logger.log(Level.FINE, "{0} procId\n", procId);
      fail("Could not find process with ID" + procId.id);
    }
  }

  private void doGetProc (AckProcess ackProc, int expectedCount, boolean shouldHaveParent)
  {
    logger.log(Level.FINE, "doGetProc ackProc {0}\n", ackProc);
    ProcCounter o = new ProcCounter();
    Manager.host.observableProcAddedXXX.addObserver(o);

    /*
     * This finds out how many processes are associated with the frysk process.
     * For example: init->gnome terminal->bash->frysk.
     */
    Manager.host.getSelf();
    int preFind = o.getCount();

    /*
     * Find out how many processes are associated with the test process. Should
     * be just the one.
     */
    Host.FindProc finder = new MyFinder(new ProcId(ackProc.getPid()), shouldHaveParent);
    Manager.host.requestGetProc(new ProcId(ackProc.getPid()), finder);
    assertRunUntilStop("testGetProc");

    int postFind = o.getCount();

    assertEquals(expectedCount, postFind - preFind);
    logger.log(Level.FINE, "doGetProc exiting");
  }

  public void testGetAndRefreshFailed ()
  {

    Host.FindProc finder = new Host.FindProc()
    {
      public void procFound (ProcId procId)
      {
        logger.log(Level.FINE, "{0} procId\n", procId);
        fail("Found proc 0, should have failed.");
      }

      public void procNotFound (ProcId procId, Exception e)
      {
        logger.log(Level.FINE, "{0} procId\n", procId);
        Manager.eventLoop.add(new RequestStopEvent(Manager.eventLoop));

      }
    };

    Manager.host.requestGetProc(new ProcId(0), finder);
    assertRunUntilStop("testFindFailed");

  }
  
  public void testGetProcDetached ()
  {
    AckProcess ackProc = new DetachedAckProcess();
    doGetProc(ackProc, 1, false);  
  }

  public void testGetProcAttached ()
  {
    AckProcess ackProc = new AttachedAckProcess();

    // expect no additional processes to be added to the procPool.
    doGetProc(ackProc, 0, true);
  }

  public void testGetProcAckDaemon ()
  {
    AckProcess ackProc = new AckDaemonProcess();
    doGetProc(ackProc, 1, false);   
  }
  
}
