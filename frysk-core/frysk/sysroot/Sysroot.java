// This file is part of the program FRYSK.
//
// Copyright 2008, Red Hat Inc.
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

package frysk.sysroot;

import java.io.File;
import java.io.IOException;

/**
 * Map from a Task's executable to its sysroot special root directory.
 */  

public class Sysroot {
    File sysrootFile;

    public Sysroot (File path) {
	sysrootFile = path;
    }

    /**
     * return a pathname of an executable.
     * 
     * @param pathname
     *		is the executable.
     * @return this executable's pathname, searched for on $PATH
     */
    public File getPathViaDefaultRoot (String pathname) {
        String pathVar = System.getenv("PATH");
        return findExe(pathVar, "/", pathname);
    }
    
    /**
     * return a pathname of an executable.  Used only for testing.
     */
    File getPathViaDefaultRoot (String pathname, String pathVar) {
	return findExe(pathVar, "/", pathname);
    }

    /**
     * return a pathname of an executable.
     * 
     * @param pathname
     *		is the executable.
     * @return this executable's pathname, searched for on $PATH in the special 
     * 		root directory.
     */
    public File getPathViaSysroot (String pathname) {
        String pathVar = System.getenv("PATH");
        return findExe(pathVar, sysrootFile.getPath(), pathname);
    }
    
    /**
     * return a pathname of an executable in a sysroot.  Used only for testing.
     */
    File getPathViaSysroot (String pathname, String pathVar) {
        return findExe(pathVar, sysrootFile.getPath(), pathname);
    }

    /**
     * return a pathname of an executable's source.
     * 
     * @param pathname
     *		is the executable's compilation directory.
     * @param file
     *		this executable's source name.
     * @return the pathname of the executable's source.
     */
    public File getSourcePathViaDefaultRoot (File compilationDir, File f) {
	try {
	    if (! f.isAbsolute()) {
		// The file refers to a path relative to the
		// compilation directory, so prepend that directory path.
		return new File(compilationDir.getPath(), f.getPath()).getCanonicalFile();
	    }
	    else
		return new File(f.getPath()).getCanonicalFile();
	} catch (IOException e) {
	    return f;
    	} 
    }

    /**
     * return a pathname of an executable's source.
     * 
     * @param pathname
     *		is the executable's compilation directory.
     * @param file
     *		this executable's source name.
     * @return the pathname of the executable's source searched for in the
     *		special root directory.
     */
    public File getSourcePathViaSysroot (File compilationDir, File f) {
	try {
	    if (! f.isAbsolute()) {
		// The file refers to a path relative to the
		// compilation directory, so prepend that directory path.
		File parent = new File(sysrootFile.getPath(), compilationDir.getPath());
		return new File(parent, f.getPath()).getCanonicalFile();
	    }
	    else
		return new File(sysrootFile.getPath(), f.getPath()).getCanonicalFile();
	} catch (IOException e) {
	    return f;
	}
    }

    private static File findExe(String pathVar, String sysrootFile, String arg0) {
        if (pathVar == null) {
            return new File(arg0);
        }

        if (arg0.startsWith("/")) {
            return new File(arg0);
        }

        String[] path = pathVar.split(":");
        if (path == null) {
            return new File(arg0);
        }

        for (int i = 0; i < path.length; i++) {
            File file = new File(new File(sysrootFile, path[i]), arg0);
            if (file.exists()) {
                return file;
            }
        }
        return new File(arg0); // punt
    }
}
