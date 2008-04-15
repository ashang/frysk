// This file is part of the program FRYSK.
//
// Copyright (C) 2006-2007 IBM
// Copyright 2007, Red Hat Inc.
//
// Contributed by
// Jose Flavio Aguilar Paulino <jflavio@br.ibm.com> <joseflavio@gmail.com>
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

package frysk.isa.registers;

import frysk.value.StandardTypes;

public class PPC64Registers extends Registers {

    /*
     * General Purpose Registers
     */

    public static final Register GPR0
	= new Register("gpr0", StandardTypes.INT64B_T);
    public static final Register GPR1
	= new Register("gpr1", StandardTypes.INT64B_T);
    public static final Register GPR2
	= new Register("gpr2", StandardTypes.INT64B_T);
    public static final Register GPR3
	= new Register("gpr3", StandardTypes.INT64B_T);
    public static final Register GPR4
	= new Register("gpr4", StandardTypes.INT64B_T);
    public static final Register GPR5
	= new Register("gpr5", StandardTypes.INT64B_T);
    public static final Register GPR6
	= new Register("gpr6", StandardTypes.INT64B_T);
    public static final Register GPR7
	= new Register("gpr7", StandardTypes.INT64B_T);
    public static final Register GPR8
	= new Register("gpr8", StandardTypes.INT64B_T);
    public static final Register GPR9
	= new Register("gpr9", StandardTypes.INT64B_T);
    public static final Register GPR10
	= new Register("gpr10", StandardTypes.INT64B_T);
    public static final Register GPR11
	= new Register("gpr11", StandardTypes.INT64B_T);
    public static final Register GPR12
	= new Register("gpr12", StandardTypes.INT64B_T);
    public static final Register GPR13
	= new Register("gpr13", StandardTypes.INT64B_T);
    public static final Register GPR14
	= new Register("gpr14", StandardTypes.INT64B_T);
    public static final Register GPR15
	= new Register("gpr15", StandardTypes.INT64B_T);
    public static final Register GPR16
	= new Register("gpr16", StandardTypes.INT64B_T);
    public static final Register GPR17
	= new Register("gpr17", StandardTypes.INT64B_T);
    public static final Register GPR18
	= new Register("gpr18", StandardTypes.INT64B_T);
    public static final Register GPR19
	= new Register("gpr19", StandardTypes.INT64B_T);
    public static final Register GPR20
	= new Register("gpr20", StandardTypes.INT64B_T);
    public static final Register GPR21
	= new Register("gpr21", StandardTypes.INT64B_T);
    public static final Register GPR22
	= new Register("gpr22", StandardTypes.INT64B_T);
    public static final Register GPR23
	= new Register("gpr23", StandardTypes.INT64B_T);
    public static final Register GPR24
	= new Register("gpr24", StandardTypes.INT64B_T);
    public static final Register GPR25
	= new Register("gpr25", StandardTypes.INT64B_T);
    public static final Register GPR26
	= new Register("gpr26", StandardTypes.INT64B_T);
    public static final Register GPR27
	= new Register("gpr27", StandardTypes.INT64B_T);
    public static final Register GPR28
	= new Register("gpr28", StandardTypes.INT64B_T);
    public static final Register GPR29
	= new Register("gpr29", StandardTypes.INT64B_T);
    public static final Register GPR30
	= new Register("gpr30", StandardTypes.INT64B_T);
    public static final Register GPR31
	= new Register("gpr31", StandardTypes.INT64B_T);

    /*
     * Special registers
     */

    /* Next Instruction Pointer register */
    public static final Register NIP
        = new Register("nip", StandardTypes.INT64B_T);

    /* Machine State Register */
    public static final Register MSR
        = new Register("msr", StandardTypes.INT64B_T);

    /* Orig_R3, this is the content of the R3 which
     * is lost when there is a system call (used to restart a syscall) */
     public static final Register ORIGR3
        = new Register("orig_r3", StandardTypes.INT64B_T);

    /* Counter reg */
    public static final Register CTR
            = new Register("ctr", StandardTypes.INT64B_T);

