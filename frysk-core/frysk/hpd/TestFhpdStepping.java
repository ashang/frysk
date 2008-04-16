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

package frysk.hpd;

import java.io.File;

import frysk.config.Config;
import frysk.testbed.TestfileTokenScanner;

public class TestFhpdStepping extends TestLib {

    TestfileTokenScanner scanner = null;
    
    public void testInstructionStep () {
	
	if (unresolved(4914))
	    return;
	
	e = new HpdTestbed();
	
	String source = Config.getPkgLibSrcDir() + "funit-stepping-asm.S";
	this.scanner = new TestfileTokenScanner(new File(source));
	int startLine = this.scanner.findTokenLine("_instructionStep_");
	
	e = HpdTestbed.run("funit-stepping-asm");
	
	// Remove this - #4919 and #4914.
	try { Thread.sleep(2000); } catch (Exception e) {}
	
	e.send("break #" + source + "#" + startLine + "\n");
	e.expect("breakpoint.*\n" + prompt);
	
	System.err.println("send go");
	e.send("go\n");
	e.expect("go.*\n" + prompt + "Breakpoint.*#*");

	e.send("stepi\n");
	e.expect("Task stopped at line " + startLine + ".*\n" + prompt);

	e.send("quit\n");
	e.expect("quit.*\nQuitting...");
	e.close();
    }
    
    public void testLineStep () {
	
	if (unresolved(4914))
	    return;
	
	String source = Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-stepping-asm.S";
	this.scanner = new TestfileTokenScanner(new File(source));
	int startLine = this.scanner.findTokenLine("_instructionStep_");
	int endLine = this.scanner.findTokenLine("_lineStepEnd_");
	
	e = HpdTestbed.run("funit-stepping-asm");
	
	// Remove this - #4919 and #4914.
	try { Thread.sleep(2000); } catch (Exception e) {}
	
	e.send("break #" + source + "#" + startLine + "\n");
	e.expect("breakpoint.*\n" + prompt);
	
	e.send("go\n");
	e.expect("go.*\n" + prompt + "Breakpoint.*#*");
	
	e.send("step\n");
	e.expect("Task stopped at line " + endLine + ".*\n" + prompt);
	
	e.send("quit\n");
	e.expect("quit.*\nQuitting...");
	e.close();
    }
    
    public void testNextStep () {
	
	if (unresolved(4914))
	    return;
	
	String source = Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-stepping-asm.S";
	this.scanner = new TestfileTokenScanner(new File(source));
	int startLine = this.scanner.findTokenLine("_stepOverStart_");
	int endLine = this.scanner.findTokenLine("_stepOverEnd_");
	
	e = HpdTestbed.run("funit-stepping-asm");
	
	// Remove this - #4919 and #4914.
	try { Thread.sleep(2000); } catch (Exception e) {}
	
	e.send("break #" + source + "#" + startLine + "\n");
	e.expect("breakpoint.*\n" + prompt);
	
	e.send("go\n");
	e.expect("go.*\n" + prompt + "Breakpoint.*#*");
	
	e.send("next\n");
	e.expect("Task stopped at line " + endLine + ".*\n" + prompt);
	
	e.send("quit\n");
	e.expect("quit.*\nQuitting...");
	e.close();
    }
    
    public void testNextiStep () {
	
	if (unresolved(4914))
	    return;
	
	String source = Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-stepping-asm.S";
	this.scanner = new TestfileTokenScanner(new File(source));
	int startLine = this.scanner.findTokenLine("_stepOverStart_");
	int endLine = this.scanner.findTokenLine("_stepOverEnd_");
	
	e = HpdTestbed.run("funit-stepping-asm");
	
	// Remove this - #4919 and #4914.
	try { Thread.sleep(2000); } catch (Exception e) {}
	
	e.send("break #" + source + "#" + startLine + "\n");
	e.expect("breakpoint.*\n" + prompt);
	
	e.send("go\n");
	e.expect("go.*\n" + prompt + "Breakpoint.*#*");
	
	e.send("nexti\n");
	e.expect("Task stopped at line " + endLine + ".*\n" + prompt);
	
	e.send("quit\n");
	e.expect("quit.*\nQuitting...");
	e.close();
    }
    
    public void testFinishStep () {
	
	if (unresolved(4914))
	    return;
	
	String source = Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-stepping-asm.S";
	this.scanner = new TestfileTokenScanner(new File(source));
	int startLine = this.scanner.findTokenLine("_stepOutStart_");
	int endLine = this.scanner.findTokenLine("_stepOverEnd_");
	
	e = HpdTestbed.run("funit-stepping-asm");
	
	// Remove this - #4919 and #4914.
	try { Thread.sleep(2000); } catch (Exception e) {}
	
	e.send("break #" + source + "#" + startLine + "\n");
	e.expect("breakpoint.*\n" + prompt);
	
	e.send("go\n");
	e.expect("go.*\n" + prompt + "Breakpoint.*#*");
	
	e.send("finish\n");
	e.expect("Task stopped at line " + endLine + ".*\n" + prompt);
	
	e.send("quit\n");
	e.expect("quit.*\nQuitting...");
	e.close();
    }
}
