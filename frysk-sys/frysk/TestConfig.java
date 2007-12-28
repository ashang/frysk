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

package frysk;

import frysk.junit.TestCase;

/**
 * All the run-time (install time) configuration information.
 */

public class TestConfig
    extends TestCase
{
    private Config old;
    /**
     * Save the current config so that this doesn't interfere with
     * other tests.
     */
    public void setUp ()
    {
	old = Config.get ();
    }
    /**
     * Restore the old config settings.
     */
    public void tearDown ()
    {
	Config.set (old);
    }

    private void validate(boolean pure) {
	// The expected paths are valid.
	assertNotNull("getGladeDir", Config.getGladeDir ());
	assertNotNull("getHelpDir", Config.getHelpDir ());
	assertNotNull("getImagesDir", Config.getImagesDir ());
	assertNotNull("getBinFile", Config.getBinFile (null));
	assertNotNull("getPkgDataFile", Config.getPkgDataFile (null));
	assertNotNull("getPkgLibFile", Config.getPkgLibFile (null));
	if (pure) {
	    assertNull("getPkgLib32File", Config.getPkgLib32File(null));
	    assertNull("getPkgLib64File", Config.getPkgLib64File(null));
	} else {
	    assertNotNull("getPkgLib32File", Config.getPkgLib32File(null));
	    assertNotNull("getPkgLib64File", Config.getPkgLib64File(null));
	    assertEquals("getPkgLibFile is getPkgLib64File",
			 Config.getPkgLibFile(null),
			 Config.getPkgLib64File(null));
	}
    }

    /**
     * Perform basic validation on the default install-directory
     * configuration.
     */
    public void testInstallDirs ()
    {
	Config.set (Config.createInstallConfig ());
	switch (Config.getWordSize()) {
	case 32:
	    validate(true);
	    break;
	case 64:
	    validate(false);
	    break;
	default:
	    fail("unknown word size");
	}
    }

    /**
     * Perform basic validation of the default build-tree
     * configuration.
     */
    public void testBuildDirs ()
    {
	Config.set (Config.createBuildConfig ("src-dir", "build-dir"));
	switch (Config.getWordSize()) {
	case 32:
	    validate(true);
	    break;
	case 64:
	    validate(false);
	    break;
	default:
	    fail("unknown word size");
	}
    }

    public void testBuild32() {
	Config.set (Config.createBuildConfig32("src-dir", "build-dir"));
	switch (Config.getWordSize()) {
	case 32:
	case 64: 
	    validate (true);
	    break;
	default:
	    fail("unknown word size");
	}
    }
    public void testInstall32() {
	Config.set (Config.createInstallConfig32());
	switch (Config.getWordSize()) {
	case 32:
	case 64: 
	    validate (true);
	    break;
	default:
	    fail("unknown word size");
	}
    }
    public void testBuild64() {
	Config.set (Config.createBuildConfig64("src-dir", "build-dir"));
	switch (Config.getWordSize()) {
	case 32:
	    assertNull("config", Config.get());
	    break;
	case 64: 
	    validate (true);
	    break;
	default:
	    fail("unknown word size");
	}
    }
    public void testInstall64() {
	Config.set (Config.createInstallConfig64());
	switch (Config.getWordSize()) {
	case 32:
	    assertNull("config", Config.get());
	    break;
	case 64: 
	    validate (true);
	    break;
	default:
	    fail("unknown word size");
	}
    }

}
