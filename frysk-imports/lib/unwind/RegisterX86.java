

package lib.unwind;

public class RegisterX86
{
  // General registers
  public static final RegisterX86 EAX = new RegisterX86(0);

  public static final RegisterX86 EDX = new RegisterX86(1);

  public static final RegisterX86 ECX = new RegisterX86(2);

  public static final RegisterX86 EBX = new RegisterX86(3);

  public static final RegisterX86 ESI = new RegisterX86(4);

  public static final RegisterX86 EDI = new RegisterX86(5);

  public static final RegisterX86 EBP = new RegisterX86(6);

  public static final RegisterX86 ESP = new RegisterX86(7);

  public static final RegisterX86 EIP = new RegisterX86(8);

  public static final RegisterX86 EFLAGS = new RegisterX86(9);

  public static final RegisterX86 TRAPNO = new RegisterX86(10);

  // MMX/Stacked-fp
  public static final RegisterX86 STO = new RegisterX86(11);

  public static final RegisterX86 ST1 = new RegisterX86(12);

  public static final RegisterX86 ST2 = new RegisterX86(13);

  public static final RegisterX86 ST3 = new RegisterX86(14);

  public static final RegisterX86 ST4 = new RegisterX86(15);

  public static final RegisterX86 ST5 = new RegisterX86(16);

  public static final RegisterX86 ST6 = new RegisterX86(17);

  public static final RegisterX86 ST7 = new RegisterX86(18);

  public static final RegisterX86 FCW = new RegisterX86(19);

  public static final RegisterX86 FSW = new RegisterX86(20);

  public static final RegisterX86 FTW = new RegisterX86(21);

  public static final RegisterX86 FOP = new RegisterX86(22);

  public static final RegisterX86 FCS = new RegisterX86(23);

  public static final RegisterX86 FIP = new RegisterX86(24);

  public static final RegisterX86 FEA = new RegisterX86(25);

  public static final RegisterX86 FDS = new RegisterX86(26);

  // SSE registers
  public static final RegisterX86 XMMO_lo = new RegisterX86(27);

  public static final RegisterX86 XMMO_hi = new RegisterX86(28);

  public static final RegisterX86 XMM1_lo = new RegisterX86(29);

  public static final RegisterX86 XMM1_hi = new RegisterX86(30);

  public static final RegisterX86 XMM2_lo = new RegisterX86(31);

  public static final RegisterX86 XMM2_hi = new RegisterX86(32);

  public static final RegisterX86 XMM3_lo = new RegisterX86(33);

  public static final RegisterX86 XMM3_hi = new RegisterX86(34);

  public static final RegisterX86 XMM4_lo = new RegisterX86(35);

  public static final RegisterX86 XMM4_hi = new RegisterX86(36);

  public static final RegisterX86 XMM5_lo = new RegisterX86(37);

  public static final RegisterX86 XMM5_hi = new RegisterX86(38);

  public static final RegisterX86 XMM6_lo = new RegisterX86(39);

  public static final RegisterX86 XMM6_hi = new RegisterX86(40);

  public static final RegisterX86 XMM7_lo = new RegisterX86(41);

  public static final RegisterX86 XMM7_hi = new RegisterX86(42);

  public static final RegisterX86 MXCSR = new RegisterX86(43);

  // Segment registers
  public static final RegisterX86 GS = new RegisterX86(44);

  public static final RegisterX86 FS = new RegisterX86(45);

  public static final RegisterX86 ES = new RegisterX86(46);

  public static final RegisterX86 DS = new RegisterX86(47);

  public static final RegisterX86 SS = new RegisterX86(48);

  public static final RegisterX86 CS = new RegisterX86(49);

  public static final RegisterX86 TSS = new RegisterX86(50);

  public static final RegisterX86 LDT = new RegisterX86(51);

  // Frame info (read only)
  public static final RegisterX86 CFA = new RegisterX86(52);

  public static final RegisterX86 TDEP_LAST_REG = LDT;

  public static final RegisterX86 TDEP_IP = EIP;

  public static final RegisterX86 TDEP_SP = CFA;

  public static final RegisterX86 TDEP_EH = EAX;

  private final static RegisterX86[] regs = { EAX, EDX, ECX, EBX, ESI, EDI, EBP, ESP,
                                      EIP, EFLAGS, TRAPNO,

                                      // MMX/Stacked-fp
                                      STO, ST1, ST2, ST3, ST4, ST5, ST6, ST7,

                                      FCW, FSW, FTW, FOP, FCS, FIP, FEA, FDS,

                                      // SSE registers
                                      XMMO_lo, XMMO_hi, XMM1_lo, XMM1_hi,
                                      XMM2_lo, XMM2_hi, XMM3_lo, XMM3_hi,
                                      XMM4_lo, XMM4_hi, XMM5_lo, XMM5_hi,
                                      XMM6_lo, XMM6_hi, XMM7_lo, XMM7_hi,

                                      MXCSR,

                                      // Segment registers
                                      GS, FS, ES, DS, SS, CS, TSS, LDT,

                                      // Frame info (read only)
                                      CFA };

  private int num;

  private RegisterX86 (int num)
  {
    this.num = num;
  }

  protected int getNum ()
  {
    return num;
  }

  public boolean equals (Object o)
  {
    return (o instanceof RegisterX86) && (((RegisterX86) o).num == this.num);
  }

  public static RegisterX86 intern (int val)
  {
    if(val > CFA.num || val < 0)
      return null;
    
    return regs[val];
  }
}