    /* Link Register
     * (after a branch-and-link addr is saved here,
     * to return from function calls) */
    public static final Register LR
            = new Register("lr", StandardTypes.VOIDPTR64B_T);

    /* Fixed-point status and control register */
    public static final Register XER
            = new Register("xer", StandardTypes.INT64B_T);

    /* Condition Code Register */
    /* (In truth it is a 32 bit wide reg, 
       but usually it comes with padding for 64) */
    public static final Register CCR
            = new Register("ccr", StandardTypes.INT64B_T);

    /* If software interrupts were enabled 
       (its a read only register used only by kernel) */
    public static final Register SOFTE
            = new Register("softe", StandardTypes.INT64B_T);

    /* Trap (when ocourred) */
    public static final Register TRAP
            = new Register("trap", StandardTypes.INT64B_T);

    /* DAR, Data Address Register */
    public static final Register DAR
            = new Register("dar", StandardTypes.VOIDPTR64B_T);

    /* DSISR, Data Storage Interrupt Status Register */
    public static final Register DSISR
            = new Register("dsisr", StandardTypes.INT64B_T);

    /* Result of a System call is stored by ptrace here */
    public static final Register RESULT
            = new Register("result", StandardTypes.INT64B_T);

    /* 
     * Floating Pointer Registers
     */
    public static final Register FPR0
	= new Register("fpr0", StandardTypes.FLOAT64B_T);
    public static final Register FPR1
	= new Register("fpr1", StandardTypes.FLOAT64B_T);
    public static final Register FPR2
	= new Register("fpr2", StandardTypes.FLOAT64B_T);
    public static final Register FPR3
	= new Register("fpr3", StandardTypes.FLOAT64B_T);
    public static final Register FPR4
	= new Register("fpr4", StandardTypes.FLOAT64B_T);
    public static final Register FPR5
	= new Register("fpr5", StandardTypes.FLOAT64B_T);
    public static final Register FPR6
	= new Register("fpr6", StandardTypes.FLOAT64B_T);
    public static final Register FPR7
	= new Register("fpr7", StandardTypes.FLOAT64B_T);
    public static final Register FPR8
	= new Register("fpr8", StandardTypes.FLOAT64B_T);
    public static final Register FPR9
	= new Register("fpr9", StandardTypes.FLOAT64B_T);
    public static final Register FPR10
	= new Register("fpr10", StandardTypes.FLOAT64B_T);
    public static final Register FPR11
	= new Register("fpr11", StandardTypes.FLOAT64B_T);
    public static final Register FPR12
	= new Register("fpr12", StandardTypes.FLOAT64B_T);
    public static final Register FPR13
	= new Register("fpr13", StandardTypes.FLOAT64B_T);
    public static final Register FPR14
	= new Register("fpr14", StandardTypes.FLOAT64B_T);
    public static final Register FPR15
	= new Register("fpr15", StandardTypes.FLOAT64B_T);
    public static final Register FPR16
	= new Register("fpr16", StandardTypes.FLOAT64B_T);
    public static final Register FPR17
	= new Register("fpr17", StandardTypes.FLOAT64B_T);
    public static final Register FPR18
	= new Register("fpr18", StandardTypes.FLOAT64B_T);
    public static final Register FPR19
	= new Register("fpr19", StandardTypes.FLOAT64B_T);
    public static final Register FPR20
	= new Register("fpr20", StandardTypes.FLOAT64B_T);
    public static final Register FPR21
	= new Register("fpr21", StandardTypes.FLOAT64B_T);
    public static final Register FPR22
	= new Register("fpr22", StandardTypes.FLOAT64B_T);
    public static final Register FPR23
	= new Register("fpr23", StandardTypes.FLOAT64B_T);
    public static final Register FPR24
	= new Register("fpr24", StandardTypes.FLOAT64B_T);
    public static final Register FPR25
	= new Register("fpr25", StandardTypes.FLOAT64B_T);
    public static final Register FPR26
	= new Register("fpr26", StandardTypes.FLOAT64B_T);
    public static final Register FPR27
	= new Register("fpr27", StandardTypes.FLOAT64B_T);
    public static final Register FPR28
	= new Register("fpr28", StandardTypes.FLOAT64B_T);
    public static final Register FPR29
	= new Register("fpr29", StandardTypes.FLOAT64B_T);
    public static final Register FPR30
	= new Register("fpr30", StandardTypes.FLOAT64B_T);
    public static final Register FPR31
	= new Register("fpr31", StandardTypes.FLOAT64B_T);

