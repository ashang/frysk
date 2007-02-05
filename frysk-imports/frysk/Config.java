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
     * Do not allow extension.
     */
    private Config ()
    {
    }

    /**
     * The currently selected Configuration.
     */
    static private Config current;
    /**
     * Select the specified configuration.
     */
    public static final void set (Config config)
    {
	current = config;
    }

    /**
     * Return the current config.
     */
    static final Config get ()
    {
	return current;
    }

    /**
     * Create the standard install tree configuration.
     *
     * The 32-bit and 64-bit specific library paths are set to NULL.
     */
    public static native final Config createInstallConfig ();

    /**
     * Create the 32-bit install tree configuration (used when running
     * 32-bit tests on a 64-bit install tree).
     */
    public static native final Config createInstall32On64Config ();

    /**
     * Create the standard build-tree configuration.
     *
     * The 32-bit and 64-bit specific library paths are set to NULL.
     */
    public static native final Config createBuildConfig (String absSrcDir,
							 String absBuildDir);

    /**
     * Create the 32-bit on 64-bit build-tree configuration.
     */
    public static native final Config createBuild32On64Config (String absSrcDir,
							       String absBuildDir);

    /**
     * Directory containing the .glade files describing frysk's UI
     * windows.
     */
    public static final String getGladeDir ()
    {
	return current.theGladeDir;
    }
    private String theGladeDir;

    /**
     * Directory containing the frysk help files.  Does not include
     * trailing slash.
     */
    public static final String getHelpDir ()
    {
	return current.theHelpDir;
    }
    private String theHelpDir;

    /**
     * Root directory of frysk's images (or icons).  Does not include
     * trailing slash.
     */
    public static final String getImageDir ()
    {
	return current.theImageDir;
    }
    private String theImageDir;

    /**
     * Frysk's shared, and 32-bit and 64-bit independant, data
     * directory.  Typically <tt>/usr/share/frysk</tt>.
     */
    public static final String getPkgDataDir ()
    {
	return current.thePkgDataDir;
    }
    private String thePkgDataDir;

    /**
     * Frysk's user-visible executable directory.  Typically <tt>/usr/bin</tt>.
     *
     * Used by install-tree testing when specifying the path to
     * installed frysk executables.
     */
    public static final String getBinDir ()
    {
	return current.theBinDir;
    }
    private String theBinDir;

    /**
     * Frysk's library directory.  Typically either
     * <tt>/usr/lib/frysk</tt> or <tt>/usr/lib64/frysk</tt>.
     *
     * Used by tests when they need to run an executable of the same
     * bit-size as frysk.
     */
    public static final String getPkgLibDir ()
    {
	return current.thePkgLibDir;
    }
    private String thePkgLibDir;

    /**
     * Frysk's 32-bit library directory.  Typically
     * <tt>/usr/lib/frysk</tt>.
     *
     * Solely for use by 32-bit on 64-bit tests when a 32-bit
     * executable is required.
     */
    public static final String getPkgLib32Dir ()
    {
	return current.thePkgLib32Dir;
    }
    private String thePkgLib32Dir;

    /**
     * Frysk's 64-bit library directory.  Typically
     * <tt>/usr/lib64/frysk</tt>.
     *
     * Solely for use by 32-bit on 64-bit tests when a 64-bit
     * executable is required.
     */
    public static final String getPkgLib64Dir ()
    {
	return current.thePkgLib64Dir;
    }
    private String thePkgLib64Dir;

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

    /**
     * Return the <tt>autoconf</tt>target_cpu</tt> specified when
     * building frysk.
     *
     * XXX: This variable's value is not a reliable indicator of the
     * system's architecture.  For instance, on an IA032 system, it
     * could contain any of i386, i586, or i686.
     *
     * XXX: This code was folded in from frysk.*.Build.BUILD_ARCH when
     * that file was deleted.  It is much more likely that code needs
     * to know the arch family, and not some arbitrary string.
     */
    public static final native String getTargetCpuXXX ();
}
