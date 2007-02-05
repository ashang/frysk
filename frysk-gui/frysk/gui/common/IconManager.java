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

package frysk.gui.common;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.gnu.gdk.Pixbuf;
import org.gnu.gdk.PixbufAnimation;
import org.gnu.gtk.IconFactory;
import org.gnu.gtk.IconSet;
import org.gnu.gtk.IconSize;
import org.gnu.gtk.IconSource;

import frysk.Config;
import frysk.gui.monitor.TrayIcon;

public class IconManager
{

  // Image files - search bar
  public static final String FIND_NEXT_PNG = "findNext.png"; //$NON-NLS-1$

  public static final String FIND_PREV_PNG = "findPrev.png"; //$NON-NLS-1$

  public static final String FIND_GO_PNG = "findGo.png"; //$NON-NLS-1$

  public static final String HIGHLIGHT_PNG = "highlight.png"; //$NON-NLS-1$

  // Names of the image files - Toolbar
  private static final String RUN_PNG = "stock-execute.png";

  private static final String STOP_PNG = "stop.png";

  private static final String STEP_PNG = "stepI.png"; //$NON-NLS-1$

  private static final String NEXT_PNG = "nextI.png"; //$NON-NLS-1$

  private static final String FINISH_PNG = "finish.png"; //$NON-NLS-1$

  private static final String CONTINUE_PNG = "continue.png"; //$NON-NLS-1$

  private static final String STEP_AI_PNG = "stepAI.png"; //$NON-NLS-1$

  private static final String NEXT_AI_PNG = "nextAI.png"; //$NON-NLS-1$

  private static final String DOWN_PNG = "down.png"; //$NON-NLS-1$

  private static final String UP_PNG = "up.png"; //$NON-NLS-1$

  private static final String TOP_PNG = "top.png"; //$NON-NLS-1$

  // Tray Icon prefix
  private static final String TRAY_PREFIX = "fryskTrayIcon";

  public static IconFactory[] factories;

  private static String[] sizeDirs = new String[] { "16", "24", "32" };

  public static PixbufAnimation anim;

  public static Pixbuf windowIcon;

  public static Pixbuf splashImage;

  private static boolean useSmallIcons = true;

  public static final String os_name = System.getProperty("os.name");

  public static final String os_version = System.getProperty("os.version");

  public static final String os_arch = System.getProperty("os.arch");

  /*
   * getHostInfo gets host information to pass to the tooltip for the
   * Frysk eggtrayicon.  This info is displayed when the icon is hovered
   * over.
   * 
   * @param which - tells which piece of info to get
   *                hostname = return the host's name
   *                ipaddr = return the host's ip address
   *                
   * @return the information requested
   */
  public static String getHostInfo (String which)
  {
    String host_name = "";
    String ip_addr = "";
    try
      {
        InetAddress addr = InetAddress.getLocalHost();
        ip_addr = addr.getHostAddress();
        host_name = addr.getHostName();
      }
    catch (UnknownHostException e)
      {
        System.err.println("Unable to get host name or ip address from "
                           + "getHostAddress()/getHostName()");
        return "Unable to get info";
        //e.printStackTrace();
      }
    catch (NullPointerException npe)
    {
      System.err.println("Unable to get host name or ip address from "
                         + "getHostAddress()/getHostName()");
      return "Unable to get info";
    }
    if (which.equals("hostname"))
      {
        return host_name;
      }
    if (which.equals("ipaddr"))
      {
        return ip_addr;
      }
    return "";
  }

  public static TrayIcon trayIcon = new TrayIcon("Frysk Monitor/Debugger"
                                                 + "\nhost:  "
                                                 + getHostInfo("hostname")
                                                 + "\nos:  " + os_name
                                                 + "\nkernel version:  "
                                                 + os_version + "\narch:  "
                                                 + os_arch + "\nIP address:  "
                                                 + getHostInfo("ipaddr"), false);

