import java.io.*;
import java.lang.String;
import java.util.Hashtable;

public class Judb {
    private final int REGSET_GPRS  = 0;
    private final int REGSET_FPRS  = 1;
    private final int REGSET_FPRX  = 2;
    private final int REGSET_DESC  = 3;
    private final int REGSET_DEBUG = 4;

    private class RegDesc {
	String regid;
	int regset;
	int regnr;

	RegDesc(String ri, int rs, int rn) {
	    regid  = ri;
	    regset = rs;
	    regnr  = rn;
	}
    }

    final RegDesc regGPRS   = new RegDesc ("gprs",  REGSET_GPRS,  -1);
    final RegDesc regFPRS   = new RegDesc ("fprs",  REGSET_FPRS,  -1);
    final RegDesc regFPRX   = new RegDesc ("fprx",  REGSET_FPRX,  -1);
    final RegDesc regDESC   = new RegDesc ("desc",  REGSET_DESC,  -1);
    final RegDesc regDEBUG  = new RegDesc ("debug", REGSET_DEBUG, -1);
    final RegDesc regEBX    = new RegDesc ("ebx",   REGSET_GPRS,   0);
    final RegDesc regECX    = new RegDesc ("ecx",   REGSET_GPRS,   1);
    final RegDesc regEDX    = new RegDesc ("edx",   REGSET_GPRS,   2);
    final RegDesc regESI    = new RegDesc ("esi",   REGSET_GPRS,   3);
    final RegDesc regEDI    = new RegDesc ("edi",   REGSET_GPRS,   4);
    final RegDesc regEBP    = new RegDesc ("ebp",   REGSET_GPRS,   5);
    final RegDesc regEAX    = new RegDesc ("eax",   REGSET_GPRS,   6);
    final RegDesc regXDS    = new RegDesc ("xds",   REGSET_GPRS,   7);
    final RegDesc regXES    = new RegDesc ("xes",   REGSET_GPRS,   8);
    final RegDesc regXFS    = new RegDesc ("xfs",   REGSET_GPRS,   9);
    final RegDesc regXGS    = new RegDesc ("xgs",   REGSET_GPRS,  10);
    final RegDesc regOEAX   = new RegDesc ("orig_eax", REGSET_GPRS,  11);
    final RegDesc regEIP    = new RegDesc ("eip",   REGSET_GPRS,  12);
    final RegDesc regXCS    = new RegDesc ("xcs",   REGSET_GPRS,  13);
    final RegDesc regEFLAGS = new RegDesc ("eflags", REGSET_GPRS,  14);
    final RegDesc regESP    = new RegDesc ("esp",   REGSET_GPRS,  15);
    final RegDesc regXSS    = new RegDesc ("xss",   REGSET_GPRS,  16);
    final RegDesc regGDTR   = new RegDesc ("gdtr",  REGSET_DESC,   0);
    final RegDesc regLDTR   = new RegDesc ("ldtr",  REGSET_DESC,   1);
    final RegDesc regIDTR   = new RegDesc ("idtr",  REGSET_DESC,   2);
    
    private Hashtable reg_hash_table;


    private void doGetRegs (long udb_pid, String[] tokens) {
	int regset = -1;
	int regnr  = -1;
	
	if (1 < tokens.length) {
	    RegDesc rd = (RegDesc)reg_hash_table.get (tokens[1]);
	    if (null != rd) {
		regset = rd.regset;
		regnr  = rd.regnr;
		if ((-1 == regnr) && (2 < tokens.length)) {
		    RegDesc rd2 = (RegDesc)reg_hash_table.get (tokens[2]);
		    if (null != rd2) {
			if (rd2.regset == regset) regnr = rd2.regnr;
			else {
			    System.out.println ("Inconsistent argument.");
			    regset = -1;
			}
		    }
		    else {
			try {
			    regnr = Integer.parseInt (tokens[2]);
			} catch (NumberFormatException nfe) {
			    System.out.println ("Incvalid numeric argument.");
			    regset = -1;
			}
		    }
		}
	    }
	    else System.out.println ("keyword " + tokens[1] +
				     " not found.");
	}
	else System.out.println ("getregs requires a argument.");

	if (-1 != regset) {
	    System.out.println ("Found " + tokens[1] +
				" in set " + regset +
				" at " + regnr);
	}
    }
    
    private void doAttach (long udb_pid, String[] tokens) {
	if (1 < tokens.length) {
	    int pid = Integer.parseInt (tokens[1]);
	    System.out.print ("attaching " + pid);
	    int rc = Utrace.attach (udb_pid, pid, 1, 0);
	    System.out.println ("   rc = " + rc);
	}
	else System.out.println ("attach requires a argument.");
    }
    
