// This file is part of the program FRYSK.
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
package frysk.proc;

import inua.PrintWriter;

class Syscall
{
    int number;
    int numArgs;
    String name;
    String argList;
    boolean noreturn;

    Syscall (String name, int number, int numArgs, 
	     String argList, boolean noreturn)
    {
	this.name = name;
	this.number = number;
	this.numArgs = numArgs;
	this.argList = argList;
	this.noreturn = noreturn;
    }
    Syscall (String name, int number, int numArgs, String argList)
    {
	this.name = name;
	this.number = number;
	this.numArgs = numArgs;
	this.argList = argList;
    }        

    Syscall (String name, int number, int numArgs)
    {
	this (name, number, numArgs, "i:iiiiiiii");
    }
    
    Syscall (String name, int number)
    {
	this (name, number, 0, "i:");
    }

    Syscall (int number)
    {
	this ("<" + number + ">", number, 0, "i:");
    }
    
    private void printStringArg (PrintWriter writer,
				 frysk.proc.Task task,
				 long addr)
    {
	if (addr == 0)
	    writer.print ("0x0");
	else {
	    int index = 0;
	    writer.print ("\"");
	    StringBuffer x = new StringBuffer ();
	    task.memory.get (addr, 20, x);
	    if (x.length () == 20)
		x.append ("...");
	    x.append ("\"");
	    writer.print (toString ());
	}
    }
    
    PrintWriter printCall (PrintWriter writer,
			   frysk.proc.Task task,
			   SyscallEventInfo syscall)
    {
	long addr = 0;
	long arg = 0;
	writer.print ("<SYSCALL> " + name + " (");
	for (int i = 1; i <= numArgs; ++i) {
	    char fmt = argList.charAt (i + 1);
	    switch (fmt) {
	    case 'a':
	    case 'b':
	    case 'p':
		arg = syscall.arg (task, i);
		if (arg == 0)
		    writer.print ("NULL");
		else
		    writer.print ("0x" + Long.toHexString (arg));
		break;
	    case 's':
	    case 'S':
		addr = syscall.arg (task, i);
		printStringArg (writer, task, addr);
		break;
	    case 'i':
	    default:
		arg = (int)syscall.arg (task, i);
		writer.print (arg);
		break;
	    }
	    if (i < numArgs)
		writer.print (",");
	}
	if (noreturn)
	    writer.println (")");
	else
	    writer.print (")");
	return writer;
    }
    
    PrintWriter printReturn (PrintWriter writer,
			     frysk.proc.Task task,
			     SyscallEventInfo syscallEventInfo)
    {
	long addr = 0;
	long arg = 0;
	
	writer.print (" = ");
	
	switch (argList.charAt (0)) {
	case 'a':
	case 'b':
	case 'p':
	    arg = syscallEventInfo.returnCode (task);
	    if (arg == 0)
		writer.println ("NULL");
	    else
		writer.println ("0x" + Long.toHexString (arg));
	    break;
	case 's':
	case 'S':
	    addr = syscallEventInfo.returnCode (task);
	    printStringArg (writer, task, addr);
	    writer.println ("");
	    break;
	case 'i':
	    arg = (int)syscallEventInfo.returnCode (task);
	    if (arg < 0) {
		writer.print ("-1");
		writer.println (" ERRNO=" + (-arg));
	    }
	    else
		writer.println (syscallEventInfo.returnCode (task));
	    break;
	default:
	    writer.println (syscallEventInfo.returnCode (task));
	    break;
	}
	return writer;
    }

    // XXX: Eventually this will be moved down to Linux, or even
    // further.
    static Syscall syscallByNum (int num)
    {
	return syscallList[num];
    }

