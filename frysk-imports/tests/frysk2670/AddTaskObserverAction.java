package frysk.gui.monitor.actions;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.observers.TaskObserverRoot;
import frysk.proc.Task;

public class AddTaskObserverAction
    extends TaskAction
{
    public AddTaskObserverAction()
    {
	super(); 
    }
    public void execute(Task task)
    {
    }
    public GuiObject getCopy()
    {
	return null;
    }
    public void setObserver(TaskObserverRoot taskObserver)
    {
    }
    public boolean setArgument(String argument)
    {
	return false;
    }
    public String getArgument()
    {
	return null;
	
    }
    public ObservableLinkedList getArgumentCompletionList()
    {
	return null;
    }
}
