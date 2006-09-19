// This file is part of the program FRYSK.
//
// Copyright 2005, 2006 Red Hat Inc.
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

public class LinuxX8664Syscall
{

  // This is used to keep track of syscalls whose number we do not
  // know.
  static HashMap unknownSyscalls = new HashMap();

  static class X8664Syscall 
    extends Syscall
  {
    X8664Syscall (String name, int number, int numArgs, 
	     String argList, boolean noreturn)
    {
      super (name, number, numArgs, argList, noreturn);
    }
    X8664Syscall (String name, int number, int numArgs, String argList)
    {
      super (name, number, numArgs, argList);
    }      
    X8664Syscall (String name, int number, int numArgs)
    {
      super (name, number, numArgs);
    }
    X8664Syscall (String name, int number)
    {
      super (name, number);
    }
    X8664Syscall (int number)
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

      switch (n) {
      case 0:
	return isa.getRegisterByName ("orig_rax").get(task);
      case 1:
	return isa.getRegisterByName("rdi").get(task);
      case 2:
	return isa.getRegisterByName("rsi").get (task);
      case 3:
	return isa.getRegisterByName("rdx").get (task);
      case 4:
	return isa.getRegisterByName("r10").get (task);
      case 5:
	return isa.getRegisterByName("r8").get (task);
      case 6:
	return isa.getRegisterByName("r9").get (task);
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
      return isa.getRegisterByName ("rax").get (task);
    }
  }
  static Syscall[] syscallList = {
    new X8664Syscall ("read",  0, 3, "i:ibn "),
    new X8664Syscall ("write", 1, 3, "i:ibn "),
    new X8664Syscall ("open",  2, 3, "i:siv "),
    new X8664Syscall ("close", 3, 1, "i:i "),
    new X8664Syscall ("stat", 4, 2, "i:sp "),
    new X8664Syscall ("fstat", 5, 2, "i:ip "),
    new X8664Syscall ("lstat", 6, 2, "i:sp "),
    new X8664Syscall ("poll", 7, 3, "i:pii "),
    new X8664Syscall ("lseek", 8, 3, "i:iii "),
    new X8664Syscall ("mmap", 9, 6, "b:aniiii "),
    new X8664Syscall ("mprotect", 10, 3, "i:aii "),
    new X8664Syscall ("munmap", 11, 2, "i:ai "),
    new X8664Syscall ("brk", 12, 1, "i:p "),
    new X8664Syscall ("rt_sigaction", 13),
    new X8664Syscall ("rt_sigprocmask", 14),
    new X8664Syscall ("rt_sigreturn", 15),
    new X8664Syscall ("ioctl", 16, 3, "i:iiI "),
    new X8664Syscall ("pread64", 17),
    new X8664Syscall ("pwrite64", 18),
    new X8664Syscall ("readv", 19, 3, "i:ipi "),
    new X8664Syscall ("writev", 20, 3, "i:ipi "),
    new X8664Syscall ("access", 21, 2, "i:si "),
    new X8664Syscall ("pipe", 22, 1, "i:f "),
    new X8664Syscall ("select", 23, 5, "i:iPPPP "),
    new X8664Syscall ("sched_yield", 24, 0, "i: "),
    new X8664Syscall ("mremap", 25, 4, "b:aini "),
    new X8664Syscall ("msync", 26, 3, "i:aii "),
    new X8664Syscall ("mincore", 27, 3, "i:anV "),
    new X8664Syscall ("madvise", 28, 3, "i:pii "),
    new X8664Syscall ("shmget", 29, 3, "i:iii "),
    new X8664Syscall ("shmat", 30, 3, "p:ipi "),
    new X8664Syscall ("shmctl", 31, 3, "i:iip "),
    new X8664Syscall ("dup", 32, 1, "i:i "),
    new X8664Syscall ("dup2", 33, 2, "i:ii "),
    new X8664Syscall ("pause", 34, 0, "i: "),
    new X8664Syscall ("nanosleep", 35, 2, "i:pp "),
    new X8664Syscall ("getitimer", 36, 2, "i:ip "),
    new X8664Syscall ("alarm", 37, 1, "i:i "),
    new X8664Syscall ("setitimer", 38, 3, "i:ipp "),
    new X8664Syscall ("getpid", 39, 0, "i: "),
    new X8664Syscall ("sendfile", 40, 4, "i:iipi "),
    new X8664Syscall ("socket", 41, 3, "i:iii "),
    new X8664Syscall ("connect", 42, 3, "i:ipi "),
    new X8664Syscall ("accept", 43, 3, "i:ipp "),
    new X8664Syscall ("sendto", 44, 6, "i:ipiipi"),
    new X8664Syscall ("recvfrom", 45, 6, "i:ipiipp "),
    new X8664Syscall ("sendmsg", 46, 5, "i:iiipp "),
    new X8664Syscall ("recvmsg", 47, 5, "i:iiipp "),
    new X8664Syscall ("shutdown", 48, 2, "i:ii "),
    new X8664Syscall ("bind", 49, 3, "i:ipi "),
    new X8664Syscall ("listen", 50, 2, "i:ii "),
    new X8664Syscall ("getsockname", 51, 3, "i:ipp "),
    new X8664Syscall ("getpeername", 52, 4, "i:iiip "),
    new X8664Syscall ("socketpair", 53, 4, "i:iiip "),
    new X8664Syscall ("setsockopt", 54, 5, "i:iiipp "),
    new X8664Syscall ("getsockopt", 55, 5, "i:iiipp "),
    new X8664Syscall ("clone", 56, 2, "i:ip "),
    new X8664Syscall (58),
    new X8664Syscall ("fork", 57, 0, "i: "),
    new X8664Syscall ("execve", 59, 3, "i:ppp "),
    new X8664Syscall ("exit", 60, 1),
    new X8664Syscall ("wait4", 61, 4, "i:iWiP "),
    new X8664Syscall ("kill", 62, 2, "i:ii "),
    new X8664Syscall ("uname", 63, 1, "i:p "),
    new X8664Syscall ("semget", 64),
    new X8664Syscall ("semop", 65),
    new X8664Syscall ("semctl", 66),
    new X8664Syscall ("shmdt", 67),
    new X8664Syscall ("msgget", 68),
    new X8664Syscall ("msgsnd", 69),
    new X8664Syscall ("msgrcv", 70),
    new X8664Syscall ("msgctl", 71),
    new X8664Syscall ("fcntl", 72, 3, "i:iiF "),
    new X8664Syscall ("flock", 73, 2, "i:ii "),
    new X8664Syscall ("fsync", 74, 1, "i:i "),
    new X8664Syscall ("fdatasync", 75, 1, "i:i "),
    new X8664Syscall ("truncate", 76, 2, "i:si "),
    new X8664Syscall ("ftruncate", 77, 2, "i:ii "),
    new X8664Syscall ("getdents", 78),
    new X8664Syscall ("getcwd", 79, 2, "i:bi "),
    new X8664Syscall ("chdir", 80, 1, "i:s "),
    new X8664Syscall ("fchdir", 81, 1, "i:i "),
    new X8664Syscall ("rename", 82, 2, "i:ss "),
    new X8664Syscall ("mkdir", 83, 2, "i:si "),
    new X8664Syscall ("rmdir", 84, 1, "i:s "),
    new X8664Syscall ("creat", 85, 2, "i:sv"),
    new X8664Syscall ("link", 86, 2, "i:ss "),
    new X8664Syscall ("unlink", 87, 1, "i:s "),
    new X8664Syscall ("symlink", 88, 2, "i:ss "),
    new X8664Syscall ("readlink", 89, 3, "i:spi "),
    new X8664Syscall ("chmod", 90, 2, "i:si "),
    new X8664Syscall ("fchmod", 91, 2, "i:ii "),
    new X8664Syscall ("chown", 92, 3, "i:sii "),
    new X8664Syscall ("fchown", 93, 3, "i:iii "),
    new X8664Syscall ("lchown", 94, 3, "i:sii "),
    new X8664Syscall ("umask", 95, 1, "i:i "),
    new X8664Syscall ("gettimeofday", 96, 2, "i:PP "),
    new X8664Syscall ("getrlimit", 97, 2, "i:ip "),
    new X8664Syscall ("getrusage", 98, 2, "i:ip "),
    new X8664Syscall ("sysinfo", 99, 1, "i:p "),
    new X8664Syscall ("times", 100, 1, "i:p "),
    new X8664Syscall ("ptrace", 101, 4, "i:iiii "),
    new X8664Syscall ("getuid", 102, 0, "i: "),
    new X8664Syscall ("syslog", 103),
    new X8664Syscall ("getgid", 104, 0, "i: "),
    new X8664Syscall ("setuid", 105, 1, "i:i "),
    new X8664Syscall ("setgid", 106, 1, "i:i "),
    new X8664Syscall ("geteuid", 107, 0, "i: "),
    new X8664Syscall ("getegid", 108, 0, "i: "),
    new X8664Syscall ("setpgid", 109, 2, "i:ii "),
    new X8664Syscall ("getppid", 110, 0, "i: "),
    new X8664Syscall ("getpgrp", 111, 0, "i: "),
    new X8664Syscall ("setsid", 112, 0, "i: "),
    new X8664Syscall ("setreuid", 113, 2, "i:ii "),
    new X8664Syscall ("setregid", 114, 2, "i:ii "),
    new X8664Syscall ("getgroups", 115, 2, "i:ip "),
    new X8664Syscall ("setgroups", 116, 2, "i:ip "),
    new X8664Syscall ("setresuid", 117, 3, "i:iii "),
    new X8664Syscall ("getresuid", 118, 3, "i:ppp "),
    new X8664Syscall ("setresgid", 119, 3, "i:iii "),
    new X8664Syscall ("getresgid", 120, 3, "i:ppp "),
    new X8664Syscall ("getpgid", 121, 1, "i:i "),
    new X8664Syscall ("setfsuid", 122, 1, "i:i "),
    new X8664Syscall ("setfsgid", 123, 1, "i:i "),
    new X8664Syscall ("getsid", 124, 1, "i:i "),
    new X8664Syscall ("capget", 125, 2, "i:pp "),
    new X8664Syscall ("capset", 126, 2, "i:pp "),
    new X8664Syscall ("rt_sigpending", 127, 2, "i:pi "),
    new X8664Syscall ("rt_sigtimedwait", 128, 4, "i:pppi " ),
    new X8664Syscall ("rt_sigqueueinfo", 129 ),
    new X8664Syscall ("rt_sigsuspend", 130 ),
    new X8664Syscall ("sigaltstack", 131, 2, "i:PP "),
    new X8664Syscall ("utime", 132, 2, "i:sP "),
    new X8664Syscall ("mknod", 133, 3, "i:sii "),
    new X8664Syscall ("uselib", 134, 1, "i:s "),
    new X8664Syscall ("personality", 135, 1, "i:i "),
    new X8664Syscall ("ustat", 136, 2, "i:ip "),
    new X8664Syscall ("statfs", 137, 2, "i:sp "),
    new X8664Syscall ("fstatfs", 138, 2, "i:ip "),
    new X8664Syscall ("sysfs", 139, 1, "i:i "),
    new X8664Syscall ("getpriority", 140, 2, "i:ii "),
    new X8664Syscall ("setpriority", 141, 3, "i:iii "),
    new X8664Syscall ("sched_setparam", 142),
    new X8664Syscall ("sched_getparam", 143),
    new X8664Syscall ("sched_setscheduler", 144),
    new X8664Syscall ("sched_getscheduler", 145),
    new X8664Syscall ("sched_get_priority_max", 146),
    new X8664Syscall ("sched_get_priority_min", 147),
    new X8664Syscall ("sched_rr_get_interval", 148),
    new X8664Syscall ("mlock", 149, 2, "i:bn "),
    new X8664Syscall ("munlock", 150, 2, "i:ai "),
    new X8664Syscall ("mlockall", 151, 1, "i:i "),
    new X8664Syscall ("munlockall", 152, 0, "i: "),
    new X8664Syscall ("vhangup", 153, 1, "i:i "),
    new X8664Syscall ("modify_ldt", 154, 3, "i:ipi "),
    new X8664Syscall ("pivot_root", 155, 2, "i:ss "),
    new X8664Syscall ("_sysctl", 156),
    new X8664Syscall ("prctl", 157, 5, "i:iiiii "),
    new X8664Syscall ("arch_prctl", 158),
    new X8664Syscall ("adjtimex", 159, 1, "i:p "),
    new X8664Syscall ("setrlimit", 160, 2, "i:ip "),
    new X8664Syscall ("chroot", 161, 1, "i:s "),
    new X8664Syscall ("sync", 162, 0, "i: "),
    new X8664Syscall ("acct", 163, 1, "i:S "),
    new X8664Syscall ("settimeofday", 164, 2, "i:PP "),
    new X8664Syscall ("mount", 165, 5, "i:sssip "),
    new X8664Syscall ("umount2", 166, 2, "i:si "),
    new X8664Syscall ("swapon", 167, 2, "i:si "),
    new X8664Syscall ("swapoff", 168, 1, "i:s "),
    new X8664Syscall ("reboot", 169, 1, "i:i "),
    new X8664Syscall ("sethostname", 170, 2, "i:pi "),
    new X8664Syscall ("setdomainname", 171),
    new X8664Syscall ("iopl", 172, 1, "i:i "),
    new X8664Syscall ("ioperm", 173, 3, "i:iii "),
    new X8664Syscall ("create_module", 174, 3),
    new X8664Syscall ("init_module", 175, 5),
    new X8664Syscall ("delete_module", 176, 3),
    new X8664Syscall ("get_kernel_syms", 177, 1, "i:p "),
    new X8664Syscall ("query_module", 178, 5, "i:sipip "),
    new X8664Syscall ("quotactl", 179, 4, "i:isip "),
    new X8664Syscall ("nfsservctl", 180, 3, "i:ipp "),
    new X8664Syscall ("getpmsg", 181),
    new X8664Syscall ("putpmsg", 182),
    new X8664Syscall ("afs_syscall", 183),
    new X8664Syscall ("tuxcall", 184),
    new X8664Syscall ("security", 185),
    new X8664Syscall ("gettid", 186, 0),
    new X8664Syscall ("readahead", 187, 4, "i:ihli "),
    new X8664Syscall ("setxattr", 188),
    new X8664Syscall ("lsetxattr", 189 ),
    new X8664Syscall ("fsetxattr", 190 ),
    new X8664Syscall ("getxattr", 191 ),
    new X8664Syscall ("lgetxattr", 192 ),
    new X8664Syscall ("fgetxattr", 193 ),
    new X8664Syscall ("listxattr", 194 ),
    new X8664Syscall ("llistxattr", 195 ),
    new X8664Syscall ("flistxattr", 196 ),
    new X8664Syscall ("removexattr", 197 ),
    new X8664Syscall ("lremovexattr", 198 ),
    new X8664Syscall ("fremovexattr", 199 ),
    new X8664Syscall ("tkill", 200, 2, "i:ii "),
    new X8664Syscall ("time", 201, 1, "i:P "),
    new X8664Syscall ("futex", 202, 4, "i:piip "),
    new X8664Syscall ("sched_setaffinity", 203 ),
    new X8664Syscall ("sched_getaffinity", 204 ),
    new X8664Syscall ("set_thread_area", 205, 1, "i:p "),
    new X8664Syscall ("io_setup", 206, 2, "i:ip "),
    new X8664Syscall ("io_destroy", 207, 1, "i:i "),
    new X8664Syscall ("io_getevents", 208, 5, "i:iiipp "),
    new X8664Syscall ("io_submit", 209, 3, "i:iip "),
    new X8664Syscall ("io_cancel", 210, 3, "i:ipp "),
    new X8664Syscall ("get_thread_area", 211, 1, "i:p "),
    new X8664Syscall ("lookup_dcookie", 212),
    new X8664Syscall ("epoll_create", 213),
    new X8664Syscall ("epoll_ctl_old", 214),
    new X8664Syscall ("epoll_wait_old", 215),
    new X8664Syscall ("remap_file_pages", 216),
    new X8664Syscall ("getdents64", 217, 3, "i:ipi "),
    new X8664Syscall ("set_tid_address", 218),
    new X8664Syscall ("restart_syscall", 219),
    new X8664Syscall ("semtimedop", 220),
    new X8664Syscall ("fadvise64", 221),
    new X8664Syscall ("timer_create", 222),
    new X8664Syscall ("timer_settime", 223),
    new X8664Syscall (224),
    new X8664Syscall (225),
    new X8664Syscall ("timer_delete", 226),
    new X8664Syscall ("clock_settime", 227),
    new X8664Syscall ("clock_gettime", 228),
    new X8664Syscall ("clock_getres", 229),
    new X8664Syscall ("clock_nanosleep", 230),
    new X8664Syscall ("exit_group", 231, 1, " :i "),
    new X8664Syscall ("epoll_wait", 232),
    new X8664Syscall ("epoll_ctl", 233),
    new X8664Syscall ("tgkill", 234),
    new X8664Syscall ("utimes", 235),
    new X8664Syscall ("vserver", 236),
    new X8664Syscall ("mbind", 237),
    new X8664Syscall ("set_mempolicy", 238),
    new X8664Syscall ("get_mempolicy", 239),
    new X8664Syscall ("mq_open", 240),
    new X8664Syscall ("mq_unlink", 241),
    new X8664Syscall ("mq_timedsend", 242),
    new X8664Syscall ("mq_timedreceive", 243),
    new X8664Syscall ("mq_notify", 244),
    new X8664Syscall ("mq_getsetattr", 245),
    new X8664Syscall ("kexec_load", 246),
    new X8664Syscall ("waitid", 247),
    new X8664Syscall ("add_key", 248),
    new X8664Syscall ("request_key", 249),
    new X8664Syscall ("keyctl", 250),
    new X8664Syscall ("ioprio_set", 251),
    new X8664Syscall ("ioprio_get", 252),
    new X8664Syscall ("inotify_init", 253),
    new X8664Syscall ("inotify_add_watch", 254),
    new X8664Syscall ("inotify_rm_watch", 255)
    };

  public static Syscall syscallByNum (Task task, int number)
  {
    return Syscall.syscallByNum (number, task);
  }
}
