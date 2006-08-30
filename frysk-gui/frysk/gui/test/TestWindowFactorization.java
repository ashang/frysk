

package frysk.gui.test;

import junit.framework.TestCase;

import frysk.proc.Task;
import frysk.gui.common.IconManager;
import frysk.gui.memory.MemoryWindow;
import frysk.gui.register.RegisterWindow;
import frysk.proc.Manager;
import frysk.proc.DummyProc;
import frysk.proc.DummyTask;
import frysk.gui.Build;
import org.gnu.glade.LibGlade;
import org.gnu.gtk.Gtk;

//import org.gnu.glib.CustomEvents;

public class TestWindowFactorization
    extends TestCase
{

  public String[] gladePaths = { "glade/", "frysk/gui/glade/",
                                "../../frysk/frysk-gui/frysk/gui/glade/",
                                "/home/mcvet/workspace/build/frysk-gui/../../frysk/frysk-gui/frysk/gui/glade/" };

  String[] imagePaths = new String[] { Build.ABS_SRCDIR + "/" + BASE_PATH
                                       + "images/" };
  
  private static final String BASE_PATH = "frysk/gui/";

  private Task theTask = null;

  private LibGlade gladem = null;

  private LibGlade glader = null;

  private MemoryWindow mw = null;

  private RegisterWindow rw = null;

  public void setUp ()
  {
    Gtk.init(new String[] {});
    IconManager.setImageDir(imagePaths);
    IconManager.loadIcons();
    IconManager.useSmallIcons();
  }

  public void testWindowFactorization ()
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
  {
    int i = 0;
    for (; i < gladePaths.length; i++)
      {
        try
          {
            gladem = new LibGlade(gladePaths[i] + "/" + "memorywindow.glade",
                                  null);
            glader = new LibGlade(gladePaths[i] + "/" + "registerwindow.glade",
                                  null);
          }
        catch (Exception e)
          {
            if (i < gladePaths.length - 1)
              // If we don't find the glade file, look at the next file
              continue;
            else
              {
                if (i < gladePaths.length - 1)
                  // If we don't find the glade file, look at the next file
                  continue;
                else
                  fail("GLADE FAILED");

              }
          }
        // If we've found it, break
        break;
      }

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
