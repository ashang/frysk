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
 * All the run-time (install time) configuration information.
 */

public class Config {
    /**
     * Directory containing the .glade files describing frysk's UI
     * windows.
     *
     * XXX: This is a String so that it works better with Java-GNOME.
     */
    public static final String getGladeDir () {
	return Prefix.gladeDir();
    }

    /**
     * Directory containing the frysk help files.
     */
    public static final File getHelpDir () {
	return Prefix.helpDir();
    }

    /**
     * Root directory of frysk's images (or icons).
     *
     * XXX: This is a String so that it works better with Java-GNOME.
     */
    public static final String getImagesDir () {
	return Prefix.imagesDir();
    }

    /**
     * A file in frysk's shared, and 32-bit and 64-bit independant,
     * data directory.  Typically <tt>/usr/share/frysk/FILE</tt>
     */
    public static final File getPkgDataFile (String file) {
	return Prefix.pkgDataFile(file);
    }

    /**
     * A file in Frysk's user-visible executable directory.  Typically
     * <tt>/usr/bin/FILE</tt>.
     *
     * Used by install-tree testing when needing to run a user-visible
     * executable.
     */
    public static final File getBinFile(String file) {
	return Prefix.binFile(file);
    }

    /**
     * A file in Frysk's library directory.  Typically either
     * <tt>/usr/lib/frysk/FILE</tt> or <tt>/usr/lib64/frysk/FILE</tt>.
     *
     * Used by tests when they need to run an executable of the same
     * bit-size as frysk.
     */
    public static final File getPkgLibFile (String file) {
	return Prefix.pkgLibFile(file);
    }

    /**
     * A file in frysk's 32-bit library directory.  Typically
     * <tt>/usr/lib/frysk/FILE</tt>.  Returns NULL when the Config
     * does not include a 32-bit specific directory.
     *
     * Solely for use by 32-bit on 64-bit tests when a 32-bit
     * executable is required.
     */
    public static final File getPkgLib32File(String file) {
	return Prefix.pkgLib32File(file);
    }

    /**
     * A file in frysk's 64-bit library directory.  Typically
     * <tt>/usr/lib64/frysk/FILE</tt>.  Returns NULL when the Config
     * does not include a 64-bit specific directory.
     *
     * Solely for use by 32-bit on 64-bit tests when a 64-bit
     * executable is required.
     */
    public static final File getPkgLib64File(String file) {
	return Prefix.pkgLib64File(file);
    }

    public static File getFryskDir(){
	File file = new File(getHomeDir()+"/"+".frysk/");
	if(file.exists()){
	    file.mkdir();
	}
	return file;
    }
    
    public static File getHomeDir() {
	//XXX: Should not use user.home property.
	return new File(System.getProperty("user.home"));
    }
}
