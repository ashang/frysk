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

abstract class ByteOrdered
{
    ByteOrder byteOrder;
    ByteOrdered (ByteOrder byteOrder)
    {
	this.byteOrder = byteOrder;
    }
    abstract short peekShort (ByteBuffer b);
    abstract short peekShort (ByteBuffer b, long cursor);
    abstract int peekUShort (ByteBuffer b);
    abstract int peekUShort (ByteBuffer b, long cursor);
    abstract int peekInt (ByteBuffer b);
    abstract int peekInt (ByteBuffer b, long cursor);
    abstract long peekUInt (ByteBuffer b);
    abstract long peekUInt (ByteBuffer b, long cursor);
    abstract long peekLong (ByteBuffer b);
    abstract long peekLong (ByteBuffer b, long cursor);
    abstract long peekULong (ByteBuffer b);
    abstract long peekULong (ByteBuffer b, long cursor);
    abstract float peekFloat (ByteBuffer b);
    abstract float peekFloat (ByteBuffer b, long cursor);
    abstract double peekDouble (ByteBuffer b);
    abstract double peekDouble (ByteBuffer b, long cursor);
    
    abstract void pokeShort (ByteBuffer b, short v);
    abstract void pokeShort (ByteBuffer b, long cursor, short v);
    abstract void pokeUShort (ByteBuffer b, int v);
    abstract void pokeUShort (ByteBuffer b, long cursor, int v);
    abstract void pokeInt (ByteBuffer b, int v);
    abstract void pokeInt (ByteBuffer b, long cursor, int v);
    abstract void pokeUInt (ByteBuffer b, long v);
    abstract void pokeUInt (ByteBuffer b, long cursor, long v);
    abstract void pokeLong (ByteBuffer b, long v);
    abstract void pokeLong (ByteBuffer b, long cursor, long v);
    abstract void pokeULong (ByteBuffer b, long v);
    abstract void pokeULong (ByteBuffer b, long cursor, long v);
    abstract void pokeFloat (ByteBuffer b, float v );
    abstract void pokeFloat (ByteBuffer b, long cursor, float v);
    abstract void pokeDouble (ByteBuffer b, double v );
    abstract void pokeDouble (ByteBuffer b, long cursor, double v);

