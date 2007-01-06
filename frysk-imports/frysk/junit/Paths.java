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

package frysk.junit;

/**
 * Directory prefixes that should be used by JUnit tests when they
 * need to access files.  Set differently according to the build or
 * install.
 */
public class Paths
{
    /**
     * Do not alow extension.
     */
    private Paths ()
    {
    }
    /** Variable to hold the program's basename.  */
    static private String runnerBasename;
    /**
     * Set the TestRunner's basename.
     */
    static public void setRunnerBasename (String runnerBasename)
    {
	Paths.runnerBasename = runnerBasename;
    }
    /**
     * Get the TestRunner's basename.
     */
    static public String getRunnerBasename ()
    {
	return Paths.runnerBasename;
    }
    static private String execPrefix;
    static private String exec32Prefix; 
    static private String dataPrefix;
    static private String gladePrefix;
    static private String imagePrefix;
    /**
     * Set the TestRunner's exec paths.  Tests should prefix any
     * program paths with these.
     */
    static public void setExecPrefixes (String execPrefix,
					String exec32Prefix)
    {
	Paths.execPrefix = execPrefix;
	Paths.exec32Prefix = exec32Prefix;
    }
    /**
     * Set the TestRunner's data directories.  Tests should prefix any
     * data paths with these.
     */
    static public void setDataPrefixes (String dataPrefix,
					String gladePrefix,
					String imagePrefix)
    {
	Paths.dataPrefix = dataPrefix;
	Paths.gladePrefix = gladePrefix;
	Paths.imagePrefix = imagePrefix;
    }
    /**
     * Get the TestRunner's executable working directory.  Tests
     * requring external executables prefix any programs with this
     * path.
     *
     * On a 32-bit system this directory will contain 32-bit
     * executables, and on a 64-bit system this directory will contain
     * 64-bit executables.
     */
    static public String getExecPrefix ()
    {
	return execPrefix;
    }
    /**
     * Get the TestRunner's 32-bit executable working directory.
     * Tests requring external 32-bit executables prefix any 32-bit
     * programs with this path.
     *
     * Only 64-bit tests, requiring 32-bit executables, use this path.
     */
    static public String getExec32Prefix ()
    {
	return exec32Prefix;
    }
    /**
     * Get the TestRunner's data working directory.  Tests requiring
     * external data files should prefix those files with this path.
     */
    static public String getDataPrefix ()
    {
	return dataPrefix;
    }
    /**
     * Get the TestRunner's glade directories.
     */
    static public String getGladePrefix ()
    {
	return gladePrefix;
    }
    /**
     * Get the TestRunner's image directories.
     */
    static public String getImagePrefix ()
    {
	return imagePrefix;
    }
}
