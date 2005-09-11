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
