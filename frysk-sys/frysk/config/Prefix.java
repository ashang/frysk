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

public class Prefix {

    /**
     * The currently selected Prefix.
     */
    static private Prefix current;

    /**
     * Select the specified configuration.
     */
    public static final void set(Prefix config) {
	current = config;
    }

    /**
     * Return the current config.
     */
    static final Prefix get() {
	return current;
    }

    /**
     * Return either the file or directory.
     */
    private static File getFile(File dir, String file) {
	if (dir == null)
	    // Directory isn't valid, no files allowed.
	    return null;
	if (file == null)
	    return dir;
	return new File(dir, file);
    }

    /**
     * Directory containing the .glade files describing frysk's UI
     * windows.
     *
     * XXX: This is a String, and not a File, so that it works better
     * with Java-GNOME 2.x.
     */
    public static final String gladeDir () {
	return current.gladeDir;
    }
    private final String gladeDir;

    /**
     * Directory containing the frysk help files.
     */
    public static final File helpDir() {
	return current.helpDir;
    }
    private final File helpDir;

    /**
     * Root directory of frysk's images (or icons).
     *
     * XXX: This is a String, and not a File, so that it works better
     * with Java-GNOME 2.x.
     */
    public static final String imagesDir() {
	return current.imagesDir;
    }
    private final String imagesDir;

    /**
     * A file in Frysk's user-visible executable directory.  Typically
     * <tt>/usr/bin/FILE</tt>.
     *
     * Used by install-tree testing when needing to invoke a
     * user-visible executable.
     */
    public static final File binFile(String file) {
	return getFile(current.binDir, file);
    }
    private final File binDir;

    /**
     * A file in frysk's shared, 32-bit and 64-bit independant, data
     * directory.  Typically <tt>/usr/share/frysk/FILE</tt>
     */
    public static final File pkgDataFile (String file) {
	return getFile(current.pkgDataDir, file);
    }
    private final File pkgDataDir;

    /**
     * A file in Frysk's library directory.  Typically either
     * <tt>/usr/lib/frysk/FILE</tt> or <tt>/usr/lib64/frysk/FILE</tt>.
     *
     * Used by tests when they need to run an executable of the same
     * bit-size as frysk.
     */
    public static final File pkgLibFile (String file) {
	return getFile(current.pkgLibDir, file);
    }
    private final File pkgLibDir;

    /**
     * A file in frysk's 32-bit library directory.  Typically
     * <tt>/usr/lib/frysk/FILE</tt>.  Returns NULL when the Config
     * does not include a 32-bit specific directory.
     *
     * Solely for use by 32-bit on 64-bit tests when a 32-bit
     * executable is required.
     */
    public static final File pkgLib32File(String file) {
	return getFile(current.pkgLib32Dir, file);
    }
    private final File pkgLib32Dir;

    /**
     * A file in frysk's 64-bit library directory.  Typically
     * <tt>/usr/lib64/frysk/FILE</tt>.  Returns NULL when the Config
     * does not include a 64-bit specific directory.
     *
     * Solely for use by 32-bit on 64-bit tests when a 64-bit
     * executable is required.
     */
    public static final File pkgLib64File(String file) {
	return getFile(current.pkgLib64Dir, file);
    }
    private final File pkgLib64Dir;

    /**
     * A file within frysk's source tree.
     *
     * This is used by testing when constructing and comparing paths
     * to source files.
     */
    public static final File sourceFile(String file) {
        return getFile(current.sourceDir, file);
    }
    private final File sourceDir;

    /**
     * Construct a Prefix.
     *
     * Package-private
     */
    Prefix(String gladeDir, File helpDir, String imagesDir,
	   File binDir, File pkgDataDir, File pkgLibDir,
	   File pkgLib32Dir, File pkgLib64Dir,
	   File sourceDir) {
	this.gladeDir = gladeDir;
	this.helpDir = helpDir;
	this.imagesDir = imagesDir;
	this.binDir = binDir;
	this.pkgDataDir = pkgDataDir;
	this.pkgLibDir = pkgLibDir;
	this.pkgLib32Dir = pkgLib32Dir;
	this.pkgLib64Dir = pkgLib64Dir;
	this.sourceDir = sourceDir;
    }
}
