
package frysk.gui.test;

import junit.framework.TestCase;

import frysk.proc.Task;
import frysk.gui.memory.MemoryWindow;
import frysk.gui.register.RegisterWindow;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.TaskObserver;
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

  private MemoryWindow mw = null;

  private RegisterWindow rw = null;

  public void setUp ()
  {
    Gtk.init(new String[] {});
  }

  public void testWindowFactorization ()
  {

    Manager.eventLoop.start();

    for (int j = 0; j < 15; j++)
      {
        String[] exec = { "/bin/ed" };
        Manager.host.requestCreateAttachedProc(exec, new AttachedObserver());
        System.out.println("#" + j);
        initGlades();

        mw = new MemoryWindow(gladem);
        System.gc();
        rw = new RegisterWindow(glader);
        System.gc();

        int x = 0;
        /* Throw some entropy in there along with GC as we wait for the process
         * to be created. */
        while (theTask == null)
          {
            x = x + 3;
            x = x - 2;
            if (x == 2500000)
              {
                x = 0;
                System.gc();
              }

          }
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
        //      If we've found it, break
        break;
      }

    MemoryWindow mw = new MemoryWindow(gladem);
    mw.getClass();
    //mw.showAll();
    RegisterWindow rw = new RegisterWindow(glader);
    //rw.showAll();
    rw.getClass();
  }

  public void setTasks ()
  {
    System.out.println("Setting tasks");
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
      if (task == null)
        System.out.println("Aww crap...");
      else
        System.out.println(task);
      System.out.println("theTask: " + theTask);
      blocker = this;
      task.requestAddTerminatedObserver(new TaskTerminatedObserver());

      CustomEvents.addEvent(new Runnable()
      {
        public void run ()
        {
          setTasks();
        }
      });

      System.out.println("About to finish");
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
      System.out.println("updateTerminated");
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
