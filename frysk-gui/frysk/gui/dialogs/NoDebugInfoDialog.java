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
 * Created on Sep 26, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.dialogs;

import org.gnu.gtk.GtkStockItem;
import org.gnu.gtk.HBox;
import org.gnu.gtk.IconSize;
import org.gnu.gtk.Image;
import org.gnu.gtk.Label;
import org.gnu.gtk.event.DialogEvent;
import org.gnu.gtk.event.DialogListener;


public class NoDebugInfoDialog extends FryskDialog{

	private String title = ""; //$NON-NLS-1$
	private String message = ""; //$NON-NLS-1$
	public static final int QUIT = 1;
	public static final int IGNORE = 2;

	
	public NoDebugInfoDialog(String message) {
		super();
		this.title = "No Debug Info"; //$NON-NLS-1$
	    this.message = message;
		doImplementation();
	}

	
	private  void doImplementation()
	{
		
		this.addButton(GtkStockItem.CLOSE, 1);
		this.setTitle(this.title);
		HBox mainBox = new HBox(false,2);
		mainBox.setSpacing(12);
		mainBox.setBorderWidth(6);
		
		this.getDialogLayout().add(mainBox);

		Image warningIcon = new Image(GtkStockItem.DIALOG_WARNING,IconSize.DIALOG);

		mainBox.packStart(warningIcon,true, true, 0);	
		mainBox.packStart(new Label(this.message),true,true,0);

		this.addListener(new DialogListener(){
			public boolean dialogEvent(DialogEvent arg0) {
				hideAll();
				return false;
			}
		});
		

		
	}
	
		
}
