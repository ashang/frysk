// This file is part of INUA.  Copyright 2004, 2005, Andrew Cagney
//
// INUA is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// INUA is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with INUA; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Andrew Cagney. gives You the
// additional right to link the code of INUA with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of INUA through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Andrew Cagney may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the INUA code and other code
// used in conjunction with INUA except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.


package inua.eio;

abstract class WordSized
{
  WordSized (int wordSize)
  {
    this.wordSize = wordSize;
  }

  int wordSize;

  abstract long getWord (ByteBuffer b);

  abstract long getUWord (ByteBuffer b);

  abstract void putWord (ByteBuffer b, long w);

  abstract void putUWord (ByteBuffer b, long w);

  private static final WordSized[] wordSizes = { new WordSized(2)
  {
    long getWord (ByteBuffer b)
    {
      return b.getShort();
    }

    long getUWord (ByteBuffer b)
    {
      return b.getUShort();
    }

    void putWord (ByteBuffer b, long w)
    {
      b.putShort((short) w);
    }

    void putUWord (ByteBuffer b, long w)
    {
      b.putUShort((int) w);
    }
  }, new WordSized(4)
  {
    long getWord (ByteBuffer b)
    {
      return b.getInt();
    }

    long getUWord (ByteBuffer b)
    {
      return b.getUInt();
    }

    void putWord (ByteBuffer b, long w)
    {
      b.putInt((int) w);
    }

    void putUWord (ByteBuffer b, long w)
    {
      b.putUInt(w);
    }
  }, new WordSized(8)
  {
    long getWord (ByteBuffer b)
    {
      return b.getLong();
    }

    long getUWord (ByteBuffer b)
    {
      return b.getULong();
    }

    void putWord (ByteBuffer b, long w)
    {
      b.putLong(w);
    }

    void putUWord (ByteBuffer b, long w)
    {
      b.putULong(w);
    }
  } };

  static WordSized wordSize (int w)
  {
    for (int i = 0; i < wordSizes.length; i++)
      {
        if (w == wordSizes[i].wordSize)
          {
            return wordSizes[i];
          }
      }
    throw new RuntimeException("Bad word size " + w);
  }

}
