/*
 *  utracer.c - implement a tracing engine based on utrace in a module
 */
#include <linux/module.h>
#include <linux/moduleparam.h>
#include <linux/kernel.h>
#include <linux/proc_fs.h>
#include <linux/utrace.h>
#include <linux/binfmts.h>
#include <asm/uaccess.h>

#include "../include/utracer.h"
#include "utracer-private.h"

MODULE_LICENSE("GPL");
MODULE_AUTHOR("Chris Moller");

extern int utracer_ioctl (struct inode * inode,
			  struct file * file,
			  unsigned int a1,
			  unsigned long a2);

static u32
client_report_exec (struct utrace_attached_engine *engine,
	     struct task_struct *tsk,
	     const struct linux_binprm *bprm,
	     struct pt_regs *regs)
{
  printk(KERN_ALERT "reporting exec \"%s\" \"%s\"\n",
  	 (bprm->filename) ? bprm->filename : "unk",
  	 (bprm->interp) ? bprm->interp : "unk");
  return UTRACE_ACTION_RESUME;
}


static u32
client_report_exit (struct utrace_attached_engine *engine,
	     struct task_struct *tsk,
	     long orig_code, long *code)
{
  utracing_info_s * utracing_info_found = lookup_utracing_info (tsk->pid);
      
  if (utracing_info_found)
    remove_utracing_info_entry (utracing_info_found);
  
  return UTRACE_ACTION_RESUME;			// utracing_info list
}

static u32
client_report_death (struct utrace_attached_engine *engine,
	      struct task_struct *tsk)
{
  utracing_info_s * utracing_info_found = lookup_utracing_info (tsk->pid);
  struct proc_dir_entry * de = utracing_info_found->de_utracing_client;
  printk(KERN_ALERT "client reporting death count = %d\n",
	 de ? atomic_read(&de->count) : -999);

  return UTRACE_ACTION_RESUME;
}

static const struct utrace_engine_ops utracing_utrace_ops = {
  .report_syscall_entry	= NULL,
  .report_syscall_exit	= NULL,
  .report_exec		= client_report_exec,
  .report_jctl		= NULL,
  .report_signal	= NULL,
  .report_vfork_done	= NULL,
  .report_clone		= NULL,
  .report_exit		= client_report_exit,
  .report_death		= client_report_death,
  .report_reap		= NULL,
  .report_quiesce	= NULL,
  .unsafe_exec		= NULL,
  .tracer_task		= NULL,
  .allow_access_process_vm = NULL,
};

static int
cf_file_read ( char *buffer,
               char **buffer_location,
               off_t offset,
               int buffer_length,
               int *eof,
               void *data)
{
  utracing_info_s * utracing_info_found  = (utracing_info_s *)data;
  
  if (utracing_info_found) {
    int wrc;
    // wrc == 0		==> condition true
    // wrc == -ERESTARTSYS	==> signal intr
    wrc = wait_event_interruptible (utracing_info_found->ifr_wait,
			(1 == utracing_info_found->response_ready));
    memset (buffer, 1, buffer_length);
    *buffer_location = buffer;
    return buffer_length;
  }
  else return -UTRACER_ETRACING;
}


