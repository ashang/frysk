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

package frysk.proc;

import java.util.HashMap;

public class LinuxIa32Syscall
{
  static final int SOCKET_NUM = 102;
  static final int IPC_NUM = 117;

  // This is used to keep track of syscalls whose number we do not
  // know.
  static HashMap unknownSyscalls = new HashMap();

  static class Ia32Syscall 
    extends Syscall
  {
    Ia32Syscall (String name, int number, int numArgs, 
	     String argList, boolean noreturn)
    {
      super (name, number, numArgs, argList, noreturn);
    }
    Ia32Syscall (String name, int number, int numArgs, String argList)
    {
      super (name, number, numArgs, argList);
    }
    Ia32Syscall (String name, int number, int numArgs)
    {
      super (name, number, numArgs);
    }
    Ia32Syscall (String name, int number)
    {
      super (name, number);
    }
    Ia32Syscall (int number)
    {
      super (number);
    }

    public long getArguments (Task task, int n)
    {
      Isa isa;
      try
	{
	  isa = task.getIsa();
	}
      catch (Exception e)
	{
	  throw new RuntimeException ("Could not get isa");
	}

      switch (n)
	{
	case 0:
	  return isa.getRegisterByName ("orig_eax").get (task);
	case 1:
	  return isa.getRegisterByName("ebx").get (task);
	case 2:
	  return isa.getRegisterByName("ecx").get (task);
	case 3:
	  return isa.getRegisterByName("edx").get (task);
	case 4:
	  return isa.getRegisterByName("esi").get (task);
	case 5:
	  return isa.getRegisterByName("edi").get (task);
	case 6:
	  return isa.getRegisterByName("eax").get (task);
	default:
	  throw new RuntimeException ("unknown syscall arg");
	}
    }
    public long getReturnCode (Task task)
    {
      Isa isa;
      try
	{
	  isa = task.getIsa();
	}
      catch (Exception e)
	{
	  throw new RuntimeException ("Could not get isa");
	}
      return isa.getRegisterByName ("eax").get (task);
    }
  }
  
