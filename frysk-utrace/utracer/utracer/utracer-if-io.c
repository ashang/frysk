/*
 *  utracer.c - implement a tracing engine based on utrace in a module
 */
#include <linux/module.h>
#include <linux/moduleparam.h>
#include <linux/kernel.h>
#include <linux/init.h>
#include <linux/stat.h>
#include <linux/slab.h>
#include <linux/proc_fs.h>
#include <linux/utrace.h>
#include <linux/binfmts.h>
#include <asm/uaccess.h>
#include <linux/tracehook.h>
#include <asm-i386/tracehook.h>

#include "utracer.h"
#include "utracer-private.h"

MODULE_LICENSE("GPL");
MODULE_AUTHOR("Chris Moller");

static void
queue_response (utracing_info_s * utracing_info_found,
		void * resp,  int resp_len,
		void * extra, int extra_len)
{
  wait_event (utracing_info_found->ifw_wait,
	      (0 >= utracing_info_found->queued_data_length));
    
  utracing_info_found->queued_data_length = resp_len + extra_len;
  utracing_info_found->queued_data =
    kmalloc (resp_len + extra_len, GFP_KERNEL);
  memcpy (utracing_info_found->queued_data, resp, resp_len);
  if (extra)
    memcpy (utracing_info_found->queued_data + resp_len, extra, extra_len);
  wake_up (&(utracing_info_found->ifr_wait));
}

static u32
report_quiesce (struct utrace_attached_engine *engine,
		struct task_struct *tsk)
{
  //  printk(KERN_ALERT "reporting quiesce\n");
  return UTRACE_ACTION_RESUME;
}

extern int cfr_data_ready;
static u32
report_clone (struct utrace_attached_engine *engine,
	      struct task_struct *parent,
	      unsigned long clone_flags,
	      struct task_struct *child)
{
  utracing_info_s * utracing_info_found =
    (void *)engine->data;

  //fixme -- autoattach?
  printk(KERN_ALERT "reporting clone\n");
  
  if (utracing_info_found) {
    clone_resp_s clone_resp = {IF_RESP_CLONE_DATA,
			       parent->pid, child->pid};
    queue_response (utracing_info_found,
		    &clone_resp, sizeof(clone_resp), NULL, 0);
			       
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
  //  printk(KERN_ALERT "reporting exec \"%s\" \"%s\"\n",
  //  	 (bprm->filename) ? bprm->filename : "unk",
  //  	 (bprm->interp) ? bprm->interp : "unk");
  utracing_info_s * utracing_info_found = (void *)engine->data;
  u32 rc = UTRACE_ACTION_RESUME;
  
  if (utracing_info_found) {
    utraced_info_s * utraced_info_found =
      lookup_utraced_info (utracing_info_found, (long)tsk->pid);
    if (utraced_info_found) {
      unsigned long flags;
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
		    &exit_resp, sizeof(exit_resp), NULL, 0);
  }
  return UTRACE_ACTION_RESUME;
}

static u32
report_death (struct utrace_attached_engine *engine,
	      struct task_struct *tsk)
{
  utracing_info_s * utracing_info_found = (void *)engine->data;

  // fixme -- utrace_detach?  remove info?
  if (utracing_info_found) {
    death_resp_s death_resp = {IF_RESP_DEATH_DATA,
			       (long)tsk->pid };
    queue_response (utracing_info_found,
		    &death_resp, sizeof(death_resp), NULL, 0);
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
		    &signal_resp, sizeof(signal_resp), NULL, 0);
  }
  return UTRACE_ACTION_RESUME;
}

static const struct utrace_engine_ops utraced_utrace_ops = {
  .report_syscall_entry	= NULL,
  .report_syscall_exit	= NULL,
  .report_exec		= report_exec,
  .report_jctl		= NULL,
  .report_signal	= report_signal,
  .report_vfork_done	= report_vfork_done,
  .report_clone		= report_clone,
  .report_exit		= report_exit,
  .report_death		= report_death,
  .report_reap		= NULL,
  .report_quiesce	= report_quiesce,
  .unsafe_exec		= NULL,
  .tracer_task		= NULL,
  .allow_access_process_vm = NULL,
};

