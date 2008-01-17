// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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

package frysk.util;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import jline.Terminal;

import frysk.sys.FileDescriptor;
import frysk.sys.Size;
import frysk.sys.termios.Local;
import frysk.sys.termios.Special;
import frysk.sys.termios.Termios;


/** Class that implements the jline Terminal abstract class using
 * frysk.sys.termios calls. This copies jline.PtyTerminal pretty closely.
 */
public class PtyTerminal
  extends Terminal
{
  private final FileDescriptor fd;

  public FileDescriptor getFd()
  {
    return fd;
  }

  public PtyTerminal(FileDescriptor fd)
  {
    this.fd = fd;

    try
      {
	initializeTerminal();
      }
    catch (IOException ioe)
      {
	System.out.println(ioe);
      }
    catch (InterruptedException e)
      {
	System.out.println(e);
      }
  }

  public PtyTerminal(File file)
  {
    this(new FileDescriptor(file, FileDescriptor.RDWR));
  }

  public PtyTerminal(String fname)
  {
    this(new File(fname));
  }
  
  /**
   *  Remove line-buffered input by invoking "stty -icanon min 1"
   *  against the current terminal.
   */
  public void initializeTerminal()
    throws IOException, InterruptedException
  {
    final Termios initialTermios = new Termios(fd);
    final Termios termios = new Termios(fd);
    // set the console to be character-buffered instead of line-buffered
    termios.set(Local.CANONICAL, false);
    termios.set(Local.ECHO_INPUT, false);
    termios.set(Special.NON_CANONICAL_READ_MINIMUM, (char)1);
    termios.set(fd);
    // at exit, restore the original tty configuration
    try
      {
	Runtime.getRuntime().addShutdownHook(new Thread()
	  {
	    public void start ()
	    {
	      initialTermios.set(fd);
	    }
	  });
      }
    catch (AbstractMethodError ame)
      {
	System.out.println(ame);
      }
  }

  public int readVirtualKey (InputStream in)
    throws IOException
  {
    int c = readCharacter (in);

    // in Unix terminals, arrow keys are represented by
    // a sequence of 3 characters. E.g., the up arrow
    // key yields 27, 91, 68
    if (c == jline.PtyTerminal.ARROW_START)
      {
	c = readCharacter (in);
	if (c == jline.PtyTerminal.ARROW_PREFIX)
	  {
	    c = readCharacter (in);
	    if (c == jline.PtyTerminal.ARROW_UP)
	      return CTRL_P;
	    else if (c == jline.PtyTerminal.ARROW_DOWN)
	      return CTRL_N;
	    else if (c == jline.PtyTerminal.ARROW_LEFT)
	      return CTRL_B;
	    else if (c == jline.PtyTerminal.ARROW_RIGHT)
	      return CTRL_F;
	  }
      }
    return c;
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
   */
  public int getTerminalWidth()
  {
    Size size = fd.getSize();
    return size.getRows();
  }

  /**
   *	Returns the value of "stty size" height param.
   */
  public int getTerminalHeight()
  {
    Size size = fd.getSize();
    return size.getColumns();
  }
}