// This file is part of the program FRYSK.
//
// Copyright 2008 Red Hat Inc.
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

package frysk.util;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Locale;
import java.text.BreakIterator;
import java.text.StringCharacterIterator;

/**
 * This class is a PrintWriter which word-wraps the output.  It
 * provides settings which control over the number of columns and
 * indentation of wrapped lines.
 */
public class WordWrapWriter extends PrintWriter {
    // Number of columns available.
    private int columns;

    // Amount of indentation to use on wrapped line.
    private int wrapIndent;

    // The break iterator, used to find the next line-break
    // opportunity.
    private BreakIterator iter;

    // The current column.
    private int column;

    public WordWrapWriter(Writer outStream, int columns, int wrapIndent, Locale locale) {
	// Always enable auto-flush.
	super(outStream, true);
	this.columns = columns;
	this.wrapIndent = wrapIndent;
	this.iter = BreakIterator.getWordInstance(locale);
    }

    public WordWrapWriter(Writer outStream, int columns) {
	this(outStream, columns, 0, Locale.getDefault());
    }

    public WordWrapWriter(Writer outStream) {
	this(outStream, 72, 0, Locale.getDefault());
    }

    public void setColumns(int columns) {
	this.columns = columns;
    }

    public void setWrapIndent(int wrapIndent) {
	this.wrapIndent = wrapIndent;
    }

    public void setWrapIndentFromColumn() {
	this.wrapIndent = this.column;
    }

    public void write(char[] buf, int offset, int len) {
	// A bit inefficient but we don't care much.
	write(new String(buf, offset, len));
    }

    public void write(int b) {
	write(String.valueOf((char) b));
    }

    // Update 'column' assuming STR is printed.  We use a helper
    // function here to properly handle tabs.
    private void updateColumn(String str) {
	int len = str.length();
	for (int i = 0; i < len; ++i) {
	    if (str.charAt(i) == '\t') {
		column = 8 * ((column + 8) / 8);
	    } else {
		++column;
	    }
	}
    }

    public void write(String str, int offset, int len) {
	int term = offset + len;
	while (offset < term) {
	    // Find the next newline, if there is one.
	    int nlIndex = str.indexOf('\n', offset);
	    if (nlIndex >= term)
		nlIndex = -1;
	    // Only operate on the current line.  If we don't see a
	    // \n, operate on all the remaining text.
	    int subLen = (nlIndex < 0) ? len : (nlIndex - offset + 1);
	    iter.setText(new StringCharacterIterator(str, offset,
						     offset + subLen, offset));
	    int start = iter.first();
	    int end = iter.next();
	    boolean first = column == 0;
	    while (end != BreakIterator.DONE) {
		String word = str.substring(start, end);
		updateColumn(word);
		if (!first && column >= columns) {
		    super.write('\n');
		    for (int i = 0; i < wrapIndent; ++i)
			super.write(' ');
		    column = wrapIndent;
		    // The first word on a line should not start with
		    // a space.  We use '<=' to work like String.trim,
		    // and to eliminate tabs as well.
		    int j = 0;
		    while (j < word.length() && word.charAt(j) <= ' ')
			++j;
		    if (j > 0)
			word = word.substring(j);
		    updateColumn(word);
		}
		first = false;
		// Avoid recursion here...
		super.write(word, 0, word.length());
		start = end;
		end = iter.next();
	    }

	    if (nlIndex >= 0) {
		// We already wrote the \n.
		column = 0;
	    }

	    offset += subLen;
	    len -= subLen;
	}
    }
}
