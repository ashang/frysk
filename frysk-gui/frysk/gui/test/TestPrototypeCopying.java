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

package frysk.gui.test;

import java.util.Iterator;

import junit.framework.TestCase;

import org.gnu.gtk.Gtk;

import frysk.gui.monitor.actions.ActionPoint;
import frysk.gui.monitor.filters.FilterPoint;
import frysk.gui.monitor.observers.ObserverManager;
import frysk.gui.monitor.observers.ObserverRoot;
import frysk.gui.monitor.observers.TaskObserverRoot;

public class TestPrototypeCopying extends TestCase{

	public void testPrototypeCopying(){
		Gtk.init(new String[]{});
		ObserverManager observerManager = new ObserverManager();
		Iterator iter = observerManager.getTaskObservers().iterator();
		while (iter.hasNext()) {
			TaskObserverRoot a = (TaskObserverRoot) iter.next();
			System.out.println("Testing copy of :" + a.getName());
			ObserverRoot b = observerManager.getObserverCopy(a);
			assertCorrectCopy(a, b);
		}
	}
	
	private void assertCorrectCopy(ObserverRoot a, ObserverRoot b){
		assertFalse("Observers are not the same object ",a==b);

		assertEquals("Number of filterPoints ", a.getFilterPoints().size(), b.getFilterPoints().size());

		Iterator aIter = a.getFilterPoints().iterator();
		Iterator bIter = b.getFilterPoints().iterator();
		
		while (aIter.hasNext()) {
			FilterPoint aFilterPoint = (FilterPoint) aIter.next();
			FilterPoint bFilterPoint = (FilterPoint) bIter.next();
			assertFalse("FilterPoints are not the same object ", aFilterPoint == bFilterPoint);
			assertEquals("FilterPoint type", aFilterPoint.getClass(), bFilterPoint.getClass());
			assertEquals("Number of filters ", 0, bFilterPoint.getFilters().size());
			//XXX: should filters be copied ?
//			assertEquals("Number of filters ", aFilterPoint.getFilters().size(), bFilterPoint.getFilters().size());
//			Iterator aFilterIter = aFilterPoint.getFilters().iterator();
//			Iterator bFilterIter = bFilterPoint.getFilters().iterator();
//			while(aFilterIter.hasNext()){
//				Filter aFilter = (Filter) aFilterIter.next();
//				Filter bFilter = (Filter) bFilterIter.next();
//				assertFalse("Filters are not the same object ", aFilter == bFilter);
//			}
		}

		
		
		assertEquals("Number of actionPoints ", a.getActionPoints().size(), b.getActionPoints().size());

		aIter = a.getActionPoints().iterator();
		bIter = b.getActionPoints().iterator();
		
		while (aIter.hasNext()) {
			ActionPoint aActionPoint = (ActionPoint) aIter.next();
			ActionPoint bActionPoint = (ActionPoint) bIter.next();
			assertFalse("ActionPoints are not the same object ", aActionPoint == bActionPoint);
			assertEquals("ActionPoint type", aActionPoint.getClass(), bActionPoint.getClass());
			assertEquals("Number of actions ", 0, bActionPoint.getActions().size());
			//XXX: should actions be copied ?
//			assertEquals("Number of actions ", aActionPoint.getActions().size(), bActionPoint.getActions().size());
//			Iterator aActionIter = aActionPoint.getActions().iterator();
//			Iterator bActionIter = bActionPoint.getActions().iterator();
//			while(aActionIter.hasNext()){
//				Action aAction = (Action) aActionIter.next();
//				Action bAction = (Action) bActionIter.next();
//				assertFalse("Actions are not the same object ", aAction == bAction);
//			}
		}

	}
}
