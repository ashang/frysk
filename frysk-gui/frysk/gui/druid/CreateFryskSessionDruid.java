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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

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
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeRowReference;
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
import frysk.gui.monitor.WindowManager;
import frysk.gui.monitor.observers.ObserverManager;
import frysk.gui.monitor.observers.ObserverRoot;
import frysk.gui.sessions.DebugProcess;
import frysk.gui.sessions.Session;
import frysk.gui.sessions.SessionManager;
import frysk.gui.srcwin.SourceWindowFactory;

public class CreateFryskSessionDruid
    extends Dialog
    implements LifeCycleListener
{

  private ProcWiseDataModel dataModel;

  private ProcWiseTreeView procWiseTreeView;
  
  private ProcWiseTreeView addedProcsTreeView;

  private CheckedListView observerSelectionTreeView;

  private ListView processObserverSelectionTreeView;

  private TextView observerDescriptionTextView;

  private TextBuffer observerDescBuffer;

  private Label warningLabel;

  private Image warningIcon;

  private Session currentSession = new Session();

  private Entry nameEntry;

  private Notebook notebook;
  
  private Button debugButton;

  private Button nextButton;

  private Button backButton;

  private Button finishButton;

  private Button saveButton;

  private Button cancelButton;

  private int processSelected = 0;

  private boolean editSession;
  
  private boolean loadSession;

  private String oldSessionName;
  
  private HashMap procMap;

  /**
   * Create a new instance of the Session Assistant
   * 
   * @param glade - instance of the parsed glade file
   */
  public CreateFryskSessionDruid (LibGlade glade)
  {
    super(glade.getWidget("SessionDruid").getHandle());
    setIcon(IconManager.windowIcon);

    this.procMap = new HashMap();
    getDruidStructureControls(glade);
    getProcessSelectionControls(glade);
    getProcessObserverControls(glade);
    this.addListener(this);
  }

  /**
   * Sets the druid up into new session mode.
   */
  public void setNewSessionMode ()
  {
    nextButton.showAll();
    nextButton.setSensitive(false);
    backButton.showAll();
    backButton.setSensitive(false);
    debugButton.showAll();
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
    nameEntry.setText(setInitialName());
    nameEntry.selectRegion(0, nameEntry.getText().length());
    editSession = false;
    unFilterData();
  }

  /**
   * Sets the druid up into edit mode. Edit mode loads and existing session into
   * the Assistant, and populates the existing widgets with the session data
   * 
   * @param givenSession - session to edit
   */
  public void setEditSessionMode (Session givenSession)
  {
    currentSession = givenSession;
    if (currentSession == null)
      {
        currentSession = new Session();
      }

    processSelected = currentSession.getProcesses().size();
    
    LinkedList oldSessionProcesses = new LinkedList(this.currentSession.getProcesses());
    LinkedList allProcs = new LinkedList();
    HashSet observers = new HashSet();
    LinkedList observersList = new LinkedList();
    
    Iterator i = oldSessionProcesses.iterator();
    String prev = "";
    
    /* Find all possible observers that were requested for this session, and 
     * keep track of them. Then collect all processes from the model
     * with this name. */
    while (i.hasNext())
      {
        DebugProcess dp = (DebugProcess) i.next();
        if (prev.equals(dp.getName()))
          continue;
        
        Iterator j = dp.getObservers().iterator();
        while (j.hasNext())
          {
            ObserverRoot or = (ObserverRoot) j.next();
            if (!observers.contains(or.getBaseName()))
              {
                observers.add(or.getBaseName());
                observersList.add(or);
              }
          }
        
        prev = dp.getName();
        this.dataModel.collectProcs(prev, allProcs);
      }
    
    this.currentSession.clearProcesses();
    i = allProcs.iterator();
    
    /* Create new DebugProcesses for each of the collected GuiProcs. */
    while (i.hasNext())
      {
        GuiProc guiProc = (GuiProc) i.next();
        
        DebugProcess dp = new DebugProcess(guiProc.getExecutableName(),
                                                         guiProc.getExecutableName(), 
                                                         guiProc.getNiceExecutablePath());
        
        dp.addProc(guiProc);
        Iterator j = observersList.iterator();
        while (j.hasNext())
          {
            dp.addObserver((ObserverRoot) j.next());
          }                      
        currentSession.addProcess(dp);
      }
    
    oldSessionProcesses = null;
    observers = null;
    
    
    attachLinkedListsToWidgets();

    notebook.setShowTabs(true);
    notebook.setCurrentPage(0);
    debugButton.hideAll();
    finishButton.hideAll();
    nextButton.hideAll();
    backButton.hideAll();
    cancelButton.showAll();
    saveButton.showAll();
    saveButton.setSensitive(true);
    editSession = true;
    oldSessionName = currentSession.getName();
    warningLabel.setMarkup("Select a <b>Name</b> for the session, and some <b>Processes</b> to monitor");
    warningIcon.set(GtkStockItem.INFO, IconSize.BUTTON);
    editSession = true;
    unFilterData();
    filterDataInSession();
    observerSelectionTreeView.setSensitive(true);

  }
  
  public void loadSessionMode (Session givenSession)
  {
    currentSession = givenSession;
    if (currentSession == null)
      {
        currentSession = new Session();
      }

    processSelected = currentSession.getProcesses().size();
    
    LinkedList oldSessionProcesses = new LinkedList(this.currentSession.getProcesses());
    LinkedList allProcs = new LinkedList();
    HashSet observers = new HashSet();
    LinkedList observersList = new LinkedList();
    
    Iterator i = oldSessionProcesses.iterator();
    String prev = "";
    /* Find all possible obsevers that were requested for this session, and 
     * keep track of them. Then collect all proceses from the model
     * with this name. */
    while (i.hasNext())
      {
        DebugProcess dp = (DebugProcess) i.next();
        if (prev.equals(dp.getName()))
          continue;
        
        Iterator j = dp.getObservers().iterator();
        while (j.hasNext())
          {
            ObserverRoot or = (ObserverRoot) j.next();
            if (!observers.contains(or.getBaseName()))
              {
                observers.add(or.getBaseName());
                observersList.add(or);
              }
          }
        
        prev = dp.getName();
        this.dataModel.collectProcs(prev, allProcs);
      }
    
    this.currentSession.clearProcesses();
    i = allProcs.iterator();
    
    /* Create new DebugProcesses for each of the collected GuiProcs. */
    while (i.hasNext())
      {
        GuiProc guiProc = (GuiProc) i.next();
        
        DebugProcess dp = new DebugProcess(guiProc.getExecutableName(),
                                                         guiProc.getExecutableName(), 
                                                         guiProc.getNiceExecutablePath());
        
        dp.addProc(guiProc);
        Iterator j = observersList.iterator();
        while (j.hasNext())
          {
            dp.addObserver((ObserverRoot) j.next());
          }                      
        currentSession.addProcess(dp);
      }
    
    oldSessionProcesses = null;
    observers = null;
    
    attachLinkedListsToWidgets();

    notebook.setShowTabs(false);
    notebook.setCurrentPage(0);
    debugButton.showAll();
    nextButton.setSensitive(true);
    nextButton.showAll();
    cancelButton.showAll();
    saveButton.hideAll();
    saveButton.setSensitive(false);
    loadSession = true;
    oldSessionName = currentSession.getName();
    warningLabel.setMarkup("Select a <b>Name</b> for the session, and some <b>Processes</b> to monitor");
    warningIcon.set(GtkStockItem.INFO, IconSize.BUTTON);
    unFilterData();
    filterDataInSession();
    observerSelectionTreeView.setSensitive(true);
  }

  private void filterDataInSession ()
  {
    // Does the current session have any processes?
    if (currentSession.getProcesses() == null)
      {
        return;
      }

    // If so, get them
    final Iterator i = currentSession.getProcesses().iterator();
    if (i != null)
      {
        String altName = "";
        while (i.hasNext())
          {

            // Get the first process iteration.
            final DebugProcess currentDebugProcess = (DebugProcess) i.next();
            if (altName.equals(currentDebugProcess.getName()))
              continue;
            
            Iterator j = this.dataModel.searchAllNames(currentDebugProcess.getName()).iterator();
            while (j.hasNext())
              {
            TreePath processPath = (TreePath) j.next();

            if (processPath != null)
              {

                // Covert to iter from path
                final TreeIter foundIter = procWiseTreeView.dataModel.getModel().getIter(
                                                                                           processPath);
                if (procWiseTreeView.dataModel.getModel().isIterValid(
                                                                        foundIter))
                  {

                    // Get the process alternate name
                    altName = procWiseTreeView.dataModel.getModel().getValue(
                                                                                            foundIter,
                                                                                            procWiseTreeView.dataModel.getNameDC());

                    // Set in tree
                    procWiseTreeView.dataModel.getModel().setValue(
                                                                     foundIter,
                                                                     procWiseTreeView.dataModel.getSelectedDC(),
                                                                     true);
                  }
              }
              }
        //  }
            // set the names
            currentDebugProcess.setRealName(currentDebugProcess.getName());
            currentDebugProcess.setAlternativeDisplayName(altName);
            currentDebugProcess.setName(altName);
          }
      }

  }

  /**
   * Sets the selection criteria (ie processes added to a session or not added
   * to a sesion) in the data model.
   * 
   * @param selected - Iter in tree to affect (from viewer).
   * @param setSelected - set the selected value.
   * @param setChildren - cascade down to children if true.
   */
  private void setTreeSelected (TreeIter selected, boolean setSelected)
  {
    dataModel.setSelected(selected, setSelected);
  }

  /**
   * If a child is a selected in a tree, we have to add the child's parent
   * also. This is because only process groups can be added in the druid (PIDs
   * and individual processes have no meaning inbetween sessions. This is a utility
   * function that given a Iter, will add that tree iters parent as well.
   * 
   * @param unfilteredProcessIter - parent
   */
 
  private void addProcessParent(TreeIter unfilteredProcessIter) 
  {
     if (unfilteredProcessIter == null)
       return;
     if (!dataModel.getModel().isIterValid(unfilteredProcessIter))
       return;

     GuiProc guiProc = (GuiProc) dataModel.getModel().getValue(
							    unfilteredProcessIter, dataModel.getObjectDC());
     if (guiProc == null)
      {
        final TreeIter childIter = unfilteredProcessIter.getChild(0);
        guiProc = (GuiProc) dataModel.getModel().getValue(childIter,
                                                    dataModel.getObjectDC());
      }
     
     final DebugProcess debugProcess = new DebugProcess(guiProc.getExecutableName(),
							 dataModel.getModel().getValue(unfilteredProcessIter,
							 dataModel.getNameDC()), guiProc.getNiceExecutablePath());
     debugProcess.addProc(guiProc);
     currentSession.addProcess(debugProcess);
     this.procMap.put(guiProc, debugProcess);
  }


  /**
   * Convert a TreeModelFilterIter to a TreePath
   * 
   * @param tree - TreeView the source iter is from
   * @param filter - Filtered TreePath
   * @return - TreePath, returns the unfiltered iter in the view
   */
  private TreePath deFilterPath (ProcWiseTreeView tree, TreePath filter)
  {
    return tree.deFilterPath(filter);
  }

  /**
   * Changes the transition on a group of processes from one tree to the other
   * As the druid removes items from one tree and adds them to another, it must
   * transport and translate those selected TreePaths. Ugly and really needs to
   * be re-written
   * 
   * @param tree - TreeView in question
   * @param selectedProcs - a TreePath[] of the selected processes
   * @param filtered - Is this a filtered tree?
   * @param state - What selected state do you want to render the selected
   *          TreePath[]s
   */
  private void changeGroupState (ProcWiseTreeView tree, TreePath[] selectedProcs,
                                 boolean filtered, boolean state)
  {

    // Create a TreeRowReference. TreeRowReferences track model changes and
    // so allow multiple selection.
    final TreeRowReference[] paths = new TreeRowReference[selectedProcs.length];
    TreeIter unfilteredProcessIter = null;

    if (selectedProcs.length > 0)
      {
        // Build the TreeRowReference. De filter from the model if needed.
        for (int i = 0; i < selectedProcs.length; i++)
          {
            if (selectedProcs[i] == null)
              {
                continue;
              }

            if (filtered)
              unfilteredProcessIter = dataModel.getModel().getIter(
                                                                   deFilterPath(
                                                                                tree,
                                                                                selectedProcs[i]));
            else
              unfilteredProcessIter = dataModel.getModel().getIter(
                                                                   selectedProcs[i]);

            paths[i] = new TreeRowReference(dataModel.getModel(),
                                            unfilteredProcessIter.getPath());

            if (state)
              {
                processSelected++;
                addProcessParent(dataModel.getModel().getIter(
                                                              paths[i].getPath()));
              }
            else
              processSelected--;
          }
        for (int i = selectedProcs.length - 1; i >= 0; i--)
          {
            setTreeSelected(dataModel.getModel().getIter(paths[i].getPath()),
                            state);
          }
      }
    setProcessNext(processSelected);
  }

  /**
   * @param processCount
   */
  private void setProcessNext (int processCount)
  {
    if (editSession == false && loadSession == false)
      {
        if (processCount > 0
            && nameEntry.getText().length() > 0
            && SessionManager.theManager.getSessionByName(nameEntry.getText()) == null)
          {
            nextButton.setSensitive(true);
            saveButton.setSensitive(true);
          }
        else
          {
            nextButton.setSensitive(false);
            saveButton.setSensitive(false);
          }
      }
    else
      {
        if (processCount > 0 && nameEntry.getText().length() > 0)
          {
            nextButton.setSensitive(true);
            saveButton.setSensitive(true);
          }
        else
          {
            nextButton.setSensitive(false);
            saveButton.setSensitive(false);
          }
      }
  }

  private void unFilterData ()
  {
    procWiseTreeView.dataModel.unFilterData();
  }

  private void getProcessSelectionControls (LibGlade glade)
  {

    Button addProcessButton;
    Button removeProcessButton;

    // Page 1 of the Druid. Initial Process Selection.

    // Create New Live Data Model and mount on the TreeView
    dataModel = new ProcWiseDataModel();
    procWiseTreeView = new ProcWiseTreeView(
                                            glade.getWidget(
                                                            "sessionDruid_procWiseTreeView").getHandle(),
                                            dataModel);
    procWiseTreeView.setFilter(true);

    procWiseTreeView.getSelection().setMode(SelectionMode.MULTIPLE);

    procWiseTreeView.addListener(new TreeViewListener()
    {
      public void treeViewEvent (TreeViewEvent event)
      {
        if (event.isOfType(TreeViewEvent.Type.ROW_ACTIVATED))
          {
            changeGroupState(procWiseTreeView,
                             procWiseTreeView.getSelection().getSelectedRows(),
                             true, true);
            if (! currentSession.getProcesses().isEmpty())
              {
                observerSelectionTreeView.setSensitive(true);
              }
          }

      }
    });

    addedProcsTreeView = new ProcWiseTreeView(
                                            glade.getWidget(
                                                            "sessionDruid_addedProcsTreeView").getHandle(),
                                            dataModel);
    addedProcsTreeView.setFilter(false);
    addedProcsTreeView.getSelection().setMode(SelectionMode.MULTIPLE);
    addedProcsTreeView.addListener(new TreeViewListener()
    {
      public void treeViewEvent (TreeViewEvent event)
      {
        if (event.isOfType(TreeViewEvent.Type.ROW_ACTIVATED))
          {
            final DebugProcess currentDebugProcess = (DebugProcess) addedProcsTreeView.getSelectedObject();
            if (currentDebugProcess != null)
              {
                final TreePath foo = dataModel.searchName(currentDebugProcess.getName());
                changeGroupState(procWiseTreeView, new TreePath[] { foo },
                                 false, false);
                currentSession.removeProcess(currentDebugProcess);
              }
            if (currentSession.getProcesses().isEmpty())
              {
                observerSelectionTreeView.setSensitive(false);
              }
            else
              {
                observerSelectionTreeView.setSensitive(true);
              }
          }
      }
    });
    setUpCurrentPage();

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

    final SizeGroup sizeGroup = new SizeGroup(SizeGroupMode.BOTH);
    sizeGroup.addWidget(procWiseTreeView);
    sizeGroup.addWidget(addedProcsTreeView);

    addProcessButton = (Button) glade.getWidget("sessionDruid_addProcessButton");
    removeProcessButton = (Button) glade.getWidget("sessionDruid_removeProcessButton");

    addProcessButton.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent event)
      {
        if (event.isOfType(ButtonEvent.Type.CLICK))
          {
            changeGroupState(procWiseTreeView,
                             procWiseTreeView.getSelection().getSelectedRows(),
                             true, true);
//            changeGroupState(addedProcsTreeView, addedProcsTreeView.getSelection().getSelectedRows(), true, true);
          }
        if (! currentSession.getProcesses().isEmpty())
          {
            observerSelectionTreeView.setSensitive(true);
          }
      }
    });

    removeProcessButton.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent event)
      {
        if (event.isOfType(ButtonEvent.Type.CLICK))
          {
            if (addedProcsTreeView.getSelectedObjects() == null)
              return;
            
            Iterator i = addedProcsTreeView.getSelectedObjects().iterator();
            if (i != null)
              {
                while (i.hasNext())
                  {
                    GuiProc gp = (GuiProc) i.next();
                    DebugProcess currentDebugProcess = (DebugProcess) procMap.remove(gp);
                    TreePath foo = dataModel.searchPid(gp.getProc().getPid());
                    changeGroupState(procWiseTreeView, new TreePath[] { foo },
                                     false, false);
                    currentSession.removeProcess(currentDebugProcess);
                  }
              }

            if (currentSession.getProcesses().isEmpty())
              {
                observerSelectionTreeView.setSensitive(false);
              }
            else
              {
                observerSelectionTreeView.setSensitive(true);
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

    processObserverSelectionTreeView.watchGuiProcs(currentSession.getProcesses());
    processObserverSelectionTreeView.getSelection().addListener(
                                                                new TreeSelectionListener()
                                                                {
                                                                  public void selectionChangedEvent (
                                                                                                     TreeSelectionEvent arg0)
                                                                  {
                                                                    final DebugProcess selected = (DebugProcess) processObserverSelectionTreeView.getSelectedObject();
                                                                    if (selected != null)
                                                                      {
                                                                        final Iterator i = selected.getObservers().iterator();
                                                                        observerSelectionTreeView.clearChecked();
                                                                        while (i.hasNext())
                                                                          {
                                                                            final ObserverRoot givenObserver = (ObserverRoot) i.next();
                                                                            observerSelectionTreeView.setCheckedByName(
                                                                                                                       givenObserver.getName(),
                                                                                                                       true);
                                                                          }
                                                                      }
                                                                  }
                                                                });

    observerSelectionTreeView.watchLinkedList(ObserverManager.theManager.getTaskObservers());
    observerSelectionTreeView.getCellRendererToggle().addListener(
                                                                  new CellRendererToggleListener()
                                                                  {
                                                                    public void cellRendererToggleEvent (
                                                                                                         CellRendererToggleEvent arg0)
                                                                    {
                                                                      final GuiObject selected = observerSelectionTreeView.getSelectedObject();
                                                                      final DebugProcess observerProcessSelected = (DebugProcess) processObserverSelectionTreeView.getSelectedObject();
                                                                      if (observerSelectionTreeView.isChecked(selected))
                                                                        {
                                                                          if (! observerProcessSelected.getObservers().contains(
                                                                                                                                selected))
                                                                            {
                                                                              observerProcessSelected.addObserver((ObserverRoot) selected.getCopy());
                                                                            }
                                                                        }
                                                                      else
                                                                        {
                                                                          observerProcessSelected.removeObserverByName(((ObserverRoot) selected).getName());
                                                                        }
                                                                    }
                                                                  });

    observerSelectionTreeView.getSelection().addListener(
                                                         new TreeSelectionListener()
                                                         {
                                                           public void selectionChangedEvent (
                                                                                              TreeSelectionEvent arg0)
                                                           {
                                                             if (observerSelectionTreeView.getSelectedObject() != null)
                                                               {
                                                                 if (observerSelectionTreeView.getSelectedObject().getToolTip() != null)
                                                                   {
                                                                     observerDescBuffer.setText(observerSelectionTreeView.getSelectedObject().getToolTip());
                                                                   }
                                                               }
                                                           }
                                                         });

    final SizeGroup sizeGroup = new SizeGroup(SizeGroupMode.BOTH);
    sizeGroup.addWidget(observerSelectionTreeView);
    sizeGroup.addWidget(processObserverSelectionTreeView);

    final Button customObserver = (Button) glade.getWidget("SessionDruid_createCustomObserver");
    customObserver.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent event)
      {
        if (event.isOfType(ButtonEvent.Type.CLICK))
          {
            WindowManager.theManager.observersDialog.showAll();
          }
      }
    });

    setUpCurrentPage();
  }

  private void getDruidStructureControls (LibGlade glade)
  {
    notebook = (Notebook) glade.getWidget("sessionDruid_sessionNoteBook");
    
    this.debugButton = (Button) glade.getWidget("sessionDruid_debugButton");
    this.debugButton.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent event)
      {
        if (event.isOfType(ButtonEvent.Type.CLICK))
          {
            LinkedList list = addedProcsTreeView.getListedObjects();
            if (list != null && list.size() > 0)
              {
                Iterator i = list.iterator();
                
                while (i.hasNext())
                  {
                    SourceWindowFactory.createSourceWindow(((GuiProc) i.next()).getProc());
                  }
                hide();
              }
            else
              {
                if (procWiseTreeView.getSelectedObjects() != null)
                  {
                    Iterator i = procWiseTreeView.getSelectedObjects().iterator();
                    while (i.hasNext())
                      {
                        SourceWindowFactory.createSourceWindow(((GuiProc) i.next()).getProc());
                      }
                    hide();
                  }
              }
          }
      }
    });

    nextButton = (Button) glade.getWidget("sessionDruid_nextButton");
    nextButton.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent event)
      {
        if (event.isOfType(ButtonEvent.Type.CLICK))
          {
            nextPage();
          }
      }
    });
    nextButton.setSensitive(false);

    backButton = (Button) glade.getWidget("sessionDruid_backButton");
    backButton.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent event)
      {
        if (event.isOfType(ButtonEvent.Type.CLICK))
          {
            previousPage();
          }
      }
    });

    finishButton = (Button) glade.getWidget("sessionDruid_finishButton");
    finishButton.setSensitive(true);
    finishButton.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent event)
      {
        if (event.isOfType(ButtonEvent.Type.CLICK))
          {
            Iterator i = currentSession.getProcesses().iterator();
            while (i.hasNext())
              ((DebugProcess) i.next()).addObservers();
            
            if (!loadSession)
              SessionManager.theManager.addSession(currentSession);
            
            procMap.clear();
            SessionManager.theManager.save();
            LinkedList ll = new LinkedList();
            ll.add(WindowManager.theManager.mainWindow);
            IconManager.trayIcon.setPopupWindows(ll);
            WindowManager.theManager.mainWindow.setSession(currentSession);
//            WindowManager.theManager.mainWindow.buildTerminal();
            WindowManager.theManager.mainWindow.showAll();
            WindowManager.theManager.sessionManager.hideAll();
            hide();
          }
      }
    });

    saveButton = (Button) glade.getWidget("sessionDruid_saveEditSessionButton");
    saveButton.hideAll();
    saveButton.addListener(new ButtonListener()
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

    cancelButton = (Button) glade.getWidget("sessionDruid_cancelButton");
    cancelButton.addListener(new ButtonListener()
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

    warningIcon = (Image) glade.getWidget("sessionDruid_feedbackImage");
    warningLabel = (Label) glade.getWidget("sessionDruid_feedbackLabel");
  }

  public void attachLinkedListsToWidgets ()
  {
    if (! currentSession.getName().equals("NoName"))
      {
        nameEntry.setText(currentSession.getName());
      }
    processObserverSelectionTreeView.watchGuiProcs(currentSession.getProcesses());
  }

  private void nextPage ()
  {

    // Process previous page data
    final int page = notebook.getCurrentPage();

    notebook.setCurrentPage(page + 1);
    setUpCurrentPage();
  }

  private void previousPage ()
  {
    notebook.setCurrentPage(notebook.getCurrentPage() - 1);
    setUpCurrentPage();
  }

  private void setUpCurrentPage ()
  {
    final int page = notebook.getCurrentPage();

    if (page == 0)
      {
        backButton.setSensitive(false);
        nextButton.setSensitive(true);
      }
    else
      {
        backButton.setSensitive(true);
      }

    if (page == notebook.getNumPages() - 1)
      {
        nextButton.hideAll();
        debugButton.hideAll();
        finishButton.showAll();
      }
    else
      {
        nextButton.showAll();
        debugButton.showAll();
        finishButton.hideAll();
      }

    if (page == 1)
      {
        setProcessNext(processSelected);
      }
  }

  private String setInitialName ()
  {
    final String Name = "New Frysk Session";
    String filler = "00";
    if (SessionManager.theManager.getSessionByName(Name) == null)
      {
        return Name;
      }
    else
      {
        for (int i = 1; i < Integer.MAX_VALUE; i++)
          {
            if (i < 10)
              {
                filler = "0" + i;
              }
            else
              {
                filler = "" + i;
              }
            if (SessionManager.theManager.getSessionByName(Name + " " + filler) == null)
              {
                return Name + " " + filler;
              }
          }
      }
    return "Error Finding Name";
  }

  public void showAll ()
  {
    super.showAll();
    setUpCurrentPage();
  }

  public void lifeCycleEvent (LifeCycleEvent event)
  {
  }

  public boolean lifeCycleQuery (LifeCycleEvent event)
  {
    if (event.isOfType(LifeCycleEvent.Type.DESTROY)
        || event.isOfType(LifeCycleEvent.Type.DELETE))
      {
        hide();
        return true;
      }
    return false;
  }

}
