// This file is part of the program FRYSK.
//
// Copyright 2006 Red Hat Inc.
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
package lib.elf;

/**
 * Constants for the machine field of the Elf File's EHeader. Copied
 * from libelf/elf.h. 
 */
public class ElfEMachine
{
  // I would like to do away with the EM_ nonsense, but several
  // constants would be just numbers if we did that.
  public static final int EM_NONE = 0;		/* No machine */
  public static final int EM_M32 = 1;		/* AT&T WE 32100 */
  public static final int EM_SPARC = 2;		/* SUN SPARC */
  public static final int EM_386 = 3;		/* Intel 80386 */
  public static final int EM_68K = 4;		/* Motorola m68k family */
  public static final int EM_88K = 5;		/* Motorola m88k family */
  public static final int EM_860 = 7;		/* Intel 80860 */
  public static final int EM_MIPS = 8;		/* MIPS R3000 big-endian */
  public static final int EM_S370 = 9;		/* IBM System/370 */
  public static final int EM_MIPS_RS3_LE = 10;		/* MIPS R3000 little-endian */

  public static final int EM_PARISC = 15;		/* HPPA */
  public static final int EM_VPP500 = 17;		/* Fujitsu VPP500 */
  public static final int EM_SPARC32PLUS = 18;		/* Sun's "v8plus" */
  public static final int EM_960 = 19;		/* Intel 80960 */
  public static final int EM_PPC = 20;		/* PowerPC */
  public static final int EM_PPC64 = 21;		/* PowerPC 64-bit */
  public static final int EM_S390 = 22;		/* IBM S390 */

  public static final int EM_V800 = 36;		/* NEC V800 series */
  public static final int EM_FR20 = 37;		/* Fujitsu FR20 */
  public static final int EM_RH32 = 38;		/* TRW RH-32 */
  public static final int EM_RCE = 39;		/* Motorola RCE */
  public static final int EM_ARM = 40;		/* ARM */
  public static final int EM_FAKE_ALPHA = 41;		/* Digital Alpha */
  public static final int EM_SH = 42;		/* Hitachi SH */
  public static final int EM_SPARCV9 = 43;		/* SPARC v9 64-bit */
  public static final int EM_TRICORE = 44;		/* Siemens Tricore */
  public static final int EM_ARC = 45;		/* Argonaut RISC Core */
  public static final int EM_H8_300 = 46;		/* Hitachi H8/300 */
  public static final int EM_H8_300H = 47;		/* Hitachi H8/300H */
  public static final int EM_H8S = 48;		/* Hitachi H8S */
  public static final int EM_H8_500 = 49;		/* Hitachi H8/500 */
  public static final int EM_IA_64 = 50;		/* Intel Merced */
  public static final int EM_MIPS_X = 51;		/* Stanford MIPS-X */
  public static final int EM_COLDFIRE = 52;		/* Motorola Coldfire */
  public static final int EM_68HC12 = 53;		/* Motorola M68HC12 */
  public static final int EM_MMA = 54;		/* Fujitsu MMA Multimedia Accelerator*/
  public static final int EM_PCP = 55;		/* Siemens PCP */
  public static final int EM_NCPU = 56;		/* Sony nCPU embeeded RISC */
  public static final int EM_NDR1 = 57;		/* Denso NDR1 microprocessor */
  public static final int EM_STARCORE = 58;		/* Motorola Start*Core processor */
  public static final int EM_ME16 = 59;		/* Toyota ME16 processor */
  public static final int EM_ST100 = 60;		/* STMicroelectronic ST100 processor */
  public static final int EM_TINYJ = 61;		/* Advanced Logic Corp. Tinyj emb.fam*/
  public static final int EM_X86_64 = 62;		/* AMD x86-64 architecture */
  public static final int EM_PDSP = 63;		/* Sony DSP Processor */

  public static final int EM_FX66 = 66;		/* Siemens FX66 microcontroller */
  public static final int EM_ST9PLUS = 67;		/* STMicroelectronics ST9+ 8/16 mc */
  public static final int EM_ST7 = 68;		/* STmicroelectronics ST7 8 bit mc */
  public static final int EM_68HC16 = 69;		/* Motorola MC68HC16 microcontroller */
  public static final int EM_68HC11 = 70;		/* Motorola MC68HC11 microcontroller */
  public static final int EM_68HC08 = 71;		/* Motorola MC68HC08 microcontroller */
  public static final int EM_68HC05 = 72;		/* Motorola MC68HC05 microcontroller */
  public static final int EM_SVX = 73;		/* Silicon Graphics SVx */
  public static final int EM_ST19 = 74;		/* STMicroelectronics ST19 8 bit mc */
  public static final int EM_VAX = 75;		/* Digital VAX */
  public static final int EM_CRIS = 76;		/* Axis Communications 32-bit embedded processor */
  public static final int EM_JAVELIN = 77;		/* Infineon Technologies 32-bit embedded processor */
  public static final int EM_FIREPATH = 78;		/* Element 14 64-bit DSP Processor */
  public static final int EM_ZSP = 79;		/* LSI Logic 16-bit DSP Processor */
  public static final int EM_MMIX = 80;		/* Donald Knuth's educational 64-bit processor */
  public static final int EM_HUANY = 81;		/* Harvard University machine-independent object files */
  public static final int EM_PRISM = 82;		/* SiTera Prism */
  public static final int EM_AVR = 83;		/* Atmel AVR 8-bit microcontroller */
  public static final int EM_FR30 = 84;		/* Fujitsu FR30 */
  public static final int EM_D10V = 85;		/* Mitsubishi D10V */
  public static final int EM_D30V = 86;		/* Mitsubishi D30V */
  public static final int EM_V850 = 87;		/* NEC v850 */
  public static final int EM_M32R = 88;		/* Mitsubishi M32R */
  public static final int EM_MN10300 = 89;		/* Matsushita MN10300 */
  public static final int EM_MN10200 = 90;		/* Matsushita MN10200 */
  public static final int EM_PJ = 91;		/* picoJava */
  public static final int EM_OPENRISC = 92;		/* OpenRISC 32-bit embedded processor */
  public static final int EM_ARC_A5 = 93;		/* ARC Cores Tangent-A5 */
  public static final int EM_XTENSA = 94;		/* Tensilica Xtensa Architecture */
  public static final int EM_NUM = 95;

}