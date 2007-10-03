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
package frysk.gui.monitor;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.gnu.glade.LibGlade;
// import org.gnu.gtk.Notebook;
import org.gnu.gtk.Window;

import frysk.gui.Gui;

public class MainWindow extends Window implements Saveable{
	
	private ProcViewPage procViewPage;
	
	private Logger errorLog = Logger.getLogger (Gui.ERROR_LOG_ID);
	public MainWindow(LibGlade glade) throws IOException {
		super(((Window)glade.getWidget("procpopWindow")).getHandle()); //$NON-NLS-1$
		
		try {
			this.procViewPage = new ProcViewPage(glade);
			ProgramViewPage programViewPage = new ProgramViewPage(glade);
			procViewPage.getClass();
			programViewPage.getClass();
		} catch (IOException e){
			errorLog.log(Level.SEVERE,"IOException from Proc Widget",e); //$NON-NLS-1$
		}
		
		TearOffNotebook noteBook = new TearOffNotebook((glade.getWidget("noteBook")).getHandle()); //$NON-NLS-1$
		//XXX:
		noteBook.getClass();
		noteBook.removePage(1);
		this.showAll();
	}

	public void save(Preferences prefs) {
		prefs.putInt("position.x", this.getPosition().getX()); //$NON-NLS-1$
		prefs.putInt("position.y", this.getPosition().getY()); //$NON-NLS-1$
		
		prefs.putInt("size.height", this.getSize().getHeight()); //$NON-NLS-1$
		prefs.putInt("size.width", this.getSize().getWidth()); //$NON-NLS-1$
		
		procViewPage.save(Preferences.userRoot().node(prefs.absolutePath() + "/allProcWidget")); //$NON-NLS-1$
	}

	public void load(Preferences prefs) {
		int x = prefs.getInt("position.x", this.getPosition().getX()); //$NON-NLS-1$
		int y = prefs.getInt("position.y", this.getPosition().getY()); //$NON-NLS-1$
		if ((x >=0) && (y >=0))
			this.move(x,y);
		
		int width  = prefs.getInt("size.width", this.getSize().getWidth()); //$NON-NLS-1$
		int height = prefs.getInt("size.height", this.getSize().getHeight()); //$NON-NLS-1$
		
		if ((width > 0) && (height > 0))
			this.resize(width, height);
		
		procViewPage.load(Preferences.userRoot().node(prefs.absolutePath() + "/allProcWidget")); //$NON-NLS-1$
	}
	
}

