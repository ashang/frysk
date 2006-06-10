package frysk.gui.monitor;

public abstract class LiaisonItem
    extends GuiObject
{
    public LiaisonItem()
    {
	super();
    }
    public LiaisonItem(LiaisonItem other)
    {
	super(other);
    }
    public LiaisonItem(String name, String toolTip)
    {
	super(name, toolTip);
    }
    public abstract GuiObject getCopy();
    public abstract boolean setArgument(String argument);
    public abstract String getArgument();
    public abstract ObservableLinkedList getArgumentCompletionList();
}
