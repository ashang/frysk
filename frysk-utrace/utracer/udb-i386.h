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

#ifndef UDB_I386_H
#define UDB_I386_H

#include <search.h>
#include <asm/ptrace.h>

#ifdef DO_UDB_INIT
#define DECL(v,i) v = i
#define REG_DECL(v,s,r) v = {s,r}
#else
#define DECL(v,i) v
#define REG_DECL(v,s,r) v
#endif

#ifndef DO_UDB_INIT
extern
#endif
const char * i386_signals[]
#ifdef DO_UDB_INIT
= {
  "unused",			//  0
  "SIGHUP",			//  1
  "SIGINT",			//  2
  "SIGQUIT",			//  3
  "SIGILL",			//  4
  "SIGTRAP",			//  5
  "SIGABRT",			//  6
  "SIGBUS",			//  7
  "SIGFPE",			//  8
  "SIGKILL",			//  9
  "SIGUSR1",			// 10
  "SIGSEGV",			// 11
  "SIGUSR2",			// 12
  "SIGPIPE",			// 13
  "SIGALRM",			// 14
  "SIGTERM",			// 15
  "SIGSTKFLT",			// 16
  "SIGCHLD",			// 17
  "SIGCONT",			// 18
  "SIGSTOP",			// 19
  "SIGTSTP",			// 20
  "SIGTTIN",			// 21
  "SIGTTOU",			// 22
  "SIGURG",			// 23
  "SIGXCPU",			// 24
  "SIGXFSZ",			// 25
  "SIGVTALRM",			// 26
  "SIGPROF",			// 27
  "SIGWINCH",			// 28
  "SIGIO",			// 29
  "SIGPWR",			// 30
  "SIGUNUSED"			// 31
}
#endif
  ;

int nr_signals
#ifdef DO_UDB_INIT
= sizeof(i386_signals)/sizeof(char *)
#endif
  ;

#ifndef DO_UDB_INIT
extern
#endif
const char * regset_names[]
#ifdef DO_UDB_INIT
= {
  "GPRS",
  "FPRS",
  "FPRX",
  "Descriptors",
  "Debug"
}
#endif
  ;

enum {
  REGSET_GPRS,
  REGSET_FPRS,
  REGSET_FPRX,
  REGSET_DESC,
  REGSET_DEBUG
};

#ifndef DO_UDB_INIT
extern
#endif
char * descriptor_names[]
#ifdef DO_UDB_INIT
= {
  "GDTR",
  "LDTR",
  "IDTR"
}
#endif
  ;


typedef struct {
  long regset;
  long reg;
} reg_data_s;

// derived from <kernel_src>/include/asm-i386/elf.h
// this doesn't match include/asm-i386/user.h
REG_DECL (reg_data_s ebx_data,		REGSET_GPRS,  0);
REG_DECL (reg_data_s ecx_data,		REGSET_GPRS,  1);
REG_DECL (reg_data_s edx_data,		REGSET_GPRS,  2);
REG_DECL (reg_data_s esi_data,		REGSET_GPRS,  3);
REG_DECL (reg_data_s edi_data,		REGSET_GPRS,  4);
REG_DECL (reg_data_s ebp_data,		REGSET_GPRS,  5);
REG_DECL (reg_data_s eax_data,		REGSET_GPRS,  6);
REG_DECL (reg_data_s xds_data,		REGSET_GPRS,  7);
REG_DECL (reg_data_s xes_data,		REGSET_GPRS,  8);
REG_DECL (reg_data_s xfs_data,		REGSET_GPRS,  9);
#if 1		// fixme -- <kernel>/asm/ptrace.h omits this, but utrace
//		returns 17 regs in gprs; there's a  mismatch somewhere
REG_DECL (reg_data_s xgs_data,		REGSET_GPRS, 10);
#endif
REG_DECL (reg_data_s orig_eax_data,	REGSET_GPRS, 11);
REG_DECL (reg_data_s eip_data,		REGSET_GPRS, 12);
REG_DECL (reg_data_s xcs_data,		REGSET_GPRS, 13);
REG_DECL (reg_data_s eflags_data,	REGSET_GPRS, 14);
REG_DECL (reg_data_s esp_data,		REGSET_GPRS, 15);
REG_DECL (reg_data_s xss_data,		REGSET_GPRS, 16);
REG_DECL (reg_data_s gdtr_data,		REGSET_DESC,  0);
REG_DECL (reg_data_s ldtr_data,		REGSET_DESC,  1);
REG_DECL (reg_data_s idtr_data,		REGSET_DESC,  2);
REG_DECL (reg_data_s debug_data,	REGSET_DEBUG, -1);
REG_DECL (reg_data_s gprs_data,		REGSET_GPRS,  -1);
REG_DECL (reg_data_s fprs_data,		REGSET_FPRS,  -1);
REG_DECL (reg_data_s fprx_data,		REGSET_FPRX,  -1);
REG_DECL (reg_data_s desc_data,		REGSET_DESC,  -1);

