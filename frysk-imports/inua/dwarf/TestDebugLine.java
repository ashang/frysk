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
package inua.dwarf;

class TestDebugLine
    extends TestLib
{
    public static void main (String[] args)
    {
	TestLib test = new TestLib ();
	test.debugLine = new short[] {
	    0x00, 0x00, 0x02, 0xa6,
	    0x00, 0x02, 0x00, 0x00,
	    0x01, 0x9c, 0x04, 0x01,
	    0xfb, 0x0e, 0x0a, 0x00,

	    0x01, 0x01, 0x01, 0x01,
	    0x00, 0x00, 0x00, 0x01,
	    0x2e, 0x2e, 0x2f, 0x76,
	    0x65, 0x6e, 0x75, 0x73,

	    0x2f, 0x75, 0x74, 0x69,
	    0x6c, 0x2f, 0x65, 0x69,
	    0x6f, 0x00, 0x6a, 0x61,
	    0x76, 0x61, 0x2f, 0x6c,

	    0x61, 0x6e, 0x67, 0x00,
	    0x6a, 0x61, 0x76, 0x61,
	    0x2f, 0x69, 0x6f, 0x00,
	    0x6a, 0x61, 0x76, 0x61,

	    0x2f, 0x75, 0x74, 0x69,
	    0x6c, 0x00, 0x67, 0x6e,
	    0x75, 0x2f, 0x67, 0x63,
	    0x6a, 0x2f, 0x72, 0x75,

	    0x6e, 0x74, 0x69, 0x6d,
	    0x65, 0x00, 0x6a, 0x61,
	    0x76, 0x61, 0x2f, 0x6c,
	    0x61, 0x6e, 0x67, 0x2f,

	    0x72, 0x65, 0x66, 0x6c,
	    0x65, 0x63, 0x74, 0x00,
	    0x00, 0x57, 0x6f, 0x72,
	    0x64, 0x53, 0x69, 0x7a,

	    0x65, 0x64, 0x2e, 0x6a,
	    0x61, 0x76, 0x61, 0x00,
	    0x01, 0x00, 0x00, 0x4f,
	    0x62, 0x6a, 0x65, 0x63,

	    0x74, 0x2e, 0x6a, 0x61,
	    0x76, 0x61, 0x00, 0x02,
	    0x00, 0x00, 0x43, 0x6c,
	    0x61, 0x73, 0x73, 0x2e,

	    0x6a, 0x61, 0x76, 0x61,
	    0x00, 0x02, 0x00, 0x00,
	    0x53, 0x65, 0x72, 0x69,
	    0x61, 0x6c, 0x69, 0x7a,

	    0x61, 0x62, 0x6c, 0x65,
	    0x2e, 0x6a, 0x61, 0x76,
	    0x61, 0x00, 0x03, 0x00,
	    0x00, 0x53, 0x74, 0x72,

	    0x69, 0x6e, 0x67, 0x2e,
	    0x6a, 0x61, 0x76, 0x61,
	    0x00, 0x02, 0x00, 0x00,
	    0x43, 0x6f, 0x6d, 0x70,

	    0x61, 0x72, 0x61, 0x62,
	    0x6c, 0x65, 0x2e, 0x6a,
	    0x61, 0x76, 0x61, 0x00,
	    0x02, 0x00, 0x00, 0x43,

	    0x68, 0x61, 0x72, 0x53,
	    0x65, 0x71, 0x75, 0x65,
	    0x6e, 0x63, 0x65, 0x2e,
	    0x6a, 0x61, 0x76, 0x61,

	    0x00, 0x02, 0x00, 0x00,
	    0x43, 0x6f, 0x6d, 0x70,
	    0x61, 0x72, 0x61, 0x74,
	    0x6f, 0x72, 0x2e, 0x6a,

	    0x61, 0x76, 0x61, 0x00,
	    0x04, 0x00, 0x00, 0x42,
	    0x79, 0x74, 0x65, 0x42,
	    0x75, 0x66, 0x66, 0x65,

	    0x72, 0x2e, 0x6a, 0x61,
	    0x76, 0x61, 0x00, 0x01,
	    0x00, 0x00, 0x53, 0x74,
	    0x72, 0x69, 0x6e, 0x67,

	    0x42, 0x75, 0x66, 0x66,
	    0x65, 0x72, 0x2e, 0x6a,
	    0x61, 0x76, 0x61, 0x00,
	    0x02, 0x00, 0x00, 0x53,

	    0x74, 0x72, 0x69, 0x6e,
	    0x67, 0x42, 0x75, 0x66,
	    0x66, 0x65, 0x72, 0x2e,
	    0x6a, 0x61, 0x76, 0x61,

	    0x00, 0x05, 0x00, 0x00,
	    0x43, 0x6f, 0x6e, 0x73,
	    0x74, 0x72, 0x75, 0x63,
	    0x74, 0x6f, 0x72, 0x2e,

	    0x6a, 0x61, 0x76, 0x61,
	    0x00, 0x06, 0x00, 0x00,
	    0x41, 0x63, 0x63, 0x65,
	    0x73, 0x73, 0x69, 0x62,

	    0x6c, 0x65, 0x4f, 0x62,
	    0x6a, 0x65, 0x63, 0x74,
	    0x2e, 0x6a, 0x61, 0x76,
	    0x61, 0x00, 0x06, 0x00,

	    0x00, 0x4d, 0x65, 0x6d,
	    0x62, 0x65, 0x72, 0x2e,
	    0x6a, 0x61, 0x76, 0x61,
	    0x00, 0x06, 0x00, 0x00,

	    0x42, 0x75, 0x66, 0x66,
	    0x65, 0x72, 0x2e, 0x6a,
	    0x61, 0x76, 0x61, 0x00,
	    0x01, 0x00, 0x00, 0x42,

	    0x79, 0x74, 0x65, 0x4f,
	    0x72, 0x64, 0x65, 0x72,
	    0x65, 0x64, 0x2e, 0x6a,
	    0x61, 0x76, 0x61, 0x00,

	    0x01, 0x00, 0x00, 0x42,
	    0x79, 0x74, 0x65, 0x4f,
	    0x72, 0x64, 0x65, 0x72,
	    0x2e, 0x6a, 0x61, 0x76,

	    0x61, 0x00, 0x01, 0x00,
	    0x00, 0x00, 0x00, 0x05,
	    0x02, 0x00, 0x00, 0x00,
	    0x00, 0x03, 0x12, 0x01,

	    0x03, 0x11, 0x2b, 0x03,
	    0x6f, 0x1d, 0x03, 0x11,
	    0x1d, 0x03, 0x68, 0x2b,
	    0x03, 0x18, 0x47, 0x03,

	    0x6f, 0x2b, 0x03, 0x12,
	    0x2b, 0x03, 0x6e, 0x1d,
	    0x03, 0x12, 0x1d, 0x03,
	    0x67, 0x2b, 0x03, 0x19,

	    0x47, 0x03, 0x6e, 0x2b,
	    0x03, 0x13, 0x2b, 0x03,
	    0x6d, 0x1d, 0x03, 0x13,
	    0x1d, 0x03, 0x66, 0x2b,

	    0x03, 0x1a, 0x47, 0x03,
	    0x6d, 0x2b, 0x03, 0x14,
	    0x2b, 0x03, 0x6c, 0x1d,
	    0x03, 0x14, 0x1d, 0x03,

	    0x65, 0x2b, 0x03, 0x1b,
	    0x47, 0x03, 0x6c, 0x2b,
	    0x03, 0x0a, 0x2b, 0x03,
	    0x76, 0x1d, 0x03, 0x0a,

	    0x1d, 0x03, 0x6f, 0x2b,
	    0x03, 0x11, 0x2b, 0x03,
	    0x6f, 0x2b, 0x03, 0x11,
	    0x2b, 0x03, 0x76, 0x47,

	    0x03, 0x0b, 0x2b, 0x03,
	    0x75, 0x1d, 0x03, 0x0b,
	    0x1d, 0x03, 0x6e, 0x2b,
	    0x03, 0x12, 0x47, 0x03,

	    0x75, 0x2b, 0x03, 0x0c,
	    0x2b, 0x03, 0x74, 0x1d,
	    0x03, 0x0c, 0x1d, 0x03,
	    0x6d, 0x39, 0x03, 0x13,

	    0x47, 0x03, 0x74, 0x1d,
	    0x03, 0x0d, 0x2b, 0x03,
	    0x73, 0x1d, 0x03, 0x0d,
	    0x1d, 0x03, 0x6c, 0x2b,

	    0x03, 0x14, 0x47, 0x03,
	    0x73, 0x2b, 0x2e, 0x1a,
	    0x20, 0x03, 0x76, 0x2b,
	    0x03, 0x0a, 0x2b, 0x03,

	    0x76, 0x2b, 0x03, 0x0a,
	    0x2b, 0x44, 0x2f, 0x19,
	    0x21, 0x03, 0x75, 0x2b,
	    0x03, 0x0b, 0x2b, 0x03,

	    0x75, 0x2b, 0x03, 0x0b,
	    0x2b, 0x43, 0x30, 0x18,
	    0x22, 0x03, 0x74, 0x39,
	    0x03, 0x0c, 0x47, 0x18,

	    0x31, 0x03, 0x7a, 0x1d,
	    0x23, 0x03, 0x73, 0x39,
	    0x03, 0x0d, 0x47, 0x03,
	    0x71, 0x1d, 0x71, 0x1e,

	    0x1e, 0x03, 0x75, 0x71,
	    0x39, 0x8d, 0x39, 0x8d,
	    0x39, 0x03, 0x2b, 0x8d,
	    0x7f, 0x2c, 0x56, 0xee,

	    0x2a, 0x08, 0x32, 0x6c,
	    0x29, 0x03, 0x55, 0x71,
	    0x03, 0x12, 0x2b, 0x03,
	    0x6e, 0x47, 0x03, 0x12,

	    0x39, 0x03, 0x6e, 0x08,
	    0xa9, 0x02, 0x06, 0x00,
	    0x01, 0x01
	};

	inua.Misc.verifyPrint (new PrintDebugLine (test), new String[] {
"",
"Dump of debug contents of section .debug_line:",
"",
"  Length:                      678",
"  DWARF Version:               2",
"  Prologue Length:             412",
"  Minimum Instruction Length:  4",
"  Initial value of 'is_stmt':  1",
"  Line Base:                   -5",
"  Line Range:                  14",
"  Opcode Base:                 10",
"  (Pointer size:               4)",
"",
" Opcodes:",
"  Opcode 1 has 0 args",
"  Opcode 2 has 1 args",
"  Opcode 3 has 1 args",
"  Opcode 4 has 1 args",
"  Opcode 5 has 1 args",
"  Opcode 6 has 0 args",
"  Opcode 7 has 0 args",
"  Opcode 8 has 0 args",
"  Opcode 9 has 1 args",
"",
" The Directory Table:",
"  ../venus/util/eio",
"  java/lang",
"  java/io",
"  java/util",
"  gnu/gcj/runtime",
"  java/lang/reflect",
"",
" The File Name Table:",
"  Entry	Dir	Time	Size	Name",
"  1	1	0	0	WordSized.java",
"  2	2	0	0	Object.java",
"  3	2	0	0	Class.java",
"  4	3	0	0	Serializable.java",
"  5	2	0	0	String.java",
"  6	2	0	0	Comparable.java",
"  7	2	0	0	CharSequence.java",
"  8	4	0	0	Comparator.java",
"  9	1	0	0	ByteBuffer.java",
"  10	2	0	0	StringBuffer.java",
"  11	5	0	0	StringBuffer.java",
"  12	6	0	0	Constructor.java",
"  13	6	0	0	AccessibleObject.java",
"  14	6	0	0	Member.java",
"  15	1	0	0	Buffer.java",
"  16	1	0	0	ByteOrdered.java",
"  17	1	0	0	ByteOrder.java",
"",
" Line Number Statements:",
"  Extended opcode 2: set Address to 0x0",
"  Advance Line by 18 to 19",
"  Copy",
"  Advance Line by 17 to 36",
"  Special opcode 33: advance Address by 8 to 0x8 and Line by 0 to 36",
"  Advance Line by -17 to 19",
"  Special opcode 19: advance Address by 4 to 0xc and Line by 0 to 19",
"  Advance Line by 17 to 36",
"  Special opcode 19: advance Address by 4 to 0x10 and Line by 0 to 36",
"  Advance Line by -24 to 12",
"  Special opcode 33: advance Address by 8 to 0x18 and Line by 0 to 12",
"  Advance Line by 24 to 36",
"  Special opcode 61: advance Address by 16 to 0x28 and Line by 0 to 36",
"  Advance Line by -17 to 19",
"  Special opcode 33: advance Address by 8 to 0x30 and Line by 0 to 19",
"  Advance Line by 18 to 37",
"  Special opcode 33: advance Address by 8 to 0x38 and Line by 0 to 37",
"  Advance Line by -18 to 19",
"  Special opcode 19: advance Address by 4 to 0x3c and Line by 0 to 19",
"  Advance Line by 18 to 37",
"  Special opcode 19: advance Address by 4 to 0x40 and Line by 0 to 37",
"  Advance Line by -25 to 12",
"  Special opcode 33: advance Address by 8 to 0x48 and Line by 0 to 12",
"  Advance Line by 25 to 37",
"  Special opcode 61: advance Address by 16 to 0x58 and Line by 0 to 37",
"  Advance Line by -18 to 19",
"  Special opcode 33: advance Address by 8 to 0x60 and Line by 0 to 19",
"  Advance Line by 19 to 38",
"  Special opcode 33: advance Address by 8 to 0x68 and Line by 0 to 38",
"  Advance Line by -19 to 19",
"  Special opcode 19: advance Address by 4 to 0x6c and Line by 0 to 19",
"  Advance Line by 19 to 38",
"  Special opcode 19: advance Address by 4 to 0x70 and Line by 0 to 38",
"  Advance Line by -26 to 12",
"  Special opcode 33: advance Address by 8 to 0x78 and Line by 0 to 12",
"  Advance Line by 26 to 38",
"  Special opcode 61: advance Address by 16 to 0x88 and Line by 0 to 38",
"  Advance Line by -19 to 19",
"  Special opcode 33: advance Address by 8 to 0x90 and Line by 0 to 19",
"  Advance Line by 20 to 39",
"  Special opcode 33: advance Address by 8 to 0x98 and Line by 0 to 39",
"  Advance Line by -20 to 19",
"  Special opcode 19: advance Address by 4 to 0x9c and Line by 0 to 19",
"  Advance Line by 20 to 39",
"  Special opcode 19: advance Address by 4 to 0xa0 and Line by 0 to 39",
"  Advance Line by -27 to 12",
"  Special opcode 33: advance Address by 8 to 0xa8 and Line by 0 to 12",
"  Advance Line by 27 to 39",
"  Special opcode 61: advance Address by 16 to 0xb8 and Line by 0 to 39",
"  Advance Line by -20 to 19",
"  Special opcode 33: advance Address by 8 to 0xc0 and Line by 0 to 19",
"  Advance Line by 10 to 29",
"  Special opcode 33: advance Address by 8 to 0xc8 and Line by 0 to 29",
"  Advance Line by -10 to 19",
"  Special opcode 19: advance Address by 4 to 0xcc and Line by 0 to 19",
"  Advance Line by 10 to 29",
"  Special opcode 19: advance Address by 4 to 0xd0 and Line by 0 to 29",
"  Advance Line by -17 to 12",
"  Special opcode 33: advance Address by 8 to 0xd8 and Line by 0 to 12",
"  Advance Line by 17 to 29",
"  Special opcode 33: advance Address by 8 to 0xe0 and Line by 0 to 29",
"  Advance Line by -17 to 12",
"  Special opcode 33: advance Address by 8 to 0xe8 and Line by 0 to 12",
"  Advance Line by 17 to 29",
"  Special opcode 33: advance Address by 8 to 0xf0 and Line by 0 to 29",
"  Advance Line by -10 to 19",
"  Special opcode 61: advance Address by 16 to 0x100 and Line by 0 to 19",
"  Advance Line by 11 to 30",
"  Special opcode 33: advance Address by 8 to 0x108 and Line by 0 to 30",
"  Advance Line by -11 to 19",
"  Special opcode 19: advance Address by 4 to 0x10c and Line by 0 to 19",
"  Advance Line by 11 to 30",
"  Special opcode 19: advance Address by 4 to 0x110 and Line by 0 to 30",
"  Advance Line by -18 to 12",
"  Special opcode 33: advance Address by 8 to 0x118 and Line by 0 to 12",
"  Advance Line by 18 to 30",
"  Special opcode 61: advance Address by 16 to 0x128 and Line by 0 to 30",
"  Advance Line by -11 to 19",
"  Special opcode 33: advance Address by 8 to 0x130 and Line by 0 to 19",
"  Advance Line by 12 to 31",
"  Special opcode 33: advance Address by 8 to 0x138 and Line by 0 to 31",
"  Advance Line by -12 to 19",
"  Special opcode 19: advance Address by 4 to 0x13c and Line by 0 to 19",
"  Advance Line by 12 to 31",
"  Special opcode 19: advance Address by 4 to 0x140 and Line by 0 to 31",
"  Advance Line by -19 to 12",
"  Special opcode 47: advance Address by 12 to 0x14c and Line by 0 to 12",
"  Advance Line by 19 to 31",
"  Special opcode 61: advance Address by 16 to 0x15c and Line by 0 to 31",
"  Advance Line by -12 to 19",
"  Special opcode 19: advance Address by 4 to 0x160 and Line by 0 to 19",
"  Advance Line by 13 to 32",
"  Special opcode 33: advance Address by 8 to 0x168 and Line by 0 to 32",
"  Advance Line by -13 to 19",
"  Special opcode 19: advance Address by 4 to 0x16c and Line by 0 to 19",
"  Advance Line by 13 to 32",
"  Special opcode 19: advance Address by 4 to 0x170 and Line by 0 to 32",
"  Advance Line by -20 to 12",
"  Special opcode 33: advance Address by 8 to 0x178 and Line by 0 to 12",
"  Advance Line by 20 to 32",
"  Special opcode 61: advance Address by 16 to 0x188 and Line by 0 to 32",
"  Advance Line by -13 to 19",
"  Special opcode 33: advance Address by 8 to 0x190 and Line by 0 to 19",
"  Special opcode 36: advance Address by 8 to 0x198 and Line by 3 to 22",
"  Special opcode 16: advance Address by 4 to 0x19c and Line by -3 to 19",
"  Special opcode 22: advance Address by 4 to 0x1a0 and Line by 3 to 22",
"  Advance Line by -10 to 12",
"  Special opcode 33: advance Address by 8 to 0x1a8 and Line by 0 to 12",
"  Advance Line by 10 to 22",
"  Special opcode 33: advance Address by 8 to 0x1b0 and Line by 0 to 22",
"  Advance Line by -10 to 12",
"  Special opcode 33: advance Address by 8 to 0x1b8 and Line by 0 to 12",
"  Advance Line by 10 to 22",
"  Special opcode 33: advance Address by 8 to 0x1c0 and Line by 0 to 22",
"  Special opcode 58: advance Address by 16 to 0x1d0 and Line by -3 to 19",
"  Special opcode 37: advance Address by 8 to 0x1d8 and Line by 4 to 23",
"  Special opcode 15: advance Address by 4 to 0x1dc and Line by -4 to 19",
"  Special opcode 23: advance Address by 4 to 0x1e0 and Line by 4 to 23",
"  Advance Line by -11 to 12",
"  Special opcode 33: advance Address by 8 to 0x1e8 and Line by 0 to 12",
"  Advance Line by 11 to 23",
"  Special opcode 33: advance Address by 8 to 0x1f0 and Line by 0 to 23",
"  Advance Line by -11 to 12",
"  Special opcode 33: advance Address by 8 to 0x1f8 and Line by 0 to 12",
"  Advance Line by 11 to 23",
"  Special opcode 33: advance Address by 8 to 0x200 and Line by 0 to 23",
"  Special opcode 57: advance Address by 16 to 0x210 and Line by -4 to 19",
"  Special opcode 38: advance Address by 8 to 0x218 and Line by 5 to 24",
"  Special opcode 14: advance Address by 4 to 0x21c and Line by -5 to 19",
"  Special opcode 24: advance Address by 4 to 0x220 and Line by 5 to 24",
"  Advance Line by -12 to 12",
"  Special opcode 47: advance Address by 12 to 0x22c and Line by 0 to 12",
"  Advance Line by 12 to 24",
"  Special opcode 61: advance Address by 16 to 0x23c and Line by 0 to 24",
"  Special opcode 14: advance Address by 4 to 0x240 and Line by -5 to 19",
"  Special opcode 39: advance Address by 8 to 0x248 and Line by 6 to 25",
"  Advance Line by -6 to 19",
"  Special opcode 19: advance Address by 4 to 0x24c and Line by 0 to 19",
"  Special opcode 25: advance Address by 4 to 0x250 and Line by 6 to 25",
"  Advance Line by -13 to 12",
"  Special opcode 47: advance Address by 12 to 0x25c and Line by 0 to 12",
"  Advance Line by 13 to 25",
"  Special opcode 61: advance Address by 16 to 0x26c and Line by 0 to 25",
"  Advance Line by -15 to 10",
"  Special opcode 19: advance Address by 4 to 0x270 and Line by 0 to 10",
"  Special opcode 103: advance Address by 28 to 0x28c and Line by 0 to 10",
"  Special opcode 20: advance Address by 4 to 0x290 and Line by 1 to 11",
"  Special opcode 20: advance Address by 4 to 0x294 and Line by 1 to 12",
"  Advance Line by -11 to 1",
"  Special opcode 103: advance Address by 28 to 0x2b0 and Line by 0 to 1",
"  Special opcode 47: advance Address by 12 to 0x2bc and Line by 0 to 1",
"  Special opcode 131: advance Address by 36 to 0x2e0 and Line by 0 to 1",
"  Special opcode 47: advance Address by 12 to 0x2ec and Line by 0 to 1",
"  Special opcode 131: advance Address by 36 to 0x310 and Line by 0 to 1",
"  Special opcode 47: advance Address by 12 to 0x31c and Line by 0 to 1",
"  Advance Line by 43 to 44",
"  Special opcode 131: advance Address by 36 to 0x340 and Line by 0 to 44",
"  Special opcode 117: advance Address by 32 to 0x360 and Line by 0 to 44",
"  Special opcode 34: advance Address by 8 to 0x368 and Line by 1 to 45",
"  Special opcode 76: advance Address by 20 to 0x37c and Line by 1 to 46",
"  Special opcode 228: advance Address by 64 to 0x3bc and Line by -1 to 45",
"  Special opcode 32: advance Address by 8 to 0x3c4 and Line by -1 to 44",
"  Advance PC by constant 68 to 0x408",
"  Special opcode 40: advance Address by 8 to 0x410 and Line by 7 to 51",
"  Special opcode 98: advance Address by 28 to 0x42c and Line by -5 to 46",
"  Special opcode 31: advance Address by 8 to 0x434 and Line by -2 to 44",
"  Advance Line by -43 to 1",
"  Special opcode 103: advance Address by 28 to 0x450 and Line by 0 to 1",
"  Advance Line by 18 to 19",
"  Special opcode 33: advance Address by 8 to 0x458 and Line by 0 to 19",
"  Advance Line by -18 to 1",
"  Special opcode 61: advance Address by 16 to 0x468 and Line by 0 to 1",
"  Advance Line by 18 to 19",
"  Special opcode 47: advance Address by 12 to 0x474 and Line by 0 to 19",
"  Advance Line by -18 to 1",
"  Advance PC by constant 68 to 0x4b8",
"  Special opcode 159: advance Address by 44 to 0x4e4 and Line by 0 to 1",
"  Advance PC by 24 to 4fc",
"  Extended opcode 1: End of Sequence",
"",
""
		     });
    }
}
