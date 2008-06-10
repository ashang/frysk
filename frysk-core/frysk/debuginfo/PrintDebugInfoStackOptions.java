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

import frysk.stack.PrintStackOptions;

public class PrintDebugInfoStackOptions extends PrintStackOptions {

    public PrintDebugInfoStackOptions() {
	// Note, the super calls clear.
    }
    
    private boolean printParameters;
    private boolean printLocals;
    private boolean printInlineFunctions;
    private boolean printDebugNames;
    private boolean printValues;
    private boolean printSourcePaths;
    
    /**
     * Clear all options.
     */
    public void clear() {
	super.clear();
	printParameters = false;
	printLocals = false;
	printInlineFunctions = false;
	printDebugNames = false;
	printValues = false;
    }

    /**
     * Set things up for a light-weight, or low-cost, back-trace by
     * limiting things to just the elf information.
     */
    public void setLite() {
	setAbi();
	setPrintDebugNames(true);
    }

    /**
     * Set things up for a rich, or detailed, back-trace by including
     * inline frames and parameter information.
     */
    public void setRich() {
	setAbi();
	setPrintParameters(true);
	setPrintInlineFunctions(true);
	setPrintDebugNames(true);
    }

    /**
     * Print the full path to any source file.
     */
    public void setPrintPaths(boolean printPaths) {
	super.setPrintPaths(printPaths);
	setPrintSourcePaths(printPaths);
    }

    /**
     * Print the parameter list (see also printValues).
     */
    public boolean printParameters() {
	return printParameters;
    }
    public void setPrintParameters(boolean printParameters) {
	this.printParameters = printParameters;
    }

    /**
     * Print paramter and variable values (rather than just their
     * names).
     */
    public boolean printValues() {
	return printValues;
    }
    public void setPrintValues(boolean printValues) {
	this.printValues = printValues;
    }

    /**
     * Print the function's local variables.
     */
    public boolean printLocals() {
	return printLocals;
    }
    public void setPrintLocals(boolean printLocals) {
	this.printLocals = printLocals;
    }

    /**
     * Print inline function instances.
     */
    public boolean printInlineFunctions() {
	return printInlineFunctions;
    }
    public void setPrintInlineFunctions(boolean printInlineFunctions) {
	this.printInlineFunctions = printInlineFunctions;
    }

    /**
     * Print function and variable names using debug, rather than ABI,
     * information.
     */
    public boolean printDebugNames() {
	return printDebugNames;
    }
    public void setPrintDebugNames(boolean printDebugNames) {
	this.printDebugNames = printDebugNames;
    }

    /**
     * Print the full path to source files (instead of just the file
     * name).
     */
    public boolean printSourcePaths() {
	return printSourcePaths;
    }
    public void setPrintSourcePaths(boolean printSourcePaths) {
	this.printSourcePaths = printSourcePaths;
    }

    public boolean abiOnly() {
	return ! (printLocals
		  || printInlineFunctions
		  || printParameters
		  || printValues
		  || printDebugNames);
    }
}