    static final ByteOrdered[] byteOrders = {
	new ByteOrdered (ByteOrder.BIG_ENDIAN)
	{
	    short peekShort (ByteBuffer b) { return (short) b.peekBig (2); }
	    short peekShort (ByteBuffer b, long cursor) { return (short) b.peekBig (cursor, 2); }
	    int peekUShort (ByteBuffer b) { return (int) b.peekBig (2); }
	    int peekUShort (ByteBuffer b, long cursor) { return (int) b.peekBig (cursor, 2); }
	    int peekInt (ByteBuffer b) { return (int) b.peekBig (4); }
	    int peekInt (ByteBuffer b, long cursor) { return (int) b.peekBig (cursor, 4); }
	    long peekUInt (ByteBuffer b) { return b.peekBig (4); }
	    long peekUInt (ByteBuffer b, long cursor) { return b.peekBig (cursor, 4); }
	    long peekULong (ByteBuffer b) { return b.peekBig (8); }
	    long peekULong (ByteBuffer b, long cursor) { return b.peekBig (cursor, 8); }
	    long peekLong (ByteBuffer b) { return b.peekBig (8); }
	    long peekLong (ByteBuffer b, long cursor) { return b.peekBig (cursor, 8); }
	    float peekFloat (ByteBuffer b) { return b.peekBigFloat (4); }
	    float peekFloat (ByteBuffer b, long cursor) { return b.peekBigFloat (cursor, 4); }
	    double peekDouble (ByteBuffer b) { return b.peekBigDouble (8); }
	    double peekDouble (ByteBuffer b, long cursor) { return b.peekBigDouble (cursor, 8); }
	    
	    void pokeShort (ByteBuffer b, short v) { b.pokeBig (2, v); }
	    void pokeShort (ByteBuffer b, long cursor, short v) { b.pokeBig (cursor, 2, v); }
	    void pokeUShort (ByteBuffer b, int v) { b.pokeBig (2, v); }
	    void pokeUShort (ByteBuffer b, long cursor, int v) { b.pokeBig (cursor, 2, v); }
	    void pokeInt (ByteBuffer b, int v) { b.pokeBig (4, v); }
	    void pokeInt (ByteBuffer b, long cursor, int v) { b.pokeBig (cursor, 4, v); }
	    void pokeUInt (ByteBuffer b, long v) { b.pokeBig (4, v); }
	    void pokeUInt (ByteBuffer b, long cursor, long v) { b.pokeBig (cursor, 4, v); }
	    void pokeLong (ByteBuffer b, long v) { b.pokeBig (8, v); }
	    void pokeLong (ByteBuffer b, long cursor, long v) { b.pokeBig (cursor, 8, v); }
	    void pokeULong (ByteBuffer b, long v) { b.pokeBig (8, v); }
	    void pokeULong (ByteBuffer b, long cursor, long v) { b.pokeBig (cursor, 8, v); }
	    void pokeFloat (ByteBuffer b, float v) { b.pokeBig (4, v); }
	    void pokeFloat (ByteBuffer b, long cursor, float v) { b.pokeBig (cursor, 4, v); }
	    void pokeDouble (ByteBuffer b, double v) { b.pokeBig (4, v); }
	    void pokeDouble (ByteBuffer b, long cursor, double v) { b.pokeBig (cursor, 8, v); }
	},
	new ByteOrdered (ByteOrder.LITTLE_ENDIAN)
	{
	    short peekShort (ByteBuffer b) { return (short) b.peekLittle (2); }
	    short peekShort (ByteBuffer b, long cursor) { return (short) b.peekLittle (cursor, 2); }
	    int peekUShort (ByteBuffer b) { return (int) b.peekLittle (2); }
	    int peekUShort (ByteBuffer b, long cursor) { return (int) b.peekLittle (cursor, 2); }
	    int peekInt (ByteBuffer b) { return (int) b.peekLittle (4); }
	    int peekInt (ByteBuffer b, long cursor) { return (int) b.peekLittle (cursor, 4); }
	    long peekUInt (ByteBuffer b) { return b.peekLittle (4); }
	    long peekUInt (ByteBuffer b, long cursor) { return b.peekLittle (cursor, 4); }
	    long peekULong (ByteBuffer b) { return b.peekLittle (8); }
	    long peekULong (ByteBuffer b, long cursor) { return b.peekLittle (cursor, 8); }
	    long peekLong (ByteBuffer b) { return b.peekLittle (8); }
	    long peekLong (ByteBuffer b, long cursor) { return b.peekLittle (cursor, 8); }
	    float peekFloat (ByteBuffer b) { return b.peekLittleFloat (4); }
	    float peekFloat (ByteBuffer b, long cursor) { return b.peekLittleFloat (cursor, 4); }
	    double peekDouble (ByteBuffer b) { return b.peekLittleDouble (8); }
	    double peekDouble (ByteBuffer b, long cursor) { return b.peekLittleDouble (cursor, 8); }
	    void pokeShort (ByteBuffer b, short v) { b.pokeLittle (2, v); }
	    void pokeShort (ByteBuffer b, long cursor, short v) { b.pokeLittle (cursor, 2, v); }
	    void pokeUShort (ByteBuffer b, int v) { b.pokeLittle (2, v); }
	    void pokeUShort (ByteBuffer b, long cursor, int v) { b.pokeLittle (cursor, 2, v); }
	    void pokeInt (ByteBuffer b, int v) { b.pokeLittle (4, v); }
	    void pokeInt (ByteBuffer b, long cursor, int v) { b.pokeLittle (cursor, 4, v); }
	    void pokeUInt (ByteBuffer b, long v) { b.pokeLittle (4, v); }
	    void pokeUInt (ByteBuffer b, long cursor, long v) { b.pokeLittle (cursor, 4, v); }
	    void pokeLong (ByteBuffer b, long v) { b.pokeLittle (8, v); }
	    void pokeLong (ByteBuffer b, long cursor, long v) { b.pokeLittle (cursor, 8, v); }
	    void pokeULong (ByteBuffer b, long v) { b.pokeLittle (8, v); }
	    void pokeULong (ByteBuffer b, long cursor, long v) { b.pokeLittle (cursor, 8, v); }
	    void pokeFloat (ByteBuffer b, float v) { b.pokeLittle (4, v); }
	    void pokeFloat (ByteBuffer b, long cursor, float v) { b.pokeLittle (cursor, 4, v); }
	    void pokeDouble (ByteBuffer b, double v) { b.pokeLittle (8, v); }
	    void pokeDouble (ByteBuffer b, long cursor, double v) { b.pokeLittle (cursor, 8, v); }
	}
    };

    static final ByteOrdered order (ByteOrder bo)
    {
	for (int i = 0; i < byteOrders.length; i++) {
	    if (bo == byteOrders[i].byteOrder) {
		return byteOrders[i];
	    }
	}
	throw new RuntimeException ("Unknown byte-order " + bo);
    }

}
