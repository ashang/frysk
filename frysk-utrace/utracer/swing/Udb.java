// This file is part of the Utracer kernel module and it's userspace
// interfaces. 
//
// Copyright 2007, Red Hat Inc.
//
// Utracer is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// Utracer is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Utracer; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
//
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of Utracer with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of Utracer through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the Utracer code and other code
// used in conjunction with Utracer except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

import javax.swing.*;
import java.util.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;


public class Udb extends JFrame {

    private JTabbedPane jtp;
    private JToolBar toolBar;

    static final String Dec   = "dec";
    static final String Bin   = "binary";
    static final String Hex   = "hex";
    static final String Oct   = "octal";
    static final String Flt   = "float";
    static final String Chr   = "char";
    static final String ReadRegs   = "Read Regs";
    
    static final String gprs[] = new String[] {
	"ebx",
	"ecx",        
	"edx",    	    
	"esi",    	    
	"edi",        
	"ebp",        
	"eax",        
	"xds",        
	"xes",        
	"xfs",        
	"xgs",        
	"xcs",        
	"orig_eax",   
	"eip",        
	"eflags",     
	"esp",        
	"xss"};
    
    static final String asciiTable[] = new String[] {
	"NULL",	"SOH", "STX", "ETX",
	"EOT",  "ENQ", "ACK", "BEL",
	"BS",   "HT",  "LF",  "VT",
	"FF",   "CR",  "SO",  "SI",
	"DLE",  "DC1", "DC2", "DC3",
	"DC4",  "NAK", "SYN", "ETB",
	"CAN",  "EM",  "SUB", "ESC",
	"FS",   "GS",  "RS",  "US"};

    static int[] gprVals = new int[gprs.length];
    static JTextField[] fields   = new JTextField[gprs.length];
    static JSpinner[]   spinners = new JSpinner[gprs.length];

    private String cvtVal (JSpinner js, int v) {
	String rc;
	Object tp = js.getValue();

	if (tp.equals (Dec))      rc = Integer.toString (v);
	else if (tp.equals (Bin)) rc = Integer.toBinaryString (v);
	else if (tp.equals (Hex)) rc = Integer.toHexString (v);
	else if (tp.equals (Oct)) rc = Integer.toOctalString (v);
	else if (tp.equals (Flt))
	    rc = Float.toString (Float.intBitsToFloat (v));
	else if (tp.equals (Chr)) {
	    if (32 > v) rc = asciiTable[v];
	    else rc = "'" + Character.toString((char)v) + "'";
	}
	else rc = "Unknown";

	return rc;
    }

    class Updater implements ChangeListener {
	JSpinner js;
	int ridx;
	
	public Updater (int i, JSpinner js) {
	    this.js = js;
	    this.ridx = i;
	}
	
	public void stateChanged(ChangeEvent ev) {
	    fields[ridx].setText (cvtVal (js, gprVals[ridx]));
	}
    };

    private Component ShowGPRs () {
	final String fmts[] = new String[] {Dec, Bin, Hex, Oct, Flt, Chr};

	Container fc = new Container();

	fc.setLayout(new GridLayout(0,3));

	for (int i = 0; i < gprs.length; i++) {
	    gprVals[i] = 33 * i;

	    SpinnerListModel slm = new RolloverSpinnerListModel (fmts);
	    spinners[i] = new JSpinner (slm);
	    Updater updater = new Updater(i, spinners[i]);
	    slm.addChangeListener(updater);
	    
	    fields[i] = new JTextField (cvtVal(spinners[i], gprVals[i]));
	    fields[i].setHorizontalAlignment(JTextField.RIGHT);
	    
	    fc.add (new JLabel(gprs[i]));
	    fc.add (fields[i]);
	    fc.add (spinners[i]);
	}

	return fc;
    }

    class UdbAction extends AbstractAction {
	public UdbAction (String text, Icon icon) {
	    super(text, icon);
	}

	public void actionPerformed(ActionEvent e) {
	    String ac = e.getActionCommand();
	    if (ac.equals (ReadRegs)) {
		
		Utrace.getregs (gprVals);
		
		for (int i = 0; i < gprs.length; i++) {
		    fields[i].setText (cvtVal (spinners[i], gprVals[i]));
		}
	    }
	}

    }

    public Udb() {
	super("Udb");

	toolBar = new JToolBar();
	toolBar.setBorder (new EtchedBorder());

	UdbAction readRegistersAction =
	    new UdbAction (ReadRegs, new ImageIcon ("reafreg.gif"));

	JButton readRegistersButton = new JButton (readRegistersAction);
	toolBar.add (readRegistersButton);

	Container c = getContentPane();

	jtp = new JTabbedPane();
	jtp.addTab ("GPR",   ShowGPRs());
	jtp.addTab ("FPR",   new JLabel ("Floating Point Registers"));
	jtp.addTab ("FPRX",  new JLabel ("Extended Floating Point Registers"));
	jtp.addTab ("DESC",  new JLabel ("Descriptor Registers"));
	jtp.addTab ("DEBUG", new JLabel ("Debug Registers"));

    }

    public static void main(String args[]) {
	Udb udb = new Udb();
	JFrame frame = new JFrame("UDB");
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.getContentPane().add(udb.toolBar, BorderLayout.NORTH);
	frame.getContentPane().add(udb.jtp, BorderLayout.CENTER);
	frame.setSize(200,200);
	frame.setSize(300, 450);
	frame.setVisible(true);
    }
}
