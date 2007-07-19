// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, Red Hat Inc.
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

package frysk.bindir;

import java.io.File;

import frysk.Config;
import frysk.expunit.Expect;
import frysk.junit.TestCase;

/**
 * This performs a "sniff" test of Fstack, confirming basic
 * functionality.
 */

public class TestFhd
  extends TestCase
{
  Expect e;
  Expect child;
  String prompt = "\\(fhpd\\) ";
  public void tearDown ()
  {
    if (e != null)
      e.close ();
    e = null;
    if (child != null)
      child.close ();
    child = null;
  }
  
  public void testHpdPid ()
  {
    child = new Expect(new String[] 
               { 
                 new File(Config.getPkgLibDir(), "hpd-c").getPath() 
               });
    e = new Expect(new String[] 
               { 
                 new File(Config.getBinDir(), "fhpd").getPath(), 
                 child.getPid().toString() 
                 });
    e.expect(5, "Attached to process.*\n" + prompt);
    e.close();
  }
  
  public void testHpdCommand ()
  {
    e = new Expect(new String[] 
                              { 
                                new File(Config.getBinDir(), "fhpd").getPath(), 
                                new File(Config.getPkgLibDir(), "hpd-c").getPath() 
                                });
                   e.expect(5, "Attached to process.*\n" + prompt);
                   e.close();
  }
  
  public void testHpdCore ()
  {
      e = new Expect(new String[]
                                {
	      			  new File(Config.getBinDir(), "fhpd").getPath(),
	      			  new File(Config.getPkgDataDir(), "test-core").getPath()
                                });
      e.expect(5, "Attached to core file.*");
      e.close();
  }
  public void testHpdCoreCore()
  {
      e = new Expect(new String[]
                                {
	      			  new File(Config.getBinDir(), "fhpd").getPath(),
                                });
      e.expect(prompt);
      e.send("core " + new File(Config.getPkgDataDir(), "test-core").getPath() + "\n");
      e.expect(5, "Attached to core file.*");
      e.close();
  }

  public void testHpdTraceStack ()
  {
    child = new Expect (new String[] 
	{
	  new File (Config.getPkgLibDir (), "hpd-c").getPath ()
	});
    e = new Expect (new String[]
	{
	  new File (Config.getBinDir (), "fhpd").getPath ()
	});
    e.expect (prompt);
    // Add
    e.send ("print 2+2\n");
    e.expect ("\r\n4\r\n" + prompt);
    // Attach
    e.send ("attach " + child.getPid () + "\n\n");
    e.expect (5, "attach.*\n" + prompt);
    // Where
    e.send ("where\n");
    e.expect ("where.*#0.*" + prompt);
    // int_21
    e.send ("print int_21\n");
    e.expect ("print.*2.*\r\n" + prompt);
    // Down
    e.send ("d\t");
    e.expect (".*defset.*delete.*detach.*disable.*down.*" + prompt + ".*");
    e.send ("own\n");
    e.expect ("own.*#1.*" + prompt);
    // int_21
    e.send ("print int_21\n");
    e.expect ("print.*int_21.*(fhpd)");
    e.send ("up\n");
    e.expect ("up.*#0.*" + prompt);
    e.close();
  }

  public void testHpdScalars ()
  {
    child = new Expect (new String[]
	{
	  new File (Config.getPkgLibDir (), "hpd-c").getPath ()
	});
    e = new Expect (new String[]
	{
	  new File (Config.getBinDir (), "fhpd").getPath ()
	});
    e.expect (prompt);
    // Attach
    e.send ("attach " + child.getPid () + "\n\n");
    e.expect (5, "attach.*\n" + prompt);
    // simode
    e.send ("what simode\n");
    e.expect ("what.*int .*\n" + prompt);
    // volatile int_22
    e.send ("print int_22\n");
    e.expect ("print.*22.*\r\n" + prompt);
    // int_21
    e.send ("print int_21\n");
    e.expect ("print.*2.*\r\n" + prompt);
    // volatile int_22
    e.send ("print int_22\n");
    e.expect ("print.*22.*\r\n" + prompt);
    // char_21
    e.send ("print ch\t");
    e.expect ("print.*char_21");
    e.send ("print char_21\n");
    e.expect ("print.*a.*" + prompt);
    // long_21
    // 	e.send ("print long_21\n");
    // 	e.expect ("print.*10.*" + prompt);
    // float_21
    // e.send ("print float_21\n");
    // e.expect ("print.*1\\.1.*" + prompt);
    // double_21
    // e.send ("print double_21\n");
    // e.expect ("print.*1\\.2.*" + prompt);
    // static_int
    e.send ("print static_int\n");
    e.expect ("print.*4.*" + prompt);
    e.close();
  }
    
  public void testHpdClass ()
  {
    child = new Expect (new String[]
	{
	  new File (Config.getPkgLibDir (), "hpd-c").getPath ()
	});
    e = new Expect (new String[]
	{
	  new File (Config.getBinDir (), "fhpd").getPath ()
	});
    e.expect (prompt);
    // Attach
    e.send ("attach " + child.getPid () + "\n\n");
    e.expect (5, "attach.*\n" + prompt);
    // static_class
    e.send ("print static_class\n");
    e.expect ("print.*12\\.34.*" + prompt);
    e.send ("print static_class.\t");
    e.expect (".*double_1.*int_1.*" + prompt + ".*");
    e.send ("int_1\n");
    e.expect (".*51.*" + prompt);
    // class
    e.send ("print class_2\n");
    e.expect ("print.*1.0.*1.*2.0.*2.*" + prompt);
    e.send ("print class_3\n");
    e.expect ("print.*1,2.*3,4.*" + prompt);
    // what class
    e.send ("what static_class\n");
    e.expect ("what.*static_class_t.*hpd-c.c.*" + prompt);
    e.send ("what class_4\n");
    e.expect ("what.*astruct.*" + prompt);
    e.send ("what class_5\n");
    e.expect ("what.*simode.*float.*" + prompt);
    e.close();
  }    

  public void testHpdArray ()
  {
    child = new Expect (new String[]
	{
	  new File (Config.getPkgLibDir (), "hpd-c").getPath ()
	});
    e = new Expect (new String[]
	{
	  new File (Config.getBinDir (), "fhpd").getPath ()
	});
    e.expect (prompt);
    // Attach
    e.send ("attach " + child.getPid () + "\n\n");
    e.expect (5, "attach.*\n" + prompt);
    // arr_1
    e.send ("print arr_\t");
    e.expect ("arr_1.*arr_2.*arr_3.*arr_4.*" + prompt);
    e.send ("1\n");
    e.expect (".*1,2,3,4,5.*0,1,2.*" + prompt);
    // what array
    e.send ("what arr_2\n");
    e.expect ("what.*int.*5,6.*hpd-c.c.*" + prompt);
    // arr_2
    e.send ("print arr_2\n");
    e.expect ("print.*1,2,3,4,5,6.*5,6,7,8,9,0.*" + prompt);
    // arr_3
    e.send ("print arr_3\n");
    e.expect ("print.*1.0,2.1,3.2,4.3,5.4.*7.5,8.6,9.7,10.8,1.9.*" + prompt);
    e.send ("print arr_4\n");
    // arr_4
    e.expect ("print.*" + prompt);
    e.close();
  }    

  public void testHpdEnum ()
  {
    child = new Expect (new String[]
	{
	  new File (Config.getPkgLibDir (), "hpd-c").getPath ()
	});
    e = new Expect (new String[]
	{
	  new File (Config.getBinDir (), "fhpd").getPath ()
	});
    e.expect (prompt);
    // Attach
    e.send ("attach " + child.getPid () + "\n\n");
    e.expect (5, "attach.*\n" + prompt);
    // enumeration
    e.send ("print ssportscar\n");
    e.expect ("print.*porsche.*" + prompt);
    e.send ("print porsche\n");
    e.expect ("print.*porsche.*1.*" + prompt);
    e.close();
  }

  public void testHpdBreakpoint()
  {
    child = new Expect (new String[]
	{
	  new File (Config.getPkgLibDir (), "hpd-c").getPath ()
	});
    e = new Expect (new String[]
	{
	  new File (Config.getBinDir (), "fhpd").getPath ()
	});
    e.expect (prompt);
    // Attach
    e.send ("attach " + child.getPid () + "\n\n");
    e.expect (5, "attach.*\n" + prompt);
    // Break
    e.send("break @hpd-c.c@196\n");	// This has to break on: while (int_21)
    e.expect("breakpoint.*" + prompt);
    e.send("go\n");
    e.expect("go.*\n" + prompt + "Breakpoint.*@hpd-c.c@196");
    e.send("quit\n");
    e.expect("quit.*\nQuitting...");
    e.close();
  }

  public void testHpdBreakpointInline()
  {
    child = new Expect (new String[]
	{
	  new File (Config.getPkgLibDir (), "test1").getPath ()
	});
    e = new Expect (new String[]
	{
	  new File (Config.getBinDir (), "fhpd").getPath ()
	});
    e.expect (prompt);
    // Attach
    e.send ("attach " + child.getPid () + " -cli\n");
    e.expect ("attach.*" + prompt);
    // Break
    e.send("break add\n");
    e.expect("break.*" + prompt);
    e.send("go\n");
    e.expect("go.*" + prompt + ".*Breakpoint.*add.*");
    e.send("where\n");
    e.expect("where.*#0.* main \\(\\).*" + prompt);
    e.send("quit\n");
    e.expect("Quitting...");
    e.close();
  }

    public void testHpdBreakpointLibrary() {
	child = new Expect (new String[] {
				new File (Config.getPkgLibDir (),
					  "test1").getPath ()
			    });
	e = new Expect (new String[] {
			    new File (Config.getBinDir (), "fhpd").getPath ()
			});
	e.expect (prompt);
	// Attach
	e.send ("attach " + child.getPid () + " -cli\n");
	e.expect ("attach.*" + prompt);
	// Break
	e.send("break sin\n");
	e.expect("break.*" + prompt);
	e.send("go\n");
	e.expect("go.*" + prompt + ".*Breakpoint.*sin.*");
	e.send("where\n");
	e.expect("where.*#0.* (__)?sin \\(\\).*" + prompt);
	e.send("quit\n");
	e.expect("Quitting...");
	e.close();
    }

    public void testHpdBreakStep() {
        	child = new Expect (new String[] {
				new File (Config.getPkgLibDir (),
					  "test1").getPath ()
			    });
	e = new Expect (new String[] {
			    new File (Config.getBinDir (), "fhpd").getPath ()
			});
	e.expect (prompt);
	// Attach
	e.send ("attach " + child.getPid () + " -cli\n");
	e.expect ("attach.*" + prompt);
	// Break
        e.send("break anotherFunction\n");
        e.expect("break.*" + prompt);
        e.send("go\n");
	e.expect("go.*" + prompt + ".*Breakpoint.*anotherFunction.*");
        e.send("step\n");
        e.expect("step.*Task stopped at.*test1\\.c.*" + prompt);
        e.send("step\n");
        e.expect("step.*Task stopped at.*test1\\.c.*" + prompt);
        e.send("quit\n");
	e.expect("Quitting...");
	e.close();
    }
    
    
    public void testHpdDisplayCommands() {
	e = new Expect (new String[] {
	    new File (Config.getBinDir (), "fhpd").getPath (), 
	    new File (Config.getPkgLibDir (), "hpd-c").getPath ()
	});
	e.expect (prompt);
	// Break
        e.send("break @hpd-c.c@179\n");
        e.expect("breakpoint.*" + prompt);
        e.send("go\n");
	e.expect("go.*" + prompt + ".*Breakpoint.*@hpd-c.c@179.*");
	e.send("display int_21\n");
	e.expect("display.*1:.*int_21 = .*" + prompt);
	e.send("display int_21*2\n");
	e.expect("display.*2:.*temp = .*" + prompt);
	e.send("actionpoints -display\n");
	e.expect("actionpoints.*DISPLAYS.*2.*y.*\"int_21.*\".*\n1.*y.*\"int_21\".*"
		+ prompt);
	e.send("disable 1\n");
	e.expect("disable.*display 1 disabled.*" + prompt);
	e.send("actionpoints -display\n");
	e.expect("actionpoints.*DISPLAYS.*2.*y.*\"int_21.*\".*\n1.*n.*\"int_21\".*"
		+ prompt);
	e.send("disable -display\n");
	e.expect("disable.*display 2 disabled.*" + prompt);
	e.send("actionpoints -display\n");
	e.expect("actionpoints.*DISPLAYS.*2.*n.*\"int_21.*\".*\n1.*n.*\"int_21\".*"
		+ prompt);
	e.send("enable 2\n");
	e.expect("enable.*display 2 enabled.*" + prompt);
	e.send("enable -display\n");
	e.expect("enable.*display 1 enabled.*" + prompt);
	e.send("delete 1\n");
	e.expect("delete.*display 1 deleted.*" + prompt);
	e.send("actionpoints -display\n");
	e.expect("actionpoints.*DISPLAYS.*2.*y.*\"int_21.*\".*" + prompt);
	e.send("delete -display\n");
	e.expect("delete.*display 2 deleted.*" + prompt);
	e.send("actionpoints -display\n");
	e.expect("actionpoints.*" + prompt);
	e.send("quit\n");
	e.expect("Quitting...");
	e.close();
    }
    
    public void testHpdDisassemble() {
	child = new Expect(new String[] { new File(Config.getPkgLibDir(),
		"hpd-c").getPath() });
	e = new Expect(new String[] { new File(Config.getBinDir(), "fhpd")
		.getPath() });
	e.expect(prompt);
	// Attach
	e.send("attach " + child.getPid() + "\n\n");
	e.expect(5, "attach.*\n" + prompt);
	e.send("disassemble\n");
	e.expect(5, "\\*.*test.*\n(.*\n)*" + prompt);
	e.send("quit\n");
	e.expect("Quitting...");
	e.close();
    }
}