static int
attach_cmd_fcn (long utracing_pid, long utraced_pid,
		long quiesce, long exec_quiesce)
{
  int rc;
  struct task_struct * task = get_task (utraced_pid);
  
  utracing_info_s * utracing_info_found =
    lookup_utracing_info (utracing_pid);
    
  if (utracing_info_found) {
    if (task) {
      struct utrace_attached_engine * engine;
      engine = utrace_attach (task,
			      UTRACE_ATTACH_CREATE |
			      UTRACE_ATTACH_EXCLUSIVE |
			      UTRACE_ATTACH_MATCH_OPS,
			      &utraced_utrace_ops,
			      0UL);  //fixme -- maybe use?
      if (!IS_ERR (engine)) {
	unsigned long flags =
	  UTRACE_EVENT (EXEC)	    |
	  UTRACE_EVENT (SIGNAL)	    |
	  UTRACE_EVENT (VFORK_DONE) |
	  UTRACE_EVENT (CLONE)	    |
	  UTRACE_EVENT (EXIT)	    |
	  UTRACE_EVENT (QUIESCE)    |
	  UTRACE_EVENT (DEATH);

	if (quiesce) flags |= UTRACE_ACTION_QUIESCE;
	
	//fixme -- do something with rc?
	rc = utrace_set_flags (task,engine, flags);

	engine->data = (unsigned long)utracing_info_found;
	rc = create_utraced_info_entry (utracing_info_found,
					utraced_pid,
					engine,
					exec_quiesce);
	if (0 != rc) utrace_detach(task, engine);
      }
      else rc = -UTRACER_EENGINE;
    }
    else rc = -ESRCH;
    {
      attach_resp_s attach_resp = {IF_RESP_ATTACH_DATA,
				   utraced_pid, (0 == rc) ? 1 : 0 };
      queue_response (utracing_info_found,
		      &attach_resp, sizeof(attach_resp), NULL, 0);
    }
  }
  else rc = -UTRACER_ETRACING;

  return rc;
}

