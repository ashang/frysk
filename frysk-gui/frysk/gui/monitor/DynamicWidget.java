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
import org.gnu.gtk.Label;
import org.gnu.gtk.Table;
import org.gnu.gtk.ToolTips;
import org.gnu.gtk.event.EntryEvent;
import org.gnu.gtk.event.EntryListener;

/**
 * 
 * @author swagiaal
 *
 * Allows clients to easily create an argument entry
 * widget without having to deal with widgets. Clients
 * only have to specify the type of entries they require.
 * more to come: more types, validation, etc as needed.
 */
public class DynamicWidget extends Table {
	
	public static interface StringCallback{
		void stringChanged(String string);
	}
	
	public static interface IntCallback{
		void intChanged(int i);
	}
	
	int row;
	
	public DynamicWidget(){
		super(0,0,false);
		this.row = 0;
	}
	
	public DynamicWidget(DynamicWidget other) {
		super(0,0,false);
		this.row = other.row;
	}

	/**
	 * Adds an @link Entry to the dynamic widget.
	 * When the user edits the Entry the given @link StringCallback
	 * is notified.
	 * @param key the key representing the entry to be added. It is used
	 * to set the name and tool tip if the Label preceding the Entry.
	 * @param initText the text that the entry is initialized with.
	 * @param stringCallback the @link StringCallback object that will be
	 * notified whey the text in the entry is edited.
	 * */
	public void addString(GuiObject key, String initText,final StringCallback stringCallback){
		this.addLabel(key);
		final Entry entry = this.addTextEntry(key);
		final StringCallback thisStringCallback = stringCallback;
		
		entry.setText(initText);
		entry.addListener(new EntryListener() {
			public void entryEvent(EntryEvent event) {
				if(event.isOfType(EntryEvent.Type.CHANGED)){
					thisStringCallback.stringChanged(entry.getText());
					System.out.println(".entryEvent() " + entry.getText());
				}
			}
		});
		this.row++;
	}
	
	public void addInteger(GuiObject key, int initValue, final IntCallback intCallback){
		this.addLabel(key);

		final Entry entry = this.addTextEntry(key);
		final IntCallback thisIntCallback = intCallback;
		
		entry.setText(""+initValue);
		entry.addListener(new EntryListener() {
			public void entryEvent(EntryEvent event) {
				if(event.isOfType(EntryEvent.Type.CHANGED)){
					thisIntCallback.intChanged(Integer.parseInt(entry.getText()));
					System.out.println(".entryEvent() " + entry.getText());
				}
			}
		});
		
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
