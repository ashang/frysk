// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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

package frysk.gui.monitor;

import org.gnu.gtk.Bin;
import org.gnu.glib.Handle;
import org.gnu.gdk.Color;
import org.gnu.gnomevte.Terminal;
import java.io.*;
import frysk.sys.PseudoTerminal;
import frysk.hpd.CLI;
import jline.ConsoleReader;
import jline.PtyTerminal;
//import frysk.util.PtyTerminal;

public class ConsoleWidget extends Bin {
    private Terminal term;
	private reader rd;
	private PseudoTerminal pty;
	
	private class reader implements Runnable
	{
		CLI cli;
		ConsoleReader jlreader;
		String fname;
		boolean stop;

		reader(String fname)
		{
			this.fname = fname;
			stop = false;

			try
			{
			  cli = new CLI("(frysk) ", new PrintWriter( new FileOutputStream(new File(fname)) ) );
			}
			catch (IOException ioe)
			{
				System.out.println("ERROR: Could not create a file output stream: ");
				System.out.print(ioe.getMessage());
			}

			FileWriter out = null;
			FileInputStream in = null;

			try
			{
				out = new FileWriter(new File(fname));
			}
			catch (IOException ioe)
			{
				System.out.println("ERROR: Could not create a file writer: ");
				System.out.print(ioe.getMessage());
			}

			try
			{
				in = new FileInputStream(new File(fname));
			}
			catch (IOException ioe)
			{
				System.out.println("ERROR: Could not create a file input stream: ");
				System.out.print(ioe.getMessage());
			}

			try
			{
				jlreader = new ConsoleReader(in, out, null, new PtyTerminal(fname));
			}
			catch (IOException ioe)	{
				System.out.println("ERROR: Could not create a command line");
				System.out.print(ioe.getMessage());
			}
		}

		public void run()
		{
			String line = "";

			try
			{
				while (line != null && !line.trim().equals("quit"))
				{
					line = jlreader.readLine(cli.getPrompt());
					cli.execCommand(line);
				}

			}
			catch (IOException ioe)
			{
				System.out.println("ERROR: Could not read from command line");
				System.out.print(ioe.getMessage());
			}
		}
	}

    public ConsoleWidget(Handle handle)
	{
		super(handle);
		/*this.addListener(new LifeCycleListener() {
				public void lifeCycleEvent(LifeCycleEvent event)
				{
				}

				public boolean lifeCycleQuery(LifeCycleEvent event)
				{
					if (event.isOfType(LifeCycleEvent.Type.DESTROY) ||
						event.isOfType(LifeCycleEvent.Type.DELETE))
						ConsoleWidget.this.shutDown();
					
					return true;
				}
			});
			*/

		String[] cmdargs = new String[1];
		cmdargs[0] = "-1";

		pty = new PseudoTerminal ();
		
		int master = pty.getFd ();
		File name = pty.getFile ();
		System.out.println ("master = " + master + " name = " + name);

		term = new Terminal();
		term.setPty (master);
		
		term.setDefaultColors();
		term.setBackgroudColor(Color.WHITE);
		term.setForegroundColor(Color.BLACK);
		term.setSize (80, 25);

		this.add(term);
		this.showAll();

		System.out.println ("master = " + master + " name = " + name);

		rd = new reader(name.getPath ());
		new Thread(rd).start();
    }

    public void shutDownReader()
    {
	//send a ^C and then "quit" to jline
	pty.write (3);
	byte[] quit = "quit\n".getBytes ();
	pty.write (quit, 0, quit.length);
    }
}
