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
 * Created on 5-Jul-05
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.TextBuffer;
import org.gnu.gtk.TextView;
import org.gnu.gtk.Window;

import frysk.proc.Proc;

/**
 * @author sami wagiaalla
 * Generic log window, just prints out events it recieves
 * */
public class LogWindow extends Window implements Observer, Saveable{
	
	public TextView logTextView;
    public AttachedContinueObserver attachedContinueObserver;
    public DetachedContinueObserver detachedContinueObserver;
    public AttachedStopObserver attachedStopObserver;
    public AttachedResumeObserver attachedResumeObserver;
    
	public LogWindow(LibGlade glade){
		super(((Window)glade.getWidget("logWindow")).getHandle());
		this.logTextView = (TextView) glade.getWidget("logTextView");
        this.attachedContinueObserver = new AttachedContinueObserver();
        this.detachedContinueObserver = new DetachedContinueObserver();
        this.attachedStopObserver = new AttachedStopObserver();
        this.attachedResumeObserver = new AttachedResumeObserver();
	}
	
	static int count = 0;
	public void update(Observable observable, Object obj) {
		org.gnu.glib.CustomEvents.addEvent(new Runnable(){
			 public void run() {
				 TextBuffer tb = logTextView.getBuffer();
				 tb.insertText("event "+(count++)+" : ");
		
				 tb.insertText("\n");
			 }
		});
	}
	
    class AttachedContinueObserver implements Observer{
        public void update(Observable arg0, final Object arg1) {
        	org.gnu.glib.CustomEvents.addEvent(new Runnable(){
        		public void run() {
        			TextBuffer tb = logTextView.getBuffer();
        			tb.insertText("event "+(count++)+" : ");
        			tb.insertText("PID " + ((Proc)arg1).getPid() +" on Host XXX attached " + " \n");
        		 }
    		});     
        }
    }

    class AttachedStopObserver implements Observer{
        public void update(Observable arg0, final Object arg1) {
        	org.gnu.glib.CustomEvents.addEvent(new Runnable(){
        		public void run() {
        			TextBuffer tb = logTextView.getBuffer();
        			tb.insertText("event "+(count++)+" : ");
        		    tb.insertText("PID " + ((Proc)arg1).getPid() +" on Host XXX stopped" + " \n");
        		    System.out.println("Got here");
        		 }
    		});
        }
    }

    class AttachedResumeObserver implements Observer{
        public void update(Observable arg0, final Object arg1) {
        	org.gnu.glib.CustomEvents.addEvent(new Runnable(){
        		public void run() {
        			TextBuffer tb = logTextView.getBuffer();
        			tb.insertText("event "+(count++)+" : ");
        		    tb.insertText("PID " + ((Proc)arg1).getPid() +" on Host XXX resumed" + " \n");
        		 }
    		});
        }
    }

    
    class DetachedContinueObserver implements Observer{
        public void update(Observable arg0, final Object arg1) {
        	org.gnu.glib.CustomEvents.addEvent(new Runnable(){
        		public void run() {
        			TextBuffer tb = logTextView.getBuffer();
        			tb.insertText("event "+(count++)+" : ");
        		    tb.insertText("PID " + ((Proc)arg1).getPid() +" on Host XXX detached " + " \n");
        		 }
    		});
        }
    }
	
	public void save(Preferences prefs) {
		// TODO Auto-generated method stub
		
	}
	public void load(Preferences prefs) {
		// TODO Auto-generated method stub
		
	}
}
