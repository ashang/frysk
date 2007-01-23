//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// type filter text
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

package frysk.gui.monitor.observers;

import frysk.proc.Syscall;
import frysk.proc.SyscallEventInfo;
import frysk.proc.Task;

public class SysCallUtilyInfo {

	public static String getSysCallName(Task task) {
		Syscall syscall;

		SyscallEventInfo syscallEventInfo = null;
		syscallEventInfo = task.getSyscallEventInfo();
		
		try {
			syscall = syscallEventInfo.getSyscall(task);
		}
		catch (final RuntimeException e1) {
			return null;
		}

		return syscall.getName();
	}
	
	public static String getCallInfoFromSyscall(Task task) {

		long addr = 0;
		long arg = 0;
		StringBuffer enterCall = new StringBuffer("");
		Syscall syscall;

		SyscallEventInfo syscallEventInfo = null;
		syscallEventInfo = task.getSyscallEventInfo();
		
		try {
			syscall = syscallEventInfo.getSyscall(task);
		}
		catch (final RuntimeException e1) {
			return "**** Error: " + e1.getMessage();
		}

		enterCall.append(syscall.getName());
		if (syscall.numArgs > 0) {
		  enterCall.append(" (");
		}
		
		for (int i = 1; i <= syscall.numArgs; ++i) {
			final char fmt = syscall.argList.charAt(i + 1);
			switch (fmt) {
			case 'a':
			case 'b':
			case 'p':
				arg = syscallEventInfo.arg(task, i);
				if (arg == 0) {
					enterCall.append("NULL");
				} else {
					enterCall.append("0x" + Long.toHexString(arg));
				}
				break;
			case 's':
			case 'S':
				addr = syscallEventInfo.arg(task, i);
				if (addr == 0) {
					enterCall.append("0x0");
				} else {
					enterCall.append("\"");
					final StringBuffer x = new StringBuffer();
					task.getMemory().get(addr, 20, x);
					if (x.length() == 20) {
						x.append("...");
					}
					x.append("\"");
					enterCall.append(x);
				}
				break;
			case 'i':
			default:
				arg = (int) syscallEventInfo.arg(task, i);
				enterCall.append(arg);
				break;
			}
			if (i < syscall.numArgs) {
				enterCall.append(",");
			}
		}

		if (syscall.numArgs > 0) {
			enterCall.append(')');
		}

		return enterCall.toString();
	}

	public static String getReturnInfoFromSyscall(Task task) {
		long addr = 0;
		long arg = 0;
		StringBuffer returnCall = new StringBuffer("");
		Syscall syscall;

		SyscallEventInfo syscallEventInfo = null;
		syscallEventInfo = task.getSyscallEventInfo();

		try {
			syscall = syscallEventInfo.getSyscall(task);
		}
		catch (final RuntimeException e1) {
			return "**** Error: " + e1.getMessage();
		}

		returnCall.append(syscall.getName() + " r= ");

		switch (syscall.argList.charAt(0)) {
		case 'a':
		case 'b':
		case 'p':
			arg = syscallEventInfo.returnCode(task);
			if (arg == 0) {
				returnCall.append("NULL");
			} else {
				returnCall.append("0x" + Long.toHexString(arg));
			}
			break;
		case 's':
		case 'S':
			addr = syscallEventInfo.returnCode(task);
			if (addr == 0) {
				returnCall.append("0x0");
			} else {
				returnCall.append("\"");
				final StringBuffer x = new StringBuffer();
				task.getMemory().get(addr, 20, x);
				if (x.length() == 20) {
					x.append("...");
				}
				x.append("\"");
				returnCall.append(x);
			}
			returnCall.append("");
			break;
		case 'i':
			arg = (int) syscallEventInfo.returnCode(task);
			if (arg < 0) {
				returnCall.append("-1");
				returnCall.append(" ERRNO=" + -arg);
			} else {
				returnCall.append(syscallEventInfo.returnCode(task));
			}
			break;
		default:
			returnCall.append(syscallEventInfo.returnCode(task));
			break;
		}
		return returnCall.toString();
	}
}
