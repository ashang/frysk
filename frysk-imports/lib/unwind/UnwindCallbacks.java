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

package lib.unwind;

public interface UnwindCallbacks
{
  /**
   * Retrieves the information about a procedure that will be needed to unwind
   * it. If needinfo is true the returned struct does not need to contain values
   * for format, unwind_info_size, and unwind_info
   * @param procInfo TODO
   * @param addressSpace The unwind address space
   * @param instructionAddress The address within the procedure whose
   *          information is needed
   * @param needInfo Whether or not to fill in extra information
   * 
   * @return The value of the pointer to the returned struct.
   */
  boolean findProcInfo (long procInfo, long addressSpace,
                     long instructionAddress, boolean needInfo);

  /**
   * Releases the resources allocated by the call to findProcInfo with needInfo
   * set to true.
   * 
   * @param addressSpace The unwind address space
   * @param procInfo The value of the pointer to the proc info struc to be
   *          freed.
   */
  void putUnwindInfo (long addressSpace, long procInfo);

  /**
   * Gets the address of the head of the dynamic unwind-info registration list,
   * or zero if no such record exist.
   * 
   * @param as
   * @return the address of the head of the list, or zero if no list exists.
   */
  long getDynInfoListAddr (long addressSpace);

  /**
   * Reads a word of memory from from the given address
   * 
   * @param as The unwind_addr_space
   * @param addr The address to read from
   * @return The word at addr.
   */
  long accessMem (long addressSpace, long addr);

  /**
   * Writes a word to memory at the given address
   * 
   * @param as The unwind_addr_space
   * @param addr The address to write to
   * @param value The value to write to addr.
   */
  void writeMem (long as, long addr, long value);

  /**
   * Read the value of the register with the given number
   * 
   * @param as The unwind_addr_space
   * @param regnum The register to read from
   * @return The value in the register
   */
  long accessReg (long as, long regnum);

  /**
   * Write the provided value into the given register
   * 
   * @param as The unwind_addr_space
   * @param regnum The register to write to
   * @param value The value to store in the register
   */
  void writeReg (long as, long regnum, long value);

  /**
   * Reads a floating point value from the provided register
   * 
   * @param as The unwind_addr_space
   * @param regnum The floating point register to read from
   * @return The value in the register
   */
  double accessFpreg (long as, long regnum);

  /**
   * Write the provided floating point value into the given register
   * 
   * @param as The unwind_addr_space
   * @param regnum The floating point register to write to
   * @param value The value to write to the register
   */
  void writeFpreg (long as, long regnum, double value);

  /**
   * Resumes execution within the current stack frame.
   * 
   * @param as The unw_addr_space
   * @param cp The cursor representing the stack frame to resume execution in
   * @return 0 on success.
   */
  int resume (long as, long cp);

  /**
   * Returns the name of a static (not dynamically generated) procedure.
   * 
   * @param as The unw_addr_space
   * @param addr An address within the procedure to get the name of
   * @return The name of the procedure
   */
  String getProcName (long as, long addr);

  /**
   * Returns the offset within the procudure that the current address is located
   * at
   * 
   * @param as The unw_addr_space
   * @param addr The address to get the offset of.
   * @return The offset from the start of the current procedure to addr.
   */
  long getProcOffset (long as, long addr);
}
