

package frysk.event;

public class RequestStopEvent
    implements Event
{
  EventLoop eventLoop;

  public RequestStopEvent (EventLoop eventLoop)
  {
    this.eventLoop = eventLoop;
  }

  public void execute ()
  {
    eventLoop.requestStop();
  }

  public String toString ()
  {
    return ("RequestStop Event");
  }

}
