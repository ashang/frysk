#!/usr/bin/python
import os,posix,sys
import re
from subprocess import *
from os.path import *

########################################################################
# Manage creation of the java file
########################################################################

class j:
    def open(self,name):
#       self.j_file = open("/tmp/TestTypeEntry" + name.capitalize() + ".java", 'w')
        self.tool = name
        self.name = "TestTypeEntry" + name.capitalize()
    def write(self,str):
        print str
    def prologue(self,):
        print('''// Generated by gen-type-expect-tests.py

package frysk.debuginfo;

import java.util.logging.Logger;

import lib.dwfl.DwarfDie;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflDieBias;
import frysk.dwfl.DwflCache;
import frysk.proc.Task;
import frysk.testbed.DaemonBlockedAtSignal;
import frysk.testbed.TestLib;
''')
        if (self.tool == "value"):
            print('''
import java.io.*;
import frysk.value.Value;
import frysk.value.Format;
import frysk.debuginfo.DebugInfo;
''')
        elif (self.tool == "type"):
            print('''
import frysk.value.Type;
''')
            
        print('''
public class %s extends TestLib {
    private class ExpectTest {
	DebugInfoFrame frame;
        Task task;
	DwarfDie die;
	DwarfDie[] allDies;
	TypeEntry typeEntry;

	ExpectTest(String executable) {
	    task = new DaemonBlockedAtSignal(executable).getMainTask();
            frame = DebugInfoStackFactory.createDebugInfoStackTrace(task);
	    long pc = frame.getAdjustedAddress();
	    Dwfl dwfl = DwflCache.getDwfl(frame.getTask());
	    DwflDieBias bias = dwfl.getDie(pc);
	    die = bias.die;
	    allDies = die.getScopes(pc - bias.bias);
	    typeEntry = new TypeEntry(frame.getTask().getISA());
	}
''' % (self.name))
	
        if (self.tool == "type"):
            print('''
	void compareEqual(Expect[] expect, String myName) {
	     Type varType;
	    
	     for (int i = 0; i < expect.length; i++) {
		 DwarfDie varDie = die.getScopeVar(allDies, expect[i].symbol);
                 if (varDie == null)
                     System.out.println("Error: Cannot find " + expect[i].symbol);
		 assertNotNull(varDie);
		 varType = typeEntry.getType(varDie.getType());
                    // System.out.println("Expect: " + expect[i].symbol + "\\n'" + expect[i].output + "'\\nGot:\\n'" + valueString + "'");
		 assertNotNull(varType);
		 assertEquals(myName + expect[i].symbol, expect[i].output, varType.toPrint());
	     }
	}
    }
''')
        elif (self.tool == "value"):
            print('''
        void compareEqual(Expect[] expect, String myName) {
            // "Print" to a byte array
	    ByteArrayOutputStream baos = new ByteArrayOutputStream(32);
	    PrintWriter pw = new PrintWriter(baos, true);
	    for (int i = 0; i < expect.length; i++) {
                // ??? cache address of x so &x can be checked
	        if (expect[i].output.indexOf("&") >= 0)
		    continue;
                // ??? ignore char for now
	        if (expect[i].symbol.indexOf("char_") >= 0)
		    continue;
                DwarfDie varDie = die.getScopeVar(allDies, expect[i].symbol);
                if (varDie == null)
                    System.out.println("Error: Cannot find " + expect[i].symbol);
                assertNotNull(varDie);
		DebugInfo debugInfo = new DebugInfo(frame);
                Value value =  debugInfo.print(expect[i].symbol, frame, false);
                value.toPrint(pw, task.getMemory(), Format.NATURAL);
                pw.flush();
                String valueString = baos.toString();
                System.out.println("Expect: " + expect[i].symbol + "\\n'" + expect[i].output + "'\\nGot:\\n'" + valueString + "'");
                assertEquals(myName + expect[i].symbol, expect[i].output, valueString);
                baos.reset();
            }
	}
    }
''')

        print('''
    private class Expect
    {
	String symbol;
	String output;
	Expect (String symbol, String expect)
	{
	    this.symbol = symbol;
	    this.output = expect;
	}
    }

    Logger logger = Logger.getLogger("frysk");

''')

    def start_test(self,name):
        print('''
    public void test%s () {
        Expect [] expect  = {
''' % (name))

    def add_test(self,tool,name,type,etype,decl,value):
        name = name.rstrip()
        type = type.rstrip().replace("\n","\\n")
        etype = etype.rstrip()
        value = value.rstrip()

        print('\t    new Expect("%s",' % name)
        if (tool == "type"):
            print('"%s"' % type)
        elif (tool == "value"):
            print('"%s"' % value)
        print("),")
        
    def end_test(self, executable):
        tokens = executable.split(".")
        print('''
              };

      ExpectTest expectTest = new ExpectTest("%s");
      expectTest.compareEqual(expect, "testScalar ");
    }
''' % os.path.basename(tokens[0]))

    def epilogue(self,debug):
        print('''
    }
''')    

########################################################################
# main
########################################################################

def usage ():
    print "Usage " + sys.argv[0] + " -value OR -type <-help> OutputFile File <File>..."
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
debug=0
tool = ""
for t in sys.argv:
    if (t == "-value" or t == "-Value"):
        tool="value"
    elif (t == "-type" or t == "-Type"):
        tool="type"
    elif (t == "-debug"):
        debug=1
    elif (t == "-help"):
        print "Builds TestTypeEntry*.java from input files, using annotations"
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

current_file = 2
if (tool == ""):
    if (sys.argv[1].find("Type") >= 0):
        tool = "type"
    elif (sys.argv[1].find("Value") >= 0):
        tool = "value"
    current_file += 1

d_file = open_file(current_file)
j_file = j()
j_file.open(tool)
j_file.prologue()


test = "Types"
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
            if (test == "Types"):
                j_file.start_test(test)
            j_file.add_test(tool, name, type, type, type, value)
            name = type = etype = value = ""
        continue
    tokens = line.split()
    try:
        # Collect test info
        if (tokens[1] == "Test:"):
            if (test != "Types"):
                j_file.end_test(sys.argv[current_file])
            test = line[line.find(tokens[1]) + len(tokens[1]) + 1:].rstrip()
            j_file.start_test(test)
        elif (tokens[1] == "Name:"):
            name = name + line[line.find(tokens[1]) + len(tokens[1]) + 1:]
        elif (tokens[1] == "Type:"):
            type = type + line[line.find(tokens[1]) + len(tokens[1]) + 1:]
        elif (tokens[1] == "EType:"):
            etype = etype + line[line.find(tokens[1]) + len(tokens[1]) + 1:]
        elif (tokens[1] == "Value:"):
            value = value + line[line.find(tokens[1]) + len(tokens[1]) + 1:]
    except IndexError:
        True

j_file.end_test(sys.argv[current_file-1])
j_file.epilogue(debug)