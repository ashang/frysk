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

import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import frysk.proc.Proc;
import frysk.proc.TaskEvent;


/**
 * @author pmuldoon
 *
 */
public class EventLogger implements Observer {

        private static final String FRYSK_CONFIG = System.getProperty("user.home")
        + "/" + ".frysk" + "/";
        public static final String EVENT_LOG_ID = "frysk.gui.monitor.eventlog";
        private Logger eventLogFile = null;


        class EventFileHandler extends FileHandler {

        	public EventFileHandler(String arg0, boolean arg1) throws IOException, SecurityException {
        		super(arg0, arg1);
        		// TODO Auto-generated constructor stub
        	}
        	public synchronized void publish(LogRecord arg) {
        		
        		// As this is going to log exceptions, and as frysk might be kill -9'd
        		// I've not found a way to let normal FileHandlers do an explicit flush
        		// after each log event. So we found logfiles that were incomplete.
        		// So will cause and explicit flush after each publish, until we figure out
        		// otherwise.
        		
        		super.publish(arg);
        		super.flush();
        	}


        }

        /**{
         * Local Observers
         * */
        public AttachedContinueObserver attachedContinueObserver;
        public DetachedContinueObserver detachedContinueObserver;
        public AttachedStopObserver attachedStopObserver;
        public AttachedResumeObserver attachedResumeObserver;
        public EventTaskExecObserver eventTaskExecObserver; 
        public EventTaskExitingObserver eventTaskExitingObserver;
        /** }*/
        
        public EventLogger()
        {
                this.attachedContinueObserver = new AttachedContinueObserver();
                this.detachedContinueObserver = new DetachedContinueObserver();
                this.attachedStopObserver = new AttachedStopObserver();
                this.attachedResumeObserver = new AttachedResumeObserver();
                this.eventTaskExecObserver = new EventTaskExecObserver();
                this.eventTaskExitingObserver = new EventTaskExitingObserver();
                
                eventLogFile = Logger.getLogger(EVENT_LOG_ID);
                eventLogFile.addHandler(buildHandler());
        }
        
        
        private FileHandler buildHandler() {
                FileHandler handler = null;
                File log_dir = new File(FRYSK_CONFIG + "eventlogs" + "/");

                if (!log_dir.exists())
                        log_dir.mkdirs();

                try {

                        handler = new EventFileHandler(log_dir.getAbsolutePath()
                                        + "/" + "frysk_event_log.log", true);
                } catch (Exception e) {
                        e.printStackTrace();
                }
                
                handler.setFormatter(new EventFormatter());
                return handler;
        }
        
        public void update(Observable observable, Object arg1) {
                // this function should not be used anymore
   

        }
        
        
        // Attach/Detach/Stop/Resume Observers
        
        class AttachedContinueObserver implements Observer{
                public void update(Observable arg0, Object arg1) {
                        eventLogFile.log(Level.INFO,"PID " + ((Proc)arg1).getPid() +" Host XXX Attached ");
                }
        }
        
        class DetachedContinueObserver implements Observer{
                public void update(Observable arg0, Object arg1) {
                        eventLogFile.log(Level.INFO,"PID " + ((Proc)arg1).getPid() +" Host XXX Detached ");
                }
        }
        
        class AttachedStopObserver implements Observer{
                public void update(Observable arg0, Object arg1) {
                    eventLogFile.log(Level.INFO,"PID " + ((Proc)arg1).getPid() +" Host XXX Stopped");
            }
        }
        
        class AttachedResumeObserver implements Observer{
            public void update(Observable arg0, Object arg1) {
                eventLogFile.log(Level.INFO,"PID " + ((Proc)arg1).getPid() +" Host XXX Resumed");
            }
        }
            
        // Proc action observers
            
        class EventTaskExecObserver implements Observer {
            public void update(Observable arg0, Object arg1) {
                eventLogFile.log(Level.INFO,"PID " + ((TaskEvent)arg1).getTask().getPid() +" Host XXX Execed");
            }
        }
            
        class EventTaskExitingObserver implements Observer {
            public void update(Observable arg0, Object arg1) {
                eventLogFile.log(Level.INFO,"PID " + ((TaskEvent)arg1).getTask().getPid() +" Host XXX Exiting");
            }
        }
}
        

