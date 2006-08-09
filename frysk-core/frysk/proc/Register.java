// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

package frysk.proc;

import java.math.BigInteger;
import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;

public class Register
{
  // Type type;
  // boolean readWrite;
  int bank;
  int offset;
  int length;
  String name;

  Register(int bank, int offset, int length, String name)
  {
    this.bank = bank;
    this.offset = offset;
    this.length = length;
    this.name = name;
  }

  public long get(frysk.proc.Task task)
  {
    ByteBuffer b = task.registerBank[bank];
    long val = 0;
    if (b.order() == ByteOrder.LITTLE_ENDIAN)
      {
	for (int i = offset + length - 1; i >= offset; i--)
	  {
	    val = val << 8 | (b.get(i) & 0xff);
	  }
      }
    else
      {
	for (int i = offset; i < offset + length; i++)
	  {
	    val = val << 8 | (b.get(i) & 0xff);
	  }
      }
    return val;
  }

  /**
   * Returns the value of a register as a BigInteger.
   *
   * @param task the task from which to get the register
   * @return BigInteger value preserving the sign.
   */
  public BigInteger getBigInteger(frysk.proc.Task task)
  {
    ByteBuffer b = task.registerBank[bank];
    byte[] bytes = new byte[length];
    if (b.order() == ByteOrder.LITTLE_ENDIAN)
      {
	for (int i = 0; i < length; i++)
	  {
	    bytes[length - 1 - i] = (byte)b.get(i + offset);
	  }
      }
    else
      {
	for (int i = 0; i < length; i++)
	  {
	    bytes[i] = (byte)b.get(i + offset);
	  }
      }
    return new BigInteger(bytes);
  }

  public void put(frysk.proc.Task task, long val)
  {
    ByteBuffer b = task.registerBank[bank];
    if (b.order() == ByteOrder.LITTLE_ENDIAN)
      {
	for (int i = offset; i < offset + length; i++)
	  {
	    b.putByte(i, (byte)(val & 0xff));
	    val = val >> 8;
	  }
      }
    else
      {
	for (int i = offset + length - 1; i >= offset; i--)
	  {
	    b.putByte(i, (byte)(val & 0xff));
	    val = val >> 8;
	  }
      }
  }

  /**
   * Write a register value.
   *
   * @param task task in which to write the register
   * @param val the value
   */
  public void putBigInteger(frysk.proc.Task task, BigInteger val)
  {
    ByteBuffer b = task.registerBank[bank];
    byte[] bytes = val.toByteArray();
    int i;
    if (b.order() == ByteOrder.LITTLE_ENDIAN) 
      {
	for (i = 0; i < bytes.length; i++) 
	  {
	    b.putByte(i + offset, bytes[bytes.length - 1 - i]);
	  }
	for (; i < length; i++)
	  {
	    b.putByte(i + offset, (byte)0);
	  }
      } 
    else 
      {
	for (i = length; i >= bytes.length; i--)
	  {
	    b.putByte(i + offset, (byte)0);
	  }
	for (; i >= 0; i--) 
	  {
	    b.putByte(i + offset, bytes[i]);
	  }
      }
  }
	    

  public String getName()
  {
    return name;
  }
    
  public int getLength()
  {
    return length;
  }

  // void get(proc.Task task, byte[] bytes, int off, int len);
  // void get(Task task, byte[] array);
}
