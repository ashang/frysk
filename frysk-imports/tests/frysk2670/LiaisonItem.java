package frysk.gui.monitor;

public abstract class LiaisonItem
{
    public LiaisonItem()
    {
    }
    public void setName(String name)
    {
    }
    public String getName()
    {
	return null;
    }
    public void setToolTip(String toolTip)
    {
    }
    public String getToolTip()
    {
	return null;
    }
    public boolean shouldSaveObject()
    {
	return false;
    }
    public void doSaveObject()
    {
    }
    public void dontSaveObject()
    {
    }
    public LiaisonItem(LiaisonItem other)
    {
    }
    public LiaisonItem(String name, String toolTip)
    {
    }
    public abstract LiaisonItem getCopy();
    public abstract boolean setArgument(String argument);
    public abstract String getArgument();
    public abstract ObservableLinkedList getArgumentCompletionList();
}
