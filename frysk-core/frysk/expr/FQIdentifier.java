// This file is part of the program FRYSK.
//
// Copyright 2008, Red Hat Inc.
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

package frysk.expr;

// Both necessary for soname expansion.  In principle this doesn't
// belong here, but it doesn't seem to be worth it to create separate
// class just for soname expansion capabilities.  Right now at least.
import frysk.dwfl.ObjectFile;
import frysk.proc.Task;

public class FQIdentifier {

    final public String soname;
    final public String file;
    final public Long line;
    final public String proc;
    final public String symbol;
    final public String version;
    final public boolean wantPlt;

    final private int metasoname;
    final private static int soname_null = -1;
    final private static int soname_name = 0;
    final private static int soname_MAIN = 2;
    final private static int soname_INTERP = 3;
    final private boolean sonameIsPath;

    public FQIdentifier(FQIdentToken tok) {
	this.soname = tok.dso;
	this.file = tok.file;
	this.proc = tok.proc;
	this.symbol = tok.symbol;
	this.version = tok.version;
	this.wantPlt = tok.wantPlt;

	if (tok.line != null)
	    this.line = new Long(Long.parseLong(tok.line, 10));
	else
	    this.line = null;

	if (soname == null) {
	    this.sonameIsPath = false;
	    this.metasoname = soname_null;
	} else {
	    this.sonameIsPath = soname.indexOf('/') != -1;
	    if (soname.equals("MAIN"))
		this.metasoname = soname_MAIN;
	    else if (soname.equals("INTERP"))
		this.metasoname = soname_INTERP;
	    else
		this.metasoname = soname_name;
	}
    }

    public String toString() {
	StringBuffer buf = new StringBuffer();
	if (soname != null)
	    buf.append('#').append(soname).append('#');
	if (file != null)
	    buf.append(file).append('#');
	if (line != null)
	    buf.append(line.longValue()).append('#');
	if (proc != null)
	    buf.append(proc).append('#');
	if (wantPlt)
	    buf.append("plt:");
	buf.append(symbol);
	if (version != null)
	    buf.append('@').append(version);
	return buf.toString();
    }

    /**
     * Translate MAIN and INTERP meta-sonames to main binary and
     * dynamic linker of given task.
     */
    public String expandSoname(Task task) {
	if (metasoname == soname_null)
	    return null;
	else if (metasoname == soname_name)
	    return soname;
	else {
	    // Don't cache so that the identifier is usable for
	    // multiple tasks.
	    String path = task.getProc().getExeFile().getSysRootedPath();
	    ObjectFile objf = ObjectFile.buildFromFile(path);
	    if (metasoname == soname_MAIN)
		return objf.getSoname();
	    else
		return ObjectFile.buildFromFile(objf.getInterp()).getSoname();
	}
    }

    /**
     * Check if given ObjectFile WHAT matches this soname identifier.
     */
    public boolean sonameMatches(Task task, ObjectFile what) {
	if (metasoname == soname_null)
	    return true;
	else if (sonameIsPath)
	    return soname.equals(what.getFilename().getPath());
	else
	    return this.expandSoname(task).equals(what.getSoname());
    }
}
