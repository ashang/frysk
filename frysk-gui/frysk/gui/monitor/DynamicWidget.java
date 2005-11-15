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

import org.gnu.gtk.AttachOptions;
import org.gnu.gtk.Entry;
import org.gnu.gtk.Frame;
import org.gnu.gtk.Label;
import org.gnu.gtk.Table;
import org.gnu.gtk.ToolTips;

/**
 * Allows clients to easily create an argument entry
 * widget without having to deal with widgets. Clients
 * only have to specify the type of entries they require.
 * more to come: more types, validation, etc as needed.
 * */
public class DynamicWidget extends Frame {

	Table tabel;
	int row;
	
	public DynamicWidget(String name){
		super(name);
		this.initWidget();
	}
	
	public DynamicWidget(){
		super();
		this.initWidget();
	}
	
	private void initWidget(){
		this.tabel = new Table(0,0,false);
		this.add(tabel);
		this.row = 0;
	}
	
	public void addString(GuiObject key, final String string){
		this.addLabel(key);
		this.addTextEntry(key);
//		entry.addListener(new EntryListener() {
//			public void entryEvent(EntryEvent arg0) {
//				runnable.run();
//			}
//		});
		this.row++;
	}
	
	public void addInteger(GuiObject key, Integer integer){
		this.addLabel(key);
		this.addTextEntry(key);
		this.row++;
	}
	
	private void addLabel(GuiObject key){
		Label label = new Label(key.getName());
		ToolTips tip = new ToolTips();
		tip.setTip(label, key.getToolTip(), "");
		this.tabel.attach(label,0,1, this.row,this.row+1, AttachOptions.SHRINK, AttachOptions.SHRINK, 3, 3);
	}
	
	private Entry addTextEntry(GuiObject key){
		Entry entry = new Entry();
		ToolTips tip = new ToolTips();
		tip.setTip(entry, key.getToolTip(), "");
		this.tabel.attach(entry,1,2, this.row,this.row+1, AttachOptions.FILL, AttachOptions.SHRINK, 3, 3);
		return entry;
		
	}
}
