

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
import frysk.proc.Task;
//import frysk.proc.TaskException;
import frysk.gui.memory.MemoryWindow;
import frysk.gui.register.RegisterWindow;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.TaskObserver;
//import frysk.gui.srcwin.SourceWindow;
import org.gnu.glade.LibGlade;
import org.gnu.gtk.Gtk;
import org.gnu.glib.CustomEvents;

public class TestWindowFactorization
    extends TestCase
{

  public String[] gladePaths = { "glade/", "frysk/gui/glade/",
                                "../../frysk/frysk-gui/frysk/gui/glade/",
                                "/home/mcvet/workspace/build/frysk-gui/../../frysk/frysk-gui/frysk/gui/glade/" };

  AttachedObserver blocker = null;

  private Task theTask = null;

  private LibGlade gladem = null;

  private LibGlade glader = null;

  // LibGlade glades = null;

  private MemoryWindow mw = null;

  private RegisterWindow rw = null;

  public void setUp ()
  {
    Gtk.init(new String[] {});
  }

  public void testWindowFactorization ()
  {

    CustomEvents.addEvent(new Runnable()
    {
      public void run ()
      {
        String[] exec = { "/bin/true" };
        Manager.host.requestCreateAttachedProc(exec, new AttachedObserver());
      }
    });

    for (int j = 0; j < 15; j++)
      {

        initGlades();

        mw = new MemoryWindow(gladem);
        System.gc();
        rw = new RegisterWindow(glader);
        System.gc();

//        while (theTask == null)
//          {
//          }
        if (theTask == null)
          return;
        setTasks();
        theTask = null;
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
                if (i < gladePaths.length - 1)
                  // If we don't find the glade file, look at the next file
                  continue;
                else
                  fail("GLADE FAILED");

              }
          }
        //      If we've found it, break
        break;
      }

    MemoryWindow mw = new MemoryWindow(gladem);
    mw.getClass();
    //mw.showAll();
    RegisterWindow rw = new RegisterWindow(glader);
    //rw.showAll();
    rw.getClass();
    //        SourceWindow sw = new SourceWindow(glades, gladePaths[i], null, null);
    //        sw.showAll();
  }

  public void setTasks ()
  {
    mw.setTask(theTask);
    rw.setTask(theTask);
  }

  class AttachedObserver
      implements TaskObserver.Attached
  {

    public void addedTo (Object observable)
    {
    }

    public Action updateAttached (Task task)
    {
      System.out.println("updateAttached");
      theTask = task;
      blocker = this;
      task.requestAddTerminatedObserver(new TaskTerminatedObserver());
      return Action.BLOCK;
    }

    public void deletedFrom (Object observable)
    {
    }

    public void addFailed (Object observable, Throwable w)
    {
      throw new RuntimeException("Failed to attach to created proc", w);
    }
  }

  /**
   * We want to know when the externally-executed program has quit, so that we
   * can continue or optionally run another program.
   */
  class TaskTerminatedObserver
      implements TaskObserver.Terminated
  {
    public void addedTo (Object observable)
    {
    }

    public Action updateTerminated (Task task, boolean signal, int value)
    {
      theTask.requestUnblock(blocker);
      theTask.requestDeleteAttachedObserver(blocker);
      return Action.CONTINUE;
    }

    public void deletedFrom (Object observable)
    {
    }

    public void addFailed (Object observable, Throwable w)
    {
      throw new RuntimeException("Failed to attach to created proc", w);
    }
  }

}
