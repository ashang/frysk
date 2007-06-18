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
#include <asm/uaccess.h>
#include <linux/tracehook.h>
#include <asm-i386/tracehook.h>

#include "utracer.h"
#include "utracer-private.h"

MODULE_LICENSE("GPL");
MODULE_AUTHOR("Chris Moller");

DECLARE_WAIT_QUEUE_HEAD(cfr_wait);
int cfr_data_ready = 0;

void
wake_up_cfr_wait()
{
  wake_up_all (&cfr_wait);
}

static u32
client_report_exit (struct utrace_attached_engine *engine,
	     struct task_struct *tsk,
	     long orig_code, long *code)
{
  printk(KERN_INFO "client reporting exit\n");	// fixme use to cleanup 
  return UTRACE_ACTION_RESUME;			// utracing_info list
}

static u32
client_report_death (struct utrace_attached_engine *engine,
	      struct task_struct *tsk)
{
  printk(KERN_INFO "client reporting death\n");
  return UTRACE_ACTION_RESUME;
}

static const struct utrace_engine_ops utracing_utrace_ops = {
  .report_syscall_entry	= NULL,
  .report_syscall_exit	= NULL,
  .report_exec		= NULL,
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
  case CTL_CMD_UNREGISTER:
    {
      register_cmd_s register_cmd = ctl_cmd.register_cmd;
      long client_pid = register_cmd.client_pid;
      utracing_info_s * utracing_info_found =
	lookup_utracing_info (client_pid);

      if (utracing_info_found)
	remove_utracing_info_entry (utracing_info_found);
      else return -UTRACER_ETRACING;
    }
    break;
  case CTL_CMD_REGISTER:
    {
      register_cmd_s register_cmd = ctl_cmd.register_cmd;
      long client_pid = register_cmd.client_pid;
      utracing_info_s * utracing_info_found =
        lookup_utracing_info (client_pid);

      if (!utracing_info_found) {  // if non-null, entry already exists
        struct proc_dir_entry * de_utracing_control;
        struct proc_dir_entry * de_utracing_resp;
        char * client_cmd_pid_string =
	  kasprintf(GFP_KERNEL, "cmd_%ld", client_pid);
        char * client_resp_pid_string =
	  kasprintf(GFP_KERNEL, "resp_%ld", client_pid);
	
        if (client_cmd_pid_string && client_resp_pid_string) {
          int rc;
	  struct utrace_attached_engine * utracing_engine;
	  
          de_utracing_control = create_proc_entry(client_cmd_pid_string,
                                                  S_IFREG | 0222,
                                                  de_utrace);

          if (NULL == de_utracing_control) {
            remove_proc_entry(client_cmd_pid_string, de_utrace);
            kfree (client_cmd_pid_string);
            kfree (client_resp_pid_string);
            return -ENOMEM;
          }
	  
          de_utracing_resp = create_proc_entry(client_resp_pid_string,
					       S_IFREG | 0444,
					       de_utrace);

          if (NULL == de_utracing_resp) {
            remove_proc_entry(client_resp_pid_string, de_utrace);
            kfree (client_cmd_pid_string);
            kfree (client_resp_pid_string);
            return -ENOMEM;
          }

	  {
	    struct task_struct * task = get_task (client_pid);
	    if (!task) {
	      remove_proc_entry(client_cmd_pid_string, de_utrace);
	      remove_proc_entry(client_resp_pid_string, de_utrace);
	      kfree (client_cmd_pid_string);
	      kfree (client_resp_pid_string);
	      return -ESRCH;
	    }
	   
	    utracing_engine = utrace_attach (task,
					     UTRACE_ATTACH_CREATE |
					     UTRACE_ATTACH_EXCLUSIVE |
					     UTRACE_ATTACH_MATCH_OPS,
					     &utracing_utrace_ops,
					     0UL);  //fixme -- maybe use?
	    if (IS_ERR (utracing_engine)) {
	      remove_proc_entry(client_cmd_pid_string, de_utrace);
	      remove_proc_entry(client_resp_pid_string, de_utrace);
	      kfree (client_cmd_pid_string);
	      kfree (client_resp_pid_string);
	      return -UTRACER_EENGINE;
	    }

	    //fixme -- do something with rc?
	    rc = utrace_set_flags (task,utracing_engine,
				   UTRACE_EVENT (EXIT)	|
				   UTRACE_EVENT (DEATH));
	  }

	  rc = create_utracing_info_entry (client_pid,
					   client_cmd_pid_string,
					   client_resp_pid_string,
					   de_utracing_control,
					   de_utracing_resp,
					   utracing_engine);
          if (0 != rc) {
	    remove_proc_entry(client_cmd_pid_string, de_utrace);
	    remove_proc_entry(client_resp_pid_string, de_utrace);
            kfree (client_cmd_pid_string);
            kfree (client_resp_pid_string);
            return rc;
          }

          de_utracing_control->write_proc = if_file_write;
          de_utracing_control->read_proc  = NULL;
          de_utracing_control->owner      = THIS_MODULE;
          de_utracing_control->mode       = S_IFREG | 0222;
          de_utracing_control->uid        = 0;
          de_utracing_control->gid        = 0;
          de_utracing_control->size       = 0;
          de_utracing_control->data       = utracing_info_top;

          de_utracing_resp->write_proc = NULL;
          de_utracing_resp->read_proc  = if_file_read;
          de_utracing_resp->owner      = THIS_MODULE;
          de_utracing_resp->mode       = S_IFREG | 0444;
          de_utracing_resp->uid        = 0;
          de_utracing_resp->gid        = 0;
          de_utracing_resp->size       = 0;
          de_utracing_resp->data       = utracing_info_top;

	  return count;
        }
        else {
	  if (client_cmd_pid_string) kfree (client_cmd_pid_string);
	  if (client_resp_pid_string) kfree (client_resp_pid_string);
	  return -ENOMEM;
	}
      }
      else return -UTRACER_ETRACING;
    }
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
  int error;

  printk (KERN_INFO "about to wait in control_file_read\n");
  error = wait_event_interruptible (cfr_wait, cfr_data_ready == 1);
  printk (KERN_INFO "done waiting in control_file_read error = %d\n", error);
  cfr_data_ready = 0;

  if (0 < offset) rc = 0;
  else {
    rc = sprintf (buffer, "Control ready\n");
    *eof = 1;
  }

  return rc;
  
}


