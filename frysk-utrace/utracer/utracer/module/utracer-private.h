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

#ifndef UTRACER_PRIVATE_H
#define UTRACER_PRIVATE_H

#ifdef DO_INIT
#define DECL(v,i) v = i
#define DECLNI(v) v
#else
#define DECL(v,i) extern v
#define DECLNI(v) extern v
#endif

#ifdef DEBUG
#define DB_PRINTK printk
#else
#define DB_PRINTK(fmt, a...)
#endif

DECL (struct proc_dir_entry * de_utrace, NULL);
DECL (struct proc_dir_entry * de_utrace_control, NULL);

typedef struct _utraced_info_s {
  long utraced_pid;
  char * filename;		/* Name of binary as seen by procps */
  char * interp;		/* Name of the binary really executed. */
  long exec_quiesce;
  unsigned long * entry_bv;
  unsigned long * exit_bv;
  long bv_len;
  struct utrace_attached_engine * utraced_engine;
  struct _utraced_info_s * next;
  struct _utraced_info_s * prev;
} utraced_info_s;

#define setbit(bv, b) (bv[(b)/(8*sizeof(long))] |= (1<<((b)%(8*sizeof(long)))))
#define testbit(bv, b) (bv[(b)/(8*sizeof(long))] & (1<<((b)%(8*sizeof(long)))))
#define clearbit(bv, b) \
  (bv[(b)/(8*sizeof(long))] &= ~(1<<((b)%(8*sizeof(long)))))

typedef struct _utracing_info_s {
  long utracing_pid;
  char * client_pid_dir;
  struct proc_dir_entry * de_utracing_client;
  struct proc_dir_entry * de_utracing_cmd;
  struct proc_dir_entry * de_utracing_resp;
  struct utrace_attached_engine * utracing_engine;
  utraced_info_s * utraced_info;
  wait_queue_head_t ifr_wait;
  wait_queue_head_t ifw_wait;
  wait_queue_head_t ifq_wait;
  wait_queue_head_t ifx_wait;
  long response_ready;
  long write_in_progress;
  void * queued_data;
  long queued_data_length;
  struct _utracing_info_s * next;
  struct _utracing_info_s * prev;
  struct file_operations proc_dir_operations;
} utracing_info_s;

DECL (utracing_info_s * utracing_info_top, NULL);

#if 0
int control_file_write (struct file *file,
			const char *buffer,
			unsigned long count,
			void *data);

int control_file_read ( char *buffer,
			char **buffer_location,
			off_t offset,
			int buffer_length,
			int *eof,
			void *data);
#endif

utracing_info_s * lookup_utracing_info (long utracing_pid);

utraced_info_s *
lookup_utraced_info (utracing_info_s * utracing_info_entry, long utraced_pid);

utracing_info_s *
remove_utracing_info_entry (utracing_info_s * utracing_info_entry);

int
create_utracing_info_entry (long utracing_pid,
			    char * client_pid_dir,
			    struct proc_dir_entry * de_utracing_client,
			    struct proc_dir_entry * de_utracing_cmd,
			    struct proc_dir_entry * de_utracing_resp,
			    struct utrace_attached_engine * utracing_engine);

utraced_info_s *
remove_utraced_info_entry (utracing_info_s * utracing_info_entry,
			   utraced_info_s * utraced_info_entry);

int create_utraced_info_entry (utracing_info_s * utracing_info_entry,
			       long utraced_pid,
			       struct utrace_attached_engine * utraced_engine,
			       long exec_quiesce);

int if_file_read ( char *buffer,
		   char **buffer_location,
		   off_t offset,
		   int buffer_length,
		   int *eof,
		   void *data);

int if_file_write (struct file *file,
                   const char *buffer,
		   unsigned long count,
		   void *data);

struct task_struct * get_task (long utraced_pid);

struct utrace_attached_engine *
locate_engine (long utracing_pid, long utraced_pid);
void queue_response (utracing_info_s * utracing_info_found,
		     void * resp,   int resp_len,
		     void * extra,  int extra_len,
		     void * extra2, int extra2_len);

int handle_register (register_cmd_s * register_cmd);
int handle_unregister (register_cmd_s * register_cmd);

#endif /* UTRACER_PRIVATE_H */

