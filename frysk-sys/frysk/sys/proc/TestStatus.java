// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

package frysk.sys.proc;

import junit.framework.TestCase;

/**
 * Test the Status getUID() and getGID() a predefined set of
 * <tt>/proc$$/status</tt> buffer.
 */
public class TestStatus
    extends TestCase
{
    public void testParseStatusGetID()
    {

	// Construct valid status buffer
	String[] status = new String[] {
	    "Name:\tgaim\n",
	    "State:\tS (sleeping)\n",
	    "SleepAVG:\t88%\n",
	    "Tgid:\t2765\n",
	    "Pid:\t2765\n",
	    "PPid:\t1\n",
	    "TracerPid:\t0\n",
	    "Uid:\t500\t500\t500\t500\n",
	    "Gid:\t500\t500\t500\t500\n",
	    "FDSize:\t256\n",
	    "Groups:\t500\n",
	    "VmPeak:\t99180 kB\n",
	    "VmSize:\t99084 kB\n",
	    "VmLck:\t0 kB\n",
	    "VmHWM:\t22392 kB\n",
	    "VmRSS:\t20604 kB\n",
	    "VmData:\t7468 kB\n",
	    "VmStk:\t104 kB\n",
	    "VmExe:\t1004 kB\n",
	    "VmLib:\t25380 kB\n",
	    "VmPTE:\t148 kB\n",
	    "StaBrk:\t00a6b000 kB\n",
	    "Brk:\t0911b000 kB\n",
	    "StaStk:\tbf8199b0 kB\n",
	    "ExecLim:\t07f2f000\n",
	    "Threads:\t1\n",
	    "SigQ:\t0/16374\n",
	    "SigPnd:\t0000000000000000\n",
	    "ShdPnd:\t0000000000000000\n",
	    "SigBlk:\t0000000000000000\n",
	    "SigIgn:\t0000000020001000\n",
	    "SigCgt:\t0000000180014407\n",
	    "CapInh:\t0000000000000000\n",
	    "CapPrm:\t0000000000000000\n",
	    "CapEff:\t0000000000000000"};

	byte[] buf = TestLib.stringsToBytes (status);

    	// Test normal-expected results from a valid status buffer
	assertEquals ("Normal Process UID", 500, Status.getUID(buf));
	assertEquals ("Normal Process GID", 500, Status.getGID(buf));
    }

    public void testParseStatusNullBufferGetID()
    {
	// Test abnormal-expected results from invald status buffers

	// Test null buffer

	assertEquals ("Null Buffer Process UID", -1, Status.getUID(null));
	assertEquals ("Null Buffer Process GID", -1, Status.getGID(null));

    }	
    

    public void testParseStatusInvalidBufferGetID()
    {
	// Test abnormal-expected results from invald status buffers

	// Test non-null but invalid buffer
	
	String[] randomText = new String[] {
	    "Day after day, day after day,\n",
	    "We stuck, nor breath nor motion;\n",
	    "As idle as a painted ship\n",
	    "Upon a painted ocean."};

	byte[] buf = TestLib.stringsToBytes(randomText);

	assertEquals("Non null Buffer, invalid text Process UID", -1, Status.getUID(buf));
	assertEquals("Non null Buffer, invalid text Process GID", -1, Status.getGID(buf));
    }
}
