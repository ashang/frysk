// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, 2008, Red Hat Inc.
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

package frysk.hpd;

import java.util.SortedMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.LinkedList;
import java.util.Arrays;
import java.lang.Integer;
import java.util.Iterator;
import java.util.List;
import java.io.PrintWriter;

/**
 * Debugger variable accessible through the "set" command.
 */
public class DbgVariables {
    public static int VARTYPE_INT = 0;
    public static int VARTYPE_STRING = 1;
    public static int VARTYPE_CUSTOM = 2;

    static private class Value {
	private int type;
	private Object value;
	private Object deflt;
	private LinkedList validVals;

	public Value(int type, Object deflt) {
	    this.type = type;
	    this.value = deflt;
	    this.deflt = deflt;
	}

	public Value(int type, Object deflt, String[] validVals) {
	    this.type = type;
	    this.value = deflt;
	    this.deflt = deflt;
	    this.validVals = new LinkedList(Arrays.asList(validVals));
	}

	public int getType() {
	    return type;
	}

	public Object getValue() {
	    return value;
	}

	public Object getDefault() {
	    return deflt;
	}

	public void setValue(Object val) {
	    value = val;
	}

	public boolean valueValid(String val) {
	    return validVals.contains(val);
	}

	public String toString() {
	    return value.toString();
	}
    }

    private static final SortedMap vars = new TreeMap();

    public DbgVariables() {
	vars.put("MODE", new Value(VARTYPE_CUSTOM, "MULTILEVEL", new String[] {"THREADS", "PROCESSES", "MULTILEVEL"}));
	vars.put("START_MODEL", new Value(VARTYPE_CUSTOM, "ANY", new String[] {"ANY", "ALL"}));
	vars.put("STOP_MODEL", new Value(VARTYPE_CUSTOM, "ANY", new String[] {"ANY", "ALL"}));
	vars.put("EVENT_INTERRUPT", new Value(VARTYPE_CUSTOM, "ON", new String[] {"ON", "OFF"}));
	vars.put("VERBOSE", new Value(VARTYPE_CUSTOM, "WARN", new String[] {"WARN", "ERR", "ALL"}));
	vars.put("ERROR_CHECKS", new Value(VARTYPE_CUSTOM, "NORMAL", new String[] {"NORMAL", "MIN", "MAX"}));
	vars.put("MAX_PROMPT", new Value(VARTYPE_INT, new Integer(40)));
	vars.put("MAX_HISTORY", new Value(VARTYPE_INT, new Integer(20)));
	vars.put("MAX_LEVELS", new Value(VARTYPE_INT, new Integer(20)));
	vars.put("MAX_LIST", new Value(VARTYPE_INT, new Integer(20)));
	vars.put("PROMPT", new Value(VARTYPE_STRING, "(frysk) "));
	vars.put("SOURCE_PATH", new Value(VARTYPE_STRING, ""));
	vars.put("EXECUTABLE_PATH", new Value(VARTYPE_STRING, "./:" + System.getenv("PATH")));
    }

    public void setVariable(String var, String value) {
	Value tempval = (Value)vars.get(var);
	if (var.equals("MAX_PROMPT") || var.equals("MAX_HISTORY") ||
	    var.equals("MAX_LEVELS") || var.equals("MAX_LIST"))
	    tempval.setValue(Integer.getInteger(value));
	else 
	    tempval.setValue(value);
    }

    public void unsetVariable(String var) {
	Value tempval = (Value)vars.get(var);
	tempval.setValue(tempval.getDefault());
    }

    public void unsetAll() {
	Value tempval;
	for (Iterator iter = vars.values().iterator(); iter.hasNext();) {
	    tempval = (Value)iter.next();
	    tempval.setValue(tempval.getDefault());
	}
    }

    public boolean variableIsValid(String var) {
	return vars.containsKey(var);
    }

    public boolean valueIsValid(String var, String value) {
	boolean result = true;
	Value tempval = (Value) vars.get(var);
		
	if (tempval.getType() == VARTYPE_INT
	    && Integer.getInteger(value) == null)
	    result = false;
	else if (tempval.getType() == VARTYPE_CUSTOM
		 && !tempval.valueValid(value))
	    result = false;
	return result;
    }

    public int getIntValue(String var) {
	return ((Integer)vars.get(var)).intValue();
    }

    public String getStringValue(String var) {
      if (vars.size() == 0)
	return "";
      else
	return (String)((Value)vars.get(var)).value;
    }

    public Object getValue(String var) {
	return ((Value)vars.get(var)).getValue();
    }

    public String toString() {
	return vars.toString();
    }

    int complete(String incomplete, int base, List candidates) {
	for (Iterator i = vars.keySet().iterator(); i.hasNext(); ) {
	    String key = (String)i.next();
	    if (key.startsWith(incomplete))
		candidates.add(key);
	}
	CompletionFactory.padSingleCandidate(candidates);
	return 0;
    }

    public void print(PrintWriter out) {
	for (Iterator i = vars.entrySet().iterator(); i.hasNext(); ) {
	    Map.Entry e = (Map.Entry) i.next();
	    out.print(e.getKey());
	    out.print("=");
	    out.println(((Value) e.getValue()).getValue());
	}
    }
}
