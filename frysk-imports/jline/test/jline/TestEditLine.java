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


/** 
 *  Tests various features of editing lines.
 *  
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class TestEditLine
	extends JLineTestCase
{
	public TestEditLine (String test)
	{
		super (test);
	}


	public void testDeletePreviousWord ()
		throws Exception
	{
		Buffer b = new Buffer ("This is a test");

		assertBuffer ("This is a ", b = b.op (ConsoleReader.DELETE_PREV_WORD));
		assertBuffer ("This is ", b = b.op (ConsoleReader.DELETE_PREV_WORD));
		assertBuffer ("This ", b = b.op (ConsoleReader.DELETE_PREV_WORD));
		assertBuffer ("", b = b.op (ConsoleReader.DELETE_PREV_WORD));
		assertBuffer ("", b = b.op (ConsoleReader.DELETE_PREV_WORD));
		assertBuffer ("", b = b.op (ConsoleReader.DELETE_PREV_WORD));
	}


	public void testMoveToEnd ()
		throws Exception
	{
		Buffer b = new Buffer ("This is a test");

		assertBuffer ("This is a XtestX", new Buffer ("This is a test")
				.op (ConsoleReader.PREV_WORD)
				.append ('X')
				.op (ConsoleReader.MOVE_TO_END)
				.append ('X')
				);

		assertBuffer ("This is Xa testX", new Buffer ("This is a test")
				.op (ConsoleReader.PREV_WORD)
				.op (ConsoleReader.PREV_WORD)
				.append ('X')
				.op (ConsoleReader.MOVE_TO_END)
				.append ('X')
				);

		assertBuffer ("This Xis a testX", new Buffer ("This is a test")
				.op (ConsoleReader.PREV_WORD)
				.op (ConsoleReader.PREV_WORD)
				.op (ConsoleReader.PREV_WORD)
				.append ('X')
				.op (ConsoleReader.MOVE_TO_END)
				.append ('X')
				);
	}


	public void testPreviousWord ()
		throws Exception
	{
		assertBuffer ("This is a Xtest", new Buffer ("This is a test")
			.op (ConsoleReader.PREV_WORD)
			.append ('X'));
		assertBuffer ("This is Xa test", new Buffer ("This is a test")
			.op (ConsoleReader.PREV_WORD)
			.op (ConsoleReader.PREV_WORD)
			.append ('X'));
		assertBuffer ("This Xis a test", new Buffer ("This is a test")
			.op (ConsoleReader.PREV_WORD)
			.op (ConsoleReader.PREV_WORD)
			.op (ConsoleReader.PREV_WORD)
			.append ('X'));
		assertBuffer ("XThis is a test", new Buffer ("This is a test")
			.op (ConsoleReader.PREV_WORD)
			.op (ConsoleReader.PREV_WORD)
			.op (ConsoleReader.PREV_WORD)
			.op (ConsoleReader.PREV_WORD)
			.append ('X'));
		assertBuffer ("XThis is a test", new Buffer ("This is a test")
			.op (ConsoleReader.PREV_WORD)
			.op (ConsoleReader.PREV_WORD)
			.op (ConsoleReader.PREV_WORD)
			.op (ConsoleReader.PREV_WORD)
			.op (ConsoleReader.PREV_WORD)
			.append ('X'));
		assertBuffer ("XThis is a test", new Buffer ("This is a test")
			.op (ConsoleReader.PREV_WORD)
			.op (ConsoleReader.PREV_WORD)
			.op (ConsoleReader.PREV_WORD)
			.op (ConsoleReader.PREV_WORD)
			.op (ConsoleReader.PREV_WORD)
			.op (ConsoleReader.PREV_WORD)
			.append ('X'));
	}


	public void testLineStart ()
		throws Exception
	{
		assertBuffer ("XThis is a test", new Buffer ("This is a test")
			.ctrlA ().append ('X'));
		assertBuffer ("TXhis is a test", new Buffer ("This is a test")
			.ctrlA ().right ().append ('X'));
	}


	public void testClearLine ()
		throws Exception
	{
		assertBuffer ("", new Buffer ("This is a test").ctrlU ());
		assertBuffer ("t", new Buffer ("This is a test").left ().ctrlU ());
		assertBuffer ("st", new Buffer ("This is a test")
			.left ().left ().ctrlU ());
	}


	public void testRight ()
		throws Exception
	{
		Buffer b = new Buffer ("This is a test");
		b = b.left ().right ().back ();
		assertBuffer ("This is a tes", b);
		b = b.left ().left ().left ().right ().left ().back ();
		assertBuffer ("This is ates", b);
		b.append ('X');
		assertBuffer ("This is aXtes", b);
	}


	public void testLeft ()
		throws Exception
	{
		Buffer b = new Buffer ("This is a test");
		b = b.left ().left ().left ();
		assertBuffer ("This is a est", b = b.back ());
		assertBuffer ("This is aest", b = b.back ());
		assertBuffer ("This is est", b = b.back ());
		assertBuffer ("This isest", b = b.back ());
		assertBuffer ("This iest", b = b.back ());
		assertBuffer ("This est", b = b.back ());
		assertBuffer ("Thisest", b = b.back ());
		assertBuffer ("Thiest", b = b.back ());
		assertBuffer ("Thest", b = b.back ());
		assertBuffer ("Test", b = b.back ());
		assertBuffer ("est", b = b.back ());
		assertBuffer ("est", b = b.back ());
		assertBuffer ("est", b = b.back ());
		assertBuffer ("est", b = b.back ());
		assertBuffer ("est", b = b.back ());
	}


	public void testBackspace ()
		throws Exception
	{
		Buffer b = new Buffer ("This is a test");
		assertBuffer ("This is a tes", b = b.back ());
		assertBuffer ("This is a te", b = b.back ());
		assertBuffer ("This is a t", b = b.back ());
		assertBuffer ("This is a ", b = b.back ());
		assertBuffer ("This is a", b = b.back ());
		assertBuffer ("This is ", b = b.back ());
		assertBuffer ("This is", b = b.back ());
		assertBuffer ("This i", b = b.back ());
		assertBuffer ("This ", b = b.back ());
		assertBuffer ("This", b = b.back ());
		assertBuffer ("Thi", b = b.back ());
		assertBuffer ("Th", b = b.back ());
		assertBuffer ("T", b = b.back ());
		assertBuffer ("", b = b.back ());
		assertBuffer ("", b = b.back ());
		assertBuffer ("", b = b.back ());
		assertBuffer ("", b = b.back ());
		assertBuffer ("", b = b.back ());
	}


	public void testBuffer ()
		throws Exception
	{
		assertBuffer ("This is a test", new Buffer ("This is a test"));
	}
}