    /* Floating Point Status and Control Register
       (In truth it is a 32 bit wide reg,
        but usually it comes with padding for 64) */
    public static final Register FPSCR
        = new Register("fpscr", StandardTypes.INT32B_T);

    /*
     * Altivec (vector) registers.
     *
     * FIXME: The type of a vector register should be a union of the
     * possible types that can be stored there, like in GDB:
     *
     * (gdb) ptype $vr0
     * type = union __gdb_builtin_type_vec128 {
     *     int128_t uint128;
     *     float v4_float[4];
     *     int32_t v4_int32[4];
     *     int16_t v8_int16[8];
     *     int8_t v16_int8[16];
     * }
     */
    public static final Register VR0
	= new Register("vr0", StandardTypes.UINT128B_T);
    public static final Register VR1
	= new Register("vr1", StandardTypes.UINT128B_T);
    public static final Register VR2
	= new Register("vr2", StandardTypes.UINT128B_T);
    public static final Register VR3
	= new Register("vr3", StandardTypes.UINT128B_T);
    public static final Register VR4
	= new Register("vr4", StandardTypes.UINT128B_T);
    public static final Register VR5
	= new Register("vr5", StandardTypes.UINT128B_T);
    public static final Register VR6
	= new Register("vr6", StandardTypes.UINT128B_T);
    public static final Register VR7
	= new Register("vr7", StandardTypes.UINT128B_T);
    public static final Register VR8
	= new Register("vr8", StandardTypes.UINT128B_T);
    public static final Register VR9
	= new Register("vr9", StandardTypes.UINT128B_T);
    public static final Register VR10
	= new Register("vr10", StandardTypes.UINT128B_T);
    public static final Register VR11
	= new Register("vr11", StandardTypes.UINT128B_T);
    public static final Register VR12
	= new Register("vr12", StandardTypes.UINT128B_T);
    public static final Register VR13
	= new Register("vr13", StandardTypes.UINT128B_T);
    public static final Register VR14
	= new Register("vr14", StandardTypes.UINT128B_T);
    public static final Register VR15
	= new Register("vr15", StandardTypes.UINT128B_T);
    public static final Register VR16
	= new Register("vr16", StandardTypes.UINT128B_T);
    public static final Register VR17
	= new Register("vr17", StandardTypes.UINT128B_T);
    public static final Register VR18
	= new Register("vr18", StandardTypes.UINT128B_T);
    public static final Register VR19
	= new Register("vr19", StandardTypes.UINT128B_T);
    public static final Register VR20
	= new Register("vr20", StandardTypes.UINT128B_T);
    public static final Register VR21
	= new Register("vr21", StandardTypes.UINT128B_T);
    public static final Register VR22
	= new Register("vr22", StandardTypes.UINT128B_T);
    public static final Register VR23
	= new Register("vr23", StandardTypes.UINT128B_T);
    public static final Register VR24
	= new Register("vr24", StandardTypes.UINT128B_T);
    public static final Register VR25
	= new Register("vr25", StandardTypes.UINT128B_T);
    public static final Register VR26
	= new Register("vr26", StandardTypes.UINT128B_T);
    public static final Register VR27
	= new Register("vr27", StandardTypes.UINT128B_T);
    public static final Register VR28
	= new Register("vr28", StandardTypes.UINT128B_T);
    public static final Register VR29
	= new Register("vr29", StandardTypes.UINT128B_T);
    public static final Register VR30
	= new Register("vr30", StandardTypes.UINT128B_T);
    public static final Register VR31
	= new Register("vr31", StandardTypes.UINT128B_T);

