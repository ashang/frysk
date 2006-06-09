package frysk.gui.monitor.observers;

import frysk.gui.common.dialogs.DialogManager;
import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.actions.TaskAction;
import frysk.proc.Action;
import frysk.proc.Task;

public class ExitNotificationObserver
    extends TaskTerminatingObserver
{

    public ExitNotificationObserver()
    {
	super();
	TaskAction myAction = new TaskAction ()
	    {
		public void execute(Task task)
		{
		}
		public GuiObject getCopy()
		{
		    return null;
		}
		public boolean setArgument(String argument)
		{
		    return true;
		}
		public String getArgument()
		{
		    return null;
		}
		public ObservableLinkedList getArgumentCompletionList()
		{
		    return null;
		}			
	    };
	myAction.dontSaveObject();
    }
}
