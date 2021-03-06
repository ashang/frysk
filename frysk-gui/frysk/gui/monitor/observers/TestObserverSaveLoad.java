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

package frysk.gui.monitor.observers;

import java.util.Iterator;

import org.gnu.gtk.Gtk;
import org.jdom.Element;

import frysk.gui.monitor.ObjectFactory;
import frysk.gui.monitor.actions.ActionPoint;
import frysk.gui.monitor.actions.LogAction;
import frysk.gui.monitor.actions.PrintTask;
import frysk.gui.monitor.actions.TaskActionPoint;
import frysk.gui.monitor.filters.FilterPoint;
import frysk.gui.monitor.filters.ProcNameFilter;
import frysk.gui.monitor.filters.TaskFilterPoint;
import frysk.gui.test.GuiTestCase;

public class TestObserverSaveLoad extends GuiTestCase{

  protected void setUp () throws Exception
  {
    super.setUp();
    OBSERVERS_TEST_DIR.mkdir();
    cleanDir(OBSERVERS_TEST_DIR);
  }
  
  protected void tearDown () throws Exception
  {
    cleanDir(OBSERVERS_TEST_DIR);
    OBSERVERS_TEST_DIR.delete();
    super.tearDown();
  }
  
  
  	public void testSaveLoad(){
		Gtk.init(new String[]{});
		
		Element node = new Element("testNode");
		
		TaskForkedObserver taskForkedObserver = new TaskForkedObserver();
		taskForkedObserver.setName("blablabla");
		taskForkedObserver.setToolTip("blablobla");
		
		Iterator i = taskForkedObserver.getFilterPoints().iterator();
		while (i.hasNext()) {
			FilterPoint filterPoint = (FilterPoint) i.next();
			filterPoint.setName(""+taskForkedObserver.getFilterPoints().indexOf(filterPoint));
		}
		
		i = taskForkedObserver.getActionPoints().iterator();
		while (i.hasNext()) {
			ActionPoint actionPoint = (ActionPoint) i.next();
			actionPoint.setName(""+taskForkedObserver.getActionPoints().indexOf(actionPoint));
		}

		i = taskForkedObserver.getActionPoints().iterator();
		while (i.hasNext()) {
			ActionPoint actionPoint = (ActionPoint) i.next();
			actionPoint.setName(""+taskForkedObserver.getActionPoints().indexOf(actionPoint));
		}
		

		ObjectFactory.theFactory.saveObject(taskForkedObserver, node);
	
//		System.out.println("\n==============saved node==========");
//		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
//		try {
//			outputter.output(node, System.out);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		System.out.println("===================================\n");
		
		
		ObserverRoot observerRoot = (ObserverRoot) ObjectFactory.theFactory.loadObject(node);
	
		assertNotNull(observerRoot);
		assertEquals("Class type", taskForkedObserver.getClass() , observerRoot.getClass());
		assertEquals("Name", taskForkedObserver.getName(), observerRoot.getName());
		assertEquals("ToolTip", taskForkedObserver.getToolTip(), observerRoot.getToolTip());
		
		i = observerRoot.getFilterPoints().iterator();
		while (i.hasNext()) {
			FilterPoint filterPoint = (FilterPoint) i.next();
			assertEquals("filterPoint name", ""+ observerRoot.getFilterPoints().indexOf(filterPoint), filterPoint.getName() );
		}
		
		i = observerRoot.getActionPoints().iterator();
		while (i.hasNext()) {
			ActionPoint actionPoint = (ActionPoint) i.next();
			assertEquals("ActionPoint name", ""+ observerRoot.getActionPoints().indexOf(actionPoint), actionPoint.getName() );
		}
		
		assertEquals("ActionPoints size", observerRoot.getActionPoints().size(), taskForkedObserver.getActionPoints().size() );
		assertEquals("FilterPoints size", observerRoot.getFilterPoints().size(), taskForkedObserver.getFilterPoints().size() );
	}
	
	
	public void testFilterPointSaveLoad(){
		Element node = new Element("testNode");
		TaskFilterPoint savedFilterPoint = new TaskFilterPoint("","");

		ProcNameFilter savedFilter = new ProcNameFilter();
		savedFilter.setName("1");
        savedFilter.setFilterBoolean(true);
		savedFilterPoint.addFilter(savedFilter);

		savedFilter = new ProcNameFilter();
		savedFilter.setArgument("2");
        savedFilter.setFilterBoolean(false);
		savedFilterPoint.addFilter(savedFilter);

		ObjectFactory.theFactory.saveObject(savedFilterPoint, node);

		FilterPoint loadedFilterPoint = (FilterPoint) ObjectFactory.theFactory.loadObject(node);
		Iterator i = loadedFilterPoint.getItems().iterator();
		
		ProcNameFilter loadedFilter = (ProcNameFilter)i.next(); 
		assertEquals("FilterName", "1", loadedFilter.getName());
		assertTrue("Filter boolean", loadedFilter.getFilterBoolean());
        
		loadedFilter = (ProcNameFilter)i.next(); 
		assertEquals("FilterName", "2", loadedFilter.getArgument());
        assertFalse("Filter boolean", loadedFilter.getFilterBoolean());
        
	}
	
