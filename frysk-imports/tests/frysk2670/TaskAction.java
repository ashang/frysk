package frysk.gui.monitor.actions;

import frysk.proc.Task;

public abstract class TaskAction
    extends Action
{
    public TaskAction()
    {
    }
    public abstract void execute(Task task);
}
