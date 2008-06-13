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

    final public FQIdentPattern soname;
    final public FQIdentPattern file;
    final public FQIdentPattern proc;
    final public FQIdentPattern symbol;
    final public FQIdentPattern version;

    final public boolean wantPlt;

    final public Long line;
    final public Long processId;
    final public Long threadId;
    final public Long frameNumber;

    final private int metasoname;
    final private static int SONAME_NULL = -1;
    final private static int SONAME_NAME = 0;
    final private static int SONAME_MAIN = 2;
    final private static int SONAME_INTERP = 3;
    final private boolean sonameIsPath;


    private FQIdentPattern getPatternFor(FQIdentToken tok, String str) {
	if (str == null)
	    return FQIdentPatternAll.instance;
	else if (!tok.globs || !FQIdentParser.isWildcardPattern(str))
	    return new FQIdentPatternExact(str);
	else
	    return new FQIdentPatternGlob(str);
    }

    public FQIdentifier(FQIdentToken tok) {
	this.soname = getPatternFor(tok, tok.dso);
	this.file = getPatternFor(tok, tok.file);
	this.proc = getPatternFor(tok, tok.proc);
	this.symbol = getPatternFor(tok, tok.symbol);
	this.version = getPatternFor(tok, tok.version);
	this.wantPlt = tok.wantPlt;

	if (tok.processId != null) {
	    if (tok.threadId == null || tok.frameNumber == null)
		throw new AssertionError("Either I need a pid, a tid, AND a " +
					 "frame number, or neither of them.");
	    this.processId = new Long(Long.parseLong(tok.processId, 10));
	    this.threadId = new Long(Long.parseLong(tok.threadId, 10));
	    this.frameNumber = new Long(Long.parseLong(tok.frameNumber, 10));
	} else
	    this.processId = this.threadId = this.frameNumber = null;

	if (tok.line != null)
	    this.line = new Long(Long.parseLong(tok.line, 10));
	else
	    this.line = null;

	if (tok.dso == null) {
	    this.sonameIsPath = false;
	    this.metasoname = SONAME_NULL;
	} else {
	    this.sonameIsPath = tok.dso.indexOf('/') != -1;
	    if (tok.dso.equals("MAIN"))
		this.metasoname = SONAME_MAIN;
	    else if (tok.dso.equals("INTERP"))
		this.metasoname = SONAME_INTERP;
	    else
		this.metasoname = SONAME_NAME;
	}
    }

    public String toString() {
	StringBuffer buf = new StringBuffer();
	if (processId != null)
	    buf.append('[').append(processId).append('.')
	       .append(threadId).append('#').append(frameNumber)
	       .append(']');
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
     * Check if given ObjectFile WHAT matches this soname identifier.
     */
    public boolean sonameMatches(Task task, ObjectFile what) {
	if (metasoname == SONAME_NULL
	    || soname.cardinality() == FQIdentPattern.CARD_ALL)
	    return true;

	else if (sonameIsPath)
	    return soname.matches(what.getFilename().getPath());

	else if (metasoname == SONAME_NAME)
	    return soname.matches(what.getSoname());

	else {
	    // Don't cache so that the identifier is usable for
	    // multiple tasks.
	    String path = task.getProc().getExeFile().getSysRootedPath();
	    ObjectFile objf = ObjectFile.buildFromFile(path);
	    if (metasoname == SONAME_MAIN)
		return objf.getSoname().equals(what.getSoname());
	    else
		return ObjectFile.buildFromFile(objf.getInterp())
		    .getSoname().equals(what.getSoname());
	}
    }

    /**
     * Whether this identifier is plain, i.e. has no qualification.
     * PLT references are not considered plain symbols.
     */
    public boolean isPlain() {
	return soname.cardinality() == FQIdentPattern.CARD_ALL
	    && file.cardinality() == FQIdentPattern.CARD_ALL
	    && line == null
	    && proc.cardinality() == FQIdentPattern.CARD_ALL
	    && symbol.cardinality() == FQIdentPattern.CARD_ALL
	    && version.cardinality() == FQIdentPattern.CARD_ALL
	    && wantPlt == false
	    && processId == null
	    && threadId == null
	    && frameNumber == null;
    }
}
