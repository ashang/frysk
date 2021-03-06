#!/usr/bin/python

# This file is part of the program FRYSK.
#
# Copyright 2007, 2008, Red Hat Inc.
#
# FRYSK is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License as published by
# the Free Software Foundation; version 2 of the License.
#
# FRYSK is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with FRYSK; if not, write to the Free Software Foundation,
# Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
# 
# In addition, as a special exception, Red Hat, Inc. gives You the
# additional right to link the code of FRYSK with code not covered
# under the GNU General Public License ("Non-GPL Code") and to
# distribute linked combinations including the two, subject to the
# limitations in this paragraph. Non-GPL Code permitted under this
# exception must only link to the code of FRYSK through those well
# defined interfaces identified in the file named EXCEPTION found in
# the source code files (the "Approved Interfaces"). The files of
# Non-GPL Code may instantiate templates or use macros or inline
# functions from the Approved Interfaces without causing the
# resulting work to be covered by the GNU General Public
# License. Only Red Hat, Inc. may make changes or additions to the
# list of Approved Interfaces. You must obey the GNU General Public
# License in all respects for all of the FRYSK code and other code
# used in conjunction with FRYSK except the Non-GPL Code covered by
# this exception. If you modify this file, you may extend this
# exception to your version of the file, but you are not obligated to
# do so. If you do not wish to provide this exception without
# modification, you must delete this exception statement from your
# version and license this file solely under the GPL without
# exception.

import os,sys

########################################################################
# Manage creation of the java file
########################################################################

class j:
    def open(self):
        self.name = "TestTypeFactory"
    def write(self,str):
        print str
    def prologue(self,):
        print('''// Generated by gen-type-expect-tests.py

package frysk.debuginfo;

import java.io.*;
import lib.dwfl.DwarfDie;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflDie;
import frysk.dwfl.DwflCache;
import frysk.proc.Task;
import frysk.testbed.DaemonBlockedAtSignal;
import frysk.testbed.TestLib;
import frysk.value.Type;
import frysk.value.Value;
import frysk.value.Format;
import frysk.debuginfo.DebugInfo;
''')
            
        print('''
public class %s extends TestLib {
    private class TypeTestbed {
	DebugInfoFrame frame;
        Task task;
	DwflDie biasDie;
	DwarfDie[] allDies;
	TypeFactory typeFactory;
	String testName;

	TypeTestbed(String executable, String testName) {
	    task = new DaemonBlockedAtSignal(executable).getMainTask();
            frame = DebugInfoStackFactory.createDebugInfoStackTrace(task);
	    long pc = frame.getAdjustedAddress();
	    Dwfl dwfl = DwflCache.getDwfl(frame.getTask());
            biasDie = dwfl.getCompilationUnit(pc);
	    allDies = biasDie.getScopes(pc);
	    typeFactory = new TypeFactory(frame.getTask().getISA());
	    this.testName = testName;
	}
''' % (self.name))
	
        print('''
	void checkType(String symbol, String expected) {
	    Type varType;
	    DwarfDie varDie = biasDie.getScopeVar(allDies, symbol);
            assertNotNull("die for variable " + symbol, varDie);
	    varType = typeFactory.getType(varDie.getType());
	    assertNotNull(varType);
	    assertEquals(testName + symbol, expected, varType.toPrint());
	}

        void checkValue(String symbol, String expected) {
            // "Print" to a byte array
	    ByteArrayOutputStream baos = new ByteArrayOutputStream(32);
	    PrintWriter pw = new PrintWriter(baos, true);
	    // ??? cache address of x so &x can be checked?
	    if (expected.indexOf("&") >= 0 || symbol.indexOf("ptr") >= 0
		|| expected.length() == 0)
		return;
	    DwarfDie varDie = biasDie.getScopeVar(allDies, symbol);
            assertNotNull("die for variable " + symbol, varDie);
	    DebugInfo debugInfo = new DebugInfo(frame);
	    Value value =  debugInfo.print(symbol, frame);
	    value.toPrint(pw, task.getMemory(), Format.NATURAL, 0);
	    pw.flush();
	    String valueString = baos.toString();
	    assertEquals(testName + symbol, expected, valueString);
	}
    }
''')

    def start_test(self, executable, name):
        print("    public void test_%s () {" % (name))
        tokens = os.path.splitext(os.path.abspath(executable))
        print('	TypeTestbed typeTestbed = new TypeTestbed("%s", "test%s");' % (os.path.basename(tokens[0]), name))

    def add_test(self, name, type, etype, decl, value):
        name = name.rstrip()
        type = type.rstrip().replace("\n","\\n")
        etype = etype.rstrip()
        value = value.rstrip().replace("\n","\\n")

        print('	typeTestbed.checkType("%s","%s");' % (name, type))
        print('	typeTestbed.checkValue("%s","%s");' % (name, value))
        
    def end_test(self):
        print("    }")

    def epilogue(self):
        print("}")    

########################################################################
# main
########################################################################

def usage ():
    print "Usage " + sys.argv[0] + " <-help> OutputFile File <File>..."
    sys.exit(1)

def open_file (arg):
    if (len (sys.argv) <= arg):
        return False
    try:
        file = open(sys.argv[arg], 'r')
    except IOError:
        print (sys.argv[arg] + " not found")
        sys.exit(1)
    return file

if (len (sys.argv) == 1):
    usage()
for t in sys.argv:
    if (t == "-help"):
        print "Builds TestTypeFactory*.java from input files, using annotations"
        print "in the input files to describe the data type to be tested."
        print "e.g. Given:"
        print "static struct {"
        print "  int int_var;"
        print "} arr_struct [2] = {{1},{2}};"
        print "One would use the annotation:"
        print "// Name: arr_struct"
        print "// Value: {{1},{2}}"
        print "// Type: struct {"
        print "// Type:   int int_var;"
        print "// Type: } [2]"
        usage()
    elif (t.startswith("-")):
        usage()

current_file = 1
d_file = open_file(current_file)
filename = sys.argv[current_file]
j_file = j()
j_file.open()
j_file.prologue()


name = type = etype = value = ""
while (True):
    line = d_file.readline()
    if (line == ""):
        current_file += 1
        d_file = open_file(current_file)
        if (not d_file):
            break
    # Output collected test info
    if (line[0:2] != "//"):
        if (name != ""):
            filename = sys.argv[current_file]
            j_file.start_test(filename, name)
            j_file.add_test(name, type, type, type, value)
            j_file.end_test()
            name = type = etype = value = ""
        continue
    tokens = line.split()
    try:
        # Collect test info
        if (tokens[1] == "Name:"):
            name = line[line.find(tokens[1]) + len(tokens[1]) + 1:].rstrip()
        elif (tokens[1] == "Type:"):
            type = type + line[line.find(tokens[1]) + len(tokens[1]) + 1:]
        elif (tokens[1] == "EType:"):
            etype = etype + line[line.find(tokens[1]) + len(tokens[1]) + 1:]
        elif (tokens[1] == "Value:"):
            value = value + line[line.find(tokens[1]) + len(tokens[1]) + 1:]
    except IndexError:
        True

j_file.epilogue()
