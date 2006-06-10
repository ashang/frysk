package frysk.gui.monitor.actions;

import frysk.gui.monitor.LiaisonItem;

public abstract class Action
    extends LiaisonItem
{
    protected Runnable runnable;
	
    public Action()
    {
	super();
    }
    public Action(Action other)
    {
	super(other);
    }
    public Action(String name, String toolTip)
    {
	super(name, toolTip);
    }
}
