

package frysk.gui.test;

import junit.framework.TestCase;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Hashtable;
//import java.util.Iterator;
//
//import lib.dw.DwflLine;
//import lib.dw.NoDebugInfoException;
//
//import org.gnu.glade.LibGlade;
//import org.gnu.glib.CustomEvents;
//import org.gnu.gtk.event.LifeCycleEvent;
//import org.gnu.gtk.event.LifeCycleListener;
//import org.jdom.output.Format;
//import org.jdom.output.XMLOutputter;
//
//import frysk.dom.DOMFactory;
//import frysk.dom.DOMFrysk;
//import frysk.dom.DOMFunction;
//import frysk.dom.DOMImage;
//import frysk.gui.common.dialogs.WarnDialog;
//import frysk.gui.common.TaskBlockCounter;
//import frysk.gui.monitor.EventLogger;
//import frysk.gui.monitor.WindowManager;

//import frysk.proc.Proc;
//import frysk.proc.TaskException;
import frysk.gui.memory.MemoryWindow;
import frysk.gui.register.RegisterWindow;
//import frysk.gui.srcwin.SourceWindow;
import org.gnu.glade.LibGlade;

public class TestWindowFactorization
    extends TestCase
{

  public String[] gladePaths = { "glade/", "frysk/gui/glade/",
                                "../../frysk/frysk-gui/frysk/gui/glade/",
                                "/home/mcvet/workspace/build/frysk-gui/../../frysk/frysk-gui/frysk/gui/glade/" };

  public void testMemoryWindowFactorization ()
  {
    LibGlade gladem = null;
    LibGlade glader = null;
    // LibGlade glades = null;
    for (int j = 0; j < 15; j++)
      {
        int i = 0;
        for (; i < gladePaths.length; i++)
          {
            try
              {
                gladem = new LibGlade(gladePaths[i] + "/"
                                      + "memorywindow.glade", null);
                glader = new LibGlade(gladePaths[i] + "/"
                                      + "registerwindow.glade", null);
                // glades = new LibGlade(gladePaths[i] + "/"
                // + "frysk_source.glade", null);
              }
            catch (Exception e)
              {
                if (i < gladePaths.length - 1)
                  // If we don't find the glade file, look at the next file
                  continue;
                else
                  {
                    System.out.println("GLADE FAILED");
                    e.printStackTrace();
                    System.exit(1);
                  }
              }
            // If we've found it, break
            break;
          }
        MemoryWindow mw = new MemoryWindow(gladem);
        mw.showAll();
        RegisterWindow rw = new RegisterWindow(glader);
        rw.showAll();
        //        SourceWindow sw = new SourceWindow(glades, gladePaths[i], null, null);
        //        sw.showAll();
      }

  }
}
