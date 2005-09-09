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
