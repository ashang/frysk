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


package frysk.gui.druid;

import java.util.Iterator;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.Dialog;
import org.gnu.gtk.Entry;
import org.gnu.gtk.GtkStockItem;
import org.gnu.gtk.IconSize;
import org.gnu.gtk.Image;
import org.gnu.gtk.Label;
import org.gnu.gtk.Notebook;
import org.gnu.gtk.SelectionMode;
import org.gnu.gtk.SizeGroup;
import org.gnu.gtk.SizeGroupMode;
import org.gnu.gtk.TextBuffer;
import org.gnu.gtk.TextView;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModelFilter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeRowReference;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.CellRendererToggleEvent;
import org.gnu.gtk.event.CellRendererToggleListener;
import org.gnu.gtk.event.EntryEvent;
import org.gnu.gtk.event.EntryListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;
import org.gnu.gtk.event.TreeViewEvent;
import org.gnu.gtk.event.TreeViewListener;

import frysk.gui.common.IconManager;
import frysk.gui.monitor.CheckedListView;
import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.GuiProc;
import frysk.gui.monitor.ListView;
import frysk.gui.monitor.ProcWiseDataModel;
import frysk.gui.monitor.ProcWiseTreeView;
import frysk.gui.monitor.observers.ObserverManager;
import frysk.gui.monitor.observers.ObserverRoot;
import frysk.gui.monitor.WindowManager;
import frysk.gui.sessions.DebugProcess;
import frysk.gui.sessions.Session;
import frysk.gui.sessions.SessionManager;

