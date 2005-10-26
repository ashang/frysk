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
package frysk.gui.test;

import org.gnu.gtk.Gtk;

import frysk.gui.srcwin.SourceWindow;
import frysk.gui.srcwin.StackLevel;
import frysk.gui.srcwin.dom.DOMFrysk;
import frysk.gui.srcwin.dom.DOMSource;
import frysk.gui.srcwin.dom.DOMTestGUIBuilder;


/**
 * Simple driver program for the source window. Instantiates the window and
 * starts the Gtk loop
 * 
 * @author ajocksch
 *
 */

public class SourceWinRunner {

	public static void main(String[] args) {
		Gtk.init(args);
		
		DOMFrysk dom = DOMTestGUIBuilder.makeTestDOM();
		DOMSource source = dom.getImage("test6").getSource("test6.cpp");
		source.setFileName("test6.cpp");
		source.setFilePath("../frysk/frysk-gui/frysk/gui/srcwin/testfiles");
		StackLevel stack1 = new StackLevel(source, 11);
		
		SourceWindow s = new SourceWindow(
				new String[] {"frysk-gui/frysk/gui/glade/", 
								"../frysk/frysk-gui/frysk/gui/glade/"},
				"../frysk/frysk-gui/frysk/gui/images/",
				dom,
				stack1);
		
		s.getClass();
		
		Gtk.main();
	}
	
	public static void mainSourceWin(String[] args, String[] paths, String imageDir){
		Gtk.init(args);
		
//		SourceWindow s = new SourceWindow(paths, imageDir);
		
//		StackLevel loc = new StackLevel("/home/ajocksch/frysk/frysk-gui/frysk/gui/srcwin/testfiles/test.cpp","main()", 5);
//		StackLevel loc2 = new StackLevel("/home/ajocksch/frysk/frysk-gui/frysk/gui/srcwin/testfiles/test2.cpp", "foo()", 12);
//		loc.addNextScope(loc2);
//		StackLevel loc3 = new StackLevel("/home/ajocksch/frysk/frysk-gui/frysk/gui/srcwin/testfiles/test3.cpp", "bar()", 5);
//		loc2.addNextScope(loc3);
//		StackLevel loc4 = new StackLevel("/home/ajocksch/frysk/frysk-gui/frysk/gui/srcwin/testfiles/test4.cpp", "baz(int)", 20);
//		loc3.addInlineScope(loc4);
//		loc3.addNextScope(new StackLevel("/home/ajocksch/frysk/frysk-gui/frysk/gui/srcwin/testfiles/test5.cpp", "foobar()", 2));
		
//		s.populateStackBrowser(loc);
		
//		s.getClass();
		
		Gtk.main();
	}

}
