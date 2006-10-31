// This file is part of INUA.  Copyright 2006 Red Hat Inc.
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

/**
 * Class for doing operations on Java long values as if they were
 * unsigned. If you know Fortran, you're all set with the method names
 * :).
 */
public class ULong 
{
  public static final boolean EQ(long x1, long x2)
  {
    return x1 == x2;
  }

  public static final boolean NE(long x1, long x2)
  {
    return x1 != x2;
  }

  // Subtracting BIAS flips the sign bit. If the two values have the
  // same sign bit, then the sense of the comparison is preserved
  // without it. Otherwise, the sense of the signed comparison will
  // be reversed, which is what we want if the values are in fact
  // unsigned. Values with the sign bit set are <= 2^63 and are of
  // course greater than those without the sign bit set.
  private static final long BIAS = (1L << 63);
  
  public static final boolean LT(long x1, long x2) 
  {
    return (x1 - BIAS) < (x2 - BIAS);
  }

  public static final int compare(long x1, long x2) 
  {
    if (x1 == x2)
      return 0;
    else if (LT(x1, x2))
      return -1;
    else
      return 1;
  }
  
  public static final boolean LE(long x1, long x2) 
  {
    return (x1 - BIAS) <= (x2 - BIAS);
  }
  
  public static final boolean GT(long x1, long x2) 
  {
    return (x1 - BIAS) > (x2 - BIAS);
  }
  
  public static final boolean GE(long x1, long x2) 
  {
    return (x1 - BIAS) >= (x2 - BIAS);
  }
}
