// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// type filter text
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

package frysk.gui.monitor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

import org.gnu.gtk.GtkStockItem;
import org.gnu.gtk.HBox;
import org.gnu.gtk.PolicyType;
import org.gnu.gtk.ResponseType;
import org.gnu.gtk.ScrolledWindow;
import org.gnu.gtk.event.CellRendererToggleEvent;
import org.gnu.gtk.event.CellRendererToggleListener;

import frysk.gui.common.dialogs.FryskDialog;
import frysk.gui.monitor.datamodels.DataModelManager;
import frysk.gui.monitor.observers.ObserverManager;

/**
 * A Dialog for editing the properties of the frysk program Observer
 */
public class ProgramObserverDialog extends FryskDialog implements Saveable{
	
	PickProcsListView checkedListView;
	
    private LinkedList shouldBeCheckedList;
    
	public ProgramObserverDialog(){
      
      this.shouldBeCheckedList = new LinkedList();
      
		this.checkedListView = new PickProcsListView(null);
		
        this.checkedListView.addToggleListener(new CellRendererToggleListener()
        {
          public void cellRendererToggleEvent (CellRendererToggleEvent event)
          {
            GuiProc guiProc = (GuiProc) checkedListView.getSelectedObject();
            if(!checkedListView.isChecked(guiProc)){
              shouldNotBeCheckedList(guiProc.getName());
              ObserverManager.theManager.programObserver.unwatchProc(guiProc.getProc());
            }else{
              ObserverManager.theManager.programObserver.watchProc(guiProc.getProc());
            }
          }
        });
        
		this.checkedListView.watchLinkedList(DataModelManager.theManager.flatProcObservableLinkedList);
        DataModelManager.theManager.flatProcObservableLinkedList.itemAdded.addObserver(new Observer()
        {
          public void update (Observable observable, Object object)
          {
            GuiProc guiProc = (GuiProc) object;
            Iterator iter = shouldBeCheckedList.iterator();
            while(iter.hasNext()){
              String path = (String)iter.next();
              if(guiProc.getNiceExecutablePath().equals(path)){
                checkedListView.setChecked((GuiObject) object, true);
                ObserverManager.theManager.programObserver.watchProc(guiProc.getProc());
              }
            }
          }
        });
           
		HBox mainBox = new HBox(false,0);
		this.getDialogLayout().add(mainBox);
		
		ScrolledWindow sWindow = new ScrolledWindow(null,null);
		sWindow.setMinimumSize(500, 500);
		sWindow.setBorderWidth(10);
		sWindow.setPolicy(PolicyType.NEVER,PolicyType.AUTOMATIC);
		
		sWindow.addWithViewport(checkedListView);
		mainBox.packEnd(sWindow);
		
		this.addButton(GtkStockItem.OK, ResponseType.OK.getValue());
		 
	}

    /**
     * Adds the name of the list of names that should be checked
     * if the name showes up much later after loading it is still
     * taken care of and selected as it should be.
     * @param name
     */
	private void shouldBeChecked(String name){
      this.shouldBeCheckedList.add(name);
    }
    
    private void shouldNotBeCheckedList(String name){
      this.shouldBeCheckedList.remove(name);
    }
    
  public void save (Preferences prefs)
  {
    LinkedList list = checkedListView.getCheckedObjects();
    int numberOfObjects = list.size();
    Iterator iterator = list.iterator();
    prefs.putInt("numberOfObjects", numberOfObjects);
    while(iterator.hasNext()){
      GuiProc guiProc = (GuiProc) iterator.next();
      prefs.put("path" + list.indexOf(guiProc) , guiProc.getNiceExecutablePath());
    }
  }

  public void load (Preferences prefs)
  {
    int numberOfObjects = prefs.getInt("numberOfObjects", 0);
    for(int i = 0; i < numberOfObjects; i++){
      try{
        this.shouldBeChecked(prefs.get("path"+i,""));
      }catch(IllegalArgumentException e){
        
      }
    }
  }

}