    /* 
     * Altivec special registers 
     */
    public static final Register VSCR
	= new Register("vscr", StandardTypes.INT64B_T);
    public static final Register VRSAVE
	= new Register("vrsave", StandardTypes.INT64B_T);

    /* 
     * SPUs special registers 
     * (Cell-like processors)
     */
    public static final Register SPEACC
	= new Register("speacc", StandardTypes.INT64B_T);
    public static final Register SPEFSCR
	= new Register("spefscr", StandardTypes.INT64B_T);

    /* 
     * Defining Register Groups
     */
    public static final RegisterGroup GENERAL
        = new RegisterGroup("regs",
                  new Register[] {
                          GPR0 , GPR1 , GPR2 , GPR3 , GPR4 , GPR5 , GPR6 , GPR7 , GPR8 , GPR9 ,
                          GPR10, GPR11, GPR12, GPR13, GPR14, GPR15, GPR16, GPR17, GPR18, GPR19,
                          GPR20, GPR21, GPR22, GPR23, GPR24, GPR25, GPR26, GPR27, GPR28, GPR29,
                          GPR30, GPR31 });

    public static final RegisterGroup SPECIAL
        = new RegisterGroup("special",
                  new Register[] {
			NIP, MSR, ORIGR3, CTR, LR, XER, CCR,
			SOFTE, TRAP, FPSCR, DAR, DSISR, RESULT,
                        FPSCR, VRSAVE, VSCR, SPEACC, SPEFSCR });

    public static final RegisterGroup FLOATING_POINTER
        = new RegisterGroup("float",
                  new Register[] {
                          FPR0 , FPR1 , FPR2 , FPR3 , FPR4 , FPR5 , FPR6 , FPR7 , FPR8 , FPR9 ,
                          FPR10, FPR11, FPR12, FPR13, FPR14, FPR15, FPR16, FPR17, FPR18, FPR19,
                          FPR20, FPR21, FPR22, FPR23, FPR24, FPR25, FPR26, FPR27, FPR28, FPR29,
                          FPR30, FPR31 });

    public static final RegisterGroup VECTOR
        = new RegisterGroup("vector",
                  new Register[] {
                          VR0 , VR1 , VR2 , VR3 , VR4 , VR5 , VR6 , VR7 , VR8 , VR9 ,
                          VR10, VR11, VR12, VR13, VR14, VR15, VR16, VR17, VR18, VR19,
                          VR20, VR21, VR22, VR23, VR24, VR25, VR26, VR27, VR28, VR29,
                          VR30, VR31 });

    /*
     * Creating the special ALL group
     */
    public static final RegisterGroup ALL;
    static {
        Register[] allRegs = new Register[
                GENERAL.getRegisters().length +
                FLOATING_POINTER.getRegisters().length +
                VECTOR.getRegisters().length +
                SPECIAL.getRegisters().length];

        System.arraycopy(GENERAL.getRegisters(), 0,
                         allRegs, 0,
                         GENERAL.getRegisters().length);

        System.arraycopy(FLOATING_POINTER.getRegisters(), 0,
                         allRegs, GENERAL.getRegisters().length,
                         FLOATING_POINTER.getRegisters().length);

        System.arraycopy(VECTOR.getRegisters(), 0,
                         allRegs, GENERAL.getRegisters().length + FLOATING_POINTER.getRegisters().length,
                         VECTOR.getRegisters().length);

        System.arraycopy(SPECIAL.getRegisters(), 0,
                         allRegs, GENERAL.getRegisters().length + FLOATING_POINTER.getRegisters().length +
			 VECTOR.getRegisters().length, SPECIAL.getRegisters().length);

        ALL = new RegisterGroup("all", allRegs);
    }

    public Register getProgramCounter() {
	return NIP;
    }

    public Register getStackPointer() {
	return GPR1;
    }

    public RegisterGroup getDefaultRegisterGroup() {
	return GENERAL;
    }

    public RegisterGroup getAllRegistersGroup() {
	return ALL;
    }

    /* 
     * Default Constructor
     */
    PPC64Registers() {
	super(new RegisterGroup[] { GENERAL, FLOATING_POINTER, VECTOR, SPECIAL });
    }
}
