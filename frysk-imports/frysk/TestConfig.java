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

    /**
     * validate that the dir has a single trailing slash.
     */
    private void validateSlashed (String what, String dir)
    {
	assertTrue (what + " has trailing /", dir.endsWith ("/"));
	assertFalse (what + " has trailing //", dir.endsWith ("//"));
    }

    private void validate (boolean pure)
    {
	// The expected paths are valid.
	assertNotNull ("getGladeDir", Config.getGladeDir ());
	assertNotNull ("getHelpDir", Config.getHelpDir ());
	assertNotNull ("getImageDir", Config.getImageDir ());
	assertNotNull ("getBinDir", Config.getBinDir ());
	assertNotNull ("getPkgDataDir", Config.getPkgDataDir ());
	assertNotNull ("getPkgLibDir", Config.getPkgLibDir ());
	if (pure) {
	    assertNull ("getPkgLib32Dir", Config.getPkgLib32Dir ());
	    assertNull ("getPkgLib64Dir", Config.getPkgLib64Dir ());
	}
	else {
	    assertNotNull ("getPkgLib32Dir", Config.getPkgLib32Dir ());
	    assertNotNull ("getPkgLib64Dir", Config.getPkgLib64Dir ());
	    assertSame ("getPkgLibDir is getPkgLib32Dir",
			Config.getPkgLibDir (), Config.getPkgLib32Dir ());
	}
	// The expected paths have a trailing "/"
	validateSlashed ("getGladeDir", Config.getGladeDir ());
	assertFalse ("getHelpDir has trailing /", Config.getHelpDir ().endsWith ("/"));
	assertFalse ("getImageDir has trailing /", Config.getImageDir ().endsWith ("/"));
	validateSlashed ("getBinDir", Config.getBinDir ());
	validateSlashed ("getPkgDataDir", Config.getPkgDataDir ());
	validateSlashed ("getPkgLibDir", Config.getPkgLibDir ());
	if (!pure) {
	    validateSlashed ("getPkgLib32DirDir", Config.getPkgLib32Dir ());
	    validateSlashed ("getPkgLib64DirDir", Config.getPkgLib64Dir ());
	}
    }

    /**
     * Perform basic validation on the default install-directory
     * configuration.
     */
    public void testInstallDirs ()
    {
	Config.set (Config.createInstallConfig ());
	validate (true);
    }

    /**
     * Perform basic validation on the 32-bit on 64-bit
     * install-directory configuration.
     */
    public void testInstall32On64Dirs ()
    {
	Config.set (Config.createInstall32On64Config ());
	validate (false);
    }

    /**
     * Perform basic validation of the default build-tree
     * configuration.
     */
    public void testBuildDirs ()
    {
	Config.set (Config.createBuildConfig ("src-dir", "build-dir"));
	validate (true);
    }

    /**
     * Perform basic validation of the 32-bit on 64-bit build-tree
     * configuration.
     */
    public void testBuild32On64Dirs ()
    {
	Config.set (Config.createBuild32On64Config ("src-dir", "build-dir"));
	validate (false);
    }
}
