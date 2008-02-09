// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

package frysk.testbed;

import frysk.rsl.Log;
import java.io.File;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * A class for managing and manipulating a temporary file.
 *
 * Creates a temporary file that is automatically removed on test
 * completion.. Unlike File, this doesn't hold onto the underlying
 * file once created.  Consequently, operations such as "exists()"
 * reflect the current state of the file and not some past state.
 */

public class TearDownFile extends File {
    private static final Log fine = Log.fine(TearDownFile.class);
    static final long serialVersionUID = 1;

    /**
     * Create a temporary File using the current directory.
     */
    public static TearDownFile create () {
	try {
	    File file;
	    String name = TearDownFile.class.getName();
	    File pwd = new File(".");
	    file = File.createTempFile(name + ".", ".tmp", pwd);
	    return new TearDownFile(file.getPath ());
	} catch (java.io.IOException e) {
	    throw new RuntimeException(e);
	}
    }

    /**
     * Return TRUE if-and-only-if the file, at the moment of this
     * call, still exits.  This is different to File.exists() as that
     * test caches its results.
     */
    public boolean stillExists() {
	return new File(getPath()).exists();
    }

    /**
     * Construct a TearDownFile object with the specified name.
     */
    public TearDownFile(String name) {
	super (name);
	tmpFiles.add(this);
	fine.log(this, "new");
    }

    private static final List tmpFiles = new LinkedList();
    /**
     * TearDown all TearDownFile-s created since this method was last
     * called.
     */
    public static void tearDown() {
	for (Iterator i = tmpFiles.iterator(); i.hasNext();) {
	    TearDownFile tbd = (TearDownFile) i.next();
	    i.remove();
	    try {
		tbd.delete();
		fine.log("tearDown", tbd);
	    } catch (Exception e) {
		fine.log("tearDown", tbd, "failed", e);
	    }
	}
    }
}
