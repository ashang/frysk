// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// type filter text
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

import frysk.junit.Paths;
import frysk.junit.TestCase;
import frysk.gui.common.IconManager;
import org.gnu.gtk.IconFactory;
import org.gnu.gtk.Gtk;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class TestIconManager
    extends TestCase
{

  IconFactory defaultSet = null;

  public void testIconManagerLoad ()
  {
    Gtk.init(new String[] {});

    // Start loading factory. Need to improve this test as we are
    // basically looking for a stacktrace.
    IconManager.setImageDir(new String[] { Paths.getImagePrefix () });
    IconManager.loadIcons();
    IconManager.useSmallIcons();
  }

  public void testIconManagerGetFactory ()
  {

    // Cheat a bit and get the factory
    // if it is not null, the factory was returned
    // succesfully.
    defaultSet = IconManager.getFactory();
    assertNotNull("Testing getFactory", defaultSet);
  }

  public void testIconLookups ()
  {

    defaultSet = IconManager.getFactory();

    // Basically manually grind through each icon lookup and test
    // to make sure if the IconSet returned is not null.
    assertNotNull("Testing frysk-run icon set lookup",
                  defaultSet.lookupIconSet("frysk-run"));
    assertNotNull("Testing frysk-stop icon set lookup",
                  defaultSet.lookupIconSet("frysk-stop"));

    assertNotNull("Testing frysk-next icon set lookup",
                  defaultSet.lookupIconSet("frysk-next"));
    assertNotNull("Testing frysk-nextAI icon set lookup",
                  defaultSet.lookupIconSet("frysk-nextAI"));

    assertNotNull("Testing frysk-step icon set lookup",
                  defaultSet.lookupIconSet("frysk-step"));
    assertNotNull("Testing frysk-stepAI icon set lookup",
                  defaultSet.lookupIconSet("frysk-stepAI"));
    assertNotNull("Testing frysk-continue icon set lookup",
                  defaultSet.lookupIconSet("frysk-continue"));
    assertNotNull("Testing frysk-finish icon set lookup",
                  defaultSet.lookupIconSet("frysk-finish"));
    assertNotNull("Testing frysk-down icon set lookup",
                  defaultSet.lookupIconSet("frysk-down"));
    assertNotNull("Testing frysk-up icon set lookup",
                  defaultSet.lookupIconSet("frysk-up"));
    assertNotNull("Testing frysk-bottom icon set lookup",
                  defaultSet.lookupIconSet("frysk-top"));

    assertNotNull("Testing frysk-highlight icon set lookup",
                  defaultSet.lookupIconSet("frysk-highlight"));

    assertNotNull("Testing splash-screen", IconManager.splashImage);
    assertNotNull("Testing window-icon", IconManager.windowIcon);
    assertNotNull("Testing animation", IconManager.anim);
  }

  /*
   * testgetLocalHost will test whether or not a call to getLocalHost returns a
   * valid value or creates an exception.
   */
  public void testgetLocalHost ()
  {
    InetAddress addr = null;
    try
      {
        addr = InetAddress.getLocalHost();
      }
    catch (UnknownHostException e)
      {
        assertNull("Testing getLocalHost", addr);

      }
    assertNotNull("Testing getLocalHost", addr);
  }
}
