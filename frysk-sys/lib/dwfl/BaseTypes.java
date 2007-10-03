
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
package lib.dwfl;

// This is a good candidate for 1.5 enum
public final class BaseTypes {
  public final static int baseTypeByte = 1,
  baseTypeUnsignedByte = 2,
  baseTypeChar = 3,
  baseTypeUnsignedChar = 4,
  baseTypeShort = 5,
  baseTypeUnsignedShort = 6,
  baseTypeUnicode = 7,
  baseTypeInteger = 8,
  baseTypeUnsignedInteger = 9,
  baseTypeLong = 10,
  baseTypeUnsignedLong = 11,
  baseTypeFloat = 12,
  baseTypeDouble = 13,
  baseTypeLongDouble = 14;
      
  public final static int getTypeSize(int type) 
  {
    switch (type)
    {
    case baseTypeByte:
      return 1;
    case baseTypeUnsignedByte:
      return 1;
    case baseTypeChar:
      return 2;
    case baseTypeUnsignedChar:
      return 2;
    case baseTypeShort:
      return 2;
    case baseTypeUnsignedShort:
      return 2;
    case baseTypeInteger:
      return 4;
    case baseTypeUnsignedInteger:
      return 4;
    case baseTypeLong:
      return 8;
    case baseTypeUnsignedLong:
      return 8;
    case baseTypeFloat:
      return 4;
    case baseTypeDouble:
      return 8;
    case baseTypeLongDouble:
      return 16;
    default:
      return 0;
    }
  }
      
  /**
   * @param type
   * @return true if type is a byte, short, or int.
   */
  public final static boolean isInteger(int type)
  {
    switch (type)
    {
    case baseTypeByte:
    case baseTypeUnsignedByte:
    case baseTypeShort:
    case baseTypeUnsignedShort:
    case baseTypeInteger:
    case baseTypeUnsignedInteger:
      return true;
    default:
      return false;
    }
  }
    
  /**
   * @param type
   * @return true if type is a byte, short, int, or long.
   */
  public final static boolean isLong(int type)
  {
    switch (type)
    {
    case baseTypeByte:
    case baseTypeUnsignedByte:
    case baseTypeShort:
    case baseTypeUnsignedShort:
    case baseTypeInteger:
    case baseTypeUnsignedInteger:
    case baseTypeLong:
    case baseTypeUnsignedLong:
      return true;
    default:
      return false;
    }
  }

  /**
   * @param type
   * @return true if type is a float or double.
   */
  public final static boolean isFloat(int type)
  {
    switch (type)
    {
    case baseTypeFloat:
    case baseTypeDouble:
    case baseTypeLongDouble:
      return true;
    default:
      return false;
    }
  }
}
