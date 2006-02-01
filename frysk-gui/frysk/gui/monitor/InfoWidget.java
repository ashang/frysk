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
// 
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
/*
 * Created on Sep 19, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.util.Observable;

import org.gnu.gtk.Notebook;
import org.gnu.gtk.VBox;
import org.gnu.gtk.Widget;

import frysk.gui.common.Messages;

public class InfoWidget extends Notebook {

	public Observable notifyUser;

	public VBox procStatusVbox;
	public VBox taskStatusVbox;
	
	public InfoWidget(){
		
		//Window myWindow = this.getWindow();
		
		this.setBorderWidth(4);
		this.notifyUser = new Observable();
		
		this.procStatusVbox = new VBox(false, 0);
		this.taskStatusVbox = new VBox(false, 0);
		
		//========================================
		VBox statusVbox = new VBox(true, 0);
		NotifyingLabel statusWidgetLabel = new NotifyingLabel(Messages.getString("InfoWidget.0")); //$NON-NLS-1$
		statusVbox.packStart(procStatusVbox);
		statusVbox.packStart(taskStatusVbox);
		this.appendPage(statusVbox, statusWidgetLabel);		
		//========================================
	
		this.showAll();
	}
	
	/**
	 * Set the selected proc to the one represented by the given
	 * ProcData.
	 * @param selected ProcData, null if no ProcData is selected
	 * */
	public void setSelectedProc(ProcData data){
		Widget[] widgets = this.procStatusVbox.getChildren();
		if(widgets.length > 0){
			this.procStatusVbox.remove(widgets[0]);
		}
		if(data != null){ this.procStatusVbox.add(data.getWidget()); }
	}
	
	/**
	 * Set the selected task to the one represented by the given
	 * TaskData.
	 * @param selected TaskData, null if no ProcData is selected
	 * */
	public void setSelectedTask(TaskData data){
		Widget[] widgets = this.taskStatusVbox.getChildren();
		if(widgets.length > 0){
			this.taskStatusVbox.remove(widgets[0]);
		}
		if(data != null){ this.taskStatusVbox.add(data.getWidget()); }
	}
	
}