#ifndef DO_UDB_INIT
extern
#endif
ENTRY reg_mapping[]
#ifdef DO_UDB_INIT
= {
  {"ebx",	 (void *)&ebx_data},
  {"ecx",	 (void *)&ecx_data},
  {"edx",	 (void *)&edx_data},
  {"esi",	 (void *)&esi_data},
  {"edi",	 (void *)&edi_data},
  {"ebp",	 (void *)&ebp_data},
  {"eax",	 (void *)&eax_data},
  {"xds",	 (void *)&xds_data},
  {"xes",	 (void *)&xes_data},
  {"xfs",	 (void *)&xfs_data},
#if 1		// fixme -- <kernel>/asm/ptrace.h omits this, but utrace
//		returns 17 regs in gprs; there's a  mismatch somewhere
  {"xgs",	 (void *)&xgs_data},
#endif
  {"xcs",	 (void *)&xcs_data},
  {"orig_eax",	 (void *)&orig_eax_data},
  {"eip",	 (void *)&eip_data},
  {"eflags",	 (void *)&eflags_data},
  {"esp",	 (void *)&esp_data},
  {"xss",	 (void *)&xss_data},
  {"gdtr",	 (void *)&gdtr_data},
  {"ldtr",	 (void *)&ldtr_data},
  {"idtr",	 (void *)&idtr_data},
  {"gprs",	 (void *)&gprs_data},
  {"fprs",	 (void *)&fprs_data},
  {"fprx",	 (void *)&fprx_data},
  {"desc",	 (void *)&desc_data},
  {"debug",	 (void *)&debug_data},
}
#endif
  ;

int nr_regs
#ifdef DO_UDB_INIT
= sizeof(reg_mapping)/sizeof(ENTRY)
#endif
  ;



// from include/asm-i386/unistd.h
/*
 * This file contains the system call numbers.
 */