  static Syscall[] syscallList = {
    new Ia32Syscall ("restart_syscall", 0),
    new Ia32Syscall ("exit", 1, 1),
    new Ia32Syscall ("fork", 2, 0, "i: "),
    new Ia32Syscall ("read", 3, 3, "i:ibn "),
    new Ia32Syscall ("write", 4, 3, "i:ibn "),
    new Ia32Syscall ("open", 5, 3, "i:siv "),
    new Ia32Syscall ("close", 6, 1, "i:i "),
    new Ia32Syscall ("waitpid", 7, 3, "i:ipi "),
    new Ia32Syscall ("creat", 8, 2, "i:sv"),
    new Ia32Syscall ("link", 9, 2, "i:ss "),
    new Ia32Syscall ("unlink", 10, 1, "i:s "),
    new Ia32Syscall ("execve", 11, 3, "i:ppp "),
    new Ia32Syscall ("chdir", 12, 1, "i:s "),
    new Ia32Syscall ("time", 13, 1, "i:P "),
    new Ia32Syscall ("mknod", 14, 3, "i:sii "),
    new Ia32Syscall ("chmod", 15, 2, "i:si "),
    new Ia32Syscall ("lchown", 16, 3, "i:sii "),
    new Ia32Syscall ("break", 17),
    new Ia32Syscall ("oldstat", 18, 3, "i:pp "),
    new Ia32Syscall ("lseek", 19, 3, "i:iii "),
    new Ia32Syscall ("getpid", 20, 0, "i: "),
    new Ia32Syscall ("mount", 21, 5, "i:sssip "),
    new Ia32Syscall ("umount", 22, 1, "i:s "),
    new Ia32Syscall ("setuid", 23, 1, "i:i "),
    new Ia32Syscall ("getuid", 24, 0, "i: "),
    new Ia32Syscall ("stime", 25, 1, "i:p "),
    new Ia32Syscall ("ptrace", 26, 4, "i:iiii "),
    new Ia32Syscall ("alarm", 27, 1, "i:i "),
    new Ia32Syscall ("oldfstat", 28, 2, "i:ip "),
    new Ia32Syscall ("pause", 29, 0, "i: "),
    new Ia32Syscall ("utime", 30, 2, "i:sP "),
    new Ia32Syscall ("stty", 31),
    new Ia32Syscall ("gtty", 32),
    new Ia32Syscall ("access", 33, 2, "i:si "),
    new Ia32Syscall ("nice", 34, 1, "i:i "),
    new Ia32Syscall ("ftime", 35, 1, "i:p "),
    new Ia32Syscall ("sync", 36, 0, "i: "),
    new Ia32Syscall ("kill", 37, 2, "i:ii "),
    new Ia32Syscall ("rename", 38, 2, "i:ss "),
    new Ia32Syscall ("mkdir", 39, 2, "i:si "),
    new Ia32Syscall ("rmdir", 40, 1, "i:s "),
    new Ia32Syscall ("dup", 41, 1, "i:i "),
    new Ia32Syscall ("pipe", 42, 1, "i:f "),
    new Ia32Syscall ("times", 43, 1, "i:p "),
    new Ia32Syscall ("prof", 44),
    new Ia32Syscall ("brk", 45, 1, "i:p "),
    new Ia32Syscall ("setgid", 46, 1, "i:i "),
    new Ia32Syscall ("getgid", 47, 0, "i: "),
    new Ia32Syscall ("signal", 48, 2, "i:ii "),
    new Ia32Syscall ("geteuid", 49, 0, "i: "),
    new Ia32Syscall ("getegid", 50, 0, "i: "),
    new Ia32Syscall ("acct", 51, 1, "i:S "),
    new Ia32Syscall ("umount2", 52, 2, "i:si "),
    new Ia32Syscall ("lock", 53),
    new Ia32Syscall ("ioctl", 54, 3, "i:iiI "),
    new Ia32Syscall ("fcntl", 55, 3, "i:iiF "),
    new Ia32Syscall ("mpx", 56),
    new Ia32Syscall ("setpgid", 57, 2, "i:ii "),
    new Ia32Syscall ("ulimit", 58, 2, "i:ii "),
    new Ia32Syscall ("oldolduname", 59),
    new Ia32Syscall ("umask", 60, 1, "i:i "),
    new Ia32Syscall ("chroot", 61, 1, "i:s "),
    new Ia32Syscall ("ustat", 62, 2, "i:ip "),
    new Ia32Syscall ("dup2", 63, 2, "i:ii "),
    new Ia32Syscall ("getppid", 64, 0, "i: "),
    new Ia32Syscall ("getpgrp", 65, 0, "i: "),
    new Ia32Syscall ("setsid", 66, 0, "i: "),
    new Ia32Syscall ("sigaction", 67, 3, "i:ipp "),
    new Ia32Syscall ("sgetmask", 68),
    new Ia32Syscall ("ssetmask", 69),
    new Ia32Syscall ("setreuid", 70, 2, "i:ii "),
    new Ia32Syscall ("setregid", 71, 2, "i:ii "),
    new Ia32Syscall ("sigsuspend", 72, 1, "i:p "),
    new Ia32Syscall ("sigpending", 73, 1, "i:p "),
    new Ia32Syscall ("sethostname", 74, 2, "i:pi "),
    new Ia32Syscall ("setrlimit", 75, 2, "i:ip "),
    new Ia32Syscall ("getrlimit", 76, 2, "i:ip "),
    new Ia32Syscall ("getrusage", 77, 2, "i:ip "),
    new Ia32Syscall ("gettimeofday", 78, 2, "i:PP "),
    new Ia32Syscall ("settimeofday", 79, 2, "i:PP "),
    new Ia32Syscall ("getgroups", 80, 2, "i:ip "),
    new Ia32Syscall ("setgroups", 81, 2, "i:ip "),
    new Ia32Syscall ("select", 82, 5, "i:iPPPP "),
    new Ia32Syscall ("symlink", 83, 2, "i:ss "),
    new Ia32Syscall ("oldlstat", 84, 2, "i:pp "),
    new Ia32Syscall ("readlink", 85, 3, "i:spi "),
    new Ia32Syscall ("uselib", 86, 1, "i:s "),
    new Ia32Syscall ("swapon", 87, 2, "i:si "),
    new Ia32Syscall ("reboot", 88, 1, "i:i "),
    new Ia32Syscall ("readdir",89,3,"i:ipi"),
    new Ia32Syscall ("mmap", 90, 6, "b:aniiii "),
    new Ia32Syscall ("munmap", 91, 2, "i:ai "),
    new Ia32Syscall ("truncate", 92, 2, "i:si "),
    new Ia32Syscall ("ftruncate", 93, 2, "i:ii "),
    new Ia32Syscall ("fchmod", 94, 2, "i:ii "),
    new Ia32Syscall ("fchown", 95, 3, "i:iii "),
    new Ia32Syscall ("getpriority", 96, 2, "i:ii "),
    new Ia32Syscall ("setpriority", 97, 3, "i:iii "),
    new Ia32Syscall ("profil", 98, 4, "i:piii "),
    new Ia32Syscall ("statfs", 99, 2, "i:sp "),
    new Ia32Syscall ("fstatfs", 100, 2, "i:ip "),
    new Ia32Syscall ("ioperm", 101, 3, "i:iii "),
    new Ia32Syscall ("socketcall", 102, 2, "i:ip "),
    new Ia32Syscall ("syslog", 103, 3, "i:isi "),
    new Ia32Syscall ("setitimer", 104, 3, "i:ipp "),
    new Ia32Syscall ("getitimer", 105, 2, "i:ip "),
    new Ia32Syscall ("stat", 106, 2, "i:sp "),
    new Ia32Syscall ("lstat", 107, 2, "i:sp "),
    new Ia32Syscall ("fstat", 108, 2, "i:ip "),
    new Ia32Syscall ("olduname", 109, 1, "i:p "),
    new Ia32Syscall ("iopl", 110, 1, "i:i "),
    new Ia32Syscall ("vhangup", 111, 1, "i:i "),
    new Ia32Syscall ("idle", 112, 0, "i: "),
    new Ia32Syscall ("vm86old", 113, 1, "i:p"),
    new Ia32Syscall ("wait4", 114, 4, "i:iWiP "),
    new Ia32Syscall ("swapoff", 115, 1, "i:s "),
    new Ia32Syscall ("sysinfo", 116, 1, "i:p "),
    new Ia32Syscall ("ipc", 117, 6, "i:iiiipi "),
    new Ia32Syscall ("fsync", 118, 1, "i:i "),
    new Ia32Syscall ("sigreturn", 119),
    new Ia32Syscall ("clone", 120, 2, "i:ip "),
    new Ia32Syscall ("setdomainname", 121, 2, "i:si "),
    new Ia32Syscall ("uname", 122, 1, "i:p "),
    new Ia32Syscall ("modify_ldt", 123, 3, "i:ipi "),
    new Ia32Syscall ("adjtimex", 124, 1, "i:p "),
    new Ia32Syscall ("mprotect", 125, 3, "i:aii "),
    new Ia32Syscall ("sigprocmask", 126, 3, "i:ipp "),
    new Ia32Syscall ("create_module", 127, 3),
    new Ia32Syscall ("init_module", 128, 5),
    new Ia32Syscall ("delete_module", 129, 3),
    new Ia32Syscall ("get_kernel_syms", 130, 1, "i:p "),
    new Ia32Syscall ("quotactl", 131, 4, "i:isip "),
    new Ia32Syscall ("getpgid", 132, 1, "i:i "),
    new Ia32Syscall ("fchdir", 133, 1, "i:i "),
    new Ia32Syscall ("bdflush", 134, 2, "i:ii "),
    new Ia32Syscall ("sysfs", 135, 1, "i:i "),
    new Ia32Syscall ("personality", 136, 1, "i:i "),
    new Ia32Syscall ("afs_syscall", 137),
    new Ia32Syscall ("setfsuid", 138, 1, "i:i "),
    new Ia32Syscall ("setfsgid", 139, 1, "i:i "),
    new Ia32Syscall ("_llseek", 140, 5, "i:iuupi "),
    new Ia32Syscall ("getdents", 141, 3, "i:ipi "),
    new Ia32Syscall ("_newselect", 142, 5, "i:iPPPP "),
    new Ia32Syscall ("flock", 143, 2, "i:ii "),
    new Ia32Syscall ("msync", 144, 3, "i:aii "),
    new Ia32Syscall ("readv", 145, 3, "i:ipi "),
    new Ia32Syscall ("writev", 146, 3, "i:ipi "),
    new Ia32Syscall ("getsid", 147, 1, "i:i "),
    new Ia32Syscall ("fdatasync", 148, 1, "i:i "),
    new Ia32Syscall ("_sysctl", 149, 1, "i:p "),
    new Ia32Syscall ("mlock", 150, 2, "i:bn "),
    new Ia32Syscall ("munlock", 151, 2, "i:ai "),
    new Ia32Syscall ("mlockall", 152, 1, "i:i "),
    new Ia32Syscall ("munlockall", 153, 0, "i: "),
    new Ia32Syscall ("sched_setparam", 154, 2, "i:ip "),
    new Ia32Syscall ("sched_getparam", 155, 2, "i:ip "),
    new Ia32Syscall ("sched_setscheduler", 156, 3, "i:iip "),
    new Ia32Syscall ("sched_getscheduler", 157, 1, "i:i "),
    new Ia32Syscall ("sched_yield", 158, 0, "i: "),
    new Ia32Syscall ("sched_get_priority_max", 159, 1, "i:i "),
    new Ia32Syscall ("sched_get_priority_min", 160, 1, "i:i "),
    new Ia32Syscall ("sched_rr_get_interval", 161, 2, "i:ip "),
    new Ia32Syscall ("nanosleep", 162, 2, "i:pp "),
    new Ia32Syscall ("mremap", 163, 4, "b:aini "),
    new Ia32Syscall ("setresuid", 164, 3, "i:iii "),
    new Ia32Syscall ("getresuid", 165, 3, "i:ppp "),
    new Ia32Syscall ("vm86", 166, 1, "i:p "),
    new Ia32Syscall ("query_module", 167, 5, "i:sipip "),
    new Ia32Syscall ("poll", 168, 3, "i:pii "),
    new Ia32Syscall ("nfsservctl", 169, 3, "i:ipp "),
    new Ia32Syscall ("setresgid", 170, 3, "i:iii "),
    new Ia32Syscall ("getresgid", 171, 3, "i:ppp "),
    new Ia32Syscall ("prctl", 172, 5, "i:iiiii "),
    new Ia32Syscall ("rt_sigreturn", 173),
    new Ia32Syscall ("rt_sigaction", 174),
    new Ia32Syscall ("rt_sigprocmask", 175),
    new Ia32Syscall ("rt_sigpending", 176),
    new Ia32Syscall ("rt_sigtimedwait", 177),
    new Ia32Syscall ("rt_sigqueueinfo", 178),
    new Ia32Syscall ("rt_sigsuspend", 179),
    new Ia32Syscall ("pread64", 180),
    new Ia32Syscall ("pwrite64", 181),
    new Ia32Syscall ("chown", 182, 3, "i:sii "),
    new Ia32Syscall ("getcwd", 183, 2, "i:bi "),
    new Ia32Syscall ("capget", 184, 2, "i:pp "),
    new Ia32Syscall ("capset", 185, 2, "i:pp "),
    new Ia32Syscall ("sigaltstack", 186, 2, "i:PP "),
    new Ia32Syscall ("sendfile", 187, 4, "i:iipi "),
    new Ia32Syscall ("getpmsg", 188),
    new Ia32Syscall ("putpmsg", 189),
    new Ia32Syscall ("vfork", 190, 0, "i: "),
    new Ia32Syscall ("ugetrlimit", 191),
    new Ia32Syscall ("mmap2", 192, 6, "b:aniiii "),
    new Ia32Syscall ("truncate64", 193, 3, "i:shl "),
    new Ia32Syscall ("ftruncate64", 194, 3, "i:ihl "),
    new Ia32Syscall ("stat64", 195, 2, "i:sp "),
    new Ia32Syscall ("lstat64", 196, 2, "i:sp "),
    new Ia32Syscall ("fstat64", 197, 2, "i:ip "),
    new Ia32Syscall ("lchown32", 198, 3, "i:sii "),
    new Ia32Syscall ("getuid32", 199),
    new Ia32Syscall ("getgid32", 200),
    new Ia32Syscall ("geteuid32", 201),
    new Ia32Syscall ("getegid32", 202),
    new Ia32Syscall ("setreuid32", 203, 2, "i:ii "),
    new Ia32Syscall ("setregid32", 204, 2, "i:ii "),
    new Ia32Syscall ("getgroups32", 205, 2, "i:ip "),
    new Ia32Syscall ("setgroups32", 206, 2, "i:ip "),
    new Ia32Syscall ("fchown32", 207, 3, "i:iii "),
    new Ia32Syscall ("setresuid32", 208, 3, "i:iii "),
    new Ia32Syscall ("getresuid32", 209, 3, "i:ppp "),
    new Ia32Syscall ("setresgid32", 210, 3, "i:iii "),
    new Ia32Syscall ("getresgid32", 211, 3, "i:ppp "),
    new Ia32Syscall ("chown32", 212, 3, "i:sii "),
    new Ia32Syscall ("setuid32", 213, 1, "i:i "),
    new Ia32Syscall ("setgid32", 214, 1, "i:i "),
    new Ia32Syscall ("setfsuid32", 215, 1, "i:i "),
    new Ia32Syscall ("setfsgid32", 216, 1, "i:i "),
    new Ia32Syscall ("pivot_root", 217, 2, "i:ss "),
    new Ia32Syscall ("mincore", 218, 3, "i:anV "),
    new Ia32Syscall ("madvise", 219, 3, "i:pii "),
    new Ia32Syscall ("getdents64", 220, 3, "i:ipi "),
    new Ia32Syscall ("fcntl64", 221, 3, "i:iip "),
    new Ia32Syscall (222),
    new Ia32Syscall (223),
    new Ia32Syscall ("gettid", 224, 0),
    new Ia32Syscall ("readahead", 225, 4, "i:ihli "),
    new Ia32Syscall ("setxattr", 226),
    new Ia32Syscall ("lsetxattr", 227),
    new Ia32Syscall ("fsetxattr", 228),
    new Ia32Syscall ("getxattr", 229),
    new Ia32Syscall ("lgetxattr", 230),
    new Ia32Syscall ("fgetxattr", 231),
    new Ia32Syscall ("listxattr", 232),
    new Ia32Syscall ("llistxattr", 233),
    new Ia32Syscall ("flistxattr", 234),
    new Ia32Syscall ("removexattr", 235),
    new Ia32Syscall ("lremovexattr", 236),
    new Ia32Syscall ("fremovexattr", 237),
    new Ia32Syscall ("tkill", 238, 2, "i:ii "),
    new Ia32Syscall ("sendfile64", 239, 4, "i:iipi "),
    new Ia32Syscall ("futex", 240, 4, "i:piip "),
    new Ia32Syscall ("sched_setaffinity", 241),
    new Ia32Syscall ("sched_getaffinity", 242),
    new Ia32Syscall ("set_thread_area", 243, 1, "i:p "),
    new Ia32Syscall ("get_thread_area", 244, 1, "i:p "),
    new Ia32Syscall ("io_setup", 245, 2, "i:ip "),
    new Ia32Syscall ("io_destroy", 246, 1, "i:i "),
    new Ia32Syscall ("io_getevents", 247, 5, "i:iiipp "),
    new Ia32Syscall ("io_submit", 248, 3, "i:iip "),
    new Ia32Syscall ("io_cancel", 249, 3, "i:ipp "),
    new Ia32Syscall ("fadvise64", 250),
    new Ia32Syscall (251),
    new Ia32Syscall ("exit_group", 252, 1, " :i ", true),
    new Ia32Syscall ("lookup_dcookie", 253),
    new Ia32Syscall ("epoll_create", 254),
    new Ia32Syscall ("epoll_ctl", 255),
    new Ia32Syscall ("epoll_wait", 256),
    new Ia32Syscall ("remap_file_pages", 257),
    new Ia32Syscall ("set_tid_address", 258),
    new Ia32Syscall ("timer_create", 259),
    new Ia32Syscall ("timer_settime", 260),
    new Ia32Syscall ("timer_gettime", 261),
    new Ia32Syscall ("timer_getoverrun", 262),
    new Ia32Syscall ("timer_delete", 263),
    new Ia32Syscall ("clock_settime", 264),
    new Ia32Syscall ("clock_gettime", 265),
    new Ia32Syscall ("clock_getres", 266),
    new Ia32Syscall ("clock_nanosleep", 267),
    new Ia32Syscall ("statfs64", 268),
    new Ia32Syscall ("fstatfs64", 269),
    new Ia32Syscall ("tgkill", 270),
    new Ia32Syscall ("utimes", 271),
    new Ia32Syscall ("fadvise64_64", 272),
    new Ia32Syscall ("vserver", 273),
    new Ia32Syscall ("mbind", 274),
    new Ia32Syscall ("get_mempolicy", 275),
    new Ia32Syscall ("set_mempolicy", 276),
    new Ia32Syscall ("mq_open", 277),
    new Ia32Syscall ("mq_unlink", 278),
    new Ia32Syscall ("mq_timedsend", 279),
    new Ia32Syscall ("mq_timedreceive", 280),
    new Ia32Syscall ("mq_notify", 281),
    new Ia32Syscall ("mq_getsetattr", 282),
    new Ia32Syscall ("sys_kexec_load", 283),
    new Ia32Syscall ("waitid", 284),
    new Ia32Syscall (285),
    new Ia32Syscall ("add_key", 286),
    new Ia32Syscall ("request_key", 287),
    new Ia32Syscall ("keyctl", 288),
    new Ia32Syscall ("ioprio_set", 289),
    new Ia32Syscall ("ioprio_get", 290),
    new Ia32Syscall ("inotify_init", 291),
    new Ia32Syscall ("inotify_add_watch", 292),
    new Ia32Syscall ("inotify_rm_watch", 293),
    new Ia32Syscall ("migrate_pages", 294),
    new Ia32Syscall ("openat", 295),
    new Ia32Syscall ("mkdirat", 296),
    new Ia32Syscall ("mknodat", 297),
    new Ia32Syscall ("fchownat", 298),
    new Ia32Syscall ("futimesat", 299),
    new Ia32Syscall ("fstatat64", 300),
    new Ia32Syscall ("unlinkat", 301),
    new Ia32Syscall ("renameat", 302),
    new Ia32Syscall ("linkat", 303),
    new Ia32Syscall ("symlinkat", 304),
    new Ia32Syscall ("readlinkat", 305),
    new Ia32Syscall ("fchmodat", 306),
    new Ia32Syscall ("faccessat", 307),
    new Ia32Syscall ("pselect6", 308),
    new Ia32Syscall ("ppoll", 309),
    new Ia32Syscall ("unshare", 310),
    new Ia32Syscall ("set_robust_list", 311),
    new Ia32Syscall ("get_robust_list", 312),
    new Ia32Syscall ("splice", 313),
    new Ia32Syscall ("sync_file_range", 314),
    new Ia32Syscall ("tee", 315),
    new Ia32Syscall ("vmsplice", 316),
    new Ia32Syscall ("move_pages", 317)
    };


