// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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

package frysk.util;

import java.io.*;
import jline.ConsoleReader;

import frysk.sys.FileDescriptor;


public class ObservingTerminal
    extends PtyTerminal {
    public class Observable extends java.util.Observable {
        private final ObservingTerminal terminal;
        Observable(ObservingTerminal terminal) {
            this.terminal = terminal;
        }
        public void setChanged() {
            super.setChanged();
        }
        
        public ObservingTerminal getTerminal() {
            return terminal;
        }
    }

    final Observable observable;
    
    boolean inputEntered = false;
    
    public boolean getInputEntered() {
	return inputEntered;
    }
    
    public ObservingTerminal(FileDescriptor fd) {
        super(fd);
        observable = new Observable(this);
    }

    public ObservingTerminal(File file) {
        super(file);
        observable = new Observable(this);
    }

    public ObservingTerminal(String string) {
        super(string);
        observable = new Observable(this);
    }
    
    public void beforeReadLine(ConsoleReader reader, String prompt,
                               Character mask) {
        inputEntered = false;
        observable.setChanged();
        observable.notifyObservers();
        super.beforeReadLine(reader, prompt, mask);
    }

    public int readVirtualKey(InputStream in)
        throws IOException {
        int result = super.readVirtualKey(in);
        inputEntered = true;
        observable.setChanged();
        observable.notifyObservers();
        return result;
    }

    
    public void afterReadLine(ConsoleReader reader, String prompt,
	    Character mask) {
	inputEntered = false;
	observable.setChanged();
	observable.notifyObservers();
	super.afterReadLine(reader, prompt, mask);
    }

    public Observable getObservable() {
        return observable;
    }
               
}
