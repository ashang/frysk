/**
 *	jline - Java console input library
 *	Copyright (c) 2002, 2003, 2004, 2005, Marc Prud'hommeaux <mwp1@cornell.edu>
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
import java.util.*;


/**
 *  <p>
 *  Terminal that is used for unix platforms. Terminal initialization
 *  is handled by issuing the <em>stty</em> command against the
 *  <em>/dev/tty</em> file to disable character echoing and enable
 *  character input. All known unix systems (including
 *  Linux and Macintosh OS X) support the <em>stty</em>), so this
 *  implementation should work for an reasonable POSIX system.
 *	</p>
 *
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class PtyTerminal
	extends Terminal
{
	public static final short ARROW_START           = 27;
	public static final short ARROW_PREFIX          = 91;
	public static final short ARROW_LEFT            = 68;
	public static final short ARROW_RIGHT           = 67;
	public static final short ARROW_UP              = 65;
	public static final short ARROW_DOWN            = 66;

	private Map terminfo;
	private int width = -1;
	private int height = -1;
	private String ptyname = null;


	public PtyTerminal(String ptyname)
	{
		this.ptyname = ptyname;
	}

	/**
	 *  Remove line-buffered input by invoking "stty -icanon min 1"
	 *  against the current terminal.
	 */
	public void initializeTerminal ()
		throws IOException, InterruptedException
	{
		// save the initial tty configuration
		final String ttyConfig = stty ("-g");

		// sanity check
		if (ttyConfig.length () == 0
			|| (ttyConfig.indexOf ("=") == -1
			&& ttyConfig.indexOf (":") == -1))
		{
			throw new IOException ("Unrecognized stty code: " + ttyConfig);
		}


		// set the console to be character-buffered instead of line-buffered
		stty ("-icanon min 1");

		// disable character echoing
		stty ("-echo");

		// at exit, restore the original tty configuration (for JDK 1.3+)
		try
		{
			Runtime.getRuntime ().addShutdownHook (new Thread ()
			{
				public void start ()
				{
					try
					{
						stty (ttyConfig);
					}
					catch (Exception e)
					{
						consumeException (e);
					}
				}
			});
		}
		catch (AbstractMethodError ame)
		{
			// JDK 1.3+ only method. Bummer.
			consumeException (ame);
		}
	}


	public int readVirtualKey (InputStream in)
		throws IOException
	{
		int c = readCharacter (in);

		// in Unix terminals, arrow keys are represented by
		// a sequence of 3 characters. E.g., the up arrow
		// key yields 27, 91, 68
		if (c == ARROW_START)
		{
			c = readCharacter (in);
			if (c == ARROW_PREFIX)
			{
				c = readCharacter (in);
				if (c == ARROW_UP)
					return CTRL_P;
				else if (c == ARROW_DOWN)
					return CTRL_N;
				else if (c == ARROW_LEFT)
					return CTRL_B;
				else if (c == ARROW_RIGHT)
					return CTRL_F;
			}
		}


		return c;
	}


	/** 
	 *  No-op for exceptions we want to silently consume.
	 */
	private void consumeException (Throwable e)
	{
	}


	public boolean isSupported ()
	{
		return true;
	}


	public boolean getEcho ()
	{
		return false;
	}


	/**
 	 *	Returns the value of "stty size" width param.
	 *
	 *	<strong>Note</strong>: this method caches the value from the
	 *	first time it is called in order to increase speed, which means
	 *	that changing to size of the terminal will not be reflected
	 *	in the console.
 	 */
	public int getTerminalWidth ()
	{
		if (width != -1)
			return width;

		int val = 80;
		try
		{
			String size = stty ("size");
			if (size.length () != 0 && size.indexOf (" ") != -1)
			{
				val = Integer.parseInt (
					size.substring (size.indexOf (" ") + 1));
			}
		}
		catch (Exception e)
		{
			consumeException (e);
		}

		return width = val;
	}


	/**
 	 *	Returns the value of "stty size" height param.
	 *
	 *	<strong>Note</strong>: this method caches the value from the
	 *	first time it is called in order to increase speed, which means
	 *	that changing to size of the terminal will not be reflected
	 *	in the console.
 	 */
	public int getTerminalHeight ()
	{
		if (height != -1)
			return height;

		int val = 24;

		try
		{
			String size = stty ("size");
			if (size.length () != 0 && size.indexOf (" ") != -1)
			{
				val = Integer.parseInt (
					size.substring (0, size.indexOf (" ")));
			}
		}
		catch (Exception e)
		{
		}

		return height = val;
	}


	/**
	 *  Execute the stty command with the specified arguments
	 *  against the current active terminal.
	 */
	private String stty (final String args)
		throws IOException, InterruptedException
	{
		return exec ("stty -F " + ptyname + " " + args).trim ();
	}


	/**
	 *  Execute the specified command and return the output
	 *  (both stdout and stderr).
	 */
	private static String exec (final String cmd)
		throws IOException, InterruptedException
	{
		return exec (new String [] { "sh", "-c", cmd });
	}


	/**
	 *  Execute the specified command and return the output
	 *  (both stdout and stderr).
	 */
	private static String exec (final String [] cmd)
		throws IOException, InterruptedException
	{
		ByteArrayOutputStream bout = new ByteArrayOutputStream ();

		Process p = Runtime.getRuntime ().exec (cmd);
		int c;
		InputStream in;
			
		in = p.getInputStream ();
		while ((c = in.read ()) != -1)
			bout.write (c);

		in = p.getErrorStream ();
		while ((c = in.read ()) != -1)
			bout.write (c);

		p.waitFor ();

		String result = new String (bout.toByteArray ());
		return result;
	}
}

