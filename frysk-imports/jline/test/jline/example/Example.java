/**
 *	jline - Java console input library
 *	Copyright (c) 2002, 2003, 2004, 2005, Marc Prud'hommeaux mwp1@cornell.edu
 *	All rights reserved.
 *
 *	Redistribution and use in source and binary forms, with or
 *	without modification, are permitted provided that the following
 *	conditions are met:
 *
 *	Redistributions of source code must retain the above copyright
 *	notice, this list of conditions and the following disclaimer.
 *
 *	Redistributions in binary form must reproduce the above copyright
 *	notice, this list of conditions and the following disclaimer
 *	in the documentation and/or other materials provided with
 *	the distribution.
 *
 *	Neither the name of JLine nor the names of its contributors
 *	may be used to endorse or promote products derived from this
 *	software without specific prior written permission.
 *
 *	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *	"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 *	BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 *	AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 *	EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *	FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 *	OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *	PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *	DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 *	AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *	LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 *	IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 *	OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jline.example;

import jline.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class Example
{
	public static void usage ()
	{
		System.out.println ("Usage: java " + Example.class.getName ()
			+ " [none/simple/files/dictionary [trigger mask]]");
		System.out.println ("  none - no completors");
		System.out.println ("  simple - a simple completor that comples "
			+ "\"foo\", \"bar\", and \"baz\"");
		System.out.println ("  files - a completor that comples "
			+ "file names");
		System.out.println ("  dictionary - a completor that comples "
			+ "english dictionary words");
		System.out.println ("  classes - a completor that comples "
			+ "java class names");
		System.out.println ("  trigger - a special word which causes it to assume "
				+ "the next line is a password");
		System.out.println ("  mask - is the character to print in place of "
				+ "the actual password character");
		System.out.println ("\n  E.g - java Example simple su '*'\n"
				+ "will use the simple compleator with 'su' triggering\n"
				+ "the use of '*' as a password mask.");
	}


	public static void main (String [] args)
		throws IOException
	{
		Character mask = null;
		String trigger = null;
		
		ConsoleReader reader = new ConsoleReader ();
		reader.setDebug (new PrintWriter (new FileWriter ("writer.debug",
			true)));

		if (args == null || args.length == 0)
		{
			usage ();
			return;
		}

		List completors = new LinkedList ();
		if (args.length > 0) 
		{			
			if (args [0].equals ("none"))
			{
			}
			else if (args [0].equals ("files"))
			{
				completors.add (new FileNameCompletor ());
			}
			else if (args [0].equals ("classes"))
			{
				completors.add (new ClassNameCompletor ());
			}
			else if (args [0].equals ("dictionary"))
			{
				completors.add (
					new SimpleCompletor (new GZIPInputStream (
						Example.class.getResourceAsStream ("english.gz"))));
			}
			else if (args [0].equals ("simple"))
			{
				completors.add (
 		 			new SimpleCompletor (
						new String [] { "foo", "bar", "baz"}));
			}
			else
			{
				usage ();
				return;
			}
		}
		
		if (args.length == 3) 
		{
			mask = new Character(args[2].charAt(0));
			trigger = args[1];
		}
		
		reader.addCompletor (new ArgumentCompletor (completors));
					

		String line;
		PrintWriter out = new PrintWriter (System.out);
	
		while ((line = reader.readLine ("prompt> ")) != null)
		{
			out.println ("======>\"" + line + "\"");
			out.flush ();
			// If we input the special word then we will mask
			// the next line.
			if (trigger != null && line.compareTo(trigger) == 0) {
				line = reader.readLine("password> ", mask);
			}
		}
	}
}
