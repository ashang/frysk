// This file is part of the program FRYSK.
//
// Copyright 2006, 2007 Red Hat Inc.
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

import inua.eio.ByteBuffer;

/**
 * Class for IA32 processes running on 64 bit machines.
 *
 * For the purposes of ptrace, Linux treats a 32 bit process like a 64
 * bit process, with the same user area and register layout that a 32
 * bit process would have; the 32 bit IA32 registers are stored in the
 * corresponding slots in the 64 bit structures. In order to debug a
 * 32 bit process we use a LinuxIa32 class with new overriding methods
 * that access registers,  but otherwise things "just work;" for
 * example, reading and writing memory is fine because the process has
 * the 32 bit memory map, memory will be read / written in 32 bit
 * chunks because the Isa work length is 32 bits, but the
 * PtraceByteBuffer class knows to access memory in 64 bit chunks.
 */
class LinuxIa32On64
extends LinuxIa32
{
  private static LinuxIa32On64 isa;

  /**
   * Returns the Isa singleton object. Note that the return type is
   * not LinuxIa32On64 because that would cause a conflict with the
   * isaSingleton method in the superclass.
   *
   * @return the Isa singleton object.
   */
  static LinuxIa32 isaSingleton()
  {
    if (isa == null)
      isa = new LinuxIa32On64();
    return isa;
  }
  // The Isa object used to actually access registers in the target.
  private final IsaX8664 isa64 = new IsaX8664();

  /**
   * Get the buffers used to access registers in the different
   * banks. This Isa has just one register bank -- the USR area
   * of the x8664 -- even though Ia32 has 3.
   *
   * @return the <code>ByteBuffer</code>s used to access registers.
   */
  public ByteBuffer[] getRegisterBankBuffers(int pid) 
  {
    return isa64.getRegisterBankBuffers(pid);
  }
  
}

  
