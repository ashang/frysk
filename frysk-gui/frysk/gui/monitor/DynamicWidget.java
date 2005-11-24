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

import java.util.Vector;

import org.gnu.gtk.AttachOptions;
import org.gnu.gtk.Entry;
import org.gnu.gtk.Label;
import org.gnu.gtk.Table;
import org.gnu.gtk.ToolTips;
import org.gnu.gtk.event.EntryEvent;
import org.gnu.gtk.event.EntryListener;

/**
 * Allows clients to easily create an argument entry
 * widget without having to deal with widgets. Clients
 * only have to specify the type of entries they require.
 * more to come: more types, validation, etc as needed.
 * */
public class DynamicWidget extends Table {

//	Table tabel;
	int row;
	Vector mutables;
	
	public DynamicWidget(){
		super(0,0,false);
		this.row = 0;
		this.mutables = new Vector();
	}
	
	/**
	 * Adds an Entry to allow the client to edit the given string
	 * @param mutable string to be displayed int the Entry
	 * and to be changed when and entry is made.
	 * */
	public void addString(GuiObject key, String mutable){
		this.addLabel(key);
		final int index = this.mutables.size();
		this.mutables.add(mutable);
		final Entry entry = this.addTextEntry(key);
		entry.setText(mutable);
		
		entry.addListener(new EntryListener() {
			public void entryEvent(EntryEvent event) {
				if(event.isOfType(EntryEvent.Type.CHANGED)){
					System.out.println("DynamicWidget.addString()");
					String myString = (String) mutables.get(index);
					myString.replaceAll(myString, entry.getText()); 
				}
			}
		});
		this.row++;
	}
	
	public void addInteger(GuiObject key){
		this.addLabel(key);
		this.addTextEntry(key);
		this.row++;
	}
	
	private void addLabel(GuiObject key){
		Label label = new Label(key.getName());
		ToolTips tip = new ToolTips();
		tip.setTip(label, key.getToolTip(), "");
		this.attach(label,0,1, this.row,this.row+1, AttachOptions.SHRINK, AttachOptions.SHRINK, 3, 3);
	}
	
	private Entry addTextEntry(GuiObject key){
		Entry entry = new Entry();
		ToolTips tip = new ToolTips();
		tip.setTip(entry, key.getToolTip(), "");
		this.attach(entry,1,2, this.row,this.row+1, AttachOptions.FILL, AttachOptions.SHRINK, 3, 3);
		return entry;
		
	}
}
