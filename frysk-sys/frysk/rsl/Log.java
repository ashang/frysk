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
     * For convenience, since this is the most common case, grab the
     * FINE logger.  For other loggers use LogFactory.
     */
    public static Log fine(Class klass) {
	return LogFactory.fine(klass);
    }

    // Static?
    private static Printer out = new Printer(new PrintWriter(System.out));
    static Printer set(Printer out) {
	Printer old = Log.out;
	Log.out = out;
	return old;
    }
    static Printer set(PrintStream out) {
	return set(new Printer(new PrintWriter(out)));
    }
    static Printer set(PrintWriter out) {
	return set(new Printer(out));
    }
    public Printer prefix() {
	return out.prefix(this);
    }
    public Printer prefix(Object self) {
	return out.prefix(this, self);
    }

    // static 1 parameter
    public void log(String p1) {
	if (!logging)
	    return;
	prefix().print(p1).suffix();
    }

    // static 2 parameters
    public void log(String p1, boolean p2) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).suffix();
    }
    public void log(String p1, char p2) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).suffix();
    }
    public void log(String p1, int p2) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).suffix();
    }
    public void log(String p1, long p2) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).suffix();
    }
    public void log(String p1, Object p2) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).suffix();
    }
    // Disambiguate log(String,String) which could be either
    // log(Object,String) or log(String,Object).
    public void log(String p1, String p2) {
	log(p1, (Object)p2);
    }

    // static 3 parameters
    public void log(String p1, int p2, String p3) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).print(p3).suffix();
    }
    public void log(String p1, Object p2, String p3) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).print(p3).suffix();
    }

    // static 4 parameters
    public void log(String p1, int p2, String p3, Object p4) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).print(p3).print(p4).suffix();
    }
    public void log(String p1, long p2, String p3, int p4) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).print(p3).print(p4).suffix();
    }
    public void log(String p1, long p2, String p3, long p4) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).print(p3).print(p4).suffix();
    }
    public void log(String p1, Object p2, String p3, long p4) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).print(p3).print(p4).suffix();
    }
    public void log(String p1, Object p2, String p3, int p4) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).print(p3).print(p4).suffix();
    }
    public void log(String p1, Object p2, String p3, Object p4) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).print(p3).print(p4).suffix();
    }
    // Disambiguate log(String,String,String,String).
    public void log(String p1, String p2, String p3, String p4) {
	log(p1, (Object)p2, p3, (Object)p4);
    }

    // static 6 parameters
    public void log(String p1, int p2, String p3, Object p4, String p5, int p6) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).print(p3).print(p4).print(p5).print(p6).suffix();
    }
    public void log(String p1, Object p2, String p3, Object p4, String p5, Object p6) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).print(p3).print(p4).print(p5).print(p6).suffix();
    }
    public void log(String p1, Object p2, String p3, Object p4, String p5, int p6) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).print(p3).print(p4).print(p5).print(p6).suffix();
    }
    public void log(String p1, Object p2, String p3, Object p4, String p5, long p6) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).print(p3).print(p4).print(p5).print(p6).suffix();
    }
    public void log(String p1, Object p2, String p3, long p4, String p5, Object p6) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).print(p3).print(p4).print(p5).print(p6).suffix();
    }
    public void log(String p1, Object p2, String p3, long p4, String p5, long p6) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).print(p3).print(p4).print(p5).print(p6).suffix();
    }
    public void log(String p1, long p2, String p3, long p4, String p5, Object p6) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).print(p3).print(p4).print(p5).print(p6).suffix();
    }

    // static 8 parameters
    public void log(String p1, int p2, String p3, Object p4, String p5, Object p6, String p7, int p8) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).print(p3).print(p4).print(p5).print(p6).print(p7).print(p8).suffix();
    }
    public void log(String p1, long p2, String p3, long p4, String p5, long p6, String p7, Object p8) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).print(p3).print(p4).print(p5).print(p6).print(p7).print(p8).suffix();
    }
    public void log(String p1, Object p2, String p3, Object p4, String p5, Object p6, String p7, Object p8) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).print(p3).print(p4).print(p5).print(p6).print(p7).print(p8).suffix();
    }
    public void log(String p1, int p2, String p3, Object p4, String p5, Object p6, String p7, Object p8) {
	if (!logging)
	    return;
	prefix().print(p1).print(p2).print(p3).print(p4).print(p5).print(p6).print(p7).print(p8).suffix();
    }

    // Non-static log methods; first parameter is the object.

    // dynamic 1 parameter
    public void log(Object self, String p1) {
	if (!logging)
	    return;
	prefix(self).print(p1).suffix();
    }

    // dynamic 2 parameters
    public void log(Object self, String p1, boolean p2) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).suffix();
    }
    public void log(Object self, String p1, int p2) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).suffix();
    }
    public void log(Object self, String p1, long p2) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).suffix();
    }
    public void log(Object self, String p1, Object p2) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).suffix();
    }

    // dynamic 3 parameters
    public void log(Object self, String p1, Object p2, String p3) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).print(p3).suffix();
    }

    // dynamic 4 parameters
    public void log(Object self, String p1, long p2, String p3, boolean p4) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).print(p3).print(p4).suffix();
    }
    public void log(Object self, String p1, Object p2, String p3, boolean p4) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).print(p3).print(p4).suffix();
    }
    public void log(Object self, String p1, Object p2, String p3, long p4) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).print(p3).print(p4).suffix();
    }
    public void log(Object self, String p1, long p2, String p3, long p4) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).print(p3).print(p4).suffix();
    }
    public void log(Object self, String p1, boolean p2, String p3, int p4) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).print(p3).print(p4).suffix();
    }
    public void log(Object self, String p1, int p2, String p3, char p4) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).print(p3).print(p4).suffix();
    }
    public void log(Object self, String p1, Object p2, String p3, Object p4) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).print(p3).print(p4).suffix();
    }
    public void log(Object self, String p1, Object p2, String p3, int p4) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).print(p3).print(p4).suffix();
    }

    // dynamic 5 parameters
    public void log(Object self, String p1, Object p2, String p3, long p4, String p5) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).print(p3).print(p4).print(p5).suffix();
    }

    // dynamic 6 parameters
    public void log(Object self, String p1, Object p2, String p3, Object p4, String p5, long p6) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).print(p3).print(p4).print(p5).print(p6).suffix();
    }
    public void log(Object self, String p1, Object p2, String p3, long p4, String p5, long p6) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).print(p3).print(p4).print(p5).print(p6).suffix();
    }
    public void log(Object self, String p1, Object p2, String p3, Object p4, String p5, int p6) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).print(p3).print(p4).print(p5).print(p6).suffix();
    }
    public void log(Object self, String p1, Object p2, String p3, Object p4, String p5, Object p6) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).print(p3).print(p4).print(p5).print(p6).suffix();
    }

    // dynamic 9 parameters
    public void log(Object self, String p1, Object p2, String p3, long p4, String p5, int p6, String p7, int p8, String p9) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).print(p3).print(p4).print(p5).print(p6).print(p7).print(p8).print(p9).suffix();
    }
    public void log(Object self, String p1, Object p2, String p3, long p4, String p5, long p6, String p7, int p8, String p9) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).print(p3).print(p4).print(p5).print(p6).print(p7).print(p8).print(p9).suffix();
    }

    // dynamic 11 parameters
    public void log(Object self, String p1, Object p2, String p3, long p4, String p5, Object p6, String p7, int p8, String p9, int p10, String p11) {
	if (!logging)
	    return;
	prefix(self).print(p1).print(p2).print(p3).print(p4).print(p5).print(p6).print(p7).print(p8).print(p9).print(p10).print(p11).suffix();
    }


    /**
     * Assuming the use: <tt>log("caller", log.CALLER)</tt> prints the
     * caller of the logging fuction.
     */
    public static final Callers CALLER = new Callers(4, 4);

    /**
     * Assuming the use: <tt>log("caller", log.CALLER)</tt> a list of
     * callers.
     */
    public static final Callers CALLERS = new Callers(4, Integer.MAX_VALUE);
}
