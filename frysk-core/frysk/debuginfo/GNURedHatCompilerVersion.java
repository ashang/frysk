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

package frysk.debuginfo;

import frysk.rsl.Log;

public class GNURedHatCompilerVersion extends CompilerVersion implements
	Comparable {
    
    private static Log fine = Log.fine(GNURedHatCompilerVersion.class);

    private int version;
    private int minorVersion;
    private int patchLevel;
    private int RHRelease;

    private static GNURedHatCompilerVersion minSupportingClassType = new GNURedHatCompilerVersion(
	    "GNURedHatCompilerVersionSupportsClassType", 4, 1, 2, 37);

    private static GNURedHatCompilerVersion minSupportingInterfaceType = new GNURedHatCompilerVersion(
	    "GNURedhatCompilerVersionSupportsInterfaceType", 4, 3, 0, 7);

    public GNURedHatCompilerVersion(String string, int version,
	    int minorVersion, int patchLevel, int RHRelease) {
	super(string);

	fine.log(this, "Setting up GNU compiler");
	this.version = version;
	this.minorVersion = minorVersion;
	this.patchLevel = patchLevel;
	this.RHRelease = RHRelease;
    }

    public boolean supportsClassType() {
	boolean ret = this.compareTo(minSupportingClassType) >= 0;
	fine.log(this, "Entering supportsClassType, returning: ", Boolean.valueOf(ret));
	return ret;
    }

    public boolean supportsInterfaceType() {
	return this.compareTo(minSupportingInterfaceType) >= 0;
    }

    public int compareTo(Object arg0) {
	fine.log("Entering compareTo, arg: ", arg0);
	GNURedHatCompilerVersion that = (GNURedHatCompilerVersion) arg0;

	if (this.version != that.version) {
	    return this.version - that.version;
	} else if (this.minorVersion != that.minorVersion) {
	    return this.minorVersion - that.minorVersion;
	} else if (this.patchLevel != that.patchLevel) {
	    return this.patchLevel - that.patchLevel;
	} else {
	    return this.RHRelease - that.RHRelease;
	}
    }
    
}
