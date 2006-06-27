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

import java.lang.Character;
import java.lang.Integer;
import java.util.Vector;
import java.text.ParseException;

class SetNotationParser
{
	private int curToken;
	private String[] tokens;
	private String notation;

	public SetNotationParser()
	{
	}

	/**
	 * Parse a process/thread set notation.
	 * @param set The string notation of the set to parse, should include brackets.
	 * @return An array of ParseTreeNode objects, that look like cli/hpd/doc-files/parsetree.png
	 */
	public ParsedSet parse(String set) throws ParseException
	{
		ParsedSet result;
		set = set.replaceAll(" +", "");
		String setnobr = set.substring(1,set.length()-1); // the set with brackets removed
		boolean isstatic = false;

		if (setnobr.charAt(0) == '!')
		{
			isstatic = true;
			setnobr = set.substring(1);
		}
		
		if (setnobr.matches("\\w*"))
		{
			if (setnobr.equals("running") ||
				setnobr.equals("stopped") ||
				setnobr.equals("runnable") ||
				setnobr.equals("held"))
				result = new ParsedSet(ParsedSet.TYPE_STATE, setnobr, isstatic);
			else
				result = new ParsedSet(ParsedSet.TYPE_NAMED, setnobr, isstatic);
		}
		else if (setnobr.matches("exec(.*)"))
		{
			result = new ParsedSet(ParsedSet.TYPE_EXEC, setnobr.substring(5, setnobr.length()-1), isstatic);
		}
		else
		{
			Vector root = new Vector();

			notation = set;
			curToken = 0;
			tokenize();

			S_1(root); //call first production

			result = new ParsedSet( (ParseTreeNode[]) root.toArray(new ParseTreeNode[0]), isstatic);
		}

		return result;
	}
/*
	private static boolean setIsStatic(String set)
	{
		set = set.trim();
		set = set.replaceAll(" *","");

		if (set.charAt(1) == '!')
			return true;
		else
			return false;
	}
*/

	/*
	 * We don't need a lexer, screw that
	 */
	private void tokenize()
	{
		Vector tokens = new Vector();
		String buf = "";
		char ch = ' ';

		for (int i = 0; i < notation.length(); i++)
		{
			ch = notation.charAt(i);
			if (Character.isDigit(ch))
			{
				buf += ch;
			}
			else
			{
				if (!buf.equals(""))
				{
					tokens.add(buf);
					buf = "";
				}

				if (ch == ' ')
					continue;

				tokens.add(Character.toString(ch));
			}
		}

		this.tokens = new String[tokens.size()]; // convert vector to array so we don't have to cast all the time
		for (int i = 0; i < tokens.size(); i++)
		{
			this.tokens[i] = (String)tokens.get(i);
		}
	}

	/*
	 * This grammar might not be nice, but I have trouble coming up with anything better and this works.
	 * S_1 -> [S_2] | [!S_2]
	 * S_2 -> S_3.S_3S_6 | S_4:S_4S_6 // takes nasty looking-ahead
	 * S_3 -> num:num | num | *
	 * S_4 -> S_5.S_5
	 * S_5 -> num | *
	 * S_6 -> ,S_2 | 
	 *
	 * More hacks: things like *.3:3.5 do not make sense and *.3:*.5 does,
	 * but handling them in the grammar would be a pain, so they're simply
	 * hacked in the code.
	 * Same goes for determening that there're no ranges from a higher value to a lower one.
	 */
	
	/*
	 * S_1 -> [S_2] | [!S_2]
	 */

	/**
	 * The first rule of the grammar, creates a tree that grows from "root"
	 * and with process id and range on depth 1, and the same for task on depth 2.
	 */
	private void S_1(Vector root) throws ParseException
	{
		if (tokens[curToken].equals("["))
			curToken++;
		else
			throw new ParseException("Missing opening bracket of p/t set notation", curToken);

		if (tokens[curToken].equals("!"))
			curToken++;

		S_2(root);

		if (tokens[curToken].equals("]"))
			curToken++;
		else
			throw new ParseException("Missing closing bracket of p/t set notation", curToken);
	}