  public static void loadIcons ()
  {
    factories = new IconFactory[2];
    factories[0] = new IconFactory(); // "small" icons
    factories[1] = new IconFactory(); // "large" icons

    FryskIconSet[] runSet = new FryskIconSet[] {
	new FryskIconSet("frysk-run"),
	new FryskIconSet("frysk-run")
    };
    FryskIconSet[] stopSet = new FryskIconSet[] {
	new FryskIconSet("frysk-stop"),
	new FryskIconSet("frysk-stop")
    };
    FryskIconSet[] nextSet = new FryskIconSet[] {
	new FryskIconSet("frysk-next"),
	new FryskIconSet("frysk-next")
    };
    FryskIconSet[] nextAISet = new FryskIconSet[] {
	new FryskIconSet("frysk-nextAI"),
	new FryskIconSet("frysk-nextAI")
    };
    FryskIconSet[] stepSet = new FryskIconSet[] {
	new FryskIconSet("frysk-step"),
	new FryskIconSet("frysk-step")
    };
    FryskIconSet[] stepAISet = new FryskIconSet[] {
	new FryskIconSet("frysk-stepAI"),
	new FryskIconSet("frysk-stepAI")
    };
    FryskIconSet[] continueSet = new FryskIconSet[] {
	new FryskIconSet("frysk-continue"),
	new FryskIconSet("frysk-continue")
    };
    FryskIconSet[] finishSet = new FryskIconSet[] {
	new FryskIconSet("frysk-finish"),
	new FryskIconSet("frysk-finish")
    };
    FryskIconSet[] downSet = new FryskIconSet[] {
	new FryskIconSet("frysk-down"),
	new FryskIconSet("frysk-down")
    };
    FryskIconSet[] upSet = new FryskIconSet[] {
	new FryskIconSet("frysk-up"),
	new FryskIconSet("frysk-up")
    };
    FryskIconSet[] topSet = new FryskIconSet[] {
	new FryskIconSet("frysk-top"),
	new FryskIconSet("frysk-top")
    };
    try {
	for (int j = 0; j < sizeDirs.length - 1; j++) {
	    addIconSet(runSet[j], Config.getImageDir (), j, RUN_PNG);
	    addIconSet(stopSet[j], Config.getImageDir (), j, STOP_PNG);
	    addIconSet(nextSet[j], Config.getImageDir (), j, NEXT_PNG);
	    addIconSet(nextAISet[j], Config.getImageDir (), j, NEXT_AI_PNG);
	    addIconSet(stepSet[j], Config.getImageDir (), j, STEP_PNG);
	    addIconSet(stepAISet[j], Config.getImageDir (), j, STEP_AI_PNG);
	    addIconSet(continueSet[j], Config.getImageDir (), j, CONTINUE_PNG);
	    addIconSet(finishSet[j], Config.getImageDir (), j, FINISH_PNG);
	    addIconSet(downSet[j], Config.getImageDir (), j, DOWN_PNG);
	    addIconSet(upSet[j], Config.getImageDir (), j, UP_PNG);
	    addIconSet(topSet[j], Config.getImageDir (), j, TOP_PNG);
	    
	    // The only other image we need is the highlight image
	    IconSet set = new IconSet();
	    IconSource source = new IconSource();
	    source.setFilename(Config.getImageDir () + "/" + HIGHLIGHT_PNG);
	    source.setSize(IconSize.BUTTON);
	    set.addSource(source);
	    factories[j].addIconSet("frysk-highlight", set);
	}

	// Load the tray icons
	for (int k = 1; k <= 24; k++) {
	    IconSet set = new IconSet();
	    IconSource source = new IconSource();
	    source.setFilename(Config.getImageDir () + "/icon/" + TRAY_PREFIX
			       + (k < 10 ? "0" + k : "" + k) + ".png");
	    
	    if (k == 24) {
		windowIcon = new Pixbuf(Config.getImageDir () + "/icon/"
					+ TRAY_PREFIX + k + ".png");
	    }
	    
	    source.setSize(IconSize.BUTTON);
	    set.addSource(source);
	    factories[0].addIconSet("frysk-tray-" + k, set);
	    factories[1].addIconSet("frysk-tray-" + k, set);
	}

	anim = new PixbufAnimation(Config.getImageDir () + "/"
				   + "fryskTrayIcon.gif");
	
	// Load the spash screen image
	splashImage = new Pixbuf(Config.getImageDir () + "/" + "SplashScreen.png");

    }
    catch (Exception e) {
	throw new RuntimeException (e);
    }
    factories[0].addDefault();
  }

  public static void useSmallIcons ()
  {
    useSmallIcons = true;
    factories[1].removeDefault();
    factories[0].addDefault();
  }

  public static IconFactory getFactory ()
  {
    if (useSmallIcons)
      return factories[0];
    else
      return factories[1];
  }

  public static void useLargeIcons ()
  {
    useSmallIcons = false;
    factories[0].removeDefault();
    factories[1].addDefault();
  }

  private static void addIconSet (FryskIconSet set, String dir, int index,
                                  String iconName) throws Exception
  {
    set.addIcon(new Pixbuf(dir + "/" + sizeDirs[index] + "/" + iconName),
                IconSize.SMALL_TOOLBAR);
    set.addIcon(new Pixbuf(dir + "/" + sizeDirs[index] + "/" + iconName),
                IconSize.MENU);
    set.addIcon(new Pixbuf(dir + "/" + sizeDirs[index + 1] + "/" + iconName),
                IconSize.LARGE_TOOLBAR);
    set.addIcon(new Pixbuf(dir + "/" + sizeDirs[index + 1] + "/" + iconName),
                IconSize.BUTTON);
    set.addToFactory(factories[index]);
  }
}
