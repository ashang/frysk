/*
 *  utracer.c - implement a tracing engine based on utrace in a module
 */
#include <linux/module.h>
#include <linux/moduleparam.h>
#include <linux/kernel.h>
#include <linux/utrace.h>
#include <linux/fs.h>
#include <linux/string.h>
#include <linux/binfmts.h>
#include <asm/uaccess.h>
#include <linux/tracehook.h>
#include <asm/tracehook.h>
#include <asm/unistd.h>

#include "../include/utracer.h"
#include "utracer-private.h"

MODULE_LICENSE("GPL");
MODULE_AUTHOR("Chris Moller");


void
queue_response (utracing_info_s * utracing_info_found,
		void * resp,   int resp_len,
		void * extra,  int extra_len,
		void * extra2, int extra2_len)
{
  int wrc;
  
  //DB_PRINTK (KERN_ALERT "queue_response(%ld)\n", ((if_resp_u *)resp)->type);
  // wrc == 0			==> timed out
  // wrc == -ERESTARTSYS	==> signal intr
  // otherwise			==> ok
  wrc = wait_event_interruptible_timeout (utracing_info_found->ifq_wait,
			      (0 >= utracing_info_found->queued_data_length),
				      2 * HZ);
  
  //DB_PRINTK (KERN_ALERT "queue_response wrc = %s\n",
  //	     (0 == wrc) ? "timed out" :
  //	     ((-ERESTARTSYS == wrc) ? "intr" : "okay"));
  
  // might return 0 length if wait ended by som random interrupt
  if (0 >= utracing_info_found->queued_data_length) {
    utracing_info_found->queued_data_length =
      resp_len + extra_len + extra2_len;
    utracing_info_found->queued_data =
      kmalloc (resp_len + extra_len + extra2_len, GFP_KERNEL);
    memcpy (utracing_info_found->queued_data, resp, resp_len);
    if (extra)
      memcpy (utracing_info_found->queued_data + resp_len, extra, extra_len);
    if (extra2)
      memcpy (utracing_info_found->queued_data + resp_len + extra_len,
	      extra2, extra2_len);
    wake_up (&(utracing_info_found->ifr_wait));
  }
}

static int
allow_access_process_vm (struct utrace_attached_engine *engine,
			 struct task_struct *target,
			 struct task_struct *caller)
{
  printk (KERN_ALERT "allow_access_process_vm\n");
  return 1;
}

static u32
report_quiesce (struct utrace_attached_engine *engine,
		struct task_struct *tsk)
{
  utracing_info_s * utracing_info_found =
    (void *)engine->data;

  if (utracing_info_found) {
    quiesce_resp_s quiesce_resp = {IF_RESP_QUIESCE_DATA,
				   tsk->pid};
    queue_response (utracing_info_found,
		    &quiesce_resp, sizeof(quiesce_resp),
		    NULL, 0,
		    NULL, 0);
			       
  }
  return UTRACE_ACTION_RESUME;
}

extern int cfr_data_ready;
extern int attach_cmd_fcn (long utracing_pid, long utraced_pid,
			   long quiesce, long exec_quiesce);

static u32
report_clone (struct utrace_attached_engine *engine,
	      struct task_struct *parent,
	      unsigned long clone_flags,
	      struct task_struct *child)
{
  utracing_info_s * utracing_info_found =
    (void *)engine->data;

  //fixme -- autoattach?
  //  printk(KERN_ALERT "reporting clone\n");
  
  if (utracing_info_found) {
    int rc;
    
    rc = attach_cmd_fcn (utracing_info_found->utracing_pid,
			 child->pid, 0, 0);
    {
      clone_resp_s clone_resp = {IF_RESP_CLONE_DATA,
				 parent->pid, child->pid, (long)rc};

      // fixme -- maybe do something with quiesce
      queue_response (utracing_info_found,
		      &clone_resp, sizeof(clone_resp),
		      NULL, 0,
		      NULL, 0);
    }
  }
  return UTRACE_ACTION_RESUME;
}

static u32
report_vfork_done (struct utrace_attached_engine *engine,
		   struct task_struct *parent, pid_t child_pid)
{
  printk(KERN_ALERT "reporting vfork_done, child pid = %d\n", child_pid);
  return UTRACE_ACTION_RESUME;
}


static u32
report_exec (struct utrace_attached_engine *engine,
	     struct task_struct * tsk,
	     const struct linux_binprm *bprm,
	     struct pt_regs *regs)
{
  utracing_info_s * utracing_info_found = (void *)engine->data;
  u32 rc = UTRACE_ACTION_RESUME;

