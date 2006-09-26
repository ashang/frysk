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
package jline;

import java.io.*;

import junit.framework.*;


public abstract class JLineTestCase
	extends TestCase
{
	ConsoleReader console;


	public JLineTestCase (String test)
	{
		super (test);
	}


	public void setUp ()
		throws Exception
	{
		super.setUp ();
		console = new ConsoleReader (null, new PrintWriter (
			new OutputStreamWriter (new ByteArrayOutputStream ())));
	}


	public void assertBuffer (String expected, Buffer buffer)
		throws IOException
	{
		assertBuffer (expected, buffer, true);
	}


	public void assertBuffer (String expected, Buffer buffer, boolean clear)
		throws IOException
	{
		// clear current buffer, if any
		if (clear)
		{
			console.finishBuffer ();
			console.getHistory ().clear ();
		}

		console.setInput (new ByteArrayInputStream (buffer.getBytes ()));

		// run it through the reader
		while (console.readLine ((String)null) != null);

		assertEquals (expected, console.getCursorBuffer ().toString ());
	}


	private int getKeyForAction (short logicalAction)
	{
		int action = console.getKeyForAction (logicalAction);
		if (action == -1)
			fail ("Keystroke for logical action " + logicalAction
				+ " was not bound in the console");
		return action;
	}


	class Buffer
	{
		private final ByteArrayOutputStream bout = new ByteArrayOutputStream ();

		public Buffer ()
		{
		}


		public Buffer (String str)
		{
			append (str);
		}


		public byte [] getBytes ()
		{
			return bout.toByteArray ();
		}


		public Buffer op (short operation)
		{
			return append (getKeyForAction (operation));
		}


		public Buffer ctrlA ()
		{
			return append (getKeyForAction (ConsoleReader.MOVE_TO_BEG));
		}


		public Buffer ctrlU ()
		{
			return append (getKeyForAction (ConsoleReader.KILL_LINE_PREV));
		}


		public Buffer tab ()
		{
			return append (getKeyForAction (ConsoleReader.COMPLETE));
		}


		public Buffer back ()
		{
			return append (getKeyForAction (ConsoleReader.DELETE_PREV_CHAR));
		}


		public Buffer left ()
		{
			return append (UnixTerminal.ARROW_START)
				.append (UnixTerminal.ARROW_PREFIX)
				.append (UnixTerminal.ARROW_LEFT);
		}


		public Buffer right ()
		{
			return append (UnixTerminal.ARROW_START)
				.append (UnixTerminal.ARROW_PREFIX)
				.append (UnixTerminal.ARROW_RIGHT);
		}


		public Buffer up ()
		{
			return append (UnixTerminal.ARROW_START)
				.append (UnixTerminal.ARROW_PREFIX)
				.append (UnixTerminal.ARROW_UP);
		}


		public Buffer down ()
		{
			return append (UnixTerminal.ARROW_START)
				.append (UnixTerminal.ARROW_PREFIX)
				.append (UnixTerminal.ARROW_DOWN);
		}


		public Buffer append (String str)
		{
			byte [] bytes = str.getBytes ();

			for (int i = 0; i < bytes.length; i++)
			{
				append (bytes [i]);
			}

			return this;
		}


		public Buffer append (int i)
		{
			return append ((byte)i);
		}


		public Buffer append (byte b)
		{
			bout.write (b);

			return this;
		}
	}
}
