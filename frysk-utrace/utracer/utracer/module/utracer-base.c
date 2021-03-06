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

#include <linux/module.h>
#include <linux/moduleparam.h>
#include <linux/kernel.h>
#include <linux/init.h>
#include <linux/stat.h>
#include <linux/slab.h>
#include <linux/proc_fs.h>
#include <linux/utrace.h>
#include <asm/uaccess.h>
#include <linux/tracehook.h>
#include <asm/tracehook.h>
#include <asm/unistd.h>

#include "../include/utracer.h"
#define DO_INIT
#include "utracer-private.h"

MODULE_LICENSE("GPL");
MODULE_AUTHOR("Chris Moller");

/*
 * locate the task structure coresponding to the given pid
 *
 */
struct task_struct *
get_task (long utraced_pid)
{
  struct task_struct * task;
  
  rcu_read_lock();
  task = find_task_by_pid(utraced_pid);
  if (task) get_task_struct(task);
  rcu_read_unlock();
      
  return task;
}

/*
 * create a new traced-process entry and prepend it to the list of
 * traced-process entries in the specified tracing-process entry
 *
 */
int
create_utraced_info_entry (utracing_info_s * utracing_info_entry,
			   long utraced_pid,
			   struct utrace_attached_engine * utraced_engine,
			   long exec_quiesce)
{
  if (utracing_info_entry) {
    utraced_info_s  * utraced_info_new =
      kmalloc (sizeof(utraced_info_s), GFP_KERNEL);
    if (!utraced_info_new) return -ENOMEM;

    utraced_info_new->exec_quiesce = exec_quiesce;
    utraced_info_new->utraced_pid = utraced_pid;
    utraced_info_new->utraced_engine = utraced_engine;
    utraced_info_new->filename = NULL;
    utraced_info_new->interp = NULL;
#if defined (NR_syscalls) && (0 < NR_syscalls)
    {
      int nr_bits_per_long  = 8 * sizeof(long);
      int nr_longs =  (NR_syscalls + nr_bits_per_long)/nr_bits_per_long;
      utraced_info_new->bv_len = nr_longs * sizeof(long);
      utraced_info_new->entry_bv =
	kzalloc (nr_longs * sizeof(long),GFP_KERNEL);
      utraced_info_new->exit_bv =
	kzalloc (nr_longs * sizeof(long),GFP_KERNEL);
    }
#else
    utraced_info_new->entry_bv = NULL;
    utraced_info_new->exit_bv  = NULL;
#endif

    if (utracing_info_entry->utraced_info)
      (utracing_info_entry->utraced_info)->prev = utraced_info_new;
    utraced_info_new->next = utracing_info_entry->utraced_info;
    utracing_info_entry->utraced_info = utraced_info_new;
    utraced_info_new->prev = NULL;

    return 0;
  }
  else
    return -UTRACER_ETRACING;
}

/*
 * remove a traced-process entry.  if there's a utrace engine attached
 * to the corresponding process, detach it first.  if a tracing-process
 * entry is provided for context, unlink the traced-process entry from
 * the traced-process list in the specified tracing-process entry
 *
 */
utraced_info_s *
remove_utraced_info_entry (utracing_info_s * utracing_info_entry,
			   utraced_info_s * utraced_info_entry)
{
  utraced_info_s * next_ety = NULL;
  
  if (utraced_info_entry) {
    struct task_struct * task =
      get_task (utraced_info_entry->utraced_pid);
    if (task && utraced_info_entry->utraced_engine)
      utrace_detach (task, utraced_info_entry->utraced_engine);
    if (utracing_info_entry) {
      if (utraced_info_entry->next)
	(utraced_info_entry->next)->prev = utraced_info_entry->prev;
      if (utraced_info_entry->prev)
	(utraced_info_entry->prev)->next = utraced_info_entry->next;
      else
	utracing_info_entry->utraced_info = utraced_info_entry->next;
    }
    if (utraced_info_entry->entry_bv) kfree (utraced_info_entry->entry_bv);
    if (utraced_info_entry->exit_bv) kfree (utraced_info_entry->exit_bv);
    if (utraced_info_entry->filename) kfree (utraced_info_entry->filename);
    if (utraced_info_entry->interp) kfree (utraced_info_entry->interp);
    next_ety = utraced_info_entry->next;
    kfree(utraced_info_entry);
  }

  return next_ety;
}

/*
 * create a new tracing-process entry and prepend it to the list of
 * tracing-process entries
 *
 */