	public void testActionPointSaveLoad(){
		Element node = new Element("testNode");
		TaskActionPoint taskActionPoint = new TaskActionPoint("", "");
		PrintTask printTask = new PrintTask();
		printTask.setName("1");
		taskActionPoint.addAction(printTask);
		
		printTask = new PrintTask();
		printTask.setName("2");
		taskActionPoint.addAction(printTask);
		
		printTask = new PrintTask();
		printTask.setName("3");
		taskActionPoint.addAction(printTask);
		
		printTask = new PrintTask();
		printTask.setName("4");
		taskActionPoint.addAction(printTask);
		
		ObjectFactory.theFactory.saveObject(taskActionPoint, node);
		
		ActionPoint actionPoint = (ActionPoint) ObjectFactory.theFactory.loadObject(node);
		Iterator i = actionPoint.getActions().iterator();
		assertEquals("ActionName", ((PrintTask)i.next()).getName(),"1");
		assertEquals("ActionName", ((PrintTask)i.next()).getName(),"2");
		assertEquals("ActionName", ((PrintTask)i.next()).getName(),"3");
		assertEquals("ActionName", ((PrintTask)i.next()).getName(),"4");
	}
	
	public void testExport(){
		ObserverManager observerManager = new ObserverManager(OBSERVERS_TEST_DIR);
		observerManager.init();
		TaskForkedObserver taskForkedObserver = new TaskForkedObserver();
		
		String testObserverName = "MyCustomObserverXXX_this_should_have_been_deleted_after_test";
		//customize taskForkedObserver
		LogAction logAction = new LogAction();
		taskForkedObserver.genericActionPoint.addAction(logAction);
		taskForkedObserver.setName(testObserverName);
		observerManager.addTaskObserverPrototype(taskForkedObserver);
		observerManager.save();
		
		ObserverManager anotherObserverManager = new ObserverManager(OBSERVERS_TEST_DIR);
		anotherObserverManager.init();
		assertEquals("Number of Observers", observerManager.getTaskObservers().size(), anotherObserverManager.getTaskObservers().size());
		
	//	observerManager.removeTaskObserverPrototype(taskForkedObserver);
		
		//get custom observer
		//Iterator i = anotherObserverManager.getTaskObservers().iterator();
		ObserverRoot myLoadedObserver = anotherObserverManager.getObserverByName(testObserverName);
//		while (i.hasNext()) {
//			myLoadedObserver = (ObserverRoot) i.next();
//		}
		
		// check that they are the same
		assertNotNull("Loaded Observer", myLoadedObserver);
		assertEquals("Class Type", taskForkedObserver.getClass(), myLoadedObserver.getClass());
		assertEquals("ObserverName", taskForkedObserver.getName(), myLoadedObserver.getName());
		assertEquals("Number of Actions", taskForkedObserver.genericActionPoint.getActions().size(), myLoadedObserver.genericActionPoint.getActions().size());

		observerManager.removeTaskObserverPrototype(taskForkedObserver);
	}
	
}
