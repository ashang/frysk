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
package frysk.gui.common.dialogs;

import org.gnu.gtk.GtkStockItem;
import org.gnu.gtk.VBox;
import org.gnu.gtk.Label;
import org.gnu.gtk.PolicyType;
import org.gnu.gtk.ScrolledWindow;
import org.gnu.gtk.TextBuffer;
import org.gnu.gtk.TextView;
import org.gnu.gtk.event.DialogEvent;
import org.gnu.gtk.event.DialogListener;

import java.io.PrintWriter;
import java.io.StringWriter;


public class ErrorDialog extends FryskDialog{

	private String title = ""; //$NON-NLS-1$
	private String message = ""; //$NON-NLS-1$
	private Exception except = null;
	
	public static final int QUIT = 1;
	public static final int IGNORE = 2;

	public  ErrorDialog(String title, String message, Exception except) {
		super();
		this.title = title;
		this.message = message;
		this.except = except;
		doImplementation();
	}
	
	public ErrorDialog(String message, Exception except) {
		super();
		this.title = "Error"; //$NON-NLS-1$
	    this.message = message;
		this.except = except;

		doImplementation();
	}

	private synchronized String getStringTrace(Exception e)
	{
	  StringWriter sw = new StringWriter();
	  e.printStackTrace(new PrintWriter(sw));
	  return sw.toString();
	}
	
	private  void doImplementation()
	{
		
		this.addButton(GtkStockItem.QUIT, 1);
		this.addButton("Continue", 2);
		this.setTitle(this.title);
		VBox mainBox = new VBox(false,2);
		mainBox.setSpacing(12);
		mainBox.setBorderWidth(6);
		
		this.getDialogLayout().add(mainBox);
		
		ScrolledWindow sWindow = new ScrolledWindow(null,null);
		sWindow.setBorderWidth(12);
		sWindow.setPolicy(PolicyType.AUTOMATIC,PolicyType.AUTOMATIC);
		sWindow.setMinimumSize(400,300);

		
		String exceptionMessage;
		
		if (this.except.getMessage() == null)
			exceptionMessage = "(No exception message provided)";
		else
			exceptionMessage = this.except.getMessage();
		
		String errorText = exceptionMessage+"\n\n" + //$NON-NLS-1$ //$NON-NLS-2$
		getStringTrace(this.except);
		
		TextView warnLabel = new TextView();
		warnLabel.setIndent(6);
		warnLabel.setBorderWidth(6);
		
		TextBuffer foo = new TextBuffer();

		foo.setText(errorText);
		warnLabel.setBuffer(foo);
		warnLabel.setEditable(false);


	
		sWindow.addWithViewport(warnLabel);
		mainBox.packStart(new Label(this.message),true,true,0);
		mainBox.packStart(sWindow,true, true, 0);

		this.addListener(new DialogListener(){
			public boolean dialogEvent(DialogEvent arg0) {
				hideAll();
				return false;
			}
		});
		

		
	}
	
		
}