int
create_utracing_info_entry (long utracing_pid,
			    char * client_pid_dir,
			    struct proc_dir_entry * de_utracing_client,
			    struct proc_dir_entry * de_utracing_cmd,
			    struct proc_dir_entry * de_utracing_resp,
			    struct utrace_attached_engine * utracing_engine)
{
  int rc = 0;
  
  utracing_info_s * utracing_info_new =
    kmalloc (sizeof(utracing_info_s), GFP_KERNEL);
  if (!utracing_info_new) return -ENOMEM;

  if (utracing_info_new) {
    if (utracing_info_top) {
      utracing_info_top->prev = utracing_info_new;
      utracing_info_new->next = utracing_info_top;
    }
    else utracing_info_new->next = NULL;
    utracing_info_top = utracing_info_new;
    utracing_info_new->prev = NULL;
    
    utracing_info_new->utracing_pid		= utracing_pid;
    utracing_info_new->client_pid_dir		= client_pid_dir;
    utracing_info_new->de_utracing_client		= de_utracing_client;
    utracing_info_new->de_utracing_cmd		= de_utracing_cmd;
    utracing_info_new->de_utracing_resp		= de_utracing_resp;
    utracing_info_new->utracing_engine		= utracing_engine;
    utracing_info_new->utraced_info		= NULL;
    init_waitqueue_head (&(utracing_info_new->ifr_wait));
    init_waitqueue_head (&(utracing_info_new->ifw_wait));
    init_waitqueue_head (&(utracing_info_new->ifq_wait));
    init_waitqueue_head (&(utracing_info_new->ifx_wait));
    utracing_info_new->queued_data		= NULL;
    utracing_info_new->queued_data_length	= 0;
    utracing_info_new->response_ready		= 0;
    utracing_info_new->write_in_progress	= 0;
  }
  else rc = -ENOMEM;

  return rc;
}

/*
 * remove the specified tracing-process, making sure that any associated
 * /proc entry is removed, that all traced-process entries are closed, and
 * that everything once kmalloc()ed is kfree()ed.
 *
 */
utracing_info_s *
remove_utracing_info_entry (utracing_info_s * utracing_info_entry)
{
  utracing_info_s * utracing_info_next = NULL;
  if (utracing_info_entry) {
    utraced_info_s  * utraced_info_this;
    struct task_struct * task =
      get_task (utracing_info_entry->utracing_pid);

#if 0
    if (task) {
      wait_queue_t wait;

      init_waitqueue_entry(&wait, task);
      remove_wait_queue(&utracing_info_entry->ifr_wait, &wait);
      remove_wait_queue(&utracing_info_entry->ifw_wait, &wait);
      remove_wait_queue(&utracing_info_entry->ifq_wait, &wait);
    }
#endif
    
    if (task && utracing_info_entry->utracing_engine)
      utrace_detach (task, utracing_info_entry->utracing_engine);
    
    if (utracing_info_entry->de_utracing_client)
      remove_proc_entry (UTRACER_CMD_FN,
			 utracing_info_entry->de_utracing_client);
    
    if (utracing_info_entry->de_utracing_client)
      remove_proc_entry (UTRACER_RESP_FN,
			 utracing_info_entry->de_utracing_client);
    
    if (de_utrace && utracing_info_entry->client_pid_dir)
      remove_proc_entry (utracing_info_entry->client_pid_dir,
			 de_utrace);
    
    if (utracing_info_entry->client_pid_dir)
      kfree (utracing_info_entry->client_pid_dir);
    for (utraced_info_this = utracing_info_entry->utraced_info;
	 utraced_info_this;
	 utraced_info_this =
	   remove_utraced_info_entry (NULL, utraced_info_this));
    if (utracing_info_entry->next)
      (utracing_info_entry->next)->prev = utracing_info_entry->prev;
    if (utracing_info_entry->prev)
      (utracing_info_entry->prev)->next = utracing_info_entry->next;
    else
      utracing_info_top = utracing_info_entry->next;
    utracing_info_next = utracing_info_entry->next;
    kfree (utracing_info_entry);
  }

  return utracing_info_next;
}

/*
 * scan through the tracing-process list and find the one that matches
 * the specified pid
 *
 */
utracing_info_s *
lookup_utracing_info (long utracing_pid)
{
  utracing_info_s * utracing_info_found = NULL;
  utracing_info_s * utracing_info_this;

  for (utracing_info_this = utracing_info_top;
       utracing_info_this;
       utracing_info_this = utracing_info_this->next) {
    if (utracing_pid == utracing_info_this->utracing_pid) {
      utracing_info_found = utracing_info_this;
      break;
    }
  }

  return utracing_info_found;
}

