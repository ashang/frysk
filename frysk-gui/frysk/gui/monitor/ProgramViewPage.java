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
 * Created on Oct 20, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.io.IOException;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.Widget;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;

/**
 * A widget representing the program view page.
 * */
public class ProgramViewPage extends Widget {
	
	// private TreeView programTreeView;
	public ProgramAddWindow fileChooserDialog;

	private TreeView programTreeView;
	

	public ProgramViewPage(final LibGlade glade) throws IOException {
		super((glade.getWidget("programVBox")).getHandle());
		
		this.programTreeView = (TreeView) glade.getWidget("programTreeView");
		Button browseButton = (Button) glade.getWidget("programBrowseButton");
			
		browseButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				
				if(event.getType() == ButtonEvent.Type.CLICK){
					WindowManager.theManager.programAddWindow.showAll();
				}
			}
		});
		
//		this.fileChooserDialog.addListener(new DialogListener(){
//			public boolean dialogEvent(DialogEvent event) {
//			System.out.println("event: " + event + " RESPONCE: " + event.getResponse());
//				if(event.getType() == DialogEvent.Type.RESPONSE){
//					fileChooserDialog.hide();
//				}
//				return false;
//			}
//		});
				
		this.initProgramTreeView();
		
	}

	private void initProgramTreeView() {
		this.programTreeView.getClass();//XXX: dummy call to get rid of ecj warning
	}
	
	
}
