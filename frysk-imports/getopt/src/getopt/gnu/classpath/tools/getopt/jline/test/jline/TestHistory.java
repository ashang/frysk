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
 *  Tests command history.
 *  
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class TestHistory
	extends JLineTestCase
{
	public TestHistory (String test)
	{
		super (test);
	}


	public void testSingleHistory ()
		throws Exception
	{
		Buffer b = new Buffer ()
			.append ("test line 1").op (ConsoleReader.NEWLINE)
			.append ("test line 2").op (ConsoleReader.NEWLINE)
			.append ("test line 3").op (ConsoleReader.NEWLINE)
			.append ("test line 4").op (ConsoleReader.NEWLINE)
			.append ("test line 5").op (ConsoleReader.NEWLINE)
			.append ("");

		assertBuffer ("", b);

		assertBuffer ("test line 5", b = b.op (ConsoleReader.PREV_HISTORY));
		assertBuffer ("test line 4", b = b.op (ConsoleReader.PREV_HISTORY));
		assertBuffer ("test line 5", b = b.op (ConsoleReader.NEXT_HISTORY));
		assertBuffer ("test line 4", b = b.op (ConsoleReader.PREV_HISTORY));
		assertBuffer ("test line 3", b = b.op (ConsoleReader.PREV_HISTORY));
		assertBuffer ("test line 2", b = b.op (ConsoleReader.PREV_HISTORY));
		assertBuffer ("test line 1", b = b.op (ConsoleReader.PREV_HISTORY));

		// beginning of history
		assertBuffer ("test line 1", b = b.op (ConsoleReader.PREV_HISTORY));
		assertBuffer ("test line 1", b = b.op (ConsoleReader.PREV_HISTORY));
		assertBuffer ("test line 1", b = b.op (ConsoleReader.PREV_HISTORY));
		assertBuffer ("test line 1", b = b.op (ConsoleReader.PREV_HISTORY));


		assertBuffer ("test line 2", b = b.op (ConsoleReader.NEXT_HISTORY));
		assertBuffer ("test line 3", b = b.op (ConsoleReader.NEXT_HISTORY));
		assertBuffer ("test line 4", b = b.op (ConsoleReader.NEXT_HISTORY));
		assertBuffer ("test line 5", b = b.op (ConsoleReader.NEXT_HISTORY));


		// end of history
		assertBuffer ("", b = b.op (ConsoleReader.NEXT_HISTORY));
		assertBuffer ("", b = b.op (ConsoleReader.NEXT_HISTORY));
		assertBuffer ("", b = b.op (ConsoleReader.NEXT_HISTORY));


		assertBuffer ("test line 5", b = b.op (ConsoleReader.PREV_HISTORY));
		assertBuffer ("test line 4", b = b.op (ConsoleReader.PREV_HISTORY));
		b = b.op (ConsoleReader.MOVE_TO_BEG).append ("XXX").op (ConsoleReader.NEWLINE);
		assertBuffer ("XXXtest line 4", b = b.op (ConsoleReader.PREV_HISTORY));
		assertBuffer ("test line 5", b = b.op (ConsoleReader.PREV_HISTORY));
		assertBuffer ("test line 4", b = b.op (ConsoleReader.PREV_HISTORY));
		assertBuffer ("test line 5", b = b.op (ConsoleReader.NEXT_HISTORY));
		assertBuffer ("XXXtest line 4", b = b.op (ConsoleReader.NEXT_HISTORY));
		assertBuffer ("", b = b.op (ConsoleReader.NEXT_HISTORY));


		assertBuffer ("XXXtest line 4", b = b.op (ConsoleReader.PREV_HISTORY));
		assertBuffer ("XXXtest line 4", b = b.op (ConsoleReader.NEWLINE)
			.op (ConsoleReader.PREV_HISTORY));
		assertBuffer ("XXXtest line 4", b = b.op (ConsoleReader.NEWLINE)
			.op (ConsoleReader.PREV_HISTORY));
		assertBuffer ("XXXtest line 4", b = b.op (ConsoleReader.NEWLINE)
			.op (ConsoleReader.PREV_HISTORY));
		assertBuffer ("XXXtest line 4", b = b.op (ConsoleReader.NEWLINE)
			.op (ConsoleReader.PREV_HISTORY));
	}
}

