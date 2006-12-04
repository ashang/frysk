// This file is part of the program FRYSK.
//
// Copyright 2006 IBM Corp.
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

public class LinuxPowerPCSyscall
{
  static final int SOCKET_NUM = 102;
  static final int IPC_NUM = 117;

  // This is used to keep track of syscalls whose number we do not
  // know.
  static HashMap unknownSyscalls = new HashMap();

  static class PowerPCSyscall 
    extends Syscall
  {
    PowerPCSyscall (String name, int number, int numArgs, 
	     String argList, boolean noreturn)
    {
      super (name, number, numArgs, argList, noreturn);
    }
    PowerPCSyscall (String name, int number, int numArgs, String argList)
    {
      super (name, number, numArgs, argList);
    }      
    PowerPCSyscall (String name, int number, int numArgs)
    {
      super (name, number, numArgs);
    }
    PowerPCSyscall (String name, int number)
    {
      super (name, number);
    }
    PowerPCSyscall (int number)
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
	  return isa.getRegisterByName("gpr0").get(task);
	case 1:
	  return isa.getRegisterByName("orig_r3").get(task);
	case 2:
	  return isa.getRegisterByName("gpr4").get(task);
	case 3:
	  return isa.getRegisterByName("gpr5").get(task);
	case 4:
	  return isa.getRegisterByName("gpr6").get(task);
	case 5:
	  return isa.getRegisterByName("gpr7").get(task);
	case 6:
	  return isa.getRegisterByName("gpr8").get(task);
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

      int flag = (int) isa.getRegisterByName("ccr").get(task);
      
      if ((flag & 0x10000000) != 0)
	return -isa.getRegisterByName("gpr3").get(task);
      else
	return isa.getRegisterByName("gpr3").get(task);
    }
  }

  static Syscall[] syscallList = {
    new PowerPCSyscall (0),
    new PowerPCSyscall ("exit", 1, 1),
    new PowerPCSyscall ("fork", 2, 0, "i: "),
    new PowerPCSyscall ("read", 3, 3, "i:ibn "),
    new PowerPCSyscall ("write", 4, 3, "i:ibn "),
    new PowerPCSyscall ("open", 5, 3, "i:siv "),
    new PowerPCSyscall ("close", 6, 1, "i:i "),
    new PowerPCSyscall ("waitpid", 7, 3, "i:ipi "),
    new PowerPCSyscall ("creat", 8, 2, "i:sv"),
    new PowerPCSyscall ("link", 9, 2, "i:ss "),
    new PowerPCSyscall ("unlink", 10, 1, "i:s "),
    new PowerPCSyscall ("execve", 11, 3, "i:ppp "),
    new PowerPCSyscall ("chdir", 12, 1, "i:s "),
    new PowerPCSyscall ("time", 13, 1, "i:P "),
    new PowerPCSyscall ("mknod", 14),
    new PowerPCSyscall ("chmod", 15, 2, "i:si "),
    new PowerPCSyscall ("lchown", 16, 3, "i:sii "),
    new PowerPCSyscall ("break", 17 ),
    new PowerPCSyscall ("oldstat", 18),
    new PowerPCSyscall ("lseek", 19, 3, "i:iii "),
    new PowerPCSyscall ("getpid", 20, 0, "i: "),
    new PowerPCSyscall ("mount", 21, 5, "i:sssip "),
    new PowerPCSyscall ("umount", 22, 1, "i:s "),
    new PowerPCSyscall ("setuid", 23, 1, "i:i "),
    new PowerPCSyscall ("getuid", 24, 0, "i: "),
    new PowerPCSyscall ("stime", 25, 1, "i:p "),
    new PowerPCSyscall ("ptrace", 26, 4, "i:iiii "),
    new PowerPCSyscall ("alarm", 27, 1, "i:i "),
    new PowerPCSyscall ("oldfstat", 28),
    new PowerPCSyscall ("pause", 29, 0, "i: "),
    new PowerPCSyscall ("utime", 30, 2, "i:sP "),
    new PowerPCSyscall ("stty", 31),
    new PowerPCSyscall ("gtty", 32),
    new PowerPCSyscall ("access", 33, 2, "i:si "),
    new PowerPCSyscall ("nice", 34, 1, "i:i "),
    new PowerPCSyscall ("ftime", 35, 1, "i:p "),
    new PowerPCSyscall ("sync", 36, 0, "i: "),
    new PowerPCSyscall ("kill", 37, 2, "i:ii "),
    new PowerPCSyscall ("rename", 38, 2, "i:ss "),
    new PowerPCSyscall ("mkdir", 39, 2, "i:si "),
    new PowerPCSyscall ("rmdir", 40, 1, "i:s "),
    new PowerPCSyscall ("dup", 41, 1, "i:i "),
    new PowerPCSyscall ("pipe", 42, 1, "i:f "),
    new PowerPCSyscall ("times", 43, 1, "i:p "),
    new PowerPCSyscall ("prof", 44),
    new PowerPCSyscall ("brk", 45, 1, "i:p "),
    new PowerPCSyscall ("setgid", 46, 1, "i:i "),
    new PowerPCSyscall ("getgid", 47, 0, "i: "),
    new PowerPCSyscall ("signal", 48, 2, "i:ii "),
    new PowerPCSyscall ("geteuid", 49, 0, "i: "),
    new PowerPCSyscall ("getegid", 50, 0, "i: "),
    new PowerPCSyscall ("acct", 51, 1, "i:S "),
    new PowerPCSyscall ("umount2", 52, 2, "i:si "),
    new PowerPCSyscall ("lock", 53),
    new PowerPCSyscall ("ioctl", 54, 3, "i:iiI "),
    new PowerPCSyscall ("fcntl", 55, 3, "i:iiF "),
    new PowerPCSyscall ("mpx", 56),
    new PowerPCSyscall ("setpgid", 57, 2, "i:ii "),
    new PowerPCSyscall ("ulimit", 58, 2, "i:ii "),
    new PowerPCSyscall ("oldolduname", 59),
    new PowerPCSyscall ("umask", 60, 1, "i:i "),
    new PowerPCSyscall ("chroot", 61, 1, "i:s "),
    new PowerPCSyscall ("ustat", 62, 2, "i:ip "),
    new PowerPCSyscall ("dup2", 63, 2, "i:ii "),
    new PowerPCSyscall ("getppid", 64, 0, "i: "),
    new PowerPCSyscall ("getpgrp", 65, 0, "i: "),
    new PowerPCSyscall ("setsid", 66, 0, "i: "),
    new PowerPCSyscall ("sigaction", 67, 3, "i:ipp "),
    new PowerPCSyscall ("sgetmask", 68),
    new PowerPCSyscall ("ssetmask", 69),
    new PowerPCSyscall ("setreuid", 70, 2, "i:ii "),
    new PowerPCSyscall ("setregid", 71, 2, "i:ii "),
    new PowerPCSyscall ("sigsuspend", 72, 1, "i:p "),
    new PowerPCSyscall ("sigpending", 73, 1, "i:p "),
    new PowerPCSyscall ("sethostname", 74, 2, "i:pi "),
    new PowerPCSyscall ("setrlimit", 75, 2, "i:ip "),
    new PowerPCSyscall ("getrlimit", 76, 2, "i:ip "),
    new PowerPCSyscall ("getrusage", 77, 2, "i:ip "),
    new PowerPCSyscall ("gettimeofday", 78, 2, "i:PP "),
    new PowerPCSyscall ("settimeofday", 79, 2, "i:PP "),
    new PowerPCSyscall ("getgroups", 80, 2, "i:ip "),
    new PowerPCSyscall ("setgroups", 81, 2, "i:ip "),
    new PowerPCSyscall ("select", 82, 5, "i:iPPPP "),
    new PowerPCSyscall ("symlink", 83, 2, "i:ss "),
    new PowerPCSyscall ("oldlstat", 84, 2, "i:pp "),
    new PowerPCSyscall ("readlink", 85, 3, "i:spi "),
    new PowerPCSyscall ("uselib", 86, 1, "i:s "),
    new PowerPCSyscall ("swapon", 87, 2, "i:si "),
    new PowerPCSyscall ("reboot", 88, 1, "i:i "),
    new PowerPCSyscall ("readdir", 89),
    new PowerPCSyscall ("mmap", 90, 6, "b:aniiii "),
    new PowerPCSyscall ("munmap", 91, 2, "i:ai "),
    new PowerPCSyscall ("truncate", 92, 2, "i:si "),
    new PowerPCSyscall ("ftruncate", 93, 2, "i:ii "),
    new PowerPCSyscall ("fchmod", 94, 2, "i:ii "),
    new PowerPCSyscall ("fchown", 95, 3, "i:iii "),
    new PowerPCSyscall ("getpriority", 96, 2, "i:ii "),
    new PowerPCSyscall ("setpriority", 97, 3, "i:iii "),
    new PowerPCSyscall ("profil", 98, 4, "i:piii "),
    new PowerPCSyscall ("statfs", 99, 2, "i:sp "),
    new PowerPCSyscall ("fstatfs", 100, 2, "i:ip "),
    new PowerPCSyscall ("ioperm", 101, 3, "i:iii "),
    new PowerPCSyscall ("socketcall", 102, 2, "i:ip "),
    new PowerPCSyscall ("syslog", 103),
    new PowerPCSyscall ("setitimer", 104, 3, "i:ipp "),
    new PowerPCSyscall ("getitimer", 105, 2, "i:ip "),
    new PowerPCSyscall ("stat", 106),
    new PowerPCSyscall ("lstat", 107),
    new PowerPCSyscall ("fstat", 108),
    new PowerPCSyscall ("olduname", 109),
    new PowerPCSyscall ("iopl", 110, 1, "i:i "),
    new PowerPCSyscall ("vhangup", 111, 1, "i:i "),
    new PowerPCSyscall ("idle", 112, 0, "i: "),
    new PowerPCSyscall ("vm86", 113, 1, "i:p "),
    new PowerPCSyscall ("wait4", 114, 4, "i:iWiP "),
    new PowerPCSyscall ("swapoff", 115, 1, "i:s "),
    new PowerPCSyscall ("sysinfo", 116, 1, "i:p "),
    new PowerPCSyscall ("ipc", 117, 6, "i:iiiipi "),
    new PowerPCSyscall ("fsync", 118, 1, "i:i "),
    new PowerPCSyscall ("sigreturn", 119),
    new PowerPCSyscall ("clone", 120, 2, "i:ip "),
    new PowerPCSyscall ("setdomainname", 121),
    new PowerPCSyscall ("uname", 122, 1, "i:p "),
    new PowerPCSyscall ("modify_ldt", 123, 3, "i:ipi "),
    new PowerPCSyscall ("adjtimex", 124, 1, "i:p "),
    new PowerPCSyscall ("mprotect", 125, 3, "i:aii "),
    new PowerPCSyscall ("sigprocmask", 126, 3, "i:ipp "),
    new PowerPCSyscall ("create_module", 127, 3),
    new PowerPCSyscall ("init_module", 128, 5),
    new PowerPCSyscall ("delete_module", 129, 3),
    new PowerPCSyscall ("get_kernel_syms", 130, 1, "i:p "),
    new PowerPCSyscall ("quotactl", 131, 4, "i:isip "),
    new PowerPCSyscall ("getpgid", 132, 1, "i:i "),
    new PowerPCSyscall ("fchdir", 133, 1, "i:i "),
    new PowerPCSyscall ("bdflush", 134, 2, "i:ii "),
    new PowerPCSyscall ("sysfs", 135, 1, "i:i "),
    new PowerPCSyscall ("personality", 136, 1, "i:i "),
    new PowerPCSyscall ("afs_syscall", 137),
    new PowerPCSyscall ("setfsuid", 138, 1, "i:i "),
    new PowerPCSyscall ("setfsgid", 139, 1, "i:i "),
    new PowerPCSyscall ("_llseek", 140),
    new PowerPCSyscall ("getdents", 141),
    new PowerPCSyscall ("_newselect", 142),
    new PowerPCSyscall ("flock", 143, 2, "i:ii "),
    new PowerPCSyscall ("msync", 144, 3, "i:aii "),
    new PowerPCSyscall ("readv", 145, 3, "i:ipi "),
    new PowerPCSyscall ("writev", 146, 3, "i:ipi "),
    new PowerPCSyscall ("getsid", 147, 1, "i:i "),
    new PowerPCSyscall ("fdatasync", 148, 1, "i:i "),
    new PowerPCSyscall ("_sysctl", 149),
    new PowerPCSyscall ("mlock", 150, 2, "i:bn "),
    new PowerPCSyscall ("munlock", 151, 2, "i:ai "),
    new PowerPCSyscall ("mlockall", 152, 1, "i:i "),
    new PowerPCSyscall ("munlockall", 153, 0, "i: "),
    new PowerPCSyscall ("sched_setparam", 154),
    new PowerPCSyscall ("sched_getparam", 155),
    new PowerPCSyscall ("sched_setscheduler", 156),
    new PowerPCSyscall ("sched_getscheduler", 157),
    new PowerPCSyscall ("sched_yield", 158, 0, "i: "),
    new PowerPCSyscall ("sched_get_priority_max", 159),
    new PowerPCSyscall ("sched_get_priority_min", 160),
    new PowerPCSyscall ("sched_rr_get_interval", 161),
    new PowerPCSyscall ("nanosleep", 162, 2, "i:pp "),
    new PowerPCSyscall ("mremap", 163, 4, "b:aini "),
    new PowerPCSyscall ("setresuid", 164, 3, "i:iii "),
    new PowerPCSyscall ("getresuid", 165, 3, "i:ppp "),
    new PowerPCSyscall ("query_module", 166, 5, "i:sipip "),
    new PowerPCSyscall ("poll", 167, 3, "i:pii "),
    new PowerPCSyscall ("nfsservctl", 168, 3, "i:ipp "),
    new PowerPCSyscall ("setresgid", 169, 3, "i:iii "),
    new PowerPCSyscall ("getresgid", 170, 3, "i:ppp "),
    new PowerPCSyscall ("prctl", 171, 5, "i:iiiii "),
    new PowerPCSyscall ("rt_sigreturn", 172),
    new PowerPCSyscall ("rt_sigaction", 173),
    new PowerPCSyscall ("rt_sigprocmask", 174),
    new PowerPCSyscall ("rt_sigpending", 175),
    new PowerPCSyscall ("rt_sigtimedwait", 176),
    new PowerPCSyscall ("rt_sigqueueinfo", 177),
    new PowerPCSyscall ("rt_sigsuspend", 178),
    new PowerPCSyscall ("pread", 179),
    new PowerPCSyscall ("pwrite", 180),
    new PowerPCSyscall ("chown", 181, 3, "i:sii "),
    new PowerPCSyscall ("getcwd", 182, 2, "i:bi "),
    new PowerPCSyscall ("capget", 183, 2, "i:pp "),
    new PowerPCSyscall ("capset", 184, 2, "i:pp "),
    new PowerPCSyscall ("sigaltstack", 185, 2, "i:PP "),
    new PowerPCSyscall ("sendfile", 186, 4, "i:iipi "),
    new PowerPCSyscall ("getpmsg", 187),
    new PowerPCSyscall ("putpmsg", 188),
    new PowerPCSyscall ("vfork", 189, 0, "i: "),
    new PowerPCSyscall ("ugetrlimit", 190),
    new PowerPCSyscall ("readahead", 191, 4, "i:ihli "),
    new PowerPCSyscall ("mmap2", 192),
    new PowerPCSyscall ("truncate64", 193, 3, "i:shl "),
    new PowerPCSyscall ("ftruncate64", 194, 3, "i:ihl "),
    new PowerPCSyscall ("stat64", 195, 2, "i:sp "),
    new PowerPCSyscall ("lstat64", 196, 2, "i:sp "),
    new PowerPCSyscall ("fstat64", 197, 2, "i:ip "),
    new PowerPCSyscall ("pciconfig_read", 198),
    new PowerPCSyscall ("pciconfig_write", 199),
    new PowerPCSyscall ("pciconfig_iobase", 200),
    new PowerPCSyscall ("multiplexer", 201),
    new PowerPCSyscall ("getdents64", 202, 3, "i:ipi "),
    new PowerPCSyscall ("pivot_root", 203, 2, "i:ss "),
    new PowerPCSyscall ("fcntl64", 204, 3, "i:iip "),
    new PowerPCSyscall ("madvise", 205, 3, "i:pii "),
    new PowerPCSyscall ("mincore", 206, 3, "i:anV "),
    new PowerPCSyscall ("gettid", 207, 0),
    new PowerPCSyscall ("tkill", 208, 2, "i:ii "),
    new PowerPCSyscall ("setxattr", 209),
    new PowerPCSyscall ("lsetxattr", 210),
    new PowerPCSyscall ("fsetxattr", 211),
    new PowerPCSyscall ("getxattr", 212),
    new PowerPCSyscall ("lgetxattr", 213),
    new PowerPCSyscall ("fgetxattr", 214),
    new PowerPCSyscall ("listxattr", 215),
    new PowerPCSyscall ("llistxattr", 216),
    new PowerPCSyscall ("flistxattr", 217),
    new PowerPCSyscall ("removexattr", 218),
    new PowerPCSyscall ("lremovexattr", 219),
    new PowerPCSyscall ("fremovexattr", 220),
    new PowerPCSyscall ("futex", 221, 4, "i:piip "),
    new PowerPCSyscall ("sched_setaffinity", 222),
    new PowerPCSyscall ("sched_getaffinity", 223),
    new PowerPCSyscall (224),
    new PowerPCSyscall ("tuxcall", 225),
    new PowerPCSyscall ("sendfile64", 226, 4, "i:iipi "),
    new PowerPCSyscall ("io_setup", 227, 2, "i:ip "),
    new PowerPCSyscall ("io_destroy", 228, 1, "i:i "),
    new PowerPCSyscall ("io_getevents", 229, 5, "i:iiipp "),
    new PowerPCSyscall ("io_submit", 230, 3, "i:iip "),
    new PowerPCSyscall ("io_cancel", 231, 3, "i:ipp "),
    new PowerPCSyscall ("set_tid_address", 232),
    new PowerPCSyscall ("fadvise64", 233),
    new PowerPCSyscall ("exit_group", 234, 1, " :i "),
    new PowerPCSyscall ("lookup_dcookie", 235),
    new PowerPCSyscall ("epoll_create", 236),
    new PowerPCSyscall ("epoll_ctl", 237),
    new PowerPCSyscall ("epoll_wait", 238),
    new PowerPCSyscall ("remap_file_pages", 239),
    new PowerPCSyscall ("timer_create", 240),
    new PowerPCSyscall ("timer_settime", 241),
    new PowerPCSyscall ("timer_gettime", 242),
    new PowerPCSyscall ("timer_getoverrun", 243),
    new PowerPCSyscall ("timer_delete", 244),
    new PowerPCSyscall ("clock_settime", 245),
    new PowerPCSyscall ("clock_gettime", 246),
    new PowerPCSyscall ("clock_getres", 247),
    new PowerPCSyscall ("clock_nanosleep", 248),
    new PowerPCSyscall ("swapcontext", 249),
    new PowerPCSyscall ("tgkill", 250),
    new PowerPCSyscall ("utimes", 251),
    new PowerPCSyscall ("statfs64", 252 ),
    new PowerPCSyscall ("fstatfs64", 253),
    new PowerPCSyscall ("fadvise64_64", 254),
    new PowerPCSyscall ("rtas", 255),
    new PowerPCSyscall (256),
    new PowerPCSyscall (257),
    new PowerPCSyscall (258),
    new PowerPCSyscall (259),
    new PowerPCSyscall ("get_mempolicy", 260),
    new PowerPCSyscall ("set_mempolicy", 261),
    new PowerPCSyscall ("mq_open", 262),
    new PowerPCSyscall ("mq_unlink", 263),
    new PowerPCSyscall ("mq_timedsend", 264),
    new PowerPCSyscall ("mq_timedreceive", 265),
    new PowerPCSyscall ("mq_notify", 266),
    new PowerPCSyscall ("mq_getsetattr", 267),
    new PowerPCSyscall ("kexec_load", 268),
    new PowerPCSyscall ("add_key", 269),
    new PowerPCSyscall ("request_key", 270),
    new PowerPCSyscall ("keyctl", 271),
    new PowerPCSyscall ("waitid", 272),
    new PowerPCSyscall ("ioprio_set", 273),
    new PowerPCSyscall ("ioprio_get", 274),
    new PowerPCSyscall ("inotify_init", 275),
    new PowerPCSyscall ("inotify_add_watch", 276),
    new PowerPCSyscall ("inotify_rm_watch", 277),
    new PowerPCSyscall ("spu_run", 278),
    new PowerPCSyscall ("spu_create", 279),
    new PowerPCSyscall ("pselect6", 280),
    new PowerPCSyscall ("ppoll", 281),
    new PowerPCSyscall ("unshare", 282),
    new PowerPCSyscall ("splice", 283),
    new PowerPCSyscall ("tee", 284),
    new PowerPCSyscall ("vmsplice", 285),
    new PowerPCSyscall ("openat", 286),
    new PowerPCSyscall ("mkdirat", 287),
    new PowerPCSyscall ("mknodat", 288),
    new PowerPCSyscall ("fchownat", 289),
    new PowerPCSyscall ("futimesat", 290),
    new PowerPCSyscall ("fstatat", 291),
    new PowerPCSyscall ("unlinkat", 292),
    new PowerPCSyscall ("renameat", 293),
    new PowerPCSyscall ("linkat", 294),
    new PowerPCSyscall ("symlinkat", 295),
    new PowerPCSyscall ("readlinkat", 296),
    new PowerPCSyscall ("fchmodat", 297),
    new PowerPCSyscall ("faccessat", 298),
    new PowerPCSyscall ("get_robust_list", 299),
    new PowerPCSyscall ("set_robust_list", 300)
    };

  static class SocketSubSyscall
    extends PowerPCSyscall
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
      long base = isa.getRegisterByName("gpr4").get (task);
	
      // FIXME: There are some bi-arch issues
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
    extends PowerPCSyscall
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
  static Syscall[] ipcSubcallList = {
    new IpcSubSyscall ("semop",  IPC_NUM),
    new IpcSubSyscall("semget", IPC_NUM),
    new IpcSubSyscall("semctl",  IPC_NUM),
    new IpcSubSyscall("semtimedop", IPC_NUM),
    new IpcSubSyscall("",  IPC_NUM),
    new IpcSubSyscall("", IPC_NUM),
    new IpcSubSyscall("",  IPC_NUM),
    new IpcSubSyscall("", IPC_NUM),
    new IpcSubSyscall("",  IPC_NUM),
    new IpcSubSyscall("", IPC_NUM),
    new IpcSubSyscall("msgsnd",  IPC_NUM),
    new IpcSubSyscall("msgrcv", IPC_NUM),
    new IpcSubSyscall("msgget",  IPC_NUM),
    new IpcSubSyscall("msgctl", IPC_NUM),
    new IpcSubSyscall("",  IPC_NUM),
    new IpcSubSyscall("", IPC_NUM),
    new IpcSubSyscall("",  IPC_NUM),
    new IpcSubSyscall("", IPC_NUM),
    new IpcSubSyscall("",  IPC_NUM),
    new IpcSubSyscall("", IPC_NUM),
    new IpcSubSyscall("shmat",  IPC_NUM),
    new IpcSubSyscall("shmdt", IPC_NUM),
    new IpcSubSyscall("shmget",  IPC_NUM),
    new IpcSubSyscall("shmctl", IPC_NUM)
  };

  public static Syscall syscallByNum (Task task, int number)
  {
    if (number != SOCKET_NUM && number != IPC_NUM)
      return Syscall.syscallByNum (number, task);
    else
      {
	/** sub syscall number is in .  */
	int subSyscallNumber = 0;
	try
	  {
	    subSyscallNumber = (int) task.getIsa().getRegisterByName("orig_r3").get(task);
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
