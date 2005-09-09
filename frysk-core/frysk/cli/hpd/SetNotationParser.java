package frysk.cli.hpd;

import java.lang.Character;
import java.lang.Integer;
import java.util.Vector;
import java.text.ParseException;

public class SetNotationParser
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
	 * @return A tree with a meaningless root, processes at depth 1 and their threads at depth 2.
	 */
	public Vector parse(String set) throws ParseException
	{
		Vector root = new Vector();

		notation = set;
		curToken = 0;
		tokenize();

		S_1(root); //call first production
		// expandRanges(root);

		return root;
	}

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

		this.tokens = new String[tokens.size()]; // convert vector to array so we don't have to cast all the fucking time
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
	 * Same goes for determening that there're no ranges from one value to a lower one.
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
	 */
	private void S_2(Vector root) throws ParseException
	{
		PTNode node = null;
		int[] tempIDs = new int[4];

		// we know that the second production with yield num.num:num.num, and
		// voila:
		if (curToken+3 < tokens.length && curToken+5 < tokens.length
			&& tokens[curToken+3].equals(":") && tokens[curToken+5].equals(".")) 
		{
			node = new PTNode(PTNode.TYPE_RANGE);

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
			if (((tempIDs[0] == -1 || tempIDs[2] == -1) && tempIDs[0] != tempIDs[2]) || 
				(tempIDs[0] == -1 && tempIDs[0] == tempIDs[1]) || // no	*.*:a.b
				(tempIDs[2] == -1 && tempIDs[2] == tempIDs[3]))  // no	a.b:*.*
				throw new ParseException("Erroneous p/t set notation, erroneous use of wildcard", curToken);

			// check ranges
			if ((tempIDs[2] < tempIDs[0]) ||
				((tempIDs[0] == tempIDs[2]) && (tempIDs[3] < tempIDs[1])))
				
			throw new ParseException("Erroneous p/t set notation, illegal range", curToken);
		}
		else 
		{
			node = new PTNode(PTNode.TYPE_REG);
			
			node.setLeft(S_3());

			if (tokens[curToken].equals("."))
				curToken++;
			else
				throw new ParseException("Erroneous p/t set notation, '.' expected", curToken);

			node.setRight(S_3());
		}

		root.add(node);
		S_6(root);
	}

	/*
	 * S_3 -> num:num | num | *
	 */
	private PTNode S_3() throws ParseException 
	{
		PTNode node = new PTNode(PTNode.TYPE_RANGE);

		if (tokens[curToken].matches("\\d+"))
		{
			node.setLeft(new PTNode(Integer.parseInt(tokens[curToken]), PTNode.TYPE_REG));
			node.setRight(new PTNode(Integer.parseInt(tokens[curToken]), PTNode.TYPE_REG));
			curToken++;

			if (tokens[curToken].equals(":"))
			{
				curToken++;

				if (tokens[curToken].matches("\\d+"))
				{
					node.setRight(new PTNode(Integer.parseInt(tokens[curToken]), PTNode.TYPE_REG));
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
			node.setLeft(new PTNode(PTNode.TYPE_REG));
			node.setRight(new PTNode(PTNode.TYPE_REG));
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
	private PTNode S_4() throws ParseException
	{
		PTNode node = new PTNode(PTNode.TYPE_REG);

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
	private PTNode S_5() throws ParseException
	{
		PTNode node = null;

		if (tokens[curToken].matches("\\d+"))
		{
			node = new PTNode(Integer.parseInt(tokens[curToken]), PTNode.TYPE_REG);
			curToken++;
		}
		else if (tokens[curToken].equals("*"))
		{
			node = new PTNode(-1, PTNode.TYPE_REG);
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
