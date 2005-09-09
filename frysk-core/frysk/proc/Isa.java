// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
package frysk.proc;

import java.util.*;
import util.eio.ByteOrder;

class Isa
{
    List registers = new ArrayList ();
    Iterator RegisterIterator ()
    {
	return registers.iterator ();
    }

    Register getRegisterByName (String name)
    {
	throw new RuntimeException ("not implemented");
    }

    long pc (Task task)
    {
	throw new RuntimeException ("not implemented");
    }

    int wordSize = 4;
    ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

    // int addressSize;
    // InstructionSet;
    // FloatingPointFormat;
    // Breakpoint;
    // howToDoWatchpoints;
    // howToComputePcAfterTrap;
    // howToStepOutOfRange;
}
