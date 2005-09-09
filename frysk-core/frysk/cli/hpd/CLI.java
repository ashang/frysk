package frysk.cli.hpd;

import jline.*;
import java.io.*;
import java.util.*;
import java.text.ParseException;

public class CLI 
{
	/*
	 * Command handlers
	 */

	class LoadHandler implements CommandHandler
	{
		public void handle(String cmd) throws ParseException {
			out.println("Executing load: " + cmd);
		}
	}
	class RunHandler implements CommandHandler
	{
		public void handle(String cmd) throws ParseException {
			out.println("Executing run: " + cmd);
		}
	}
	class AliasHandler implements CommandHandler 
	{
		public void handle(String cmd) throws ParseException {
			String arguments = ParserHelper.extractArguments(cmd);
			Vector tokens = ParserHelper.tokenize(arguments); 
			out.println(tokens);
		}
	}
	class UnaliasHandler implements CommandHandler
	{
		public void handle(String cmd)  throws ParseException {
			out.println("Executing unalias: " + cmd);
		}
	}
	class SetHandler implements CommandHandler
	{
		public void handle(String cmd) throws ParseException {
			out.println("Executing set: " + cmd);
		}
	}
	class UnsetHandler implements CommandHandler
	{
		public void handle(String cmd) throws ParseException {
			out.println("Executing unset: " + cmd);
		}
	}

	/*
	 * Private variables
	 */

	private static final PrintStream out = System.out;
	private Preprocessor prepro;
	private String prompt;
	private Hashtable handlers;
	private Hashtable aliases;

	/*
	 * Public methods
	 */

	/**
	 * Constructor
	 * @param prompt String initialy to be used as the prompt
	 */
	public CLI(String prompt)
	{
		this.prompt = prompt;
		prepro = new Preprocessor();
		handlers = new Hashtable();

		handlers.put("load", new LoadHandler());
		handlers.put("run", new RunHandler());
		handlers.put("alias", new AliasHandler());
		handlers.put("unalias", new UnaliasHandler());
		handlers.put("set", new SetHandler());
		handlers.put("unset", new UnsetHandler());
	}

	public String getPrompt()
	{
		return prompt;
	}

	public String execCommand(String cmd)
	{
		String pcmd = ""; //preprocessed command
		CommandHandler handler = null;

		if (cmd != null)
		{
			for (Iterator iter = prepro.preprocess(cmd); iter.hasNext();)
			{
 				pcmd = (String)iter.next();
				
				handler = (CommandHandler)handlers.get(ParserHelper.extractAction(pcmd));

				if (handler != null)
				{
					try	{
						handler.handle(pcmd);
					}
					catch (ParseException e) {
						System.out.println("Parse Exception: " + e.getMessage());
					}
				}
				else
				{
					// SHOULD DO SOMETHING ELSE HERE
					out.println("ERROR: Unrecognized command \"" +
							ParserHelper.extractAction(pcmd) + "\"");
				}
			}
		}

		return null;
	}

	/**
	 * Main function
	 */
	public static void main(String[] args)
	{
		CLI dbg = new CLI("cli$ ");
		ConsoleReader reader = null; // the jline reader
		String line = "";

		try {
			reader = new ConsoleReader();
		}
		catch (IOException ioe)	{
			out.println("ERROR: Could not create a command line");
			out.print(ioe.getMessage());
		}

		try {
			while (line != null && !line.equals("quit")) {
				line = reader.readLine(dbg.getPrompt());
				dbg.execCommand(line);
			}

		}
		catch (IOException ioe) {
			out.println("ERROR: Could not read from command line");
			out.print(ioe.getMessage());
		}

	}
}
