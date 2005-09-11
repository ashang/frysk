// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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
package frysk.cli.hpd;

import java.util.Vector;
import java.util.Arrays;
import java.text.ParseException;

/**
 * ParserHelper provides miscellaneous helper methods for handling commands
 */
public class ParserHelper
{

	/**
	 * Tokenize the string (probably command) minding quoted statements
	 * @return Vector of string tokens
	 */
	public static Vector tokenize(String str) throws ParseException
	{
		Vector result = new Vector();
		str = str.trim();
		str = str.replaceAll(" +", " ");
		str = str.replaceAll(" *\" *", "\"");

		boolean needQuote = false;
		int qindex = -1;
		for (int i = 0; i < str.length(); i++)
		{
			if (str.charAt(i) == '\"')
			{
				if (needQuote)
				{
					result.add(str.substring(qindex+1, i));
					needQuote = false;
				}
				else
				{
					result.addAll(Arrays.asList(str.substring(qindex+1, i).split(" ")));
					needQuote = true;
				}
				qindex = i;
			}
			else if (i == str.length()-1)
			{
				if (needQuote)
					throw new ParseException("Unmatched quote.", i);
				else
					result.addAll(Arrays.asList(str.substring(qindex+1, i+1).split(" ")));
			}
		}

		return result;
	}

	public static String stripPTSet(String cmd) 
	{
		cmd = cmd.trim();
		String result = "";

		result = cmd.replaceFirst("^\\[.*\\]", "");

		return result;
	}

	public static String extractPTSet(String cmd) throws ParseException
	{
		cmd = cmd.trim();
		int openIndex = cmd.indexOf("[");
		int closeIndex = cmd.indexOf("]");
		String result = "";

		if ((openIndex == -1 && closeIndex != -1) || (openIndex != -1 && closeIndex == -1) || (openIndex < closeIndex))
			throw new ParseException("Unmatched bracket", openIndex);
		else if (openIndex == 0)
			result = cmd.substring(openIndex,closeIndex+1);

		return result;
	}

	public boolean setIsStatic(String set)
	{
		boolean result = false;

		set = set.trim();

		if (set.charAt(0) == '[' && set.charAt(1) == '!')
			result = true;
			
		return result;
	}

	/**
	 * Extract the actual command (action) without parameters, right now the
	 * first token.
	 * Make sure the whole command has been preprocessed.
	 */
	public static String extractAction(String cmd) 
	{
		String result = "";
		String[] tokens = cmd.trim().split(" ");

		if (tokens.length > 0)
			result = tokens[0];
		else
			result = "";

		return result;
	}

	/**
	 * Extract the actual command (action) without parameters.
	 * Make sure the whole command has been preprocessed.
	 */
	public static String extractArguments(String cmd)
	{
		String result = "";
		String[] tokens = cmd.trim().split(" ", 2);

		if (tokens.length > 1)
			result = tokens[1];

		return result;
	}
}
