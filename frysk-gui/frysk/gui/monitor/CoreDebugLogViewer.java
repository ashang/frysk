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

import java.util.Date;

import org.gnu.gtk.GtkStockItem;
import org.gnu.gtk.HBox;
import org.gnu.gtk.PolicyType;
import org.gnu.gtk.ResponseType;
import org.gnu.gtk.ScrolledWindow;
import org.gnu.gtk.TextBuffer;
import org.gnu.gtk.TextView;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;

import frysk.gui.common.dialogs.Dialog;
import frysk.gui.monitor.datamodels.CoreDebugLogRecord;
import frysk.gui.monitor.datamodels.DataModelManager;

/**
 * 
 * @author pmuldoon
 *
 * A Dialog that displays a list core log messages
 */
public class CoreDebugLogViewer extends Dialog {
	
	ListView logMessages;
	private TextView detailView;
	private TextBuffer detailViewBuffer;
	
	public CoreDebugLogViewer(){
		
		super.setName("Frysk Developer Debug Window");
		this.logMessages = new ListView();
		this.logMessages.watchLinkedList(DataModelManager.theManager.coreDebugDataModel);
		this.logMessages.getSelection().addListener( new TreeSelectionListener(){

			public void selectionChangedEvent(TreeSelectionEvent arg0) {
				CoreDebugLogRecord data = (CoreDebugLogRecord) logMessages.getSelectedObject();
				detailViewBuffer.setText(
						"Log Sequence: " + data.getSequence() + "\n" +
						"Log Stamp: " + new Date(data.getMillis()) + "\n" +
						"Log Severity: " + data.getLevel().toString() + "\n" +
						"Source Class: " + data.getSourceClass() + "\n" +
						"Source Method: " + data.getSourceMethod() + "\n" +
						"Message: " + data.getMessage());
				
			}});

		this.detailView = new TextView();
		this.detailViewBuffer = new TextBuffer();
		this.detailView.setBuffer(this.detailViewBuffer);
		
		HBox mainBox = new HBox(false,2);
		this.getDialogLayout().add(mainBox);
		
		ScrolledWindow sWindow = new ScrolledWindow(null,null);
		sWindow.setMinimumSize(350, 350);
		sWindow.setBorderWidth(10);
		sWindow.setPolicy(PolicyType.NEVER,PolicyType.AUTOMATIC);
		sWindow.addWithViewport(this.logMessages);
		
		ScrolledWindow dWindow = new ScrolledWindow(null,null);
		dWindow.setMinimumSize(250, 350);
		dWindow.setBorderWidth(10);
		dWindow.setPolicy(PolicyType.AUTOMATIC,PolicyType.AUTOMATIC);
		
		dWindow.addWithViewport(this.detailView);
		mainBox.packStart(sWindow);
		mainBox.packEnd(dWindow);
		
		this.addButton(GtkStockItem.CLOSE, ResponseType.OK.getValue());

		this.showAll();
	}
	
	
}
