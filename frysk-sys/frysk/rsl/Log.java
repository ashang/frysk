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

import java.io.PrintStream;
import java.text.MessageFormat;
import java.lang.reflect.Array;

/**
 * Generate log information when enabled.
 */
public final class Log {

    private final String path;
    private final String name;
    private final Level level;
    private boolean logging;
    Log(String path, String name, Level level) {
	this.path = path;
	this.name = name;
	this.level = level;
	this.logging = false;
    }

    public String toString() {
	return ("{" + super.toString()
		+ ",path=" + path
		+ ",level=" + level
		+ ",logging=" + logging
		+ "}");
    }

    /**
     * Return the "basename" of the logger.
     */
    public String name() {
	return name;
    }
    /**
     * Return the full path of the logger.
     */
    public String path() {
	return path;
    }
    /**
     * The level at which this logger starts logging.
     */
    public Level level() {
	return level;
    }
    /**
     * Enable logging; package private.
     */
    Log set(Level level) {
	this.logging = level.compareTo(this.level) >= 0;
	return this;
    }

    /**
     * Return if this logger is currently enabled for logging.
     */
    public boolean logging() {
	return logging;
    }

    /**
     * For convenience, grab the FINE logger.
     */
    public static Log fine(Class klass) {
	return LogFactory.fine(klass);
    }
    /**
     * For convenience, grab the FINEST logger.
     */
    public static Log finest(Class klass) {
	return LogFactory.finest(klass);
    }

    // Static?
    private static PrintStream out = System.out;
    static void set(PrintStream out) {
	Log.out = out;
    }

    private static final long startTime = System.currentTimeMillis();

    private void prePrefix() {
	long time = System.currentTimeMillis() - startTime;
	out.print(time / 1000);
	out.print(".");
	out.print(time % 1000);
	out.print(": ");
	out.print(path);
	out.print(":");
	out.print(level.toPrint());
    }

    private void prefix() {
	prePrefix();
	out.print(":");
    }

    private void prefix(Object o) {
	prePrefix();
	out.print(" [");
	out.print(o.toString());
	out.print("]:");
    }

    private void suffix() {
	out.println();
	out.flush();
    }
  
