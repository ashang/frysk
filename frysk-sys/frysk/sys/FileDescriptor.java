// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

package frysk.sys;

import java.io.File;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Unix file descriptor.
 *
 * This object is loosely based on the Unix file-descriptor.
 */

public class FileDescriptor
{
    private int fd = -1;
    /**
     * Package local file descriptor used by various classes when
     * returning a file descriptor.
     */
    FileDescriptor (int fd) {
	if (fd < 0) {
	    throw new RuntimeException("FileDescriptor " + fd + " invalid");
	}
	this.fd = fd;
    }
    /**
     * Create a file descriptor for the specified FILE, open in mode
     * specified by flags.
     */
    public FileDescriptor (String file, int accessMode) {
	this (open (file, accessMode, 0));
    }
    /**
     * Create a file descriptor for the specified FILE, open with MODE.
     */
    public FileDescriptor (File file, int accessMode) {
	this (open (file.getAbsolutePath (), accessMode, 0));
    }
    /**
     * Create a new file tied to FileDescriptor with accessMode and
     * protection.
     */
    public FileDescriptor (String file, int accessMode, int prot) {
	this (open (file, accessMode | CREAT, prot));
    }
    /**
     * Create a new file tied to FileDescriptor with accessMode and
     * protection.
     */
    public FileDescriptor (File file, int accessMode, int prot) {
	this (open (file.getAbsolutePath(), accessMode | CREAT, prot));
    }
    /**
     * Open file read-only.
     */
    public static final int RDONLY = 1;
    /**
     * Open file write-only
     */
    public static final int WRONLY = 2;
    /**
     * Open file read-write
     */
    public static final int RDWR = 4;
    /**
     * Create a new file.  Implied by the three-param constructor.
     */
    private static final int CREAT = 8;

    public int getFd ()
    {
	return fd;
    }

    /**
     * File descriptor corresponding to standard input.
     */
    static public final FileDescriptor in = new FileDescriptor (0);
    /**
     * File descriptor corresponding to standard output
     */
    static public final FileDescriptor out = new FileDescriptor (1);
    /**
     * File descriptor corresponding to standard error.
     */
    static public final FileDescriptor err = new FileDescriptor (2);

    /**
     * Make this FileDescriptor a dup (point at the same system
     * object) as OLD.  See dup2(2).
     */
    public native void dup (FileDescriptor old);

    /**
     * Open the specified FILE in FLAGS; returning a file descriptor.
     * If CREAT was specified, create file based on PROT.
     */
    private native static int open(String file, int accessMode, int prot);
    /**
     * Close the file-descriptor.
     */
    private static native void close(int fd);

    /**
     * Poll the file descriptor determining if there is there at least
     * one character, the end-of-file, or hangup indication available
     * for reading?
     */
    public boolean ready ()
    {
	return ready (0);
    }
    /**
     * Wait on the file descriptor for upto millesecond timeout,
     * checking for at least one character, an eof indication, or
     * hangup available for reading?
     */
    public native boolean ready (long millisecondTimeout);

    /**
     * Read a single byte from the file descriptor.  Return -1 if
     * end-of-file.
     */
    public native int read ();
    /**
     * Read bytes from the file descriptor.  Return number of bytes
     * read, or -1 of end-of-file.
     *
     * XXX: Since read is capped by byte[].length, int is returned.
     */
    public native int read(byte[] bytes, long start, long length);

    /**
     * Write a single byte to the file descriptor.
     */
    public native void write (int b);
    /**
     * Write elements of BUF to the file descriptor.
     *
     * XXX: Since write is capped by byte[].lenght, int is returned.
     */
    public native int write(byte[] bytes, long start, long length);

    /**
     * Close the file descriptor.
     */
    public void close() {
	if (fd >= 0) {
	    // Make certain that FD is invalidated before the close is
	    // attempted; stops further attempts at a close.
	    int openFd = fd;
	    fd = -1;
	    close(openFd);
	}
    }

    /**
     * Always clean up the file descriptor.
     */
    protected void finalize ()
    {
	close();
    }

    /**
     * Return an input stream that can read this file descriptor.
     */
    public InputStream getInputStream ()
    {
	return new InputStream ()
	    {
		public void close ()
		    throws IOException
		{
		    try {
			FileDescriptor.this.close ();
		    }
		    catch (Errno e) {
			throw new IOException (e.getMessage ());
		    }
		}
		public int available ()
		    throws IOException
		{
		    try {
			if (FileDescriptor.this.ready ())
			    return 1;
			else
			    return 0;
		    }
		    catch (Errno e) {
			throw new IOException (e.getMessage ());
		    }
		}
		public int read ()
		    throws IOException
		{
		    try {
			return FileDescriptor.this.read ();
		    }
		    catch (Errno e) {
			throw new IOException (e.getMessage ());
		    }
		}
		public int read (byte[] bytes, int off, int len)
		    throws IOException
		{
		    try {
			return FileDescriptor.this.read (bytes, off, len);
		    }
		    catch (Errno e) {
			throw new IOException (e.getMessage ());
		    }
		}
	    };
    }

    /** 
     * Return an output stream that can write this file descriptor.
     */
    public OutputStream getOutputStream ()
    {
	return new OutputStream ()
	    {
		public void close ()
		    throws IOException
		{
		    try {
			FileDescriptor.this.close ();
		    }
		    catch (Errno e) {
			throw new IOException (e.getMessage ());
		    }
		}
		public void write (int b)
		    throws IOException
		{
		    try {
			FileDescriptor.this.write (b);
		    }
		    catch (Errno e) {
			throw new IOException (e.getMessage ());
		    }
		}
		public void write (byte[] bytes, int off, int len)
		    throws IOException
		{
		    try {
			FileDescriptor.this.write (bytes, off, len);
		    }
		    catch (Errno e) {
			throw new IOException (e.getMessage ());
		    }
		}
	    };
    }

    public String toString ()
    {
	return "{fd=" + fd + "}";
    }

  /**
   * Return the size of a terminal window. Can throw an exception if
   * the file descriptor is not a terminal.
   */
  public native Size getSize();

  /**
   * Set the size of a terminal window.
   */
  public native void setSize(Size size);

    /**
     * Seek to OFFSET from start of file.
     */
    public native long seekSet(long offset);
    /**
     * Seek to OFFSET from end of file.
     */
    public native long seekEnd(long offset);
    /**
     * Seek to OFFSET from current position.
     */
    public native long seekCurrent(long offset);
}