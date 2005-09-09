package frysk.cli.hpd;

import java.util.Iterator;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.Enumeration;

/**
 * Preprocessor handles constructs like linebreaks and compound statements, also provides a couple of
 * static functions that might come in handy.
 */
public class Preprocessor 
{
	/**
	 * LineBuffer is a simple class for accumulating lines of multiline commands
	 */
	class LineBuffer 
	{
		String buffer;

		public LineBuffer()
		{
			buffer = new String("");
		}

		public void append(String str)
		{
			buffer += str;
		}

		public String flush()
		{
			String temp = buffer;
			buffer = "";

			return temp;
		}

		public boolean isEmpty()
		{
			boolean result = false;

			if (buffer.equals(""))
				result = true;

			return result;
		}
	}
	
	private LineBuffer buffer;

	/**
	 * Constructor
	 */
	public Preprocessor()
	{
		buffer = new LineBuffer();
	}

	/*
	 * Public methods.
	 */

	/**
	 * Preprocess the command - splitting, combining and replacing as needed
	 * @return An iterator of commands ready to be executed, empty iterator if no commands
	 */
	public Iterator preprocess(String cmd)
	{
		cmd = cmd.trim();
		Vector cmdQueue = new Vector();

		buffer.append(stripLineBreak(cmd));

		if (!isMultiline(cmd))
			cmdQueue = breakCompound(buffer.flush());

		return cmdQueue.iterator();
	}
	
	/**
	 * Remove the backslash from multiline command
	 */
	public static String stripLineBreak(String cmd)
	{
		String result = cmd;
		int i = cmd.indexOf('\\');

		if (i > 0 && isMultiline(cmd))
			result = result.substring(0, i);

		return result;
	}

	/**
	 * Break a compound command into subcommands
	 */
	public static Vector breakCompound(String cmd)
	{
		Vector result = new Vector();
		Enumeration enumer = new StringTokenizer(cmd.trim(), ";");

		while(enumer.hasMoreElements())
		{
			result.add(enumer.nextElement());
		}
		
		return result;
	}

	/**
	 * Check if this command is more than one command separated with a ;
	 */
	public static boolean isCompound(String cmd)
	{
		boolean result = false;

		if (cmd.indexOf(';') != -1)
			result = true;

		return result;
	}

	/**
	 * Check if this command spans multiple lines, that is ends with a '\'
	 */
	public static boolean isMultiline(String cmd)
	{
		boolean result = false;

		if (cmd.trim().indexOf('\\') == cmd.length()-1)
			result = true;

		return result;
	}
	
	/**
	 * Check if this command is a comment
	 */
	public static boolean isComment(String cmd)
	{
		boolean result = false;

		if (!cmd.equals("") && cmd.trim().charAt(0) == '#')
			result = true;

		return result;
	}
}
