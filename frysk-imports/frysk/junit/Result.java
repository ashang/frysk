// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

package frysk.junit;

/**
 * Possible results from running a test; see POSIX and dejagnu for
 * definition of states.
 */
class Result
{
    /** POSIX: PASS.  */
    static final Result PASS = new Result("PASS");
    /** POSIX: PASS (RESOLVED).  */
    static final Problem pass(int bug)
    {
	return new Problem(new String[] { "PASS", "UNRESOLVED" }, bug);
    }
    /** POSIX: FAIL.  */
    static final Problem fail (String what, Throwable t)
    {
	return new Problem(what, t);
    }
    /** POSIX: FAIL (RESOLVED).  */
    static final Problem fail (String what, Problem unresolved, Throwable t)
    {
	return new Problem (what, unresolved, t);
    }
    /** POSIX: UNRESOLVED.  */
    static Problem unresolved(int bug)
    {
	return new Problem (new String[] { "UNRESOLVED" }, bug);
    }
    /** POSIX: UNSUPPORTED.  */
    static final Problem unsupported (String why)
    {
	return new Problem ("UNSUPPORTED", why);
    }

    private final String what[];
    protected Result (String what)
    {
	this(new String[] { what });
    }
    protected Result (String[] what)
    {
	this.what = what;
    }
    protected Result (String what, Result unresolved)
    {
	this.what = new String[] {
	    what,
	    unresolved.what[unresolved.what.length - 1]
	};
    }
    public String toString()
    {
	StringBuffer buf = new StringBuffer(what[0]);
	for (int i = 1; i < what.length; i++)
	    buf.append(" (").append(what[i]);
	for (int i = 1; i < what.length; i++)
	    buf.append(")");
	return buf.toString();
    }
    String getWhat()
    {
	return what[what.length];
    }
    void println()
    {
	System.out.println (toString());
    }

    /**
     * A problem result.
     */
    static class Problem
	extends Result
    {
	private final String[] reasons;
	private Problem(String what, String reason)
	{
	    super(what);
	    reasons = new String[] { reason };
	}
	private Problem(String[] whats, int bug)
	{
	    super(whats);
	    reasons = new String[] { "http://sourceware.org/bugzilla/show_bug.cgi?id=" + bug };
	}
	private Problem(String what, Problem unresolved, Throwable t)
	{
	    super(what, unresolved);
	    this.reasons = new String[] { unresolved.getReason(), t.toString() };
	}
	private Problem (String what, Throwable t)
	{
	    this(what, t.toString());
	}
	void println()
	{
	    super.println();
	    for (int i = 0; i < reasons.length; i++) {
		System.out.print ("  ");
		System.out.println(reasons[i]);
	    }
	}
	String getReason()
	{
	    return reasons[0];
	}
	public boolean equals (Object o)
	{
	    if (o instanceof Problem)
		return ((Problem)o).getReason().equals (getReason());
	    return false;
	}
	public int hashCode()
	{
	    return getReason().hashCode();
	}
    }
}