    /**
     * Throwables get their message printed; along with any root
     * causes.
     */
    private void dump(Throwable t) {
	out.print("<<exception");
	Throwable cause = t;
	do {
	    out.print(":");
	    out.print(t.getMessage());
	    cause = cause.getCause();
	} while (cause != null);
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
     * Dump an arbitrary object.
     * @param o the object to dump
     */
    private void dump(Object o) {
	if (o.getClass().isArray()) {
	    out.print("[");
	    for (int i = 0; i < Array.getLength(o); i++) {
		if (i > 0)
		    out.print(",");
		dump(o, i);
	    }
	    out.print("]");
	} else if (o instanceof Throwable)
	    dump((Throwable) o);
	else if (o instanceof String)
	    dump((String)o);
	else {
	    out.print("<<");
	    out.print(o.toString());
	    out.print(">>");
	}
    }
    /**
     * Use poorly implemented reflection to dump Objects.
     */
    private void print(Object o) {
	out.print(" ");
	dump(o);
    }
    
    /**
     * Chars are printed in quotes.
     */
    private void print(char c) {
	out.print(" '");
	out.print(c);
	out.print("'");
    }
    private void print(char[] a) {
	out.print(" {");
	for (int i = 0; i < a.length; i++) {
	    if (i > 0)
		out.print(',');
	    out.print('\'');
	    out.print(a[i]);
	    out.print('\'');
	}
	out.print('}');
    }

    /**
     * Integers are printed in decimal.
     */
    private void print(int i) {
	out.print(" ");
	out.print(i);
    }
    private void print(int[] a) {
	out.print(" [");
	for (int i = 0; i < a.length; i++) {
	    if (i > 0)
		out.print(",");
	    out.print(a[i]);
	}
	out.print("]");
    }

    /**
     * Longs are printed in hex.
     */
    private void print(long l) {
	out.print(" 0x");
	out.print(Long.toHexString(l));
    }
    private void print(long[] a) {
	out.print(" [");
	for (int i = 0; i < a.length; i++) {
	    if (i > 0)
		out.print(",");
	    out.print("0x");
	    out.print(Long.toHexString(a[i]));
	}
	out.print("]");
    }
    /**
     * Strings are just copied.
     */
    private void print(String s) {
	out.print(" ");
	out.print(s);
    }
    
    /**
     * For compatibility with existing loggers.
     */
    public void format(String msg, Object[] o) {
	if (!logging)
	    return;
	prefix();
	print(MessageFormat.format(msg, o));
	suffix();
    }
    /**
     * For compatibility with existing loggers.
     */
    public void format(String msg, Object o) {
	if (!logging)
	    return;
	prefix();
	print(MessageFormat.format(msg, new Object[] { o }));
	suffix();
    }

    // Static log methods.
    public void log(String p1) {
	if (!logging)
	    return;
	prefix(); print(p1); suffix();
    }
    public void log(String p1, char p2) {
	if (!logging)
	    return;
	prefix(); print(p1); print(p2); suffix();
    }
    public void log(String p1, char[] p2) {
	if (!logging)
	    return;
	prefix(); print(p1); print(p2); suffix();
    }
    public void log(String p1, String p2) {
	if (!logging)
	    return;
	prefix(); print(p1); print(p2); suffix();
    }
    public void log(String p1, Object p2) {
	if (!logging)
	    return;
	prefix(); print(p1); print(p2); suffix();
    }
    public void log(String p1, Object p2, String p3) {
	if (!logging)
	    return;
	prefix(); print(p1); print(p2); print(p3); suffix();
    }
    public void log(String p1, Object p2, String p3, Object p4) {
	if (!logging)
	    return;
	prefix(); print(p1); print(p2); print(p3); print(p4); suffix();
    }
    public void log(String p1, long p2, String p3, int p4) {
	if (!logging)
	    return;
	prefix(); print(p1); print(p2); print(p3); print(p4); suffix();
    }
    
    // Non-static log methods; first parameter is the object.
    public void log(Object self, String p1) {
	if (!logging)
	    return;
	prefix(self); print(p1); suffix();
    }
    public void log(Object self, String p1, int p2) {
	if (!logging)
	    return;
	prefix(self); print(p1); print(p2); suffix();
    }
    public void log(Object self, String p1, long p2) {
	if (!logging)
	    return;
	prefix(self); print(p1); print(p2); suffix();
    }
    public void log(Object self, String p1, String p2) {
	if (!logging)
	    return;
	prefix(self); print(p1); print(p2); suffix();
    }
    public void log(Object self, String p1, Object p2) {
	if (!logging)
	    return;
	prefix(self); print(p1); print(p2); suffix();
    }
    public void log(Object self, String p1, int[] p2) {
	if (!logging)
	    return;
	prefix(self); print(p1); print(p2); suffix();
    }
    public void log(Object self, String p1, long[] p2) {
	if (!logging)
	    return;
	prefix(self); print(p1); print(p2); suffix();
    }
    public void log(Object self, String p1, int p2, String p3, char p4) {
	if (!logging)
	    return;
	prefix(self); print(p1); print(p2); print(p3); print(p4); suffix();
    }
    public void log(Object self, String p1, Object p2, String p3, Object p4) {
	if (!logging)
	    return;
	prefix(self); print(p1); print(p2); print(p3); print(p4); suffix();
    }
    public void log(Object self, String p1, Object p2, String p3, int p4) {
	if (!logging)
	    return;
	prefix(self); print(p1); print(p2); print(p3); print(p4); suffix();
    }
    public void log(Object self, String p1, Object p2, String p3, Object p4, String p5, Object p6) {
	if (!logging)
	    return;
	prefix(self); print(p1); print(p2); print(p3); print(p4); print(p5); print(p6); suffix();
    }
}
