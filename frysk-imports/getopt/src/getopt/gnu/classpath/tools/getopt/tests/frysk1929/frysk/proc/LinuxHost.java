package frysk.proc;
import frysk.event.SignalEvent;
import frysk.sys.Wait;
public class LinuxHost
{
    class PollWaitOnSigChld
	extends SignalEvent
    {
	PollWaitOnSigChld ()
	{
	    super (0);
	}
	Wait.Observer waitObserver = new Wait.Observer ()
	    {
	    };
	public final void execute ()
	{
	}
    }
}
