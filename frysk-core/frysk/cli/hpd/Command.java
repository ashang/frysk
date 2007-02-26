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

import java.util.ArrayList;
import java.text.ParseException;
import java.util.Arrays;

// TODO: This is not a very good class, the lexing is primitive (and doesn't work well in
// some instances). Add more commandline parsing features to it.

/**
 * Command class separates and contains different parts of a command: set, action, parameters.
 * It is immutable.
 */
class Command
{
	private String myFullCommand;
	private String mySet;
	private String myAction;
	private ArrayList myParameters;

	/**
	 * The constructor.
	 * @param cmd the full preprocessed command in the form defined in HPDF: 
	 * 		[p/t-set/prefix] verb object [option ...] [-option [value] ...]
	 * @exception ParseException thrown if error are encountered during parsing
	 */
	public Command(String cmd) throws ParseException
	{
		myFullCommand = cmd;
		mySet = null;
		myAction = null;
		myParameters = new ArrayList();

		ArrayList tokens = tokenize(myFullCommand);
		String tempToken;

		for (int i = 0; i < tokens.size(); i++)
		{
			tempToken = (String) tokens.get(i);

			// first token is either p/t-set or an action
			if (i == 0)
			{
				if (tempToken.startsWith("[") && tempToken.endsWith("]")) // if p/t-set
					mySet = tempToken;
				else
					myAction = tempToken;
			}
			// if this is second token and myAction is null this must be an action
			else if ( i == 1 && myAction == null) 
			{
				myAction = tempToken;
			}
			else
			{
				myParameters.add(tempToken);
			}
		}
	}

	public String getFullCommand()
	{
		return myFullCommand;
	}

	public String getSet()
	{
		return mySet;
	}

	public String getAction()
	{
		return myAction;
	}

	public ArrayList getParameters()
	{
		return myParameters;
	}

	public String toString()
	{
		return myFullCommand;
	}

	/**
	 * Tokenize a string (probably command) minding quoted statements
	 * @return ArrayList of string tokens
	 */
	// might be a little odd that it takes a parameter,
	// but it used to be a static function and might be later
	private ArrayList tokenize(String str) throws ParseException
	{
		ArrayList result = new ArrayList();
		str = str.trim();
		str = str.replaceAll(" +", " ");
		str = str.replaceAll(" *\" *", "\"");
		str = str.replaceAll(" *\\[ *", "[");
		str = str.replaceAll(" *\\] *", "]");

		// a kinda lexing state machine, sort of
		int tokBegin = 0;

		boolean needQuote = false;
		boolean needBracket = false;

		for (int i = 0; i < str.length(); i++)
		{
			if (str.charAt(i) == '\"')
			{
				if (needQuote)
				{
					result.add(str.substring(tokBegin, i));
					tokBegin = i+1;
					needQuote = false;
				}
				else
				{
					result.add(str.substring(tokBegin, i));
					tokBegin = i+1;
					needQuote = true;
				}
			}
			else if (str.charAt(i) == '[')
			{
				if (i != 0)
					result.add(str.substring(tokBegin, i));
				tokBegin = i;
				needBracket = true;
			}
			else if (str.charAt(i) == ']')
			{
				result.add(str.substring(tokBegin, i+1));
				tokBegin = i+1;
				needBracket = false;
			}
			else if (str.charAt(i) == ' ')
			{
				if (!needQuote && !needBracket)
				{
					result.add(str.substring(tokBegin, i));
					tokBegin = i+1;
				}
			}
			else if (i == str.length()-1)
			{
				if (needQuote)
					throw new ParseException("Unmatched quote.", i);
				else if (needBracket)
					throw new ParseException("Unmatched bracket.", i);
				else
					result.addAll(Arrays.asList(str.substring(tokBegin, i+1).split(" ")));
			}
		}

		return result;
	}

}