#ifndef DO_UDB_INIT
extern
#endif
ENTRY syscall_names[]
#ifdef DO_UDB_INIT
= {
  {"restart_syscall",	(void *)0},
  {"exit",	(void *)1},
  {"fork",	(void *)2},
  {"read",	(void *)3},
  {"write",	(void *)4},
  {"open",	(void *)5},
  {"close",	(void *)6},
  {"waitpid",	(void *)7},
  {"creat",	(void *)8},
  {"link",	(void *)9},
  {"unlink",	(void *)10},
  {"execve",	(void *)11},
  {"chdir",	(void *)12},
  {"time",	(void *)13},
  {"mknod",	(void *)14},
  {"chmod",	(void *)15},
  {"lchown",	(void *)16},
  {"break",	(void *)17},
  {"oldstat",	(void *)18},
  {"lseek",	(void *)19},
  {"getpid",	(void *)20},
  {"mount",	(void *)21},
  {"umount",	(void *)22},
  {"setuid",	(void *)23},
  {"getuid",	(void *)24},
  {"stime",	(void *)25},
  {"ptrace",	(void *)26},
  {"alarm",	(void *)27},
  {"oldfstat",	(void *)28},
  {"pause",	(void *)29},
  {"utime",	(void *)30},
  {"stty",	(void *)31},
  {"gtty",	(void *)32},
  {"access",	(void *)33},
  {"nice",	(void *)34},
  {"ftime",	(void *)35},
  {"sync",	(void *)36},
  {"kill",	(void *)37},
  {"rename",	(void *)38},
  {"mkdir",	(void *)39},
  {"rmdir",	(void *)40},
  {"dup",	(void *)41},
  {"pipe",	(void *)42},
  {"times",	(void *)43},
  {"prof",	(void *)44},
  {"brk",	(void *)45},
  {"setgid",	(void *)46},
  {"getgid",	(void *)47},
  {"signal",	(void *)48},
  {"geteuid",	(void *)49},
  {"getegid",	(void *)50},
  {"acct",	(void *)51},
  {"umount2",	(void *)52},
  {"lock",	(void *)53},
  {"ioctl",	(void *)54},
  {"fcntl",	(void *)55},
  {"mpx",	(void *)56},
  {"setpgid",	(void *)57},
  {"ulimit",	(void *)58},
  {"oldolduname",	(void *)59},
  {"umask",	(void *)60},
  {"chroot",	(void *)61},
  {"ustat",	(void *)62},
  {"dup2",	(void *)63},
  {"getppid",	(void *)64},
  {"getpgrp",	(void *)65},
  {"setsid",	(void *)66},
  {"sigaction",	(void *)67},
  {"sgetmask",	(void *)68},
  {"ssetmask",	(void *)69},
  {"setreuid",	(void *)70},
  {"setregid",	(void *)71},
  {"sigsuspend",	(void *)72},
  {"sigpending",	(void *)73},
  {"sethostname",	(void *)74},
  {"setrlimit",	(void *)75},
  {"getrlimit",	(void *)76},
  {"getrusage",	(void *)77},
  {"gettimeofday",	(void *)78},
  {"settimeofday",	(void *)79},
  {"getgroups",	(void *)80},
  {"setgroups",	(void *)81},
  {"select",	(void *)82},
  {"symlink",	(void *)83},
  {"oldlstat",	(void *)84},
  {"readlink",	(void *)85},
  {"uselib",	(void *)86},
  {"swapon",	(void *)87},
  {"reboot",	(void *)88},
  {"readdir",	(void *)89},
  {"mmap",	(void *)90},
  {"munmap",	(void *)91},
  {"truncate",	(void *)92},
  {"ftruncate",	(void *)93},
  {"fchmod",	(void *)94},
  {"fchown",	(void *)95},
  {"getpriority",	(void *)96},
  {"setpriority",	(void *)97},
  {"profil",	(void *)98},
  {"statfs",	(void *)99},
  {"fstatfs",	(void *)100},
  {"ioperm",	(void *)101},
  {"socketcall",	(void *)102},
  {"syslog",	(void *)103},
  {"setitimer",	(void *)104},
  {"getitimer",	(void *)105},
  {"stat",	(void *)106},
  {"lstat",	(void *)107},
  {"fstat",	(void *)108},
  {"olduname",	(void *)109},
  {"iopl",	(void *)110},
  {"vhangup",	(void *)111},
  {"idle",	(void *)112},
  {"vm86old",	(void *)113},
  {"wait4",	(void *)114},
  {"swapoff",	(void *)115},
  {"sysinfo",	(void *)116},
  {"ipc",	(void *)117},
  {"fsync",	(void *)118},
  {"sigreturn",	(void *)119},
  {"clone",	(void *)120},
  {"setdomainname",	(void *)121},
  {"uname",	(void *)122},
  {"modify_ldt",	(void *)123},
  {"adjtimex",	(void *)124},
  {"mprotect",	(void *)125},
  {"sigprocmask",	(void *)126},
  {"create_module",	(void *)127},
  {"init_module",	(void *)128},
  {"delete_module",	(void *)129},
  {"get_kernel_syms",	(void *)130},
  {"quotactl",	(void *)131},
  {"getpgid",	(void *)132},
  {"fchdir",	(void *)133},
  {"bdflush",	(void *)134},
  {"sysfs",	(void *)135},
  {"personality",	(void *)136},
  {"afs_syscall",	(void *)137 /* Syscall for Andrew File System */},
  {"setfsuid",	(void *)138},
  {"setfsgid",	(void *)139},
  {"_llseek",	(void *)140},
  {"getdents",	(void *)141},
  {"_newselect",	(void *)142},
  {"flock",	(void *)143},
  {"msync",	(void *)144},
  {"readv",	(void *)145},
  {"writev",	(void *)146},
  {"getsid",	(void *)147},
  {"fdatasync",	(void *)148},
  {"_sysctl",	(void *)149},
  {"mlock",	(void *)150},
  {"munlock",	(void *)151},
  {"mlockall",	(void *)152},
  {"munlockall",	(void *)153},
  {"sched_setparam",	(void *)154},
  {"sched_getparam",	(void *)155},
  {"sched_setscheduler",	(void *)156},
  {"sched_getscheduler",	(void *)157},
  {"sched_yield",		(void *)158},
  {"sched_get_priority_max",	(void *)159},
  {"sched_get_priority_min",	(void *)160},
  {"sched_rr_get_interval", (void *)161},
  {"nanosleep",	(void *)162},
  {"mremap",	(void *)163},
  {"setresuid",	(void *)164},
  {"getresuid",	(void *)165},
  {"vm86",	(void *)166},
  {"query_module",	(void *)167},
  {"poll",	(void *)168},
  {"nfsservctl",	(void *)169},
  {"setresgid",	(void *)170},
  {"getresgid",	(void *)171},
  {"prctl",	(void *)172},
  {"rt_sigreturn",	(void *)173},
  {"rt_sigaction",	(void *)174},
  {"rt_sigprocmask",	(void *)175},
  {"rt_sigpending",	(void *)176},
  {"rt_sigtimedwait",	(void *)177},
  {"rt_sigqueueinfo",	(void *)178},
  {"rt_sigsuspend",	(void *)179},
  {"pread64",	(void *)180},
  {"pwrite64",	(void *)181},
  {"chown",	(void *)182},
  {"getcwd",	(void *)183},
  {"capget",	(void *)184},
  {"capset",	(void *)185},
  {"sigaltstack",	(void *)186},
  {"sendfile",	(void *)187},
  {"getpmsg",	(void *)188},
  {"putpmsg",	(void *)189},
  {"vfork",	(void *)190},
  {"ugetrlimit",	(void *)191},
  {"mmap2",	(void *)192},
  {"truncate64",	(void *)193},
  {"ftruncate64",	(void *)194},
  {"stat64",	(void *)195},
  {"lstat64",	(void *)196},
  {"fstat64",	(void *)197},
  {"lchown32",	(void *)198},
  {"getuid32",	(void *)199},
  {"getgid32",	(void *)200},
  {"geteuid32",	(void *)201},
  {"getegid32",	(void *)202},
  {"setreuid32",	(void *)203},
  {"setregid32",	(void *)204},
  {"getgroups32",	(void *)205},
  {"setgroups32",	(void *)206},
  {"fchown32",	(void *)207},
  {"setresuid32",	(void *)208},
  {"getresuid32",	(void *)209},
  {"setresgid32",	(void *)210},
  {"getresgid32",	(void *)211},
  {"chown32",	(void *)212},
  {"setuid32",	(void *)213},
  {"setgid32",	(void *)214},
  {"setfsuid32",	(void *)215},
  {"setfsgid32",	(void *)216},
  {"pivot_root",	(void *)217},
  {"mincore",	(void *)218},
  {"madvise",	(void *)219},
  {"getdents64",	(void *)220},
  {"fcntl64",	(void *)221},
  {"unused222",	(void *)222},
  {"unused223",	(void *)223},
  {"gettid",	(void *)224},
  {"readahead",	(void *)225},
  {"setxattr",	(void *)226},
  {"lsetxattr",	(void *)227},
  {"fsetxattr",	(void *)228},
  {"getxattr",	(void *)229},
  {"lgetxattr",	(void *)230},
  {"fgetxattr",	(void *)231},
  {"listxattr",	(void *)232},
  {"llistxattr",	(void *)233},
  {"flistxattr",	(void *)234},
  {"removexattr",	(void *)235},
  {"lremovexattr",	(void *)236},
  {"fremovexattr",	(void *)237},
  {"tkill",	(void *)238},
  {"sendfile64",	(void *)239},
  {"futex",	(void *)240},
  {"sched_setaffinity",	(void *)241},
  {"sched_getaffinity",	(void *)242},
  {"set_thread_area",	(void *)243},
  {"get_thread_area",	(void *)244},
  {"io_setup",	(void *)245},
  {"io_destroy",	(void *)246},
  {"io_getevents",	(void *)247},
  {"io_submit",	(void *)248},
  {"io_cancel",	(void *)249},
  {"fadvise64",	(void *)250},
  {"unused251",	(void *)251},
  {"exit_group",	(void *)252},
  {"lookup_dcookie",	(void *)253},
  {"epoll_create	",	(void *)254},
  {"epoll_ctl",	(void *)255},
  {"epoll_wait",	(void *)256},
  {"remap_file_pages",	(void *)257},
  {"set_tid_address",	(void *)258},
  {"timer_create",	(void *)259},
  {"timer_settime",	(void *)260},
  {"timer_gettime",	(void *)261},
  {"timer_getoverrun",	(void *)262},
  {"timer_delete",	(void *)263},
  {"clock_settime",	(void *)264},
  {"clock_gettime",	(void *)265},
  {"clock_getres",	(void *)266},
  {"clock_nanosleep",	(void *)267},
  {"statfs64",	(void *)268},
  {"fstatfs64",	(void *)269},
  {"tgkill",	(void *)270},
  {"utimes",	(void *)271},
  {"fadvise64_64",	(void *)272},
  {"vserver",	(void *)273},
  {"mbind",	(void *)274},
  {"get_mempolicy",	(void *)275},
  {"set_mempolicy",	(void *)276},
  {"mq_open",		(void *)277},
  {"mq_unlink",		(void *)278},
  {"mq_timedsend",	(void *)279},
  {"mq_timedreceive",	(void *)280},
  {"mq_notify",		(void *)281},
  {"mq_getsetattr",	(void *)282},
  {"kexec_load",	(void *)283},
  {"waitid",		(void *)284},
  {"unused285",		(void *)285},
  {"add_key",		(void *)286},
  {"request_key",	(void *)287},
  {"keyctl",		(void *)288},
  {"ioprio_set",	(void *)289},
  {"ioprio_get",	(void *)290},
  {"inotify_init",	(void *)291},
  {"inotify_add_watch",	(void *)292},
  {"inotify_rm_watch",	(void *)293},
  {"migrate_pages",	(void *)294},
  {"openat",	(void *)295},
  {"mkdirat",	(void *)296},
  {"mknodat",	(void *)297},
  {"fchownat",	(void *)298},
  {"futimesat",	(void *)299},
  {"fstatat64",	(void *)300},
  {"unlinkat",	(void *)301},
  {"renameat",	(void *)302},
  {"linkat",	(void *)303},
  {"symlinkat",	(void *)304},
  {"readlinkat",	(void *)305},
  {"fchmodat",	(void *)306},
  {"faccessat",	(void *)307},
  {"pselect6",	(void *)308},
  {"ppoll",	(void *)309},
  {"unshare",	(void *)310},
  {"set_robust_list",	(void *)311},
  {"get_robust_list",	(void *)312},
  {"splice",	(void *)313},
  {"sync_file_range",	(void *)314},
  {"tee",	(void *)315},
  {"vmsplice",	(void *)316},
  {"move_pages",	(void *)317},
  {"getcpu",	(void *)318},
  {"epoll_pwait",	(void *)319}
}
#endif
  ;

int nr_syscall_names
#ifdef DO_UDB_INIT
= sizeof(syscall_names)/sizeof(ENTRY)
#endif
  ;

#endif  /*UDB_I386_H */
