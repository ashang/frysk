// This file is part of INUA.  Copyright 2004, 2005, Andrew Cagney
// Copyright 2007, Red Hat Inc.
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

/**
 * A ByteBuffer.  Just like {@link java.nio.ByteBuffer} only 64-bit.
 */


package inua.eio;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ByteBuffer
    extends Buffer
{

  private Logger logger = Logger.getLogger("frysk");
  
  protected ByteBuffer (long lowWater, long highWater)
  {
    super(lowWater, highWater);
    order(ByteOrder.BIG_ENDIAN);
    wordSize(4);
  }

  /**
   * Read a single byte at CARET.
   */
  protected abstract int peek (long caret);

  /**
   * Write VAL (as a single byte), to CARET.
   */
  protected abstract void poke (long caret, int val);

  protected int peek (long caret, byte[] bytes, int off, int len)
  {
    logger.log(Level.FINE, "entering peek, caret: 0x{0}, off: 0x{1}, len: 0x{2}",
               new Object[]{Long.toHexString(caret), Integer.toHexString(off), 
                            Integer.toHexString(len)});
    for (int i = 0; i < len; i++)
      {
	logger.log(Level.FINEST, "on byte: 0x{0}", Long.toHexString(i));
        bytes[off + i] = (byte) peek(caret + i);
      }
    return len;
  }

  protected int poke (long caret, byte[] bytes, int off, int len)
  {
    for (int i = 0; i < len; i++)
      {
        poke(caret + i, bytes[off + i]);
      }
    return len;
  }

  protected final void peekFully (long caret, byte[] bytes, int off, int len)
  {
    logger.log(Level.FINE, "entering peekFully, caret: 0x{0}, off: 0x{1}, len: 0x{2}\n",
               new Object[]{Long.toHexString(caret), Integer.toHexString(off), 
                            Integer.toHexString(len)});
    while (len > 0)
      {
        long xfer = peek(caret, bytes, off, len);
        off += xfer;
        len -= xfer;
        caret += xfer;
      }
    logger.log(Level.FINE, "exiting peekFully\n");
  }

  protected final void pokeFully (long caret, byte[] bytes, int off, int len)
  {
    while (len > 0)
      {
        long xfer = poke(caret, bytes, off, len);
        off += xfer;
        len -= xfer;
        caret += xfer;
      }
  }

  byte[] scratch = new byte[8];

  protected final long peekBig (long caret, int len)
  {
    peekFully(caret, scratch, 0, len);
    long value = 0;
    for (int i = 0; i < len; i++)
      {
        value = (value << 8) | (scratch[i] & 0xffL);
      }
    return value;
  }

  protected final float peekBigFloat (long caret, int len)
  {
    long iv = peekBig(caret, len);
    float v = Float.intBitsToFloat((int)iv);
    cursor += len;
    return v;
  }

  protected final double peekBigDouble (long caret, int len)
  {
    long iv = peekBig(caret, len);
    double v = Double.longBitsToDouble(iv);
    cursor += len;
    return v;
  }

  protected final void pokeBig (long caret, int len, long value)
  {
    for (int i = len - 1; i >= 0; i--)
      {
        scratch[i] = (byte) (value & 0xff);
        value >>= 8;
      }
    pokeFully(caret, scratch, 0, len);
  }

  protected final void pokeBig (long caret, int len, float value)
  {
    int iv = Float.floatToIntBits(value);
    pokeBig(caret, len, iv);
  }

  protected final void pokeBig (long caret, int len, double value)
  {
    long iv = Double.doubleToLongBits(value);
    pokeBig(caret, len, iv);
  }
    
  protected final long peekLittle (long caret, int len)
  {
    peekFully(caret, scratch, 0, len);
    long value = 0;
    for (int i = 0; i < len; i++)
      {
        value |= (scratch[i] & 0xffL) << i * 8;
      }
    return value;
  }

  protected final float peekLittleFloat (long caret, int len)
  {
    long iv = peekLittle(caret, len);
    float v = Float.intBitsToFloat((int)iv);
    cursor += len;
    return v;
  }

    protected final double peekLittleDouble (long caret, int len)
  {
    long lv = peekLittle(caret, len);
    double v = Double.longBitsToDouble(lv);
    cursor += len;
    return v;
  }

  protected final void pokeLittle (long caret, int len, long value)
  {
    for (int i = 0; i < len; i++)
      {
        scratch[i] = (byte) (value & 0xff);
        value >>= 8;
      }
    pokeFully(caret, scratch, 0, len);
  }

  protected final void pokeLittle (long caret, int len, float value)
  {
    int iv = Float.floatToIntBits(value);
    pokeLittle(caret, len, iv);
  }

  protected final void pokeLittle (long caret, int len, double value)
  {
    long iv = Double.doubleToLongBits(value);
    pokeLittle(caret, len, iv);
  }
    
  protected final long peekLittle (int len)
  {
    long v = peekLittle(cursor, len);
    cursor += len;
    return v;
  }

  protected final float peekLittleFloat (int len)
  {
    long iv = peekLittle(cursor, len);
    float v = Float.intBitsToFloat((int)iv);
    cursor += len;
    return v;
  }

  protected final double peekLittleDouble (int len)
  {
    long iv = peekLittle(cursor, len);
    double v = Double.longBitsToDouble(iv);
    cursor += len;
    return v;
  }

  protected final void pokeBig (int len, long value)
  {
    pokeBig(cursor, len, value);
    cursor += len;
  }

  protected final void pokeBig (int len, float value)
  {
    int iv = Float.floatToIntBits(value);
    pokeBig(cursor, len, iv);
    cursor += len;
  }

  protected final void pokeBig (int len, double value)
  {
    long iv = Double.doubleToLongBits(value);
    pokeBig(cursor, len, iv);
    cursor += len;
  }

  protected final long peekBig (int len)
  {
    long v = peekBig(cursor, len);
    cursor += len;
    return v;
  }

  protected final float peekBigFloat (int len)
  {
    long iv = peekBig(cursor, len);
    float v = Float.intBitsToFloat((int)iv);
    cursor += len;
    return v;
  }

  protected final double peekBigDouble (int len)
  {
    long iv = peekBig(cursor, len);
    double v = Double.longBitsToDouble(iv);
    cursor += len;
    return v;
  }

  protected final void pokeLittle (int len, long value)
  {
    pokeLittle(cursor, len, value);
    cursor += len;
  }

  protected final void pokeLittle (int len, float value)
  {
    int iv = Float.floatToIntBits(value);
    pokeLittle(cursor, len, iv);
    cursor += len;
  }

    protected final void pokeLittle (int len, double value)
  {
    long lv = Double.doubleToLongBits(value);
    pokeLittle(cursor, len, lv);
    cursor += len;
  }

  /**
   * Given BUFFER, return a new subBuffer. Used by {@link #slice}.
   */
  protected ByteBuffer subBuffer (ByteBuffer buffer, long lowerExtreem,
                                  long upperExtreem)
  {
    throw new RuntimeException("not implemented");
  }

  public ByteBuffer slice (long off, long len)
  {
    ByteBuffer newSlice = subBuffer(this, lowWater + off, lowWater + off + len);
    newSlice.order(order());
    newSlice.wordSize(wordSize());
    return newSlice;
  }

  public ByteBuffer slice ()
  {
    return slice(position(), limit());
  }

  public final byte get ()
  {
    return (byte) peek(cursor++);
  }

  public final byte get (long index)
  {
    return (byte) peek(lowWater + index);
  }

  public ByteBuffer get (long index, byte[] dst, int off, int len)
      throws BufferUnderflowException
  {
    logger.log(Level.FINE, "entering index: 0x{0}, off: 0x{1}, len: {2}\n",
               new Object[] {Long.toHexString(index), Long.toHexString(off),
                             new Integer(len)});
    if (ULong.GT(index + len, limit()))
      throw new BufferUnderflowException();
    peekFully(lowWater + index,dst,off,len);
    logger.log(Level.FINE, "exiting\n");
    return this;
  }
  
  public int safeGet (long index, byte[] dst, int off, int len)
  {
    int maxLen = len;
    if (ULong.GT(index + len, limit()))
      maxLen = (int) (limit() - index);
    get(index, dst, off, maxLen);
    return maxLen;
  }

  public ByteBuffer get (byte[] dst, int off, int len)
      throws BufferUnderflowException
  {
    if (ULong.GT(len, remaining()))
      throw new BufferUnderflowException();
    peek(cursor, dst, off, len);
    cursor += len;
    return this;
  }

  public final ByteBuffer get (byte[] dst) throws BufferUnderflowException
  {
    return get(dst, 0, dst.length);
  }

  public ByteBuffer put (byte[] src, int off, int len)
    throws BufferUnderflowException
  {
    if (ULong.GT(len, remaining()))
      throw new BufferUnderflowException();
    poke(cursor, src, off, len);
    cursor += len;
    return this;
  }

  public final ByteBuffer put (byte[] src) throws BufferUnderflowException
  {
    return put(src, 0, src.length);
  }

  protected ByteOrdered byteOrdered;

  public final ByteOrder order ()
  {
    return byteOrdered.byteOrder;
  }

  public final ByteBuffer order (ByteOrder bo)
  {
    byteOrdered = ByteOrdered.order(bo);
    return this;
  }

  public final byte getByte ()
  {
    return (byte) peek(cursor++);
  }

  public final byte getByte (long index)
  {
    return (byte) peek(lowWater + index);
  }

  public final short getUByte ()
  {
    return (short) peek(cursor++);
  }

  public final short getUByte (long index)
  {
    return (short) peek(lowWater + index);
  }

  public final void putByte (byte v)
  {
    poke(cursor++, v);
  }

  public final void putByte (long index, byte v)
  {
    poke(lowWater + index, v);
  }

  public final void putUByte (short v)
  {
    poke(cursor++, v);
  }

  public final void putUByte (long index, short v)
  {
    poke(lowWater + index, v);
  }

  public final short getShort ()
  {
    return byteOrdered.peekShort(this);
  }
  
  public final short getShort(ByteOrder byteOrder) {
      ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
      return byteOrdered.peekShort(this);
  }

  public final short getShort (long index)
  {
    return byteOrdered.peekShort(this, lowWater + index);
  }
  
  public final short getShort (ByteOrder byteOrder, long index)
  {
    ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    return byteOrdered.peekShort(this, lowWater + index);
  }

  public final int getUShort ()
  {
    return byteOrdered.peekUShort(this);
  }

  public final int getUShort (ByteOrder byteOrder)
  {
    ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    return byteOrdered.peekUShort(this);
  }
  
  public final int getUShort (long index)
  {
    return byteOrdered.peekUShort(this, lowWater + index);
  }
  
  public final int getUShort (ByteOrder byteOrder, long index)
  {
    ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    return byteOrdered.peekUShort(this, lowWater + index);
  }
  
  public final int getInt ()
  {
    return byteOrdered.peekInt(this);
  }
  
  public final int getInt (ByteOrder byteOrder)
  {
    ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    return byteOrdered.peekInt(this);
  }
  
  public final int getInt (long index)
  {
    logger.log(Level.FINE, "{0} getInt\n", this);
    int peek = byteOrdered.peekInt(this, lowWater + index);
    logger.log(Level.FINE, "{0} getInt Finished " + peek + " \n", this);
    return peek;
  }
  
  public final int getInt (ByteOrder byteOrder, long index)
  {
    ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    logger.log(Level.FINE, "{0} getInt\n", this);
    int peek = byteOrdered.peekInt(this, lowWater + index);
    logger.log(Level.FINE, "{0} getInt Finished " + peek + " \n", this);
    return peek;
  }
  
  public final long getUInt ()
  {
    return byteOrdered.peekUInt(this);
  }
  
  public final long getUInt (ByteOrder byteOrder)
  {
    ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    return byteOrdered.peekUInt(this);
  }
  
  public final long getUInt (long index)
  {
    return byteOrdered.peekUInt(this, lowWater + index);
  }
  
  public final long getUInt (ByteOrder byteOrder, long index)
  {
    ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    return byteOrdered.peekUInt(this, lowWater + index);
  }
  
  public final long getULong ()
  {
    return byteOrdered.peekULong(this);
  }
  
  public final long getULong (ByteOrder byteOrder)
  {
    ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    return byteOrdered.peekULong(this);
  }
  
  public final long getULong (long index)
  {
    return byteOrdered.peekULong(this, lowWater + index);
  }
  
  public final long getULong (ByteOrder byteOrder, long index)
  {
    ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    return byteOrdered.peekULong(this, lowWater + index);
  }
  
  public final long getLong ()
  {
    return byteOrdered.peekLong(this);
  }
  
  public final long getLong (ByteOrder byteOrder)
  {
    ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    return byteOrdered.peekLong(this);
  }
  
  public final long getLong (long index)
  {
    return byteOrdered.peekLong(this, lowWater + index);
  }

  public final long getLong (ByteOrder byteOrder, long index)
  {
    ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    return byteOrdered.peekLong(this, lowWater + index);
  }
  
  public final float getFloat (long index)
  {
    return byteOrdered.peekFloat(this, lowWater + index);
  }
  
  public final float getFloat (ByteOrder byteOrder, long index)
  {
    ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    return byteOrdered.peekFloat(this, lowWater + index);
  }

  public final double getDouble (long index)
  {
    return byteOrdered.peekDouble(this, lowWater + index);
  }
  
  public final double getDouble (ByteOrder byteOrder, long index) {
      ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
      return byteOrdered.peekDouble(this, lowWater + index);
  }

  public final void putShort (short v)
  {
    byteOrdered.pokeShort(this, v);
  }
  
  public final void putShort (ByteOrder byteOrder, short v)
  {
      ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    byteOrdered.pokeShort(this, v);
  }
  
  public final void putShort (long index, short v)
  {
    byteOrdered.pokeShort(this, lowWater + index, v);
  }
  
  public final void putShort (ByteOrder byteOrder, long index, short v)
  {
      ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    byteOrdered.pokeShort(this, lowWater + index, v);
  }
  
  public final void putUShort (int v)
  {
    byteOrdered.pokeUShort(this, v);
  }

  public final void putUShort (ByteOrder byteOrder, int v)
  {
      ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    byteOrdered.pokeUShort(this, v);
  }
  
  public final void putUShort (long index, int v)
  {
    byteOrdered.pokeUShort(this, lowWater + index, v);
  }

  public final void putUShort (ByteOrder byteOrder, long index, int v)
  {
      ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    byteOrdered.pokeUShort(this, lowWater + index, v);
  }
  
  public final void putInt (int v)
  {
    byteOrdered.pokeInt(this, v);
  }

  public final void putInt (ByteOrder byteOrder, int v)
  {
      ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    byteOrdered.pokeInt(this, v);
  }
  
  public final void putInt (long index, int v)
  {
    byteOrdered.pokeInt(this, lowWater + index, v);
  }
  
  public final void putInt (ByteOrder byteOrder, long index, int v)
  {
      ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    byteOrdered.pokeInt(this, lowWater + index, v);
  }
  
  public final void putUInt (long v)
  {
    byteOrdered.pokeUInt(this, v);
  }

  public final void putUInt (ByteOrder byteOrder, long v)
  {
      ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    byteOrdered.pokeUInt(this, v);
  }
 
  public final void putUInt (long index, long v)
  {
    byteOrdered.pokeUInt(this, lowWater + index, v);
  }

  public final void putUInt (ByteOrder byteOrder, long index, long v)
  {
      ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    byteOrdered.pokeUInt(this, lowWater + index, v);
  }
  
  public final void putULong (long v)
  {
    byteOrdered.pokeULong(this, v);
  }

  public final void putULong (ByteOrder byteOrder, long v)
  {
      ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    byteOrdered.pokeULong(this, v);
  }
  
  public final void putULong (long index, long v)
  {
    byteOrdered.pokeULong(this, lowWater + index, v);
  }

  public final void putULong (ByteOrder byteOrder, long index, long v)
  {
      ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    byteOrdered.pokeULong(this, lowWater + index, v);
  }
  public final void putLong (long v)
  {
    byteOrdered.pokeLong(this, v);
  }
  
  public final void putLong (ByteOrder byteOrder, long v)
  {
      ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    byteOrdered.pokeLong(this, v);
  }
  
  public final void putLong (long index, long v)
  {
    byteOrdered.pokeLong(this, lowWater + index, v);
  }
  
  public final void putLong (ByteOrder byteOrder, long index, long v)
  {
      ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    byteOrdered.pokeLong(this, lowWater + index, v);
  }
  
  public final void putFloat (float v)
  {
    byteOrdered.pokeFloat(this, v);
  }

  public final void putFloat (ByteOrder byteOrder, float v)
  {
      ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    byteOrdered.pokeFloat(this, v);
  }

  public final void putFloat (long index, float v)
  {
    byteOrdered.pokeFloat(this, lowWater + index, v);
  }

  public final void putFloat (ByteOrder byteOrder, long index, float v)
  {
      ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    byteOrdered.pokeFloat(this, lowWater + index, v);
  }

  public final void putDouble (double v)
  {
    byteOrdered.pokeDouble(this, v);
  }

  public final void putDouble (ByteOrder byteOrder, double v)
  {
      ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    byteOrdered.pokeDouble(this, v);
  }
  
  public final void putDouble (long index, double v)
  {
    byteOrdered.pokeDouble(this, lowWater + index, v);
  }

  public final void putDouble (ByteOrder byteOrder, long index, double v)
  {
      ByteOrdered byteOrdered = ByteOrdered.order(byteOrder);
    byteOrdered.pokeDouble(this, lowWater + index, v);
  }
  
  public final long getWord ()
  {
    return wordSized.getWord(this);
  }

  public final long getUWord ()
  {
    return wordSized.getUWord(this);
  }

  public final void putWord (long w)
  {
    wordSized.putWord(this, w);
  }

  public final void putUWord (long w)
  {
    wordSized.putUWord(this, w);
  }

  protected WordSized wordSized;

  public ByteBuffer wordSize (int w)
  {
    wordSized = WordSized.wordSize(w);
    return this;
  }

  public int wordSize ()
  {
    return wordSized.wordSize;
  }

  public ByteBuffer get (StringBuffer string)
  {
    string.setLength(0);
    while (hasRemaining())
      {
        byte b = (byte) peek(cursor++);
        if (b == 0)
          break;
        string.append((char) b);
      }
    return this;
  }

  public ByteBuffer get (long index, StringBuffer string)
  {
    string.setLength(0);
    long offset = lowWater + index;
    while (ULong.LT(offset, bound))
      {
        byte b = (byte) peek(offset);
        if (b == 0)
          break;
        string.append((char) b);
        offset++;
      }
    return this;
  }

  public ByteBuffer get (long index, long len, StringBuffer string)
  {
    string.setLength(0);
    long offset = lowWater + index;
    while (ULong.LT(offset, bound) && len-- > 0)
      {
        byte b = (byte) peek(offset);
        if (b == 0)
          break;
        string.append((char) b);
        offset++;
      }
    return this;
  }

}