public class CreateFryskSessionDruid
    extends Dialog
    implements LifeCycleListener
{

  private ProcWiseDataModel dataModel;

  private ProcWiseTreeView procWiseTreeView;

  // private ListView processTagSetSelectionTreeView;
  private ListView addedProcsTreeView;

  // private CheckedListView tagSetSelectionTreeView;
  private CheckedListView observerSelectionTreeView;

  private ListView processObserverSelectionTreeView;

  private TextView observerDescriptionTextView;

  private TextBuffer observerDescBuffer;

  private Label warningLabel;

  private Image warningIcon;

  private Session currentSession = new Session();

  private Entry nameEntry;

  private Notebook notebook;

  private Button nextButton;

  private Button backButton;

  private Button finishButton;

  private Button saveButton;

  private Button cancelButton;

  private int processSelected = 0;

  private boolean editSession;

  private String oldSessionName;

  public CreateFryskSessionDruid (LibGlade glade)
  {
    super(glade.getWidget("SessionDruid").getHandle());
    this.setIcon(IconManager.windowIcon);

    getDruidStructureControls(glade);
    getProcessSelectionControls(glade);
    // getTagsetObserverControls(glade);
    getProcessObserverControls(glade);
    this.addListener(this);
  }

  public void setEditSessionMode (Session givenSession)
  {
    currentSession = givenSession;
    if (currentSession == null)
      currentSession = new Session();

    processSelected = currentSession.getProcesses().size();
    attachLinkedListsToWidgets();

    notebook.setShowTabs(true);
    notebook.setCurrentPage(0);
    finishButton.hideAll();
    nextButton.hideAll();
    backButton.hideAll();
    cancelButton.showAll();
    saveButton.showAll();
    saveButton.setSensitive(true);
    editSession = true;
    oldSessionName = currentSession.getName();
    warningLabel.setMarkup("Select a <b>Name</b> for the session, and some <b>Process Groups</b> to monitor");
    warningIcon.set(GtkStockItem.INFO, IconSize.BUTTON);
    editSession = true;
    unFilterData();
  }

  public void setNewSessionMode ()
  {
    nextButton.showAll();
    nextButton.setSensitive(false);
    backButton.showAll();
    backButton.setSensitive(false);
    finishButton.hideAll();
    saveButton.hideAll();
    cancelButton.showAll();
    nameEntry.setText("");

    currentSession = null;
    currentSession = new Session();
    attachLinkedListsToWidgets();

    notebook.setShowTabs(false);
    notebook.setCurrentPage(0);
    processSelected = 0;
    this.nameEntry.setText(setInitialName());
    this.nameEntry.selectRegion(0, this.nameEntry.getText().length());
    editSession = false;
    unFilterData();
  }

  private void setTreeSelected (TreeIter selected, boolean setSelected,
                                boolean setChildren)
  {
    this.dataModel.setSelected(selected, setSelected, setChildren);
  }

  private void addProcessParent (TreeIter unfilteredProcessIter)
  {
    if (unfilteredProcessIter == null)
      {
        return;
      }

    if (! this.dataModel.getModel().isIterValid(unfilteredProcessIter))
      {
        return;
      }

    GuiProc proc = (GuiProc) this.dataModel.getModel().getValue(
                                                                unfilteredProcessIter,
                                                                this.dataModel.getObjectDC());
    if (proc == null)
      {
        if (unfilteredProcessIter.getChildCount() > 0)
          {
            TreeIter childIter = unfilteredProcessIter.getChild(0);
            proc = ((GuiProc) this.dataModel.getModel().getValue(
                                                                 childIter,
                                                                 this.dataModel.getObjectDC()));
          }
      }

    DebugProcess debugProcess = new DebugProcess(proc.getExecutableName(),
                                                 proc.getNiceExecutablePath());
    currentSession.addProcess(debugProcess);

  }

  private boolean isChild (TreePath pathTest)
  {
    // Minor hack. Sometimes path.up() returns a boolean
    // that indicates it has a parent and the up() changed the
    // state of the Path. However all operations on that then
    // return NP which indicates a false positive. Fix locally,
    // and file upstream bug.

    if (pathTest.toString().split(":").length > 1)
      return true;
    else
      return false;
  }

  private TreePath deFilterPath (TreeView tree, TreePath filter)
  {
    TreeModelFilter ts = (TreeModelFilter) tree.getModel();
    return ts.convertPathToChildPath(filter);
  }

  private void changeGroupState (TreeView tree, TreePath[] selectedProcs,
                                 boolean filtered, boolean state)
  {

	TreeRowReference[] paths = new TreeRowReference[selectedProcs.length];
	TreeIter unfilteredProcessIter = null;
    if (selectedProcs.length > 0)
      {
    	
    	for (int i=0; i<selectedProcs.length; i++)
    	{
    		if (filtered)
    		{
                unfilteredProcessIter = this.dataModel.getModel().getIter(
                        deFilterPath(
                                     tree,
                                     selectedProcs[i]));
    			
    		}
    		else
    			unfilteredProcessIter = this.dataModel.getModel().getIter(selectedProcs[i]);
    		paths[i] = new TreeRowReference(this.dataModel.getModel(),unfilteredProcessIter.getPath());
    	}
        for (int i = 0; i < paths.length; i++)
          {
            // Convert a filetered iterator to a non filtered iterator.
   
            // Scenario 1: Tree iter has children (a process group); selected
            // the parent
            if (this.dataModel.getModel().getIter(paths[i].getPath()).getChildCount() > 0)
              {
                if (state)
                  {
                    processSelected += this.dataModel.getModel().getIter(paths[i].getPath()).getChildCount() + 1;
                    addProcessParent(this.dataModel.getModel().getIter(paths[i].getPath()));
                  }
                else
                  processSelected -= this.dataModel.getModel().getIter(paths[i].getPath()).getChildCount() + 1;
                setTreeSelected(this.dataModel.getModel().getIter(paths[i].getPath()), state, true);
              }
            else
              {
                // Scenario 2: A child in the process group has been selected,
                // also select
                // the parent and siblings.
                TreePath parent_path = paths[i].getPath();
                if (isChild(parent_path))
                  {
                    // we are a child that has a parent; sibilings and parent
                    parent_path.up();
                    // Save parent iter
                    TreeIter parent_iter = this.dataModel.getModel().getIter(
                                                                             parent_path);
                    // Move the parent and children.
                    if (parent_iter.getChildCount() > 0)
                      {
                        if (state)
                          {
                            processSelected += this.dataModel.getModel().getIter(paths[i].getPath()).getChildCount() + 1;
                            addProcessParent(parent_iter);
                          }
                        else
                          processSelected -= this.dataModel.getModel().getIter(paths[i].getPath()).getChildCount() + 1;
                        setTreeSelected(parent_iter, state, true);
                      }
                  }
                else
                  {
                    if (state)
                      {
                        addProcessParent(this.dataModel.getModel().getIter(paths[i].getPath()));
                        processSelected++;
                      }
                    else
                      processSelected--;
                    // Scenario 3: No children, or siblings
                    setTreeSelected(this.dataModel.getModel().getIter(paths[i].getPath()), state, false);
                  }
              }
          }
      }
    setProcessNext(processSelected);
  }

  private void setProcessNext (int processCount)
  {
    if (editSession == false)
      {
        if ((processCount > 0)
            && (nameEntry.getText().length() > 0)
            && (SessionManager.theManager.getSessionByName(nameEntry.getText()) == null))
          {
            this.nextButton.setSensitive(true);
            this.saveButton.setSensitive(true);
          }
        else
          {
            this.nextButton.setSensitive(false);
            this.saveButton.setSensitive(false);
          }
      }
    else
      {
        if ((processCount > 0) && (nameEntry.getText().length() > 0))
          {
            this.nextButton.setSensitive(true);
            this.saveButton.setSensitive(true);
          }
        else
          {
            this.nextButton.setSensitive(false);
            this.saveButton.setSensitive(false);
          }
      }
  }

  private void unFilterData() {
	  procWiseTreeView.psDataModel.unFilterData();
  }
  private void getProcessSelectionControls (LibGlade glade)
  {

    Button addProcessGroupButton;
    Button removeProcessGroupButton;

    // Page 1 of the Druid. Initial Process Selection.

    // Create New Live Data Model and mount on the TreeView
    this.dataModel = new ProcWiseDataModel();
    procWiseTreeView = new ProcWiseTreeView(
                                            glade.getWidget(
                                            "sessionDruid_procWiseTreeView").getHandle(),
                                            this.dataModel);
    
    procWiseTreeView.getSelection().setMode(SelectionMode.MULTIPLE);
    
    procWiseTreeView.addListener(new TreeViewListener() {
		public void treeViewEvent(TreeViewEvent event) {
			if (event.isOfType(TreeViewEvent.Type.ROW_ACTIVATED))
	         changeGroupState(procWiseTreeView,
                     procWiseTreeView.getSelection().getSelectedRows(),
                     true, true);
		}});

    // Create a New ListView and mount the Linked List from Session data
    addedProcsTreeView = new ListView(
                                      glade.getWidget(
                                      "sessionDruid_addedProcsTreeView").getHandle());
    addedProcsTreeView.watchLinkedList(currentSession.getProcesses());
    addedProcsTreeView.getSelection().setMode(SelectionMode.MULTIPLE);
    addedProcsTreeView.addListener(new TreeViewListener() {
		public void treeViewEvent(TreeViewEvent event) {
			if (event.isOfType(TreeViewEvent.Type.ROW_ACTIVATED))
			{
				DebugProcess currentDebugProcess = (DebugProcess) addedProcsTreeView.getSelectedObject();
				if (currentDebugProcess != null) {
					TreePath foo = dataModel.searchName(currentDebugProcess.getName());
					changeGroupState(procWiseTreeView, new TreePath[] { foo },
							false, false);
					currentSession.removeProcess(currentDebugProcess);
				}
			}
		}});
    this.setUpCurrentPage();

    nameEntry = (Entry) glade.getWidget("sessionDruid_sessionName");
    nameEntry.addListener(new EntryListener()
    {
      public void entryEvent (EntryEvent arg0)
      {
        currentSession.setName(nameEntry.getText());
        if (editSession == false)
          {
            if (SessionManager.theManager.getSessionByName(nameEntry.getText()) != null)
              {

                warningLabel.setMarkup("<b>Warning:</b> The Session Name is already used. Please choose another.");
                warningIcon.set(GtkStockItem.DIALOG_WARNING, IconSize.BUTTON);
              }
            else
              {
                warningLabel.setMarkup("Select a <b>Name</b> for the session, and some <b>Process Groups</b> to monitor");
                warningIcon.set(GtkStockItem.INFO, IconSize.BUTTON);

              }
          }
        setProcessNext(processSelected);
      }
    });

    SizeGroup sizeGroup = new SizeGroup(SizeGroupMode.BOTH);
    sizeGroup.addWidget(procWiseTreeView);
    sizeGroup.addWidget(addedProcsTreeView);

    addProcessGroupButton = (Button) glade.getWidget("sessionDruid_addProcessGroupButton");
    removeProcessGroupButton = (Button) glade.getWidget("sessionDruid_removeProcessGroupButton");

    addProcessGroupButton.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent event)
      {
        if (event.isOfType(ButtonEvent.Type.CLICK))
          changeGroupState(procWiseTreeView,
                           procWiseTreeView.getSelection().getSelectedRows(),
                           true, true);
      }
    });

    removeProcessGroupButton.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent event)
      {
        if (event.isOfType(ButtonEvent.Type.CLICK))
          {
            Iterator i = addedProcsTreeView.getSelectedObjects();
            if (i != null)
              while (i.hasNext())
                {
                  DebugProcess currentDebugProcess = (DebugProcess) i.next();
                  TreePath foo = dataModel.searchName(currentDebugProcess.getName());
                  changeGroupState(procWiseTreeView, new TreePath[] { foo },
                                   false, false);
                  currentSession.removeProcess(currentDebugProcess);
                }
          }
      }
    });

  }

  private void getProcessObserverControls (LibGlade glade)
  {

    observerSelectionTreeView = new CheckedListView(
                                                    glade.getWidget(
                                                                    "SessionDruid_observerTreeView").getHandle());
    observerSelectionTreeView.expandAll();

    processObserverSelectionTreeView = new ListView(
                                                    glade.getWidget(
                                                                    "SessionDruid_processObserverTreeView").getHandle());
    processObserverSelectionTreeView.expandAll();

    observerDescriptionTextView = (TextView) glade.getWidget("SessionDruid_observerDescription");
    observerDescBuffer = new TextBuffer();
    observerDescriptionTextView.setBuffer(observerDescBuffer);

    processObserverSelectionTreeView.watchLinkedList(currentSession.getProcesses());
    processObserverSelectionTreeView.getSelection()
        .addListener(
                    new TreeSelectionListener()
                    {
                      public void selectionChangedEvent (TreeSelectionEvent arg0)
                      {
                        DebugProcess selected = (DebugProcess) processObserverSelectionTreeView.getSelectedObject();
                        if (selected != null)
                          {
                            Iterator i = selected.getObservers().iterator();
                            observerSelectionTreeView.clearChecked();
                            while (i.hasNext())
                              observerSelectionTreeView.setChecked((ObserverRoot) i.next(),
                                                                   true);
                          }
                      }
                    });

    observerSelectionTreeView.watchLinkedList(ObserverManager.theManager.getTaskObservers());
    observerSelectionTreeView.getCellRendererToggle()
        .addListener(
                     new CellRendererToggleListener()
                     {
                       public void cellRendererToggleEvent (
                                                            CellRendererToggleEvent arg0)
                       {
                         GuiObject selected = (GuiObject) observerSelectionTreeView.getSelectedObject();
                         DebugProcess observerProcessSelected = (DebugProcess) processObserverSelectionTreeView
                           .getSelectedObject();
                         if (observerSelectionTreeView.isChecked(selected))
                           {
                             if (! observerProcessSelected.getObservers().contains(
                                                                                   selected))
                               observerProcessSelected.addObserver((ObserverRoot) selected);
                           }
                         else
                           {
                             observerProcessSelected.removeObserver((ObserverRoot) selected);
                           }
                       }
                     });

    observerSelectionTreeView.getSelection()
        .addListener(
                     new TreeSelectionListener()
                     {
                       public void selectionChangedEvent ( TreeSelectionEvent arg0)
                       {
                         if (observerSelectionTreeView.getSelectedObject() != null)
                           if(observerSelectionTreeView.getSelectedObject().getToolTip() != null){
                             observerDescBuffer.setText(
                                                        observerSelectionTreeView.
                                                        getSelectedObject().
                                                        getToolTip());
                         }
                       }
                     });
    
    

    SizeGroup sizeGroup = new SizeGroup(SizeGroupMode.BOTH);
    sizeGroup.addWidget(observerSelectionTreeView);
    sizeGroup.addWidget(processObserverSelectionTreeView);

    Button customObserver = (Button) glade.getWidget("SessionDruid_createCustomObserver");
    customObserver.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent event)
      {
        if (event.isOfType(ButtonEvent.Type.CLICK))
        	WindowManager.theManager.observersDialog.showAll();
      }
    });
    
    setUpCurrentPage();
  }

  private void getDruidStructureControls (LibGlade glade)
  {

    this.notebook = (Notebook) glade.getWidget("sessionDruid_sessionNoteBook");

    this.nextButton = (Button) glade.getWidget("sessionDruid_nextButton");
    this.nextButton.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent event)
      {
        if (event.isOfType(ButtonEvent.Type.CLICK))
          {
            nextPage();
          }
      }
    });
    this.nextButton.setSensitive(false);

    this.backButton = (Button) glade.getWidget("sessionDruid_backButton");
    this.backButton.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent event)
      {
        if (event.isOfType(ButtonEvent.Type.CLICK))
          {
            previousPage();
          }
      }
    });

    this.finishButton = (Button) glade.getWidget("sessionDruid_finishButton");
    this.finishButton.setSensitive(true);
    this.finishButton.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent event)
      {
        if (event.isOfType(ButtonEvent.Type.CLICK))
          {
            SessionManager.theManager.addSession(currentSession);
            SessionManager.theManager.save();
            currentSession = null;
            currentSession = new Session();
            hide();
          }
      }
    });

    this.saveButton = (Button) glade.getWidget("sessionDruid_saveEditSessionButton");
    this.saveButton.hideAll();
    this.saveButton.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent event)
      {
        if (event.isOfType(ButtonEvent.Type.CLICK))
          {
            if (editSession)
              {
                SessionManager.theManager.save();

                if (! oldSessionName.equals(currentSession.getName()))
                  {
                    // If they edited the name of the session, reload the
                    // Session Manager so it picks up the old session and
                    // delete it.
                    SessionManager.theManager.load();
                    SessionManager.theManager.removeSession(SessionManager.theManager.getSessionByName(oldSessionName));
                  }
              }
            hide();
          }
      }
    });

    this.cancelButton = (Button) glade.getWidget("sessionDruid_cancelButton");
    this.cancelButton.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent event)
      {
        if (event.isOfType(ButtonEvent.Type.CLICK))
          {
            currentSession.dontSaveObject();
            SessionManager.theManager.load();
            hide();

          }
      }
    });

    this.warningIcon = (Image) glade.getWidget("sessionDruid_feedbackImage");
    this.warningLabel = (Label) glade.getWidget("sessionDruid_feedbackLabel");
  }

  public void attachLinkedListsToWidgets ()
  {
    if (! currentSession.getName().equals("NoName"))
      nameEntry.setText(currentSession.getName());
    addedProcsTreeView.watchLinkedList(currentSession.getProcesses());
    //tagSetSelectionTreeView;
    //processTagSetSelectionTreeView.watchLinkedList(currentSession.getProcesses());
    //observerSelectionTreeView;
    processObserverSelectionTreeView.watchLinkedList(currentSession.getProcesses());
  }

  private void nextPage ()
  {

    // Process previous page data
    int page = this.notebook.getCurrentPage();

    this.notebook.setCurrentPage(page + 1);
    this.setUpCurrentPage();
  }

  private void previousPage ()
  {
    this.notebook.setCurrentPage(this.notebook.getCurrentPage() - 1);
    this.setUpCurrentPage();
  }

  private void setUpCurrentPage ()
  {
    int page = this.notebook.getCurrentPage();

    if (page == 0)
      this.backButton.setSensitive(false);
    else
      this.backButton.setSensitive(true);

    if (page == this.notebook.getNumPages() - 1)
      {
        this.nextButton.hideAll();
        this.finishButton.showAll();
      }
    else
      {
        this.nextButton.showAll();
        this.finishButton.hideAll();
      }

    if (page == 1)
      setProcessNext(processSelected);
  }

  private String setInitialName ()
  {
    String Name = "New Frysk Session";
    String filler = "00";
    if (SessionManager.theManager.getSessionByName(Name) == null)
      return Name;
    else
      for (int i = 1; i < Integer.MAX_VALUE; i++)
        {
          if (i < 10)
            filler = "0" + i;
          else
            filler = "" + i;
          if (SessionManager.theManager.getSessionByName(Name + " " + filler) == null)
            return Name + " " + filler;
        }
    return "Error Finding Name";
  }

  public void showAll ()
  {
    super.showAll();
    this.setUpCurrentPage();
  }

  public void lifeCycleEvent (LifeCycleEvent event)
  {
  }

  public boolean lifeCycleQuery (LifeCycleEvent event)
  {
    if (event.isOfType(LifeCycleEvent.Type.DESTROY)
        || event.isOfType(LifeCycleEvent.Type.DELETE))
      {
        this.hide();
        return true;
      }
    return false;
  }

}