int
if_file_write (struct file *file,
                    const char *buffer,
                    unsigned long count,
                    void *data)
{
  if_cmd_u if_cmd;
  if_cmd_e if_cmd_cmd;

  if (count > sizeof(if_cmd_u)) return -ENOSPC;

  if (copy_from_user(&if_cmd, buffer, count) ) return -EFAULT;

  if_cmd_cmd = (if_cmd_e)(if_cmd.cmd);

  switch (if_cmd_cmd) {
  case IF_CMD_NULL:
    break;
  case IF_CMD_RUN:
  case IF_CMD_QUIESCE:
    {
      struct task_struct * task;

      run_cmd_s run_cmd = if_cmd.run_cmd;

      task = get_task (run_cmd.utraced_pid);

      if (task) {
        struct utrace_attached_engine * engine;

        engine =
          locate_engine (run_cmd.utracing_pid, run_cmd.utraced_pid);
        if (engine) {
          unsigned long flags =
            (IF_CMD_RUN == if_cmd_cmd) ?
	    (engine->flags & ~UTRACE_ACTION_QUIESCE) :
	    (engine->flags |  UTRACE_ACTION_QUIESCE);
          utrace_set_flags(task,engine, flags);
          return count;
        }
        else return -UTRACER_EENGINE;
      }
      else return -ESRCH;
    }
    break;
  case IF_CMD_SWITCHPID:
    {
      utracing_info_s * utracing_info_found;
      
      switchpid_cmd_s switchpid_cmd = if_cmd.switchpid_cmd;

      utracing_info_found =
	lookup_utracing_info (switchpid_cmd.utracing_pid);

      if (utracing_info_found) {
	struct task_struct * task;
	switchpid_resp_s switchpid_resp;
	task = get_task (switchpid_cmd.utraced_pid);

	switchpid_resp.type           = IF_RESP_SWITCHPID_DATA;
	switchpid_resp.utraced_pid    = switchpid_cmd.utraced_pid;
	switchpid_resp.okay = task ? 1 : 0;
      
	queue_response (utracing_info_found,
			&switchpid_resp,
			sizeof(switchpid_resp),
			NULL, 0);
      
	return count;
      }
      else return -UTRACER_ETRACING;
    }
    break;
  case IF_CMD_ATTACH:
    {
      attach_cmd_s attach_cmd = if_cmd.attach_cmd;
      int rc =
	attach_cmd_fcn (attach_cmd.utracing_pid,
			attach_cmd.utraced_pid,
			attach_cmd.quiesce,
			attach_cmd.exec_quiesce);
      if (0 == rc) rc = count;
      return rc;
    }
    break;
  case IF_CMD_DETACH:
    {
      struct task_struct * task;

      // fixme fix info lists
      attach_cmd_s attach_cmd = if_cmd.attach_cmd;

      task = get_task (attach_cmd.utraced_pid);
      if (task) {
        struct utrace_attached_engine * engine;

        engine =
          locate_engine (attach_cmd.utracing_pid, attach_cmd.utraced_pid);
        if (engine) {
	  utracing_info_s * utracing_info_found =
	    lookup_utracing_info (attach_cmd.utracing_pid);
	  if (utracing_info_found) {
	    utraced_info_s * utraced_info_found =
	      lookup_utraced_info (utracing_info_found,
				   attach_cmd.utraced_pid);
	    if (utraced_info_found) {
	      remove_utraced_info_entry (utracing_info_found,
					 utraced_info_found);
	      return count;
	    }
	    else return -UTRACER_ETRACED;
	  }
	  else return -UTRACER_ETRACING;
        }
        else return -UTRACER_EENGINE;
      }
      else return -ESRCH;
    }
    break;
  case IF_CMD_LIST_PIDS:
    {
      int rc;
      utracing_info_s * utracing_info_found;

      listpids_cmd_s listpids_cmd = if_cmd.listpids_cmd;
      
      utracing_info_found =
	lookup_utracing_info (listpids_cmd.utracing_pid);

      if (utracing_info_found) {
	utraced_info_s * utraced_info;
	pids_resp_s pids_resp;
	long * pids_list = NULL;
	int i;
	
	pids_resp.type		= IF_RESP_PIDS_DATA;
	for ( pids_resp.nr_pids	= 0,
		utraced_info = utracing_info_found->utraced_info;
	      utraced_info;
	      utraced_info = utraced_info->next) pids_resp.nr_pids++;
	if (0 < pids_resp.nr_pids) {
	  pids_list = kmalloc (pids_resp.nr_pids * sizeof(long), GFP_KERNEL);
	  for ( i = 0, utraced_info = utracing_info_found->utraced_info;
		utraced_info;
		i++, utraced_info = utraced_info->next)
	    pids_list[i] = utraced_info->utraced_pid;
	}
	//	printk (KERN_ALERT "listpids\n");
	queue_response (utracing_info_found,
			&pids_resp, sizeof(pids_resp),
			pids_list, pids_resp.nr_pids * sizeof(long));
	if (pids_list) kfree (pids_list);
	rc = count;
      }
      else rc = -UTRACER_ETRACING;

      return rc;
    }
    break;
  case IF_CMD_SET_REG:
    return count;
    break;
  case IF_CMD_READ_REG:
    {
      struct task_struct * task;

      readreg_cmd_s readreg_cmd = if_cmd.readreg_cmd;
      
      task = get_task (readreg_cmd.utraced_pid);
      if (task) {
        struct utrace_attached_engine * engine;

        engine =
          locate_engine (readreg_cmd.utracing_pid, readreg_cmd.utraced_pid);
        if (engine) {
	  const struct utrace_regset * regset;
          regset = utrace_regset(task, engine,
                                 utrace_native_view(task),
                                 readreg_cmd.regset);

	  if (unlikely(regset == NULL))
	    return -EIO;

          if (-1 ==  readreg_cmd.which) {
            utracing_info_s * utracing_info_found;
            readreg_resp_s readreg_resp;
            readreg_resp.type           = IF_RESP_REG_DATA;
            readreg_resp.utraced_pid    = readreg_cmd.utraced_pid;
            readreg_resp.regset         = readreg_cmd.regset;
            readreg_resp.which          = -1;
            readreg_resp.byte_count     = regset->size;
            readreg_resp.reg_count      = regset->n;
	    readreg_resp.data		= NULL;

            utracing_info_found =
              lookup_utracing_info (readreg_cmd.utracing_pid);

            if (utracing_info_found) {
	      int i;
	      void * rvp;
	      void * reg_vals =
		kmalloc (regset->size * regset->n, GFP_KERNEL);
	      
	      for (i = 0, rvp = reg_vals;
		   i < regset->n;
		   i++, rvp += regset->size) {
		int rc;

		rc = regset->get(task,
				 regset,
				 i<<2,		// pos
				 regset->size,	// count
				 rvp,		// kbuf
				 NULL);		// ubuf
	      }
	      queue_response (utracing_info_found,
			      &readreg_resp,
			      sizeof(readreg_resp),
			      reg_vals,
			      regset->size * regset->n);
	      if (reg_vals) kfree (reg_vals);
            }
            else return -UTRACER_ETRACING;
          }
          else if ((0 <=  readreg_cmd.which) &&
                   (readreg_cmd.which < regset->n)) {
            utracing_info_s * utracing_info_found;
            readreg_resp_s readreg_resp;
            readreg_resp.type           = IF_RESP_REG_DATA;
            readreg_resp.utraced_pid    = readreg_cmd.utraced_pid;
            readreg_resp.regset         = readreg_cmd.regset;
            readreg_resp.which          = readreg_cmd.which;
            readreg_resp.byte_count     = regset->size;
            readreg_resp.reg_count      = 1;

            utracing_info_found =
              lookup_utracing_info (readreg_cmd.utracing_pid);

            if (utracing_info_found) {
              int rc;
              rc = regset->get(task,
                               regset,
                               readreg_cmd.which << 2,  // fixme pos
                               regset->size,		// count
                               &readreg_resp.data,      // kbuf
                               NULL);   // ubuf
	      queue_response (utracing_info_found,
			      &readreg_resp,
			      sizeof(readreg_resp), NULL, 0);
            }
            else return -UTRACER_ETRACING;
          }
          else return -UTRACER_EREG;
        }
        else return -UTRACER_EENGINE;
      }
      else return -ESRCH;
    }
    break;
  }

  return count;
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
    int rc = 0;

    if (0 == offset)
      wait_event_interruptible (utracing_info_found->ifr_wait,
				(0 < utracing_info_found->queued_data_length));

    if (utracing_info_found->queued_data &&
	(0 < utracing_info_found->queued_data_length)) {
      rc = utracing_info_found->queued_data_length;
      if (rc > buffer_length) rc = buffer_length;
      memcpy (buffer, utracing_info_found->queued_data + offset, rc);
      utracing_info_found->queued_data_length -= rc;
      if (0 >= utracing_info_found->queued_data_length) {
	kfree (utracing_info_found->queued_data);
	utracing_info_found->queued_data = NULL;
	wake_up (&(utracing_info_found->ifw_wait));
      }
    }
#if 0
    if (0 >= utracing_info_found->queued_data_length) {
      //      printk (KERN_ALERT "sending eof\n");
      *eof = 1;
    }
#endif

    *buffer_location = buffer;
    return rc;
  }

  else return -UTRACER_ETRACING;
}
