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

package frysk.rsl;

import frysk.sys.Tid;
import frysk.sys.Pid;
import inua.util.PrintWriter;
import java.lang.reflect.Array;

/**
 * Class for accumulating and then displaying log messages.
 */
public final class Printer {
    private final PrintWriter out;

    /**
     * Start with this single printer, will eventually need to make
     * things more complex with sub-classes.
     */
    Printer(PrintWriter out) {
	this.out = out;
    }

    private static final long startTime = System.currentTimeMillis();

    private void prefixTimeAndPid(Level level) {
	if (level.compareTo(Level.DEFAULT) <= 0) {
	    // Prefix user visible log messages with the severity; but
	    // leave it off debugging messages.
	    print(level.toPrint());
	    out.print(": ");
	}
	long time = System.currentTimeMillis() - startTime;
	long millis = time % 1000;
	time = time / 1000;
	long secs = time % 60;
	time = time / 60;
	long mins = time % 60;
	time = time / 60;
	long hrs = time % 24;
	time = time / 24;
	long days = time;
	if (days > 0) {
	    out.print(days);
	    out.print(' ');
	}
	out.print(2, '0', hrs);
	out.print(':');
	out.print(2, '0', mins);
	out.print(':');
	out.print(2, '0', secs);
	out.print('.');
	out.print(3, '0', millis);
	out.print(' ');
	out.print(Pid.get());
	out.print('.');
	out.print(Tid.get());
	out.print(' ');
    }

    public Printer prefix(Log logger) {
	prefixTimeAndPid(logger.level());
	out.print(logger.path());
	out.print(":");
	return this;
    }

    public Printer prefix(Log logger, Object o) {
	prefixTimeAndPid(logger.level());
	out.print("[");
	out.print(o.toString());
	out.print("]:");
	return this;
    }

    public void suffix() {
	out.println();
	out.flush();
    }
  
    /**
     * Use poorly implemented reflection to dump Objects.
     */
    public Printer print(Object o) {
	out.print(' ');
	dump(o);
	return this;
    }
    private void dump(Object o) {
	if (o == null) {
	    out.print("<<null>>");
	} else if (o instanceof char[]) {
	    dump((char[])o);
	} else if (o instanceof boolean[]) {
	    dump((boolean[])o);
	} else if (o instanceof int[]) {
	    dump((int[])o);
	} else if (o instanceof long[]) {
	    dump((long[])o);
	} else if (o.getClass().isArray()) {
	    out.print("[");
	    for (int i = 0; i < Array.getLength(o); i++) {
		if (i > 0)
		    out.print(",");
		dump(o, i);
	    }
	    out.print("]");
	} else if (o instanceof Throwable) {
	    dump((Throwable) o);
	} else if (o instanceof String) {
	    dump((String)o);
	} else {
	    out.print("<<");
	    out.print(o.toString());
	    out.print(">>");
	}
    }
    /**
     * Throwables get their message printed; along with any root
     * causes.
     */
    private void dump(Throwable t) {
	out.print("<<exception ");
	while (true) {
	    out.print(t.toString());
	    StackTraceElement[] stack = t.getStackTrace();
	    for (int i = 0; i < stack.length; i++) {
		if (i >= 1) {
		    // XXX: the cap is some what arbitrary
		    out.print(" ...");
		    break;
		} else {
		    out.print(" <at> ");
		    out.print(stack[i].toString());
		}
	    }
	    t = t.getCause();
	    if (t == null)
		break;
	    out.print(" <caused-by> ");
	}
	out.print(">>");
    }
    private void dump(String s) {
	out.print("\"");
	out.print(s
		.replaceAll("\"", "\\\\\"")
		.replaceAll("\'", "\\\\\'")
		.replaceAll("\r", "\\\\r")
		.replaceAll("\n", "\\\\n")
		.replaceAll("\t", "\\\\t")
		.replaceAll("\f", "\\\\f"));
	out.print("\"");
    }
    /**
     * Dump the array object's i'th element
     * @param o the array object
     * @param i the array index
     */
    private void dump(Object o, int i) {
	// for moment assume the array contains Objects; dump recursively.
	dump(Array.get(o, i));
    }
    
    /**
     * Booleans are printed as strings.
     */
    public Printer print(boolean b) {
	out.print(' ');
	dump(b);
	return this;
    }
    private void dump(boolean b) {
	out.print(b);
    }
    private void dump(boolean[] a) {
	out.print('[');
	for (int i = 0; i < a.length; i++) {
	    if (i > 0)
		out.print(',');
	    dump(a[i]);
	}
	out.print(']');
    }

    /**
     * Chars are printed in quotes.
     */
    public Printer print(char c) {
	out.print(' ');
	dump(c);
	return this;
    }
    private void dump(char c) {
	out.print('\'');
	out.print(c);
	out.print('\'');
    }
    private void dump(char[] a) {
	out.print('[');
	for (int i = 0; i < a.length; i++) {
	    if (i > 0)
		out.print(',');
	    dump(a[i]);
	}
	out.print(']');
    }

    /**
     * Integers are printed in decimal.
     */
    public Printer print(int i) {
	out.print(' ');
	dump(i);
	return this;
    }
    private void dump(int i) {
	out.print(i);
    }
    private void dump(int[] a) {
	out.print('[');
	for (int i = 0; i < a.length; i++) {
	    if (i > 0)
		out.print(',');
	    dump(a[i]);
	}
	out.print(']');
    }

    /**
     * Longs are printed in hex.
     */
    public Printer print(long l) {
	out.print(' ');
	dump(l);
	return this;
    }
    private void dump(long l) {
	out.print("0x");
	out.printx(l);

    }
    private void dump(long[] a) {
	out.print('[');
	for (int i = 0; i < a.length; i++) {
	    if (i > 0)
		out.print(',');
	    dump(a[i]);
	}
	out.print(']');
    }

    /**
     * Strings are just copied.
     */
    public Printer print(String s) {
	out.print(" ");
	out.print(s);
	return this;
    }
}
