package frysk.cli.hpd;

import java.text.ParseException;
import java.util.Vector;

public class SetParserTest
{
	static String result = new String();

	public static void main(String[] args)
	{
		SetNotationParser pr = new SetNotationParser();
		Vector root;
		String temp = "";

		try 
		{
			root = pr.parse("[!3.2:4, 2.3, 3:4.5]");
			for (int i = 0; i < root.size(); i++)
			{
				walkTree((PTNode)root.elementAt(i));
				temp += (result + " ");
			}
		}
		catch (ParseException e)
		{
			System.out.println(e);
			result = "Error";
		}

		try 
		{
			root = pr.parse("[!2.5:3.*]");
			walkTree((PTNode)root.elementAt(0));
		}
		catch (ParseException e)
		{
			System.out.println(e);
			result = "Error";
		}

		try 
		{
			root = pr.parse("[*.5:3.*]");
			walkTree((PTNode)root.elementAt(0));
		}
		catch (ParseException e)
		{
			System.out.println(e);
			result = "Error";
		}
	}

	
	private static void walkTree(PTNode node)
	{
		if (node.getLeft() != null)
			walkTree(node.getLeft());

		if (node.getType() == PTNode.TYPE_RANGE)
			result += (":");
		else if (node.getType() == PTNode.TYPE_REG)
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