int
handle_register (register_cmd_s * register_cmd)
{
  int rc = 0;
  long client_pid = register_cmd->client_pid;
  utracing_info_s * utracing_info_found =
    lookup_utracing_info (client_pid);

  if (!utracing_info_found) {  // if non-null, entry already exists
    struct proc_dir_entry * de_utracing_client;
    struct proc_dir_entry * de_utracing_cmd;
    struct proc_dir_entry * de_utracing_resp;
    char * client_pid_dir =
      kasprintf(GFP_KERNEL, "%ld", client_pid);
    
    if (client_pid_dir) {
      de_utracing_client = proc_mkdir (client_pid_dir, de_utrace);
      if (de_utracing_client) {
	de_utracing_cmd = create_proc_entry(UTRACER_CMD_FN,
						S_IFREG | 0666,
						de_utracing_client);
	if (de_utracing_cmd) {
	  de_utracing_cmd->write_proc = if_file_write;
	  de_utracing_cmd->read_proc = cf_file_read;
	
	  de_utracing_resp = create_proc_entry(UTRACER_RESP_FN,
					       S_IFREG | 0444,
					       de_utracing_client);
	  if (de_utracing_resp) {
	    struct task_struct * task;
	    struct utrace_attached_engine * utracing_engine;
	    de_utracing_resp->read_proc  = if_file_read;
	    
	    task = get_task (client_pid);
	  
	    if (task) {
	      utracing_engine = utrace_attach (task,
					       UTRACE_ATTACH_CREATE |
					       UTRACE_ATTACH_EXCLUSIVE |
					       UTRACE_ATTACH_MATCH_OPS,
					       &utracing_utrace_ops,
					       0UL);  //fixme -- maybe use?
	      if (!IS_ERR (utracing_engine)) {
	      //fixme -- do something with rc?
		rc = utrace_set_flags (task,utracing_engine,
				       UTRACE_EVENT (EXEC)	|
				       UTRACE_EVENT (EXIT)	|
				       UTRACE_EVENT (DEATH));
	  
		rc = create_utracing_info_entry (client_pid,
						 client_pid_dir,
						 de_utracing_client,
						 de_utracing_cmd,
						 de_utracing_resp,
						 utracing_engine);
		if (0 == rc) {
		  memcpy (&utracing_info_top->proc_dir_operations,
			  de_utrace_control->proc_fops,
			  sizeof(struct file_operations));
		  utracing_info_top->proc_dir_operations.ioctl = utracer_ioctl;
		  de_utracing_cmd->proc_fops =
		    &utracing_info_top->proc_dir_operations;
	    
		  de_utracing_cmd->data       = utracing_info_top;
		  de_utracing_resp->data      = utracing_info_top;
		}
		else {
		  remove_proc_entry(UTRACER_RESP_FN, de_utracing_client);
		  remove_proc_entry(UTRACER_CMD_FN, de_utracing_client);
		  remove_proc_entry(client_pid_dir, de_utrace);
		  kfree (client_pid_dir);
		}
	      }
	      else {
		remove_proc_entry(UTRACER_RESP_FN, de_utracing_client);
		remove_proc_entry(UTRACER_CMD_FN, de_utracing_client);
		remove_proc_entry(client_pid_dir, de_utrace);
		kfree (client_pid_dir);
		rc = -UTRACER_EENGINE;
	      }
	    }
	    else {
	      remove_proc_entry(UTRACER_RESP_FN, de_utracing_client);
	      remove_proc_entry(UTRACER_CMD_FN, de_utracing_client);
	      remove_proc_entry(client_pid_dir, de_utrace);
	      kfree (client_pid_dir);
	      rc = -ESRCH;
	    }
	  }
	  else {
	    remove_proc_entry(UTRACER_CMD_FN, de_utracing_client);
	    remove_proc_entry(client_pid_dir, de_utrace);
	    kfree (client_pid_dir);
	    rc = -ENOMEM;
	  }
	}
	else {
	  remove_proc_entry(client_pid_dir, de_utrace);
	  kfree (client_pid_dir);
	  return -ENOMEM;
	}
      }
      else {
	kfree (client_pid_dir);
	rc = -ENOMEM;
      }
    }
    else rc = -ENOMEM;
  }
  else rc = -UTRACER_ETRACING;

  return rc;
}

int
handle_unregister (register_cmd_s * register_cmd)
{
  int rc = 0;
  utracing_info_s * utracing_info_found =
    lookup_utracing_info (register_cmd->client_pid);

  if (utracing_info_found)
    remove_utracing_info_entry (utracing_info_found);
  else
    rc = -UTRACER_ETRACING;

  return rc;
}

#if 0
int
control_file_write (struct file *file,
                    const char *buffer,
                    unsigned long count,
                    void *data)
{
  ctl_cmd_u ctl_cmd;

  if (count > sizeof(if_cmd_u)) return -ENOSPC;

  if (copy_from_user(&ctl_cmd, buffer, count) ) return -EFAULT;

  switch ((ctl_cmd_e)(ctl_cmd.cmd)) {
  case CTL_CMD_NULL:
    break;
  default:
    break;
  }
  return count;
}

int
control_file_read ( char *buffer,
                    char **buffer_location,
                    off_t offset,
                    int buffer_length,
                    int *eof,
                    void *data)
{
  int rc;

  if (0 < offset) rc = 0;
  else {
    rc = sprintf (buffer, "Control ready\n");
    *eof = 1;
  }

  return rc;
}
#endif