  if (utracing_info_found) {
    utraced_info_s * utraced_info_found =
      lookup_utraced_info (utracing_info_found, (long)tsk->pid);
    if (utraced_info_found) {
      unsigned long flags;
      long str_len = 
	((bprm->filename) ? (1 + strlen (bprm->filename)) : 0) +
	((bprm->interp)   ? (1 + strlen (bprm->interp))   : 0);
      exec_resp_s exec_resp = {IF_RESP_EXEC_DATA,
			       (long)tsk->pid, str_len };
      queue_response (utracing_info_found,
		      &exec_resp, sizeof(exec_resp),
		      bprm->filename, 1 + strlen(bprm->filename),
		      bprm->interp, 1 + strlen (bprm->interp));

      utraced_info_found->filename = kstrdup (bprm->filename, GFP_KERNEL);
      utraced_info_found->interp   = kstrdup (bprm->interp, GFP_KERNEL);
      
      if (utraced_info_found->exec_quiesce) {
	flags = engine->flags |  UTRACE_ACTION_QUIESCE;
	rc = UTRACE_ACTION_QUIESCE;
      }
      else {
	flags = engine->flags &  ~UTRACE_ACTION_QUIESCE;
	rc = UTRACE_ACTION_RESUME;
      }
      utrace_set_flags(tsk, engine, flags);
    }
  }
  return rc;
}

static u32
report_exit (struct utrace_attached_engine *engine,
	     struct task_struct * tsk,
	     long orig_code, long *code)
{
  utracing_info_s * utracing_info_found = (void *)engine->data;

  if (utracing_info_found) {
    exit_resp_s exit_resp = {IF_RESP_EXIT_DATA,
			     (long)tsk->pid, orig_code };
    queue_response (utracing_info_found,
		    &exit_resp, sizeof(exit_resp),
		    NULL, 0,
		    NULL, 0);
  }
  return UTRACE_ACTION_RESUME;
}

static u32
report_death (struct utrace_attached_engine *engine,
	      struct task_struct *tsk)
{
  utracing_info_s * utracing_info_found = (void *)engine->data;

  if (utracing_info_found) {
    death_resp_s death_resp = {IF_RESP_DEATH_DATA,
			       (long)tsk->pid };
    queue_response (utracing_info_found,
		    &death_resp, sizeof(death_resp),
		    NULL, 0,
		    NULL, 0);
    remove_utraced_info_entry (utracing_info_found,
			       lookup_utraced_info (utracing_info_found,
						    (long)tsk->pid));
  }
  return UTRACE_ACTION_RESUME;
}

static u32
report_signal (struct utrace_attached_engine *engine,
	       struct task_struct *tsk,
	       struct pt_regs *regs,
	       u32 action,
	       siginfo_t *info,
	       const struct k_sigaction *orig_ka,
	       struct k_sigaction *return_ka)
{
  utracing_info_s * utracing_info_found = (void *)engine->data;

  if (utracing_info_found) {
    signal_resp_s signal_resp = {IF_RESP_SIGNAL_DATA,
				 (long)tsk->pid,
				 (long)info->si_signo};
    queue_response (utracing_info_found,
		    &signal_resp, sizeof(signal_resp),
		    NULL, 0,
		    NULL, 0);
  }
  return UTRACE_ACTION_RESUME;
}

static u32
report_syscall (struct utrace_attached_engine * engine,
		struct task_struct * tsk,
		struct pt_regs * regs,
		long type)
{
  utracing_info_s * utracing_info_found = (void *)engine->data;

  if (utracing_info_found) {
    utraced_info_s * utraced_info_found =
      lookup_utraced_info (utracing_info_found, (long)tsk->pid);
    if (utraced_info_found) {
      unsigned long * bv =
	(IF_RESP_SYSCALL_EXIT_DATA == type) ?
	utraced_info_found->exit_bv :
	utraced_info_found->entry_bv;
      long syscall_nr = regs->orig_eax;	// fixme -- arch dependent
      if ((0 <= syscall_nr) && (syscall_nr < NR_syscalls) &&
	  testbit (bv, syscall_nr)) {
	syscall_resp_s syscall_resp;

	syscall_resp.type = type;
	syscall_resp.utraced_pid = (long)tsk->pid;
	syscall_resp.data_length = sizeof (struct pt_regs);
	queue_response (utracing_info_found,
			&syscall_resp, sizeof(syscall_resp),
			regs, sizeof (struct pt_regs), NULL, 0);
      }
    }
  }
  
  return UTRACE_ACTION_RESUME;
}

