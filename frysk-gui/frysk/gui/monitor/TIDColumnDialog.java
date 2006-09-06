

package frysk.gui.monitor;

import java.util.prefs.Preferences;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.CellRenderer;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.CellRendererToggle;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnBoolean;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.Dialog;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.CellRendererToggleEvent;
import org.gnu.gtk.event.CellRendererToggleListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;

import frysk.gui.common.IconManager;

/**
 * A simple dialog to accept input determining which columns should be 
 * displayed on the SessionProcTreeView.
 * 
 * @author mcvet
 */
public class TIDColumnDialog
    extends Dialog
    implements Saveable
{

  private LibGlade glade;

  private DataColumn[] cols = { new DataColumnBoolean(), new DataColumnString() };

  private TreeView colList;

  private Preferences prefs;

  private SessionProcTreeView sptv;

  public TIDColumnDialog (LibGlade glade, SessionProcTreeView sptv)
  {
    super(glade.getWidget("tidColumnDialog").getHandle());

    this.glade = glade;
    this.sptv = sptv;

    this.setIcon(IconManager.windowIcon);

    this.colList = (TreeView) this.glade.getWidget("tidColView");
    colList.setHeadersVisible(false);

    final ListStore model = new ListStore(cols);

    TreeIter iter = model.appendRow();
    model.setValue(iter, (DataColumnBoolean) cols[0], true);
    model.setValue(iter, (DataColumnString) cols[1], "Entry function");

    iter = model.appendRow();
    model.setValue(iter, (DataColumnBoolean) cols[0], true);
    model.setValue(iter, (DataColumnString) cols[1], "VSZ");

    iter = model.appendRow();
    model.setValue(iter, (DataColumnBoolean) cols[0], true);
    model.setValue(iter, (DataColumnString) cols[1], "RSS");

    iter = model.appendRow();
    model.setValue(iter, (DataColumnBoolean) cols[0], true);
    model.setValue(iter, (DataColumnString) cols[1], "TIME");

    iter = model.appendRow();
    model.setValue(iter, (DataColumnBoolean) cols[0], false);
    model.setValue(iter, (DataColumnString) cols[1], "PPID");

    iter = model.appendRow();
    model.setValue(iter, (DataColumnBoolean) cols[0], false);
    model.setValue(iter, (DataColumnString) cols[1], "STAT");

    iter = model.appendRow();
    model.setValue(iter, (DataColumnBoolean) cols[0], false);
    model.setValue(iter, (DataColumnString) cols[1], "NICE");

    TreeViewColumn col = new TreeViewColumn();
    CellRenderer renderer = new CellRendererToggle();
    col.packStart(renderer, false);
    col.addAttributeMapping(renderer, CellRendererToggle.Attribute.ACTIVE,
                            cols[0]);
    colList.appendColumn(col);

    ((CellRendererToggle) renderer).addListener(new CellRendererToggleListener()
    {
      public void cellRendererToggleEvent (CellRendererToggleEvent arg0)
      {
        TreePath path = new TreePath(arg0.getPath());

        TreeIter iter = model.getIter(path);

        boolean prev = model.getValue(iter, (DataColumnBoolean) cols[0]);
        model.setValue(iter, (DataColumnBoolean) cols[0], ! prev);
      }
    });

    col = new TreeViewColumn();
    renderer = new CellRendererText();
    col.packStart(renderer, true);
    col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT, cols[1]);
    colList.appendColumn(col);

    colList.setModel(model);

    ((Button) this.glade.getWidget("tidCloseButton")).addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent arg0)
      {
        if (arg0.isOfType(ButtonEvent.Type.CLICK))
          {
            TIDColumnDialog.this.save(TIDColumnDialog.this.prefs);
            TIDColumnDialog.this.hideAll();
          }
      }
    });

    this.addListener(new LifeCycleListener()
    {
      public boolean lifeCycleQuery (LifeCycleEvent arg0)
      {
        if (arg0.isOfType(LifeCycleEvent.Type.DELETE))
          {
            TIDColumnDialog.this.save(TIDColumnDialog.this.prefs);
            TIDColumnDialog.this.hideAll();
            return true;
          }
        return false;
      }

      public void lifeCycleEvent (LifeCycleEvent arg0)
      {
      }
    });

    this.colList.setAlternateRowColor(true);
  }

  public void save (Preferences prefs)
  {
    ListStore model = (ListStore) this.colList.getModel();
    
    TreeIter iter = model.getFirstIter();
    String[] temp = sptv.getThreadColNames();

    for (int i = 0; i < temp.length; i++)
      {
        boolean val = model.getValue(iter, (DataColumnBoolean) cols[0]);
        prefs.putBoolean(temp[i], val);
        iter = iter.getNextIter();
      }
  }

  public void load (Preferences prefs)
  {
    this.prefs = prefs;
    ListStore model = (ListStore) this.colList.getModel();
    TreeIter iter = model.getFirstIter();
    String[] temp = sptv.getThreadColNames();

    for (int i = 0; i < temp.length; i++)
      {
        /* If this is our first time loading, don't set the columns to false */
        if (prefs.get(temp[i], "") == "")
          break;
        boolean val = prefs.getBoolean(temp[i], i == 0);
        model.setValue(iter, (DataColumnBoolean) cols[0], val);
        iter = iter.getNextIter();
      }
  }
}
