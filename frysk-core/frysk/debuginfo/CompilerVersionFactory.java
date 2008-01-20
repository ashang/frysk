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

package frysk.debuginfo;

import java.util.HashMap;
import frysk.rsl.Log;

public class CompilerVersionFactory {
    private static HashMap compilerVersions = new HashMap();
    protected static Log fine = Log.fine(CompilerVersionFactory.class);
    protected static Log finest = Log.finest(CompilerVersionFactory.class);

    public static CompilerVersion getCompilerVersion(String compiler) {
	if (compilerVersions.containsKey(compiler))
	    return (CompilerVersion) compilerVersions.get(compiler);

	// XXX: GNU C specific.
	fine.log("Found compiler: ", compiler);

	CompilerVersion compVersion;

	// String looks like: GNU C++ 4.1.2 20070925 (Red Hat 4.1.2-33)
	//
	if (!compiler.matches("GNU C.*\\(Red Hat \\d+\\.\\d+\\.\\d+-\\d+\\)")) {
	    compVersion = new CompilerVersion(compiler);
	} else {
	    String preCompilerVersion = "(Red Hat ";

	    String compilerVersion = compiler.substring(compiler
		    .indexOf(preCompilerVersion)
		    + preCompilerVersion.length(), compiler.lastIndexOf(')'));

	    String[] versions = compilerVersion.split("\\.");

	    finest.log("Version string has 3 sections");

	    int version = Integer.parseInt(versions[0]);
	    int minorVersion = Integer.parseInt(versions[1]);

	    String[] minorVersions = versions[2].split("-");

	    int patchLevel = Integer.parseInt(minorVersions[0]);
	    int RHRelease = Integer.parseInt(minorVersions[1]);
	    compVersion = new GNURedHatCompilerVersion(compiler, version,
		    minorVersion, patchLevel, RHRelease);
	}
	compilerVersions.put(compiler, compVersion);
	return compVersion;
    }
}
