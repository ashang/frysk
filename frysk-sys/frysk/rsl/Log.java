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

package frysk.rsl;

import java.io.PrintStream;
import java.util.List;

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
	this.logging = this.level.compareTo(level) >= 0;
	return this;
    }

    /**
     * Return if this logger is currently enabled for logging.
     */
    public boolean logging() {
	return logging;
    }

    public static Branch get(String klass) {
	return Branch.root.get(klass);
    }
    public static Log get(String klass, Level level) {
	return Branch.root.get(klass, level);
    }
    public static Log fine(String klass) {
	return get(klass, Level.FINE);
    }

    public static Branch get(Class klass) {
	return get(klass.getName());
    }
    public static Log get(Class klass, Level level) {
	return get(klass.getName(), level);
    }
    public static Log fine(Class klass) {
	return fine(klass.getName());
    }

    public static int complete(String incomplete, List candidates) {
	return Branch.root.complete(incomplete, candidates);
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
	out.print(".");
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

    private void postfix() {
	out.println();
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
     * Longs are always printed in hex.
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
    private void print(String[] a) {
	out.print(" [");
	for (int i = 0; i < a.length; i++) {
	    if (i > 0)
		out.print(",");
	    out.print(a[i]);
	}
	out.print("]");
    }
    /**
     * Objects are wrapped in "[" and "]".
     */
    private void print(Object o) {
	out.print(" <<");
	out.print(o.toString());
	out.print(">>");
    }
    private void print(Object[] a) {
	out.print(" [");
	for (int i = 0; i < a.length; i++) {
	    if (i > 0)
		out.print(",");
	    out.print("<<");
	    out.print(a[i].toString());
	    out.print(">>");
	}
	out.print("]");
    }

    // Add at will and on demand.
    public void log(String p1) {
	if (!logging)
	    return;
	prefix();
	print(p1);
	postfix();
    }

    // Add at will and on demand.
    public void log(Object self, String p1, int p2) {
	if (!logging)
	    return;
	prefix(self);
	print(p1);
	print(p2);
	postfix();
    }

    // Add at will and on demand.
    public void log(Object self, String p1, long p2) {
	if (!logging)
	    return;
	prefix(self);
	print(p1);
	print(p2);
	postfix();
    }

    // Add at will and on demand.
    public void log(Object self, String p1, String p2) {
	if (!logging)
	    return;
	prefix(self);
	print(p1);
	print(p2);
	postfix();
    }

    // Add at will and on demand.
    public void log(Object self, String p1, Object p2) {
	if (!logging)
	    return;
	prefix(self);
	print(p1);
	print(p2);
	postfix();
    }

    // Add at will and on demand.
    public void log(Object self, String p1, int[] p2) {
	if (!logging)
	    return;
	prefix(self);
	print(p1);
	print(p2);
	postfix();
    }

    // Add at will and on demand.
    public void log(Object self, String p1, long[] p2) {
	if (!logging)
	    return;
	prefix(self);
	print(p1);
	print(p2);
	postfix();
    }

    // Add at will and on demand.
    public void log(Object self, String p1, String[] p2) {
	if (!logging)
	    return;
	prefix(self);
	print(p1);
	print(p2);
	postfix();
    }

    // Add at will and on demand.
    public void log(Object self, String p1, Object[] p2) {
	if (!logging)
	    return;
	prefix(self);
	print(p1);
	print(p2);
	postfix();
    }
}