static u32
report_syscall_exit (struct utrace_attached_engine *engine,
		     struct task_struct *tsk,
		     struct pt_regs *regs)
{
  return report_syscall (engine, tsk, regs, IF_RESP_SYSCALL_EXIT_DATA);
}

static u32
report_syscall_entry (struct utrace_attached_engine *engine,
		      struct task_struct *tsk,
		      struct pt_regs *regs)
{
  return report_syscall (engine, tsk, regs, IF_RESP_SYSCALL_ENTRY_DATA);
}

static void
report_reap (struct utrace_attached_engine *engine,
	     struct task_struct *tsk)
{
  printk(KERN_ALERT "reporting reap\n");
}


const struct utrace_engine_ops utraced_utrace_ops = {
  .report_syscall_entry	= report_syscall_entry,
  .report_syscall_exit	= report_syscall_exit,
  .report_exec		= report_exec,
  .report_jctl		= NULL,
  .report_signal	= report_signal,
  .report_vfork_done	= report_vfork_done,
  .report_clone		= report_clone,
  .report_exit		= report_exit,
  .report_death		= report_death,
  .report_reap		= report_reap,
  .report_quiesce	= report_quiesce,
  .unsafe_exec		= NULL,
  .tracer_task		= NULL,
  .allow_access_process_vm = allow_access_process_vm,
};


/*
 * this is more or less a no-op now but could be used in the future
 * to pass bulk data (e.g., memory writes).
 */

int
if_file_write (struct file *file,
	       const char *buffer,
	       unsigned long count,
	       void * data)
{
  if_cmd_u if_cmd;
  int wrc;
  int rc = count;
  utracing_info_s * utracing_info_found  = (utracing_info_s *)data;

  // wrc == 0			==> condition true
  // wrc == -ERESTARTSYS	==> signal intr
  wrc = wait_event_interruptible (utracing_info_found->ifw_wait,
			  (0 == utracing_info_found->write_in_progress));

  //  DB_PRINTK (KERN_ALERT "if_file_write wrc = %s\n",
  //	     (0 == wrc) ? "okay" : "intr");
  // might return 0 length if wait ended by som random interrupt
  if (0 == utracing_info_found->write_in_progress) {
    utracing_info_found->write_in_progress = 1;
  
    if (count > sizeof(if_cmd_u)) return -ENOSPC;

    if (copy_from_user(&if_cmd, buffer, count) ) return -EFAULT;

    switch (if_cmd.cmd) {
    case IF_CMD_NULL:
      DB_PRINTK (KERN_ALERT "IF_CMD_NULL\n");
      break;
    }

    utracing_info_found->write_in_progress = 0;
  }

  
  return rc;
}

int
if_file_read ( char *buffer,
               char **buffer_location,
               off_t offset,
               int buffer_length,
               int *eof,
               void *data)
{
  utracing_info_s * utracing_info_found  = (utracing_info_s *)data;
  
  if (utracing_info_found) {
    int wrc = 0;
    int rc = 0;

    if (1 != utracing_info_found->response_ready) {
      utracing_info_found->response_ready = 1;
      wake_up (&(utracing_info_found->ifx_wait));
    }

    if (0 == offset) {
      // wrc == 0		==> condition true
      // wrc == -ERESTARTSYS	==> signal intr
      wrc = wait_event_interruptible (utracing_info_found->ifr_wait,
			      (0 < utracing_info_found->queued_data_length));
      //      DB_PRINTK (KERN_ALERT "if_file_read wrc = %s\n",
      //		 (0 == wrc) ? "okay" : "intr");
    }
    
    // might return 0 length if wait ended by som random interrupt
    if (utracing_info_found->queued_data &&
	(0 < utracing_info_found->queued_data_length)) {
      rc = utracing_info_found->queued_data_length;
      if (rc > buffer_length) rc = buffer_length;
      memcpy (buffer, utracing_info_found->queued_data + offset, rc);
      utracing_info_found->queued_data_length -= rc;
      if (0 >= utracing_info_found->queued_data_length) {
	kfree (utracing_info_found->queued_data);
	utracing_info_found->queued_data = NULL;
	wake_up (&(utracing_info_found->ifq_wait));
      }
    }

    *buffer_location = buffer;
    return rc;
  }
  else return -UTRACER_ETRACING;
}