/*
 * scan through the traced-process list of the specified tracing-process entry
 * and find the one that matches the specified pid
 *
 */
utraced_info_s *
lookup_utraced_info (utracing_info_s * utracing_info_entry, long utraced_pid)
{
  utraced_info_s * utraced_info_found = NULL;
  utraced_info_s * utraced_info_this;

  if (utracing_info_entry) {
    for (utraced_info_this = utracing_info_entry->utraced_info;
	 utraced_info_this;
	 utraced_info_this = utraced_info_this->next) {
      if (utraced_pid == utraced_info_this->utraced_pid) {
	utraced_info_found = utraced_info_this;
	break;
      }
    }
  }

  return utraced_info_found;
}

/*
 * find the tracing-process entry asssociated with the specified tracing pid,
 * then scan thought the associated list of traced-process entries for one
 * that corresponds to the specified traced-process pid.  return the
 * corresponding utrace engine
 *
 */
struct utrace_attached_engine *
locate_engine (long utracing_pid, long utraced_pid)
{
  utracing_info_s * utracing_info_found;
  struct utrace_attached_engine * engine;

  engine = NULL;
  utracing_info_found = lookup_utracing_info (utracing_pid);
  if (utracing_info_found) {
    utraced_info_s  * utraced_info_this;
    for (utraced_info_this = utracing_info_found->utraced_info;
	 utraced_info_this;
	 utraced_info_this = utraced_info_this->next) {
      if (utraced_info_this->utraced_pid == utraced_pid) {
	engine = utraced_info_this->utraced_engine;
	break;
      }
    }
  }

  return engine;
}

int
control_ioctl (struct inode * inode,
	       struct file * file,
	       unsigned int a1,
	       unsigned long a2)
{
  ctl_cmd_u ctl_cmd;
  int rc = 0;

  if (copy_from_user(&ctl_cmd, (void *)a2, a1))
    return -EFAULT;

  switch (ctl_cmd.cmd) {
  case CTL_CMD_REGISTER:
    DB_PRINTK (KERN_ALERT "CTL_CMD_REGISTER--ioctl\n");
    rc = handle_register (&ctl_cmd.register_cmd);
    break;
  case CTL_CMD_UNREGISTER:
    DB_PRINTK (KERN_ALERT "CTL_CMD_UNREGISTER--ioctl\n");
    rc = handle_unregister (&ctl_cmd.register_cmd);
    break;
  }
  
  return rc;
}

static int __init utracer_init(void)
{
  // create /proc/utrace
  de_utrace = proc_mkdir(UTRACER_BASE_DIR, NULL);

  if (!de_utrace) {
    remove_proc_entry(UTRACER_BASE_DIR, &proc_root);
    return -ENOMEM;
  }

  // create /proc/utrace/control
  DB_PRINTK ("module opening %s with ioctl to control_ioctl\n",
	     UTRACER_CONTROL_FN);
  de_utrace_control = create_proc_entry(UTRACER_CONTROL_FN,
                                        S_IFREG | 0666, de_utrace);
  DB_PRINTK ("de_utrace_control = %p\n", de_utrace_control);

  if (!de_utrace_control) {
    remove_proc_entry(UTRACER_CONTROL_FN, de_utrace);
    return -ENOMEM;
  }

  {
    // struct file_operations from include/linux/fs.h
    struct file_operations * proc_dir_operations =
      kmalloc (sizeof(struct file_operations), GFP_KERNEL);
    memcpy (proc_dir_operations, de_utrace_control->proc_fops,
	    sizeof(struct file_operations)); 
    de_utrace_control->proc_fops = proc_dir_operations;
    proc_dir_operations->ioctl = control_ioctl;
  }
#if 0
  de_utrace_control->write_proc = control_file_write;
  de_utrace_control->read_proc  = control_file_read;
#else
  de_utrace_control->write_proc = NULL;
  de_utrace_control->read_proc  = NULL;
#endif

  return 0;
}

static void __exit utracer_exit (void)
{
  utracing_info_s * utracing_info_entry;

  for (utracing_info_entry = utracing_info_top;
       utracing_info_entry;
       utracing_info_entry = remove_utracing_info_entry (utracing_info_entry));

  remove_proc_entry (UTRACER_CONTROL_FN, de_utrace);
  if (de_utrace_control && de_utrace_control->proc_fops)
    kfree (de_utrace_control->proc_fops);
  remove_proc_entry (UTRACER_BASE_DIR, &proc_root);
}

module_init (utracer_init);
module_exit (utracer_exit);
