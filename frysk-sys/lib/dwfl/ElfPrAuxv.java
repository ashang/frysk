// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, 2008 Red Hat Inc.
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

package lib.dwfl;

import inua.eio.ByteBuffer;
import inua.eio.ArrayByteBuffer;
import inua.eio.ByteOrder;

/**
 * Java Representation of the the NT_AUXV notes secion found in core
 * files
 **/
public class ElfPrAuxv extends ElfNhdr.ElfNoteSectionEntry {

    private byte[] noteData;
    private ByteBuffer noteBuffer;

    public ElfPrAuxv(int length, int wordSize, ByteOrder byteOrder) {
	noteData = new byte[length * 2 * wordSize];
	noteBuffer = new ArrayByteBuffer(noteData);
	noteBuffer.order(byteOrder);
	noteBuffer.wordSize(wordSize);
    }

    private ElfPrAuxv(Elf elf, byte[] noteData) {
	this.noteData = noteData;
	noteBuffer = new ArrayByteBuffer(noteData);
	ElfEHeader header = elf.getEHeader();
	noteBuffer.order(header.getByteOrder());
	switch (header.machine) {
	case ElfEMachine.EM_386:
	case ElfEMachine.EM_PPC:
	    noteBuffer.wordSize(4);
	    break;
	case ElfEMachine.EM_X86_64:
	case ElfEMachine.EM_PPC64:
	    noteBuffer.wordSize(8);
	    break;
	default:
	    return;
	}
    }

    public static ElfPrAuxv decode(ElfData noteData) {
	final byte data[] = getNoteData(noteData);
	ElfPrAuxv auxData = new ElfPrAuxv(noteData.getParent(), data);
	return auxData;
    }

    /**
     *
     * Return auxv data, in raw form
     *
     * @return: buffer - byte[] array containing buffer
     *
     */
    public ByteBuffer getByteBuffer() {
	return noteBuffer;
    }

    public byte[] getByteArray() {
	return noteData;
    }

    /**
     * Returns the entry size associated with this notes buffer.
     */
    public long getEntrySize() {
	return noteData.length;
    }

    /**
     * This is called when the notes section is filled.  Fill the
     * passed buffer with your own data, starting at startAddress
     */
    public long fillMemRegion(byte[] buffer, long startAddress) {
	System.arraycopy(noteData, 0, buffer, (int)startAddress,
			 noteData.length);
	return noteData.length;
    }

    private native static byte[] getNoteData(ElfData data);
}
