/**
 * 
 */


package frysk.gui.monitor;

import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.ToolTips;
import org.gnu.gtk.event.MenuItemEvent;
import org.gnu.gtk.event.MenuItemListener;

import frysk.gui.monitor.PIDColumnDialog;

/**
 * A menu to appear on the right-click of any of the column headers in the
 * SessionProcTreeView, to handle the manipulation of these columns.
 * 
 * Not used until column header bugs are worked out!
 * 
 * @author mcvet
 */
public class PIDMenu
    extends Menu
{

  private PIDColumnDialog pidColumnDialog;

  public PIDMenu (PIDColumnDialog pcd)
  {
    super();

    this.pidColumnDialog = pcd;
    MenuItem item = new MenuItem("Edit Columns...", false);
    ToolTips tip = new ToolTips();
    tip.setTip(item, "Edit Columns...", "");

    item.addListener(new MenuItemListener()
    {
      public void menuItemEvent (MenuItemEvent arg0)
      {
        pidColumnDialog.showAll();
      }
    });
    
    this.add(item);

    this.showAll();
  }

  /**
   * Show the popup menu. selected operation is to be 
   * applied to process with id pid
   * */
  public void popup ()
  {
    this.popup();
  }

}