  static class SocketSubSyscall
    extends Ia32Syscall
  {
    SocketSubSyscall (String name, int number)
    {
      super (name, number);
    }
    SocketSubSyscall (String name, int number, int numArgs, String argList)
    {
      super (name, number, numArgs, argList);
    }

    public long getArguments (Task task, int n)
    {
      /** Arguments in socket subcalls are dereferenced. */
      Isa isa;
      try
	{
	  isa = task.getIsa();
	}
      catch (Exception e)
	{
	  throw new RuntimeException ("Could not get isa");
	}
      long base = isa.getRegisterByName("ecx").get (task);
	
      //System.out.println(Long.toHexString(base) + " " + n);
      return task.getMemory().getInt(base + (n-1) * isa.getWordSize());
    }
  }

  static Syscall[] socketSubcallList = {
    new SocketSubSyscall ("", SOCKET_NUM),
    new SocketSubSyscall ("socket",     SOCKET_NUM, 3, "i:iii"),
    new SocketSubSyscall ("bind",       SOCKET_NUM, 3, "i:ipi "),
    new SocketSubSyscall ("connect",    SOCKET_NUM, 3, "i:ipi "),
    new SocketSubSyscall ("listen",     SOCKET_NUM, 2, "i:ii "),
    new SocketSubSyscall ("accept",     SOCKET_NUM, 3, "i:ipp "),
    new SocketSubSyscall ("getsockname",SOCKET_NUM, 3, "i:ipp "),
    new SocketSubSyscall ("getpeername",SOCKET_NUM, 4, "i:iiip "),
    new SocketSubSyscall ("socketpair", SOCKET_NUM, 4, "i:iiip "),
    new SocketSubSyscall ("send",       SOCKET_NUM, 4, "i:ipii "),
    new SocketSubSyscall ("recv",       SOCKET_NUM, 4, "i:ipii "),
    new SocketSubSyscall ("sendto",     SOCKET_NUM, 6, "i:ipiipi"),
    new SocketSubSyscall ("recvfrom",   SOCKET_NUM, 6, "i:ipiipp "),
    new SocketSubSyscall ("shutdown",   SOCKET_NUM, 2, "i:ii "),
    new SocketSubSyscall ("setsockopt", SOCKET_NUM, 5, "i:iiipp "),
    new SocketSubSyscall ("getsockopt", SOCKET_NUM, 5, "i:iiipp "),
    new SocketSubSyscall ("sendmsg",    SOCKET_NUM, 5, "i:iiipp "),
    new SocketSubSyscall ("recvmsg",    SOCKET_NUM, 5, "i:iiipp ")
  };


