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

import frysk.junit.TestCase;
import org.gnu.gtk.Gtk;
import frysk.gui.srcwin.tags.Tag;
import frysk.gui.srcwin.tags.Tagset;
import frysk.gui.srcwin.tags.TagsetManager;

public class TestTagsetSaveLoad extends TestCase {
	
	public void testSaveLoad(){
		Gtk.init(new String[]{});
		
		// Create a TagSet Manager, TagSet, and two Tags
		TagsetManager tagSetManager = new TagsetManager();
		Tagset myTagset = new Tagset("FooTest", "Test Tagset", "/usr/bin/tagTest", "1.0");
		Tag myTag1 = new Tag("tagTest.cpp", "", 100,"cout << test;");
		Tag myTag2 = new Tag("tagTest.cpp", "",1202, "cout << fake test two");
		
		// Add the Tags to the Tag Set
		myTagset.addTag(myTag1);
		myTagset.addTag(myTag2);
		int tagCount = 2;
		
		// Add the Tag Set to the TagSet Manager
		tagSetManager.addTagset(myTagset);
	
		// Save the TagSet Manager
		tagSetManager.save();
		
		// Crate a new TagSet Manager	
		TagsetManager loadedTagSetManager = new TagsetManager();

				
		// Get the tag set we just created and saved
		Tagset myLoadedTagset = loadedTagSetManager.getTagsetByName("FooTest");
		
		// Start test too see if loaded is identical to original.
		assertNotNull("loaded Tagset session", myLoadedTagset);
		assertEquals("Tagset name", myLoadedTagset.getName(),myTagset.getName());
		assertEquals("Tagset desc", myLoadedTagset.getDesc(), myTagset.getDesc());
		assertEquals("Tagset command",myLoadedTagset.getCommand(), myTagset.getCommand());
		assertEquals("Tagset version",myLoadedTagset.getVersion(), myTagset.getVersion());
		
		Iterator j = myTagset.getTags();
		Iterator i = myLoadedTagset.getTags();
		
		int count = 0;
		while (j.hasNext() && i.hasNext())
		{
			count++;
			
			Tag myTag = (Tag) j.next();
			Tag loadedTag = (Tag) i.next();
			
			assertEquals("Tag set " + count + " match",myTag.equals(loadedTag),true);
		}
		
		assertEquals("Tag count",count,tagCount);
			
		tagSetManager.removeTagset(myTagset);
	}
}
