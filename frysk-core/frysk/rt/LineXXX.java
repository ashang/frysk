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

package frysk.rt;

import java.io.IOException;
import java.io.File;
import frysk.dom.DOMFactory;
import frysk.dom.DOMFrysk;
import frysk.dom.DOMFunction;
import frysk.dom.DOMImage;
import frysk.dom.DOMSource;
import frysk.proc.Proc;
import frysk.scopes.SourceLocation;

public class LineXXX {
    private final SourceLocation sourceLocation;
    private final Proc proc;
    private DOMSource source;
    private DOMFunction function;
    private final File file;
    private final int line;
    private final int column;
  
    public File getFile () {
	return file;
    }
    
    public int getLine () {
	return line;
    }

    public int getColumn () {
	return column;
    }

    public SourceLocation getSourceLocation() {
	return sourceLocation;
    }

    public LineXXX(SourceLocation sourceLocation, Proc proc) {
	this.sourceLocation = sourceLocation;
	this.file = sourceLocation.getFile();
	this.line = sourceLocation.getLine();
	this.column = sourceLocation.getColumn();
	this.proc = proc;
    }

    public DOMFunction getDOMFunction () {
	if (this.function == null) {
	    if (this.source == null) {
		if (getDOMSource() == null)
		    return null;
	    }
	    
	    this.function = this.source.findFunction(this.getLine());
	}
	return this.function;
    }
  
    public DOMSource getDOMSource () {
	if (this.source == null) {
	    DOMFrysk dom = DOMFactory.getDOM(proc);
	    
	    if (dom == null)
		return null;
	    
	    DOMImage image = dom.getImage(this.proc.getMainTask().getName());
	    this.source = image.getSource(this.getFile().getName());
	    if (this.source == null || ! this.source.isParsed()) {
		// source has not been parsed, go put it in the DOM and
		// parse it
		try {
		    this.source = image.addSource(this.proc, this.sourceLocation,
						  DOMFactory.getDOM(this.proc));
		} catch (IOException ioe) {
		    System.err.println(ioe.getMessage());
		}
	    }
	}
	return this.source;
    }
    
}