  static class IpcSubSyscall
    extends Ia32Syscall
  {
    IpcSubSyscall (String name, int number)
    {
      super (name, number);
    }
    IpcSubSyscall (String name, int number, int numArgs, String argList)
    {
      super (name, number, numArgs, argList);
    }
    public long getArguments (Task task, int n)
    {
      if (n == 0)
	return super.getArguments (task, 0);
      else
	// these arguements are shifted by one.
	return super.getArguments (task, n+1);
    }
 
  }
  /**FIXME: No argument list here.*/
  static Syscall[] ipcSubcallList = {
    new IpcSubSyscall ("semop",  IPC_NUM),
    new IpcSubSyscall ("semget", IPC_NUM),
    new IpcSubSyscall ("semctl",  IPC_NUM),
    new IpcSubSyscall ("semtimedop", IPC_NUM),
    new IpcSubSyscall ("",  IPC_NUM),
    new IpcSubSyscall ("", IPC_NUM),
    new IpcSubSyscall ("",  IPC_NUM),
    new IpcSubSyscall ("", IPC_NUM),
    new IpcSubSyscall ("",  IPC_NUM),
    new IpcSubSyscall ("", IPC_NUM),
    new IpcSubSyscall ("msgsnd",  IPC_NUM),
    new IpcSubSyscall ("msgrcv", IPC_NUM),
    new IpcSubSyscall ("msgget",  IPC_NUM),
    new IpcSubSyscall ("msgctl", IPC_NUM),
    new IpcSubSyscall ("",  IPC_NUM),
    new IpcSubSyscall ("", IPC_NUM),
    new IpcSubSyscall ("",  IPC_NUM),
    new IpcSubSyscall ("", IPC_NUM),
    new IpcSubSyscall ("",  IPC_NUM),
    new IpcSubSyscall ("", IPC_NUM),
    new IpcSubSyscall ("shmat",  IPC_NUM),
    new IpcSubSyscall ("shmdt", IPC_NUM),
    new IpcSubSyscall ("shmget",  IPC_NUM),
    new IpcSubSyscall ("shmctl", IPC_NUM)
  };

  public static Syscall syscallByNum (Task task, int number)
  {
    if (number != SOCKET_NUM && number != IPC_NUM)
      return Syscall.syscallByNum (number, task);
    else
      {
	/** sub syscall number is in %ebx.  */
	int subSyscallNumber = 0;
	try
	  {
	    subSyscallNumber = (int) task.getIsa().getRegisterByName("ebx").get (task);
	  }
	catch (Exception e)
	  {
	    throw new RuntimeException ("Could not get isa");
	  }
	
	if (number == SOCKET_NUM)
	  {
	    return socketSubcallList[subSyscallNumber];
	  }
	else
	  {
	    return ipcSubcallList[subSyscallNumber];
	  }
      }
  }
}