	/*
	 * S_2 -> S_3.S_3S_6 | S_4:S_4S_6 // takes nasty looking-ahead
	 * Also checks validity of range notation
	 */
	private void S_2(Vector root) throws ParseException
	{
		ParseTreeNode node = null;
		int[] tempIDs = new int[4];

		// we know that the second production with yield num.num:num.num, and
		// voila:
		if (curToken+3 < tokens.length && curToken+5 < tokens.length
			&& tokens[curToken+3].equals(":") && tokens[curToken+5].equals(".")) 
		{
			node = new ParseTreeNode(ParseTreeNode.TYPE_RANGE);

			node.setLeft(S_4());

			if (tokens[curToken].equals(":"))
				curToken++;
			else
				throw new ParseException("Erroneous p/t set notation, '.' expected", curToken);

			node.setRight(S_4());

			// makes sure that there's no nonsense like *.3:4.5
			// only the following forms are valid:
			// *.a:*.b ; a.*:b.* ; a.*:b.c ; a.b:c.*
			tempIDs[0] = node.getLeft().getLeft().getID();
			tempIDs[1] = node.getLeft().getRight().getID();
			tempIDs[2] = node.getRight().getLeft().getID();
			tempIDs[3] = node.getRight().getRight().getID();
			if (((tempIDs[0] == -1 || tempIDs[2] == -1) && tempIDs[0] != tempIDs[2]) || //no *.a:b.c or a.b:*.c
				((tempIDs[1] == -1 || tempIDs[3] == -1) && tempIDs[1] != tempIDs[3]) || //no a.*:b.c or a.b:c.*
				(tempIDs[0] == -1 && tempIDs[0] == tempIDs[1]) || // no	*.*:a.b
				(tempIDs[2] == -1 && tempIDs[2] == tempIDs[3])) // no	a.b:*.*
				throw new ParseException("Erroneous p/t set notation, erroneous use of wildcard", curToken);

			// check ranges
			if ((tempIDs[2] < tempIDs[0]) ||
				((tempIDs[0] == tempIDs[2]) && (tempIDs[3] < tempIDs[1])))
				throw new ParseException("Erroneous p/t set notation, illegal range", curToken);
		}
		else 
		{
			node = new ParseTreeNode(ParseTreeNode.TYPE_REG);
			
			node.setLeft(S_3());

			if (tokens[curToken].equals("."))
				curToken++;
			else
				throw new ParseException("Erroneous p/t set notation, '.' expected", curToken);

			node.setRight(S_3());

			//Check that ranges go from low to high
			tempIDs[0] = node.getLeft().getLeft().getID();
			tempIDs[1] = node.getLeft().getRight().getID();
			tempIDs[2] = node.getRight().getLeft().getID();
			tempIDs[3] = node.getRight().getRight().getID();

			if ((tempIDs[1] < tempIDs[0]) || (tempIDs[3] < tempIDs[2]))
				throw new ParseException("Erroneous p/t set notation, illegal range", curToken);
		}

		root.add(node);
		S_6(root);
	}

	/*
	 * S_3 -> num:num | num | *
	 * Also checks validity of the notation
	 */
	private ParseTreeNode S_3() throws ParseException 
	{
		ParseTreeNode node = new ParseTreeNode(ParseTreeNode.TYPE_RANGE);

		if (tokens[curToken].matches("\\d+"))
		{
			node.setLeft(new ParseTreeNode(Integer.parseInt(tokens[curToken]), ParseTreeNode.TYPE_REG));
			node.setRight(new ParseTreeNode(Integer.parseInt(tokens[curToken]), ParseTreeNode.TYPE_REG));
			curToken++;

			if (tokens[curToken].equals(":"))
			{
				curToken++;

				if (tokens[curToken].matches("\\d+"))
				{
					node.setRight(new ParseTreeNode(Integer.parseInt(tokens[curToken]), ParseTreeNode.TYPE_REG));
					curToken++;
					
					if (node.getRight().getID() < node.getLeft().getID())
						throw new ParseException("Erroneous p/t set notation, illegal range", curToken);
				}
				else
					throw new ParseException("Erroneous p/t set notation, non-negative integer expected", curToken);
			}
		}
		else if (tokens[curToken].equals("*"))
		{
			node.setLeft(new ParseTreeNode(ParseTreeNode.TYPE_REG));
			node.setRight(new ParseTreeNode(ParseTreeNode.TYPE_REG));
			curToken++;
		}
		else
		{
			throw new ParseException("Erroneous p/t set notation, non-negative integer or '*' expected", curToken);
		}


		return node;
	}

	/*
	 * S_4 -> S_5.S_5
	 */
	private ParseTreeNode S_4() throws ParseException
	{
		ParseTreeNode node = new ParseTreeNode(ParseTreeNode.TYPE_REG);

		node.setLeft(S_5()); //add a process node as a child

		if (tokens[curToken].equals("."))
			curToken++;
		else
			throw new ParseException("Erroneous p/t set notation, '.' expected", curToken);

		node.setRight(S_5());
		return node;
	}

	/*
	 * S_5 -> num | *
	 */
	private ParseTreeNode S_5() throws ParseException
	{
		ParseTreeNode node = null;

		if (tokens[curToken].matches("\\d+"))
		{
			node = new ParseTreeNode(Integer.parseInt(tokens[curToken]), ParseTreeNode.TYPE_REG);
			curToken++;
		}
		else if (tokens[curToken].equals("*"))
		{
			node = new ParseTreeNode(-1, ParseTreeNode.TYPE_REG);
			curToken++;
		}
		else
			throw new ParseException("Erroneous p/t set notation, non-negative integer or '*' expected", curToken);

		return node;
	}

	/*
	 * S_6 -> ,S_2 | empty_string
	 */
	private void S_6(Vector root) throws ParseException
	{
		if (curToken != tokens.length-1)
		{
			if (tokens[curToken].matches(","))
				curToken++;
			else
				throw new ParseException("Erroneous p/t set notation, ',' expected", curToken);

			S_2(root);
		}
	}
}
