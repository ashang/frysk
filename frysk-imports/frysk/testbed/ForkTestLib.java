// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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

package frysk.testbed;

import frysk.sys.Errno;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * Utility class for creating a forked process.
 */
public class ForkTestLib
{
  /**
   * Returned by the fork() method to supply the process id an
   * InputStream and an OutputStream for communicating with the newly
   * created process.
   */
  public static final class ForkedProcess
  {
    public final int pid;
    public final InputStream in;
    public final OutputStream out;

    /* Package private contructor used by fork(). */
    ForkedProcess(int pid, InputStream in, OutputStream out)
    {
      this.pid = pid;
      this.in = in;
      this.out = out;
    }
  }

  /**
   * Package private InputStream subclass created by fork().  Reads
   * bytes from the stdout of the newly created process.
   */
  static final class ForkedInputStream extends InputStream
  {
    final int fd;

    ForkedInputStream(int fd)
    {
      this.fd = fd;
    }

    public native int read() throws IOException;
    public native int read(byte[] buf, int off, int len) throws IOException;
  }

  /**
  * Package private OutputStream subclass created by fork().  Writes
  * bytes to the stdin of the newly created process.
  */
  static final class ForkedOutputStream extends OutputStream
  {
    final int fd;

    ForkedOutputStream(int fd)
    {
      this.fd = fd;
    }

    public native void write(int i) throws IOException;
  }

  /**
   * Creates a child process running ARGV[0] with arguments.  Returns
   * the pid, InputStream and OutputStream that can be used to
   * communicate with the process. Unlike Runtime.exec() this will not
   * setup any new Threads or try to reap the process by waiting on
   * signals.
   */
  public static native ForkedProcess fork(String[] argv);

  /**
   * For use in the cni code when an unexpected errno is encountered.
   */
  static void throwErrno(int errno, String msg) throws Errno
  {
    // We cannot actually insert the original errno, nor can
    // we use Errno.throwErrno() to get an appropriate subclass :{
    // We actually need to subclass Errno for our own use.
    throw new ForkedErrno(errno, msg);
  }

  static class ForkedErrno extends Errno
  {
    // Fake one to keep the ecj warning check pass happy.
    private static final long serialVersionUID = 1;

    private final int errno;

    ForkedErrno(int errno, String msg)
    {
      super(msg);
      this.errno = errno;
    }

    public String toString()
    {
      return super.toString() + " (" + errno + ")";
    }
  }
}
