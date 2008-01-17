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

import java.util.*;

/** 
 *  Tests command history.
 *  
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class TestCompletion
	extends JLineTestCase
{
	public TestCompletion (String test)
	{
		super (test);
	}


	public void testSimpleCompletor ()
		throws Exception
	{
		// clear any current completors
		for (Iterator i = console.getCompletors ().iterator ();
			i.hasNext (); console.removeCompletor ((Completor)i.next ()));

 		console.addCompletor (new SimpleCompletor (
			new String [] { "foo", "bar", "baz"}));

		assertBuffer ("foo ", new Buffer ("f").op (ConsoleReader.COMPLETE));
		// single tab completes to unabbiguous "ba"
		assertBuffer ("ba", new Buffer ("b").op (ConsoleReader.COMPLETE));
		assertBuffer ("ba", new Buffer ("ba").op (ConsoleReader.COMPLETE));
		assertBuffer ("baz ", new Buffer ("baz").op (ConsoleReader.COMPLETE));
	}


	public void testArgumentCompletor ()
		throws Exception
	{
		// clear any current completors
		for (Iterator i = console.getCompletors ().iterator ();
			i.hasNext (); console.removeCompletor ((Completor)i.next ()));

 		console.addCompletor (new ArgumentCompletor (
			new SimpleCompletor (new String [] { "foo", "bar", "baz"})));

		assertBuffer ("foo foo ", new Buffer ("foo f")
			.op (ConsoleReader.COMPLETE));
		assertBuffer ("foo ba", new Buffer ("foo b")
			.op (ConsoleReader.COMPLETE));
		assertBuffer ("foo ba", new Buffer ("foo ba")
			.op (ConsoleReader.COMPLETE));
		assertBuffer ("foo baz ", new Buffer ("foo baz")
			.op (ConsoleReader.COMPLETE));

		// test completion in the mid range
		assertBuffer ("foo baz", new Buffer ("f baz")
			.left ().left ().left ().left ()
			.op (ConsoleReader.COMPLETE));
		assertBuffer ("ba foo", new Buffer ("b foo")
			.left ().left ().left ().left ()
			.op (ConsoleReader.COMPLETE));
		assertBuffer ("foo ba baz", new Buffer ("foo b baz")
			.left ().left ().left ().left ()
			.op (ConsoleReader.COMPLETE));
		assertBuffer ("foo foo baz", new Buffer ("foo f baz")
			.left ().left ().left ().left ()
			.op (ConsoleReader.COMPLETE));
	}
}


