// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

package frysk.ftrace;

import frysk.config.Config;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.testbed.*;

import java.util.*;
import java.util.regex.*;

/**
 * This is a test for basic ltrace capabilities.
 */
public class TestMappingGuard
    extends TestLib
{
    /* Note: also used in TestLtrace. */
    static class DummyMappingObserver implements MappingObserver {
	public Action updateMappedFile(Task task, MemoryMapping mapping) {
	    return Action.CONTINUE;
	}
	public Action updateUnmappedFile(Task task, MemoryMapping mapping){
	    return Action.CONTINUE;
	}
	public Action updateMappedPart(Task task, MemoryMapping mapping, MemoryMapping.Part part) {
	    return Action.CONTINUE;
	}
	public Action updateUnmappedPart(Task task, MemoryMapping mapping, MemoryMapping.Part part) {
	    return Action.CONTINUE;
	}

	public void addedTo (Object observable) {
	    Manager.eventLoop.requestStop();
	}
	public void deletedFrom (Object observable) { }
	public void addFailed (Object observable, Throwable w) {}
    }

    private void performTestAllLibrariesGetDetected() {
	class MyMappingObserver extends DummyMappingObserver {
	    public ArrayList allLibraries = new ArrayList();
	    public Action updateMappedFile(frysk.proc.Task task, MemoryMapping mapping) {
		ObjectFile objf = ObjectFile.buildFromFile(mapping.path);
		if (objf != null)
		    allLibraries.add(objf.getSoname());
		return super.updateMappedFile(task, mapping);
	    }
	}

	String[] cmd = {Config.getPkgLibFile("funit-empty").getPath()};
	DaemonBlockedAtEntry child = new DaemonBlockedAtEntry(cmd);
	Task task = child.getMainTask();
	
	MyMappingObserver observer = new MyMappingObserver();
	MappingGuard.requestAddMappingObserver(task, observer);
	assertRunUntilStop("add mapping observer");

	new StopEventLoopWhenProcRemoved(child);
	child.requestRemoveBlock();
	assertRunUntilStop("run child until exit");

	String[] expectedSonames = {"libc\\.so\\.6", "ld-linux.*\\.so\\.2", "funit-empty"};
	for (int i = 0; i < expectedSonames.length; ++i) {
	    boolean found = false;
	    for (Iterator it = observer.allLibraries.iterator(); it.hasNext(); ) {
		String soname = (String)it.next();
		if (Pattern.matches(expectedSonames[i], soname)) {
		    found = true;
		    break;
		}
	    }
	    assertTrue("library with pattern `" + expectedSonames[i] + "' found", found);
	}
	assertEquals("number of recorded libraries", expectedSonames.length, observer.allLibraries.size());
    }

    public void testDebugStateMappingGuard()
    {
	boolean save = MappingGuard.enableSyscallObserver;
	MappingGuard.enableSyscallObserver = false;
	assertTrue("debugstate observer enabled", MappingGuard.enableDebugstateObserver);
	performTestAllLibrariesGetDetected();
	MappingGuard.enableSyscallObserver = save;
    }

    public void testSyscallMappingGuard()
    {
	boolean save = MappingGuard.enableDebugstateObserver;
	MappingGuard.enableDebugstateObserver = false;
	assertTrue("syscall observer enabled", MappingGuard.enableSyscallObserver);
	performTestAllLibrariesGetDetected();
	MappingGuard.enableDebugstateObserver = save;
    }
}
