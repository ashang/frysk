package one;

// Can't remove next include.
import other.Action;

public class ExitNotificationObserver
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