    private void doDetach (long udb_pid, String[] tokens) {
	if (1 < tokens.length) {
	    int pid = Integer.parseInt (tokens[1]);
	    System.out.print ("detaching " + pid);
	    int rc = Utrace.detach (udb_pid, pid);
	    System.out.println ("   rc = " + rc);
	}
	else System.out.println ("detach requires a argument.");
    }
    
    private void doRun (long udb_pid, String[] tokens) {
	if (1 < tokens.length) {
	    int pid = Integer.parseInt (tokens[1]);
	    System.out.print ("running " + pid);
	    int rc = Utrace.run (udb_pid, pid);
	    System.out.println ("   rc = " + rc);
	}
	else System.out.println ("run requires a argument.");
    }
    
    private void doQuiesce (long udb_pid, String[] tokens) {
	if (1 < tokens.length) {
	    int pid = Integer.parseInt (tokens[1]);
	    System.out.print ("stopping " + pid);
	    int rc = Utrace.quiesce (udb_pid, pid);
	    System.out.println ("   rc = " + rc);
	}
	else System.out.println ("quiesce requires a argument.");
    }
    
    public Judb() {
	//	super ("Judb");
	
	reg_hash_table = new Hashtable();
	reg_hash_table.put (regGPRS.regid,   regGPRS);
	reg_hash_table.put (regFPRS.regid,   regFPRS);
	reg_hash_table.put (regFPRX.regid,   regFPRX); 
	reg_hash_table.put (regDESC.regid,   regDESC); 
	reg_hash_table.put (regDEBUG.regid,  regDEBUG);
	reg_hash_table.put (regEBX.regid,    regEBX);
	reg_hash_table.put (regECX.regid,    regECX); 
	reg_hash_table.put (regEDX.regid,    regEDX);  
	reg_hash_table.put (regESI.regid,    regESI);  
	reg_hash_table.put (regEDI.regid,    regEDI); 
	reg_hash_table.put (regEBP.regid,    regEBP); 
	reg_hash_table.put (regEAX.regid,    regEAX);  
	reg_hash_table.put (regXDS.regid,    regXDS);  
	reg_hash_table.put (regXES.regid,    regXES);  
	reg_hash_table.put (regXFS.regid,    regXFS);  
	reg_hash_table.put (regXGS.regid,    regXGS);  
	reg_hash_table.put (regOEAX.regid,   regOEAX);  
	reg_hash_table.put (regEIP.regid,    regEIP);
	reg_hash_table.put (regXCS.regid,    regXCS); 
	reg_hash_table.put (regEFLAGS.regid, regEFLAGS);
	reg_hash_table.put (regESP.regid,    regESP);
	reg_hash_table.put (regXSS.regid,    regXSS);  
	reg_hash_table.put (regGDTR.regid,   regGDTR); 
	reg_hash_table.put (regLDTR.regid,   regLDTR);
	reg_hash_table.put (regIDTR.regid,   regIDTR);
	
	int run = 1;

	String Curline = "";

	long udb_pid = Utrace.open();
	System.out.println ("opening client, pid = " + udb_pid);

	InputStreamReader converter = new InputStreamReader (System.in);
	BufferedReader in = new BufferedReader (converter);

	while (1 == run) {
	    System.out.print ("Judb: ");
	    try {
		Curline = in.readLine();

		String[] tokens = Curline.split("[ \t]+");

		if (0 < tokens.length) {
		    if (tokens[0].equals ("quit") ||
			tokens[0].equals ("q")) {
			run = 0;
		    }
		    else if (tokens[0].equals ("attach") ||
			     tokens[0].equals ("at")) {
			doAttach (udb_pid, tokens);
		    }
		    else if (tokens[0].equals ("detach") ||
			     tokens[0].equals ("det")) {
			doDetach (udb_pid, tokens);
		    }
		    else if (tokens[0].equals ("run") ||
			     tokens[0].equals ("r")) {
			doRun (udb_pid, tokens);
		    }
		    else if (tokens[0].equals ("quiesce") ||
			     tokens[0].equals ("halt") ||
			     tokens[0].equals ("stop")) {
			doQuiesce (udb_pid, tokens);
		    }
		    else if (tokens[0].equals ("printreg") ||
			     tokens[0].equals ("pr")) {
			doGetRegs (udb_pid, tokens);
		    }
		    else {
			System.out.println ("Sorry, haven't a clue what \""
					    + Curline + "\" means");
		    }
		}
	    } catch (IOException ioe) {
		System.out.println ("Huh?");
	    }
	}

	{
	    int urc = Utrace.unregister (udb_pid);
	    System.out.println ("unregister rc = " + urc);
	}
    }

    public static  void main(String args[]) {
        new Judb();
    }
}