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
#ifndef UDB_H
#define UDB_H

#ifdef DO_UDB_INIT
#define DECL(v,i) v = i
#else
#define DECL(v,i) v
#endif

extern int  exec_cmd(char * iline);
extern void text_ui();
extern void text_ui_init();
extern void utace_attach_if(long pid);
extern void utrace_detach_if (long pid);
extern void utrace_readreg_if (long pid, int regset, int reg);
extern void set_prompt();
extern void register_utracer(pid_t pid);
extern int parse_regspec (char ** saveptr, long * pid_p,
			  long * regset_p, long *reg_p);

pid_t udb_pid;

DECL (char * prompt, NULL);

DECL (long current_pid, -1);

DECL (int ctl_file_fd, -1);
DECL (int utracer_cmd_file_fd, -1);
DECL (int utracer_resp_file_fd, -1);

#ifdef ENABLE_MODULE_OPS
DECL (char * module_name, NULL);
#endif

DECL (char ** cl_cmds, NULL);
DECL (int cl_cmds_next, 0);
DECL (int cl_cmds_max, 0);
#define CMDS_INCR	4

DECL (char * ggg, NULL);

#define INVALID_REG	-128
#define SYSCALL_INVALID	-5

#endif  /* UDB_H */
