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

import inua.util.PrintWriter;
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
    private static PrintWriter out = new PrintWriter(System.out);
    static void set(PrintStream out) {
	Log.out = new PrintWriter(out);
    }

    private static final long startTime = System.currentTimeMillis();

    private void prefixTime() {
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
	out.print(days);
	out.print(' ');
	out.print(2, '0', hrs);
	out.print(':');
	out.print(2, '0', mins);
	out.print(':');
	out.print(2, '0', secs);
	out.print('.');
	out.print(3, '0', millis);
	out.print(' ');
    }

    private void prefix() {
	prefixTime();
	out.print(path);
	out.print(":");
    }

    private void prefix(Object o) {
	prefixTime();
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
	if (o instanceof char[]) {
	    dump((char[])o);
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
	out.print(' ');
	dump(o);
    }
    
    /**
     * Chars are printed in quotes.
     */
    private void print(char c) {
	out.print(' ');
	dump(c);
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
    private void print(int i) {
	out.print(' ');
	dump(i);
    }
    private void dump(int i) {
	out.print(i);
    }
    private void dump(int[] a) {
	out.print('[');
	for (int i = 0; i < a.length; i++) {
	    if (i > 0)
		out.print(',');
	    dump(i);
	}
	out.print(']');
    }

    /**
     * Longs are printed in hex.
     */
    private void print(long l) {
	out.print(' ');
	dump(l);
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

    // static 1 parameter
    public void log(String p1) {
	if (!logging)
	    return;
	prefix(); print(p1); suffix();
    }

    // static 2 parameters
    public void log(String p1, char p2) {
	if (!logging)
	    return;
	prefix(); print(p1); print(p2); suffix();
    }
    public void log(String p1, int p2) {
	if (!logging)
	    return;
	prefix(); print(p1); print(p2); suffix();
    }
    public void log(String p1, long p2) {
	if (!logging)
	    return;
	prefix(); print(p1); print(p2); suffix();
    }
    public void log(String p1, Object p2) {
	if (!logging)
	    return;
	prefix(); print(p1); print(p2); suffix();
    }
    public void log(String p1, String p2) {
	// Needed to disambiguate log(String,String) which could be
	// either log(Object,String) or log(String,Object).
	log(p1, (Object)p2);
    }

    // static 3 parameters
    public void log(String p1, Object p2, String p3) {
	if (!logging)
	    return;
	prefix(); print(p1); print(p2); print(p3); suffix();
    }

    // static 4 parameters
    public void log(String p1, int p2, String p3, Object p4) {
	if (!logging)
	    return;
	prefix(); print(p1); print(p2); print(p3); print(p4); suffix();
    }
    public void log(String p1, long p2, String p3, int p4) {
	if (!logging)
	    return;
	prefix(); print(p1); print(p2); print(p3); print(p4); suffix();
    }
    public void log(String p1, Object p2, String p3, long p4) {
	if (!logging)
	    return;
	prefix(); print(p1); print(p2); print(p3); print(p4); suffix();
    }
    public void log(String p1, Object p2, String p3, Object p4) {
	if (!logging)
	    return;
	prefix(); print(p1); print(p2); print(p3); print(p4); suffix();
    }

    // static 8 parameters
    public void log(String p1, Object p2, String p3, Object p4, String p5, Object p6, String p7, Object p8) {
	if (!logging)
	    return;
	prefix(); print(p1); print(p2); print(p3); print(p4); print(p5); print(p6); print(p7); print(p8); suffix();
    }
    public void log(String p1, int p2, String p3, Object p4, String p5, Object p6, String p7, Object p8) {
	if (!logging)
	    return;
	prefix(); print(p1); print(p2); print(p3); print(p4); print(p5); print(p6); print(p7); print(p8); suffix();
    }
    
    // Non-static log methods; first parameter is the object.

    // dynamic 1 parameter
    public void log(Object self, String p1) {
	if (!logging)
	    return;
	prefix(self); print(p1); suffix();
    }

    // dynamic 2 parameters
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
    public void log(Object self, String p1, Object p2) {
	if (!logging)
	    return;
	prefix(self); print(p1); print(p2); suffix();
    }

    // dynamic 3 parameters
    public void log(Object self, String p1, Object p2, String p3) {
	if (!logging)
	    return;
	prefix(self); print(p1); print(p2); print(p3); suffix();
    }

    // dynamic 4 parameters
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

    // dynamic 5 parameters
    public void log(Object self, String p1, Object p2, String p3, long p4, String p5) {
	if (!logging)
	    return;
	prefix(self); print(p1); print(p2); print(p3); print(p4); print(p5); suffix();
    }

    // dynamic 6 parameters
    public void log(Object self, String p1, Object p2, String p3, long p4, String p5, long p6) {
	if (!logging)
	    return;
	prefix(self); print(p1); print(p2); print(p3); print(p4); print(p5); print(p6); suffix();
    }
    public void log(Object self, String p1, Object p2, String p3, Object p4, String p5, Object p6) {
	if (!logging)
	    return;
	prefix(self); print(p1); print(p2); print(p3); print(p4); print(p5); print(p6); suffix();
    }

    // dynamic 9 parameters
    public void log(Object self, String p1, Object p2, String p3, long p4, String p5, int p6, String p7, int p8, String p9) {
	if (!logging)
	    return;
	prefix(self); print(p1); print(p2); print(p3); print(p4); print(p5); print(p6); print(p7); print(p8); print(p9); suffix();
    }
    public void log(Object self, String p1, Object p2, String p3, long p4, String p5, long p6, String p7, int p8, String p9) {
	if (!logging)
	    return;
	prefix(self); print(p1); print(p2); print(p3); print(p4); print(p5); print(p6); print(p7); print(p8); print(p9); suffix();
    }

    // dynamic 11 parameters
    public void log(Object self, String p1, Object p2, String p3, long p4, String p5, Object p6, String p7, int p8, String p9, int p10, String p11) {
	if (!logging)
	    return;
	prefix(self); print(p1); print(p2); print(p3); print(p4); print(p5); print(p6); print(p7); print(p8); print(p9); print(p10); print(p11); suffix();
    }

  /**
   * Convenience method to get the caller of a method in which you
   * use the Log object. Returns the caller (of the caller) of this
   * method as String or "<unknown>" if caller cannot be found or if
   * logger isn't logging. Use as:
   * <code>log.log(this, "method called by ", log.caller());</code>.
   */
  public String caller()
  {
    if (logging)
      {
	Throwable t = new Throwable();
	StackTraceElement[] stackTrace = t.getStackTrace();
	if (stackTrace.length > 2)
	  return stackTrace[2].toString();
      }

    return "<unknown>";
  }

  // Empty caller array for use in callersArray.
  static private final String[] empty = new String[0];

  // Private method that should only be directly called from
  // callers() or callers(int), which in turn should only be called
  // directly from the method that uses the Log and wants to find
  // its callers. Depends on actual caller being of depth 3.
  private String[] callersArray(int max)
  {
    if (logging)
      {
        Throwable t = new Throwable();
        StackTraceElement[] stackTrace = t.getStackTrace();
	int length = stackTrace.length > 3 ? stackTrace.length - 3 : 0;
	if (length > max)
	  length = max;
	String[] callers = new String[length];
        while (length > 0)
	  {
	    callers[length - 1]
	      = stackTrace[length + 2].toString();
	    length--;
	  }
	return callers;
      }

    return empty;
  }

  /**
   * Convenience method to get an array of callers of a method in
   * which you use the Log object. Returns the callers (of the caller)
   * of this method as a String[] or an empty array if the callers
   * cannot be found or if logger isn't logging. Use as:
   * <code>log.log(this, "method called by ", log.callers());</code>.
   * This is pretty heavyweight when the Log is enabled, so use
   * sparingly.
   */
  public String[] callers()
  {
    return callersArray(Integer.MAX_VALUE);
  }

  /**
   * Same as callers() but only returns at most max callers.
   */
  public String[] callers(int max)
  {
    return callersArray(max);
  }
}