    private static Syscall[] syscallList = {
	new Syscall (0),
	new Syscall ("exit", 1, 1),
	new Syscall ("fork", 2, 0, "i: "),
	new Syscall ("read", 3, 3, "i:ibn "),
	new Syscall ("write", 4, 3, "i:ibn "),
	new Syscall ("open", 5, 3, "i:siv "),
	new Syscall ("close", 6, 1, "i:i "),
	new Syscall ("waitpid", 7, 3, "i:ipi "),
	new Syscall ("creat", 8, 2, "i:sv"),
	new Syscall ("link", 9, 2, "i:ss "),
	new Syscall ("unlink", 10, 1, "i:s "),
	new Syscall ("execve", 11, 3, "i:ppp "),
	new Syscall ("chdir", 12, 1, "i:s "),
	new Syscall ("time", 13, 1, "i:P "),
	new Syscall ("sys_mknod", 14, 3, "i:sii "),
	new Syscall ("chmod", 15, 2, "i:si "),
	new Syscall ("lchown", 16, 3, "i:sii "),
	new Syscall ("break", 17),
	new Syscall ("old_stat", 18, 3, "i:pp "),
	new Syscall ("lseek", 19, 3, "i:iii "),
	new Syscall ("getpid", 20, 0, "i: "),
	new Syscall ("mount", 21, 5, "i:sssip "),
	new Syscall ("umount", 22, 1, "i:s "),
	new Syscall ("setuid", 23, 1, "i:i "),
	new Syscall ("getuid", 24, 0, "i: "),
	new Syscall ("stime", 25, 1, "i:p "),
	new Syscall ("ptrace", 26, 4, "i:iiii "),
	new Syscall ("alarm", 27, 1, "i:i "),
	new Syscall ("old_fstat", 28, 2, "i:ip "),
	new Syscall ("pause", 29, 0, "i: "),
	new Syscall ("utime", 30, 2, "i:sP "),
	new Syscall ("stty", 31),
	new Syscall ("gtty", 32),
	new Syscall ("access", 33, 2, "i:si "),
	new Syscall ("nice", 34, 1, "i:i "),
	new Syscall ("ftime", 35, 1, "i:p "),
	new Syscall ("sync", 36, 0, "i: "),
	new Syscall ("kill", 37, 2, "i:ii "),
	new Syscall ("rename", 38, 2, "i:ss "),
	new Syscall ("mkdir", 39, 2, "i:si "),
	new Syscall ("rmdir", 40, 1, "i:s "),
	new Syscall ("dup", 41, 1, "i:i "),
	new Syscall ("pipe", 42, 1, "i:f "),
	new Syscall ("times", 43, 1, "i:p "),
	new Syscall ("prof", 44),
	new Syscall ("brk", 45, 1, "i:p "),
	new Syscall ("setgid", 46, 1, "i:i "),
	new Syscall ("getgid", 47, 0, "i: "),
	new Syscall ("signal", 48, 2, "i:ii "),
	new Syscall ("geteuid", 49, 0, "i: "),
	new Syscall ("getegid", 50, 0, "i: "),
	new Syscall ("acct", 51, 1, "i:S "),
	new Syscall ("umount2", 52, 2, "i:si "),
	new Syscall ("lock", 53),
	new Syscall ("ioctl", 54, 3, "i:iiI "),
	new Syscall ("fcntl", 55, 3, "i:iiF "),
	new Syscall ("mpx", 56),
	new Syscall ("setpgid", 57, 2, "i:ii "),
	new Syscall ("ulimit", 58, 2, "i:ii "),
	new Syscall ("oldolduname", 59),
	new Syscall ("umask", 60, 1, "i:i "),
	new Syscall ("chroot", 61, 1, "i:s "),
	new Syscall ("ustat", 62, 2, "i:ip "),
	new Syscall ("dup2", 63, 2, "i:ii "),
	new Syscall ("getppid", 64, 0, "i: "),
	new Syscall ("getpgrp", 65, 0, "i: "),
	new Syscall ("setsid", 66, 0, "i: "),
	new Syscall ("sigaction", 67, 3, "i:ipp "),
	new Syscall ("sgetmask", 68),
	new Syscall ("ssetmask", 69),
	new Syscall ("setreuid", 70, 2, "i:ii "),
	new Syscall ("setregid", 71, 2, "i:ii "),
	new Syscall ("sigsuspend", 72, 1, "i:p "),
	new Syscall ("sigpending", 73, 1, "i:p "),
	new Syscall ("sethostname", 74, 2, "i:pi "),
	new Syscall ("setrlimit", 75, 2, "i:ip "),
	new Syscall ("getrlimit", 76, 2, "i:ip "),
	new Syscall ("getrusage", 77, 2, "i:ip "),
	new Syscall ("gettimeofday", 78, 2, "i:PP "),
	new Syscall ("settimeofday", 79, 2, "i:PP "),
	new Syscall ("getgroups", 80, 2, "i:ip "),
	new Syscall ("setgroups", 81, 2, "i:ip "),
	new Syscall ("select", 82, 5, "i:iPPPP "),
	new Syscall ("symlink", 83, 2, "i:ss "),
	new Syscall ("oldlstat", 84, 2, "i:pp "),
	new Syscall ("readlink", 85, 3, "i:spi "),
	new Syscall ("uselib", 86, 1, "i:s "),
	new Syscall ("swapon", 87, 2, "i:si "),
	new Syscall ("reboot", 88, 1, "i:i "),
	new Syscall (89),
	new Syscall ("mmap", 90, 6, "b:aniiii "),
	new Syscall ("munmap", 91, 2, "i:ai "),
	new Syscall ("truncate", 92, 2, "i:si "),
	new Syscall ("ftruncate", 93, 2, "i:ii "),
	new Syscall ("fchmod", 94, 2, "i:ii "),
	new Syscall ("fchown", 95, 3, "i:iii "),
	new Syscall ("getpriority", 96, 2, "i:ii "),
	new Syscall ("setpriority", 97, 3, "i:iii "),
	new Syscall ("profil", 98, 4, "i:piii "),
	new Syscall ("statfs", 99, 2, "i:sp "),
	new Syscall ("fstatfs", 100, 2, "i:ip "),
	new Syscall ("ioperm", 101, 3, "i:iii "),
	new Syscall ("socketcall", 102, 2, "i:ip "),
	new Syscall ("klogctl", 103, 3, "i:isi "),
	new Syscall ("setitimer", 104, 3, "i:ipp "),
	new Syscall ("getitimer", 105, 2, "i:ip "),
	new Syscall ("sys_stat", 106, 2, "i:sp "),
	new Syscall ("sys_lstat", 107, 2, "i:sp "),
	new Syscall ("sys_fstat", 108, 2, "i:ip "),
	new Syscall ("old_uname", 109, 1, "i:p "),
	new Syscall ("iopl", 110, 1, "i:i "),
	new Syscall ("vhangup", 111, 1, "i:i "),
	new Syscall ("idle", 112, 0, "i: "),
	new Syscall (113),
	new Syscall ("wait4", 114, 4, "i:iWiP "),
	new Syscall ("swapoff", 115, 1, "i:s "),
	new Syscall ("sysinfo", 116, 1, "i:p "),
	new Syscall ("ipc", 117, 6, "i:iiiipi "),
	new Syscall ("fsync", 118, 1, "i:i "),
	new Syscall ("sigreturn", 119),
	new Syscall ("clone", 120, 2, "i:ip "),
	new Syscall ("setdomain", 121, 2, "i:si "),
	new Syscall ("uname", 122, 1, "i:p "),
	new Syscall ("modify_ldt", 123, 3, "i:ipi "),
	new Syscall ("adjtimex", 124, 1, "i:p "),
	new Syscall ("mprotect", 125, 3, "i:aii "),
	new Syscall ("sigprocmask", 126, 3, "i:ipp "),
	new Syscall ("create_module", 127, 3),
	new Syscall ("init_module", 128, 5),
	new Syscall ("delete_module", 129, 3),
	new Syscall ("get_kernel_syms", 130, 1, "i:p "),
	new Syscall ("quotactl", 131, 4, "i:isip "),
	new Syscall ("getpgid", 132, 1, "i:i "),
	new Syscall ("fchdir", 133, 1, "i:i "),
	new Syscall ("bdflush", 134, 2, "i:ii "),
	new Syscall ("sysfs", 135, 1, "i:i "),
	new Syscall ("personality", 136, 1, "i:i "),
	new Syscall ("afs_syscall", 137),
	new Syscall ("setfsuid", 138, 1, "i:i "),
	new Syscall ("setfsgid", 139, 1, "i:i "),
	new Syscall ("llseek", 140, 5, "i:iuupi "),
	new Syscall ("s_getdents", 141, 3, "i:ipi "),
	new Syscall ("select", 142, 5, "i:iPPPP "),
	new Syscall ("flock", 143, 2, "i:ii "),
	new Syscall ("msync", 144, 3, "i:aii "),
	new Syscall ("readv", 145, 3, "i:ipi "),
	new Syscall ("writev", 146, 3, "i:ipi "),
	new Syscall ("getsid", 147, 1, "i:i "),
	new Syscall ("fdatasync", 148, 1, "i:i "),
	new Syscall ("sysctl", 149, 1, "i:p "),
	new Syscall ("mlock", 150, 2, "i:bn "),
	new Syscall ("munlock", 151, 2, "i:ai "),
	new Syscall ("mlockall", 152, 1, "i:i "),
	new Syscall ("munlockall", 153, 0, "i: "),
	new Syscall ("sched_setp", 154, 2, "i:ip "),
	new Syscall ("sched_getp", 155, 2, "i:ip "),
	new Syscall ("sched_sets", 156, 3, "i:iip "),
	new Syscall ("sched_gets", 157, 1, "i:i "),
	new Syscall ("sched_yield", 158, 0, "i: "),
	new Syscall ("sched_primax", 159, 1, "i:i "),
	new Syscall ("sched_primin", 160, 1, "i:i "),
	new Syscall ("sched_rr_gi", 161, 2, "i:ip "),
	new Syscall ("nanosleep", 162, 2, "i:pp "),
	new Syscall ("mremap", 163, 4, "b:aini "),
	new Syscall ("setresuid", 164, 3, "i:iii "),
	new Syscall ("getresuid", 165, 3, "i:ppp "),
	new Syscall ("vm86", 166, 1, "i:p "),
	new Syscall ("query_module", 167, 5, "i:sipip "),
	new Syscall ("poll", 168, 3, "i:pii "),
	new Syscall ("nfsservctl", 169, 3, "i:ipp "),
	new Syscall ("setresgid", 170, 3, "i:iii "),
	new Syscall ("getresgid", 171, 3, "i:ppp "),
	new Syscall ("prctl", 172, 5, "i:iiiii "),
	new Syscall ("rt_sigreturn", 173),
	new Syscall ("rt_sigaction", 174),
	new Syscall ("rt_sigprocmask", 175),
	new Syscall ("rt_sigpending", 176),
	new Syscall ("rt_sigtimedwait", 177),
	new Syscall ("rt_sigqueueinfo", 178),
	new Syscall ("rt_sigsuspend", 179),
	new Syscall ("pread64", 180),
	new Syscall ("pwrite64", 181),
	new Syscall ("chown", 182, 3, "i:sii "),
	new Syscall ("getcwd", 183, 2, "i:bi "),
	new Syscall ("capget", 184, 2, "i:pp "),
	new Syscall ("capset", 185, 2, "i:pp "),
	new Syscall ("sigaltstack", 186, 2, "i:PP "),
	new Syscall ("sendfile", 187, 4, "i:iipi "),
	new Syscall ("getpmsg", 188),
	new Syscall ("putpmsg", 189),
	new Syscall ("vfork", 190, 0, "i: "),
	new Syscall ("ugetrlimit", 191),
	new Syscall ("mmap", 192, 6, "b:aniiii "),
	new Syscall ("truncate64", 193, 3, "i:shl "),
	new Syscall ("ftruncate64", 194, 3, "i:ihl "),
	new Syscall ("stat64", 195, 2, "i:sp "),
	new Syscall ("lstat64", 196, 2, "i:sp "),
	new Syscall ("fstat64", 197, 2, "i:ip "),
	new Syscall ("lchown32", 198, 3, "i:sii "),
	new Syscall ("getuid32", 199),
	new Syscall ("getgid32", 200),
	new Syscall ("geteuid32", 201),
	new Syscall ("getegid32", 202),
	new Syscall ("setreuid32", 203, 2, "i:ii "),
	new Syscall ("setregid32", 204, 2, "i:ii "),
	new Syscall ("getgroups32", 205, 2, "i:ip "),
	new Syscall ("setgroups32", 206, 2, "i:ip "),
	new Syscall ("fchown32", 207, 3, "i:iii "),
	new Syscall ("setresuid32", 208, 3, "i:iii "),
	new Syscall ("getresuid32", 209, 3, "i:ppp "),
	new Syscall ("setresgid32", 210, 3, "i:iii "),
	new Syscall ("getresgid32", 211, 3, "i:ppp "),
	new Syscall ("chown32", 212, 3, "i:sii "),
	new Syscall ("setuid32", 213, 1, "i:i "),
	new Syscall ("setgid32", 214, 1, "i:i "),
	new Syscall ("setfsuid32", 215, 1, "i:i "),
	new Syscall ("setfsgid32", 216, 1, "i:i "),
	new Syscall ("pivot_root", 217, 2, "i:ss "),
	new Syscall ("mincore", 218, 3, "i:anV "),
	new Syscall ("madvise", 219, 3, "i:pii "),
	new Syscall ("getdents64", 220, 3, "i:ipi "),
	new Syscall ("fcntl64", 221, 3, "i:iip "),
	new Syscall (222),
	new Syscall (223),
	new Syscall ("gettid", 224, 0),
	new Syscall ("readahead", 225, 4, "i:ihli "),
	new Syscall ("setxattr", 226),
	new Syscall ("lsetxattr", 227),
	new Syscall ("fsetxattr", 228),
	new Syscall ("getxattr", 229),
	new Syscall ("lgetxattr", 230),
	new Syscall ("fgetxattr", 231),
	new Syscall ("listxattr", 232),
	new Syscall ("llistxattr", 233),
	new Syscall ("flistxattr", 234),
	new Syscall ("removexattr", 235),
	new Syscall ("lremovexattr", 236),
	new Syscall ("fremovexattr", 237),
	new Syscall ("tkill", 238, 2, "i:ii "),
	new Syscall ("sendfile64", 239, 4, "i:iipi "),
	new Syscall ("futex", 240, 4, "i:piip "),
	new Syscall ("sched_setaffinity", 241),
	new Syscall ("sched_getaffinity", 242),
	new Syscall ("set_thread_area", 243, 1, "i:p "),
	new Syscall ("get_thread_area", 244, 1, "i:p "),
	new Syscall ("io_setup", 245, 2, "i:ip "),
	new Syscall ("io_destroy", 246, 1, "i:i "),
	new Syscall ("io_getevents", 247, 5, "i:iiipp "),
	new Syscall ("io_submit", 248, 3, "i:iip "),
	new Syscall ("io_cancel", 249, 3, "i:ipp "),
	new Syscall ("fadvise64", 250),
	new Syscall (251),
	new Syscall ("exit_group", 252, 1, " :i ", true),
	new Syscall ("lookup_dcookie", 253),
	new Syscall ("epoll_create", 254),
	new Syscall ("epoll_ctl", 255),
	new Syscall ("epoll_wait", 256),
	new Syscall ("remap_file_pages", 257),
	new Syscall ("set_tid_address", 258),
	new Syscall ("timer_create", 259),
	new Syscall ("timer_settime", 260),
	new Syscall ("timer_gettime", 261),
	new Syscall ("timer_getoverrun", 262),
	new Syscall ("timer_delete", 263),
	new Syscall ("clock_settime", 264),
	new Syscall ("clock_gettime", 265),
	new Syscall ("clock_getres", 266),
	new Syscall ("clock_nanosleep", 267),
	new Syscall ("statfs64", 268),
	new Syscall ("fstatfs64", 269),
	new Syscall ("tgkill", 270),
	new Syscall ("utimes", 271),
	new Syscall ("fadvise64_64", 272),
    };
}
