// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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

import frysk.proc.SyscallEventInfo;
import frysk.proc.Task;
import frysk.proc.TaskException;

public class SysCallUtilyInfo {

	public static String getCallInfoFromSyscall(Task task) {

		long addr = 0;
		long arg = 0;
		String enterCall = "";

		SyscallEventInfo syscallEventInfo = null;
		try {
			syscallEventInfo = task.getSyscallEventInfo();
		} catch (TaskException e) {
			return "";
		}

		frysk.proc.Syscall syscall = frysk.proc.Syscall
				.syscallByNum(syscallEventInfo.number(task));
		enterCall = syscall.getName();
		if (syscall.numArgs > 0)
			enterCall += " (";

		for (int i = 1; i <= syscall.numArgs; ++i) {
			char fmt = syscall.argList.charAt(i + 1);
			switch (fmt) {
			case 'a':
			case 'b':
			case 'p':
				arg = syscallEventInfo.arg(task, i);
				if (arg == 0)
					enterCall += "NULL";
				else
					enterCall += "0x" + Long.toHexString(arg);
				break;
			case 's':
			case 'S':
				addr = syscallEventInfo.arg(task, i);
				if (addr == 0)
					enterCall += "0x0";
				else {
					enterCall += "\"";
					StringBuffer x = new StringBuffer();
					task.getMemory().get(addr, 20, x);
					if (x.length() == 20)
						x.append("...");
					x.append("\"");
					enterCall += x;
				}
				break;
			case 'i':
			default:
				arg = (int) syscallEventInfo.arg(task, i);
				enterCall += (arg);
				break;
			}
			if (i < syscall.numArgs)
				enterCall += ",";
		}

		enterCall += ')';
		return enterCall;
	}

	public static String getReturnInfoFromSyscall(Task task) {
		long addr = 0;
		long arg = 0;
		String returnCall = "";

		SyscallEventInfo syscallEventInfo = null;
		try {
			syscallEventInfo = task.getSyscallEventInfo();
		} catch (TaskException e) {
			return "";
		}

		frysk.proc.Syscall syscall = frysk.proc.Syscall
				.syscallByNum(syscallEventInfo.number(task));

		returnCall += syscall.getName() + " returns with value ";

		switch (syscall.argList.charAt(0)) {
		case 'a':ackage frysk.gui.monitor.observers;

		import frysk.proc.SyscallEventInfo;

		public class SysCallUtilyInfo {

			
			public static String getCallInfoFromSyscall(Task task) {

				long addr = 0;
				long arg = 0;
				String enterCall = "";

				SyscallEventInfo syscallEventInfo = null;
				try {
					syscallEventInfo = task.getSyscallEventInfo();
				} catch (TaskException e) {
					return "";
				}

				frysk.proc.Syscall syscall = frysk.proc.Syscall
						.syscallByNum(syscallEventInfo.number(task));
				enterCall = syscall.getName();
				if (syscall.numArgs > 0)
					enterCall += " (";

				for (int i = 1; i <= syscall.numArgs; ++i) {
					char fmt = syscall.argList.charAt(i + 1);
					switch (fmt) {
					case 'a':
					case 'b':
					case 'p':
						arg = syscallEventInfo.arg(task, i);
						if (arg == 0)
							enterCall += "NULL";
						else
							enterCall += "0x" + Long.toHexString(arg);
						break;
					case 's':
					case 'S':
						addr = syscallEventInfo.arg(task, i);
						if (addr == 0)
							enterCall += "0x0";
						else {
							enterCall += "\"";
							StringBuffer x = new StringBuffer();
							task.getMemory().get(addr, 20, x);
							if (x.length() == 20)
								x.append("...");
							x.append("\"");
							enterCall += x;
						}
						break;
					case 'i':
					default:
						arg = (int) syscallEventInfo.arg(task, i);
						enterCall += (arg);
						break;
					}
					if (i < syscall.numArgs)
						enterCall += ",";
				}

				enterCall += ')';
				return enterCall;
			}
			
		   public static String getReturnInfoFromSyscall (Task task)
		  {
			long addr = 0;
			long arg = 0;
			String returnCall = "";
			
			SyscallEventInfo syscallEventInfo = null;
			try {
				syscallEventInfo = task.getSyscallEventInfo();
			} catch (TaskException e) {
				return "";
			}

			frysk.proc.Syscall syscall = frysk.proc.Syscall
					.syscallByNum(syscallEventInfo.number(task));
			
			
			returnCall += syscall.getName() + " returns with value ";
			
			switch (syscall.argList.charAt (0)) {
			case 'a':
			case 'b':
			case 'p':
			    arg = syscallEventInfo.returnCode (task);
			    if (arg == 0)
			    	returnCall += ("NULL");
			    else
			    	returnCall += ("0x" + Long.toHexString (arg));
			    break;
			case 's':
			case 'S':
			    addr = syscallEventInfo.returnCode (task);
				if (addr == 0)
					returnCall += "0x0";
				else {
					returnCall += "\"";
					StringBuffer x = new StringBuffer();
					task.getMemory().get(addr, 20, x);
					if (x.length() == 20)
						x.append("...");
					x.append("\"");
					returnCall += x;
				}
			    returnCall += ("");
			    break;
			case 'i':
			    arg = (int)syscallEventInfo.returnCode (task);
			    if (arg < 0) {
			    	returnCall += ("-1");
			    	returnCall += (" ERRNO=" + (-arg));
			    }
			    else
			    	returnCall += (syscallEventInfo.returnCode (task));
			    break;
			default:
				returnCall += (syscallEventInfo.returnCode (task));
			    break;
			}
			return returnCall;
		  }
		}

		case 'b':
		case 'p':
			arg = syscallEventInfo.returnCode(task);
			if (arg == 0)
				returnCall += ("NULL");
			else
				returnCall += ("0x" + Long.toHexString(arg));
			break;
		case 's':
		case 'S':
			addr = syscallEventInfo.returnCode(task);
			if (addr == 0)
				returnCall += "0x0";
			else {
				returnCall += "\"";
				StringBuffer x = new StringBuffer();
				task.getMemory().get(addr, 20, x);
				if (x.length() == 20)
					x.append("...");
				x.append("\"");
				returnCall += x;
			}
			returnCall += ("");
			break;
		case 'i':
			arg = (int) syscallEventInfo.returnCode(task);
			if (arg < 0) {
				returnCall += ("-1");
				returnCall += (" ERRNO=" + (-arg));
			} else
				returnCall += (syscallEventInfo.returnCode(task));
			break;
		default:
			returnCall += (syscallEventInfo.returnCode(task));
			break;
		}
		return returnCall;
	}
}
