// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, Red Hat Inc.
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

/**
 * All the run-time (install time) configuration information.
 */

public class Config
{
    /**
     * Directory containing the .glade files describing frysk's UI
     * windows.
     */
    public static final native String getGladeDir ();
    /**
     * Root directory of frysk's images (or icons).
     */
    public static final native String getImageDir ();
    /**
     * Frysk's shared, and 32-bit and 64-bit independant, data
     * directory.
     */
    public static final native String getPkgDataDir ();
    /**
     * Frysk's library directory.
     *
     * Used by tests when they need to run an executable.
     */
    public static final native String getPkgLibDir ();
    /**
     * Frysk's 32-bit library directory.
     *
     * Used by 64-bit tests when they need to run 32-bit executables.
     */
    public static final native String getPkgLib32Dir ();
    /**
     * The frysk version number.  Typically of the form:
     * MAJOR.MINOR.PATCH.YYYY.MM.DD.
     */
    public static final native String getVersion ();

    // User's config directory.
    public static final String FRYSK_DIR
	= System.getProperty("user.home") + "/" + ".frysk" + "/";

    /**
     * The relative source root directory against which the build was
     * created; this is an install time option since, with debug-info
     * installed, the source files found under the root srcdir are
     * available.
     *
     * Note that this is the root directory, and excludes any suffix
     * such as frysk-imports et.al.
     */
    public static final native String getRootSrcDir ();
    /**
     * The absolute source root directory against which the build was
     * created; this is an install time option since, with debug-info
     * installed, these source files exist.
     *
     * Note that this is the root directory, and excludes any suffix
     * such as frysk-imports et.al.
     */
    public static final native String getAbsRootSrcDir ();
    /**
     * The word size of the underlying architecture.
     */
    public static final native int getWordSize ();
}
