// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

package frysk.config;

import java.io.File;

/**
 * Standard directory prefixes, as specified by autoconf.
 *
 * To complicate things, this can be set to specify either the in-tree
 * build-time prefix, or the install-tree prefix.
 */

public class PrefixFactory {

    private static native String gladeDir();
    private static native String helpDir();
    private static native String imagesDir();
    private static native String binDir();
    private static native String pkgDataDir();
    private static native String pkgLibDir();
    private static native String pkgLib32Dir();

    /**
     * Create an install Prefix; need to specify the location of the
     * libraries.
     */
    private static Prefix installPrefix(File lib, File lib32, File lib64) {
	return new Prefix(gladeDir(), new File(helpDir()), imagesDir(),
			  new File(binDir()), new File(pkgDataDir()),
			  lib, lib32, lib64);
    }

    /**
     * Create the "native" install tree configuration; all possible
     * paths, including 32-on-64 when applicable, are defined.
     */
    public static Prefix createInstallPrefix() {
	File libDir = new File(pkgLibDir());
	File lib32Dir = new File(pkgLib32Dir());
	File lib64Dir = null; // 64-on-32 not supported.
	return installPrefix(libDir,
			     Host.wordSize() == 32 ? libDir : lib32Dir,
			     Host.wordSize() == 32 ? lib64Dir : libDir);
    }

    /**
     * Create a 32-bit specific install configuration; return NULL if
     * this is not supported.
     */
    public static Prefix createInstallPrefix32() {
	File libDir = new File(pkgLibDir());
	File lib32Dir = new File(pkgLib32Dir());
	return installPrefix(// Test against 32-bit libraries.
			     Host.wordSize() == 32 ? libDir : lib32Dir,
			     // Disable 32-on-64 tests.
			     Host.wordSize() == 32 ? libDir : null,
			     Host.wordSize() == 32 ? null : libDir);
    }

    /**
     * Create a 64-bit specific install configuration; return NULL if
     * if this is not supported.
     */
    public static Prefix createInstallPrefix64() {
	if (Host.wordSize() == 32)
	    // 64-test on 32-bit not supported.
	    return null;
	File libDir = new File(pkgLibDir());
	// Disable 32-on-64 tests.
	return installPrefix(libDir, null, libDir);
    }

    /**
     * Create a build-tree prefix.
     */
    private static Prefix buildPrefix(String absSrcDir, String absBuildDir,
				      File lib, File lib32, File lib64) {
	return new Prefix(absSrcDir.concat("/frysk/gui/gladedir/"),
			  new File(absSrcDir, "/frysk/gui/helpdir"),
			  absSrcDir.concat("/frysk/gui/imagesdir"),
			  new File(absBuildDir, "/frysk/bindir/"),
			  new File(absBuildDir, "/frysk/pkgdatadir/"),
			  lib, lib32, lib64);
    }

    /**
     * Create the "native" build-tree configuration; all possible
     * paths, including 32-on-64 when applicable, are enabled.
     */
    public static Prefix createBuildPrefix(String absSrcDir,
					   String absBuildDir) {
	File libDir = new File(absBuildDir, "/frysk/pkglibdir/");
	File lib32Dir = new File(absBuildDir, "/frysk/pkglibdir/arch32/");
	File lib64Dir = null; // 64-on-32 not supported.
	return buildPrefix(absSrcDir, absBuildDir, libDir,
			   Host.wordSize() == 32 ? libDir : lib32Dir,
			   Host.wordSize() == 32 ? lib64Dir : libDir);
    }

    /**
     * Create a 32-bit specific build-tree configuration; return NULL
     * if this is not supported by this build.
     */
    public static Prefix createBuildPrefix32(String absSrcDir,
					     String absBuildDir) {
	File libDir = new File(absBuildDir, "/frysk/pkglibdir/");
	File lib32Dir = new File(absBuildDir, "/frysk/pkglibdir/arch32/");
	return buildPrefix(absSrcDir, absBuildDir,
			   // Test against 32-bit libraries.
			   Host.wordSize() == 32 ? libDir : lib32Dir,
			   // Disable 32-on-64 testing.
			   Host.wordSize() == 32 ? libDir : null,
			   Host.wordSize() == 32 ? null : libDir);
    }

    /**
     * Create a 64-bit specific build-tree configuration; return NULL
     * if this is not supported by this build.
     */
    public static Prefix createBuildPrefix64(String absSrcDir,
					     String absBuildDir) {
	if (Host.wordSize() == 32)
	    // 64-on-32 not supported.
	    return null;
	File libDir = new File(absBuildDir, "/frysk/pkglibdir/");
	// Disable 32-on-64 testing.
	return buildPrefix(absSrcDir, absBuildDir, libDir, null, libDir);
    }
}
