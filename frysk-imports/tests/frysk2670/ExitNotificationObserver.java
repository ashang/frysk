package frysk.gui.monitor.observers;

import frysk.gui.monitor.actions.TaskAction;
// Can't remove next include.
import frysk.proc.Action;

public class ExitNotificationObserver
    extends TaskTerminatingObserver
{

    public ExitNotificationObserver()
    {
	super();
	TaskAction myAction = new TaskAction ()
	    {
		public void execute (Object o)
		{
		}
	    };
	myAction.dontSaveObject();
    }
}
