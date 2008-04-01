// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

public class PrintStackOptions {

    private int numberOfFrames = 10;

    private boolean printFullPaths = false;

    private boolean printParams = false;
    private boolean printLocals = false;
    private boolean printLibraries = false;
    private boolean printInlineFunctions = false;
    private boolean printDebugNames = false;
    
    public PrintStackOptions() {
    }
    
    /**
     * Set things up for a light-weight, or low-cost, back-trace by
     * limiting things to just the elf information.
     */
    public PrintStackOptions setLite() {
	printParams = false;
	printLocals = false;
	printLibraries = true;
	printInlineFunctions = false;
	printDebugNames = true;
	return this;
    }

    /**
     * Set things up for a rich, or detailed, back-trace by including
     * inline frames and parameter information.
     */
    public PrintStackOptions setRich() {
	printParams = true;
	printLocals = true;
	printLibraries = true;
	printInlineFunctions = true;
	printDebugNames = true;
	return this;
    }

    /**
     * Specify the number of frames to include in the back-trace, 0 to
     * include all frames.
     */
    public void setNumberOfFrames(int numberOfFrames) {
	this.numberOfFrames = numberOfFrames;
    }
    public int numberOfFrames() {
	return numberOfFrames;
    }

    public void setPrintParams(boolean printParams) {
	this.printParams = printParams;
    }
    public boolean printParams() {
	return printParams;
    }

    public void setPrintLocals(boolean printLocals) {
	this.printLocals = printLocals;
    }
    public boolean printLocals() {
	return printLocals;
    }

    public void setPrintFullPaths(boolean printFullPaths) {
	this.printFullPaths = printFullPaths;
    }
    public boolean printFullPaths() {
	return printFullPaths;
    }

    public void setPrintLibraries(boolean printLibraries) {
	this.printLibraries = printLibraries;
    }
    public boolean printLibraries() {
	return printLibraries;
    }

    public void setPrintInlineFunctions(boolean printInlineFunctions) {
	this.printInlineFunctions = printInlineFunctions;
    }
    public boolean printInlineFunctions() {
	return printInlineFunctions;
    }

    public void setPrintDebugNames(boolean printDebugNames) {
	this.printDebugNames = printDebugNames;
    }
    public boolean printDebugNames() {
	return printDebugNames;
    }

    public boolean elfOnly() {
	return ! (printLocals || printInlineFunctions || printParams
		  || printDebugNames);
    }
    
}
