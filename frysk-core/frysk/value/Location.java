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

/**
 * Location of a variable.
 */

package frysk.value;

import inua.eio.*;

class Location
{
  ArrayByteBuffer _location;
  int _index;

  Location(long capacity)  {
    this(new ArrayByteBuffer(capacity));
  }

  Location(ArrayByteBuffer location) {
    this(location, 0);
  }

  Location(ArrayByteBuffer location, int index) {
    _location = location;
    _index = 0;
  }

  public ByteBuffer getByteBuffer() { return _location;}
  double getDouble() { return _location.getDouble(_index); }
  float getFloat() { return _location.getFloat(_index); }
  long getLong() { return _location.getLong(_index); }
  int getInt() { return _location.getInt(_index); }
  short getShort() { return _location.getShort(_index); }
  byte getByte() { return (byte)_location.getByte(_index); }

  double getDouble(int idx) { return _location.getDouble(idx); }
  float getFloat(int idx) { return _location.getFloat(idx); }
  long getLong(int idx) { return _location.getLong(idx); }
  int getInt(int idx) { return _location.getInt(idx); }
  short getShort(int idx) { return _location.getShort(idx); }
  byte getByte(int idx) { return (byte)_location.getByte(idx); }
  
  void putDouble(double value)  {_location.putDouble(_index, value);}
  void putFloat(float value)  {_location.putFloat(_index, value);}
  void putLong(long value)  {_location.putLong(_index, value);}
  void putInt(int value)  {_location.putInt(_index, value);}
  void putShort(short value)  {_location.putShort(_index, value);}
  void putByte(byte value)  {_location.putByte(_index, (byte)value);}
}
