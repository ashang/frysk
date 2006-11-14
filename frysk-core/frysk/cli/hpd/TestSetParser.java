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

import java.text.ParseException;
import frysk.junit.TestCase;

public class TestSetParser extends TestCase
{
	private String result;
	private SetNotationParser pr;
	private ParseTreeNode[] root;
	private	ParsedSet parsed;

	protected void setUp()
	{
		result = new String();
		pr = new SetNotationParser();
	}

	public void testReg()
	{
		result = "";
		String temp = "";

		try 
		{
			parsed = pr.parse("[!3.2:4, 2.3, 3:4.5]");
			root = parsed.getParseTreeNodes();

			for (int i = 0; i < root.length; i++)
			{
				walkTree(root[i]);
				temp += (result + " ");
				result = "";
			}
		}
		catch (ParseException e)
		{
			result = "Error";
		}

		assertEquals("3:3.2:4 2:2.3:3 3:4.5:5", temp.trim());
	}

	public void testRange()
	{
		result = "";
		try 
		{
			parsed = pr.parse("[! 2.5:3.*]");
			assertEquals(true, parsed.isStatic());
			root = parsed.getParseTreeNodes();
			walkTree(root[0]);
		}
		catch (ParseException e)
		{
			result = "Error";
		}
		assertEquals("Error", result);

		result = "";
		try 
		{
			parsed = pr.parse("[2.*:3.*]");
			root = parsed.getParseTreeNodes();
			walkTree(root[0]);
		}
		catch (ParseException e)
		{
			result = "Error";
		}
		assertEquals("2.-1:3.-1", result);
	
		result = "";
		try 
		{
			parsed = pr.parse("[*.5:3.*]");
			root = parsed.getParseTreeNodes();
			walkTree(root[0]);
		}
		catch (ParseException e)
		{
			result = "Error";
		}

		assertEquals("Error", result);
	}

	public void testState()
	{
		try
		{
			parsed = pr.parse("[runnable]");
			result = parsed.getName();
		}
		catch (ParseException e)
		{
			result = "Error";
		}

		assertEquals("runnable", result);
		assertEquals(ParsedSet.TYPE_STATE, parsed.getType());
	}

	public void testExec()
	{
		try
		{
			parsed = pr.parse("[exec(bash)]");
			result = parsed.getName();
		}
		catch (ParseException e)
		{
			result = "Error";
		}

		assertEquals("bash", result);
		assertEquals(ParsedSet.TYPE_EXEC, parsed.getType());
	}
	
	private void walkTree(ParseTreeNode node)
	{
		if (node.getLeft() != null)
			walkTree(node.getLeft());

		if (node.getType() == ParseTreeNode.TYPE_RANGE)
			result += (":");
		else if (node.getType() == ParseTreeNode.TYPE_REG)
		{
			if (node.isLeaf())
				result += (node.getID());
			else
				result += (".");
		}

		if (node.getRight() != null)
			walkTree(node.getRight());
	}
}
