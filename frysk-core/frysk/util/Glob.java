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

package frysk.util;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Glob {

    private static int matchCharacterClass(char[] glob, int from)
	throws PatternSyntaxException
    {
	int i = from + 2;
	while (glob[++i] != ':' && i < glob.length)
	    continue;
	if (i >= glob.length || glob[++i] != ']')
	    throw new PatternSyntaxException
		("Unmatched '['.", new String(glob), from);
	return i;
    }

    private static int matchBrack(char[] glob, int from)
	throws PatternSyntaxException
    {
	int i = from + 1;

	// Complement operator.
	if (glob[i] == '^')
	    ++i;
	else if (glob[i] == '!')
	    glob[i++] = '^';

	// On first character, both [ and ] are legal.  But when [ is
	// foolowed with :, it's character class.
	if (glob[i] == '[' && glob[i + 1] == ':')
	    i = matchCharacterClass(glob, i) + 1;
	else
	    ++i; // skip any character, including [ or ]
	boolean escape = false;
	for (; i < glob.length; ++i) {
	    char c = glob[i];
	    if (escape) {
		++i;
		escape = false;
	    }
	    else if (c == '[' && glob[i + 1] == ':')
		i = matchCharacterClass(glob, i);
	    else if (c == ']')
		return i;
	}
	throw new PatternSyntaxException
	    ("Unmatched '" + glob[from] + "'.", new String(glob), from);
    }

    // Package private so that TestGlob can access it.
    static String toRegex(char[] glob) {
	StringBuffer buf = new StringBuffer();
	boolean escape = false;
	for(int i = 0; i < glob.length; ++i) {
	    char c = glob[i];
	    if (escape) {
		if (c == '\\')
		    buf.append("\\\\");
		else if (c == '*')
		    buf.append("\\*");
		else if (c == '?')
		    buf.append('?');
		else
		    buf.append('\\').append(c);
		escape = false;
	    }
	    else {
		if (c == '\\')
		    escape = true;
		else if (c == '[') {
		    int j = matchBrack(glob, i);
		    buf.append(glob, i, j - i + 1);
		    i = j;
		}
		else if (c == '*')
		    buf.append(".*");
		else if (c == '?')
		    buf.append('.');
		else if (c == '.')
		    buf.append("\\.");
		else
		    buf.append(c);
	    }
	}
	return buf.toString();
    }

    public static Pattern compile(String glob) {
	return Pattern.compile(toRegex(glob.toCharArray()));
    }

    public static Pattern compile(String glob, int flags) {
	return Pattern.compile(toRegex(glob.toCharArray()), flags);
    }
}
