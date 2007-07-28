// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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

import frysk.expunit.Expect;
import frysk.Config;

public class TestDisplayCommand
    extends TestLib
{
    public void testHpdDisplayCommands() {
	e = new Expect (new String[] {
			    Config.getBinFile("fhpd").getPath (),
			    Config.getPkgLibFile("hpd-c").getPath ()
			});
	e.expect (prompt);
	// Break
        e.send("break #hpd-c.c#179\n");
        e.expect("breakpoint.*" + prompt);
        e.send("go\n");
	e.expect("go.*" + prompt + ".*Breakpoint.*#hpd-c.c#179.*");
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
}
