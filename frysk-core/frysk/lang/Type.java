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

package frysk.lang;

/**
 * Holds the type of a Variable and also defines possible operations. 
 * Classes extended from this type will have to define the individual operation that are defined on those types.
 * e.g. addition operation may be defined for the integer type.
 */

public abstract class Type
{
  protected int _size;
  protected int _endian;
  protected int _typeId;
  protected String _name;

  Type(int size, int endian, int typeId)
  {
    this(size, endian, typeId, "");
  }

  Type(int size, int endian, int typeId, String name)
  {
    _size = size;
    _endian = endian;
    _typeId = typeId;
    _name = name;
  }

  int getSize() { return _size;}
  int getEndian() { return _endian;}
  int getTypeId() { return _typeId;}
  String getName() { return _name;}

  public String toString() {return _name;}


  public abstract Variable add(Variable var1, Variable var2); 
  public abstract Variable newShortVariable(ShortType type, Variable val);
  public abstract Variable newIntegerVariable(IntegerType type, Variable val);
  public abstract Variable newVariable(Type type, Variable val);

}
