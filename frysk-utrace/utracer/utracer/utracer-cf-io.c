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
#include <linux/highmem.h>
#include <linux/pagemap.h>
#include <asm/uaccess.h>
#include <linux/tracehook.h>
#include <asm-i386/tracehook.h>

#include "utracer.h"
#include "utracer-private.h"

MODULE_LICENSE("GPL");
MODULE_AUTHOR("Chris Moller");

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
#if 0
  struct proc_dir_entry * de = utracing_info_found->de_utracing_client;

  if (de) {
    lock_kernel();
    if (atomic_read(&de->count)) {
      if (atomic_dec_and_test(&de->count)) {
	if (de->deleted) {
	  printk("de_put: deferred delete of %s\n",
		 de->name);
	  free_proc_entry(de);
	  utracing_info_found->de_utracing_client = NULL;
	}
      }
    }
    unlock_kernel();
  }
#endif
  
      
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

static int
handle_printenv (printenv_cmd_s * printenv_cmd)
{
  int rc = 0;
  utracing_info_s * utracing_info_found =
    lookup_utracing_info (printenv_cmd->utracing_pid);

  if (utracing_info_found) {
    struct task_struct * task = get_task (printenv_cmd->utraced_pid);
    if (task) {
      struct mm_struct * mm = get_task_mm(task);

      if (mm) {
	int ret;
	char * buffer;
	struct vm_area_struct * vma;
	struct page *page;
	long llen;
	unsigned int len = mm->env_end - mm->env_start;

	if (len > PAGE_SIZE) len = PAGE_SIZE;
	buffer = kmalloc (len, GFP_KERNEL);

	ret = get_user_pages (task,
			      mm,
			      mm->env_start,
			      len,
			      0,				// int write
			      0,				// int force
			      &page,
			      &vma);
	if (0 < ret) {
	  void * maddr = kmap (page);
	  copy_from_user_page (vma,
			       page,
			       mm->env_start,
			       buffer,
			       maddr + (mm->env_start & (PAGE_SIZE-1)),
			       len);
	  if ((len + sizeof(long)) > printenv_cmd->buffer_len)
	    len = printenv_cmd->buffer_len - sizeof(long);
	  llen = (long)len;
	  if (copy_to_user (printenv_cmd->buffer, &llen,
			    sizeof(long)))
	    rc = -EFAULT;
	  else {
	    if (copy_to_user (printenv_cmd->buffer + sizeof(long),
			      buffer, len))
	      rc = -EFAULT;
	  }
	}
	else rc = 9999; // fixme

	kfree (buffer);
      }
      else rc = -8888; // fixme -- mm not found
    }
    else rc = -ESRCH;
  }
  else rc = -UTRACER_ETRACING;

  return rc;
}

static int
utracer_ioctl (struct inode * inode,
	       struct file * file,
	       unsigned int a1,
	       unsigned long a2)
{
  int rc = 0;
  if_cmd_u if_cmd;
  
  printk (KERN_ALERT "utracer_ioctl len = %d\n", a1);

  if (copy_from_user(&if_cmd, (void *)a2, a1))
    return -EFAULT;

  printk (KERN_ALERT "if_cmd.cmd = %ld\n", if_cmd.cmd);

  switch (if_cmd.cmd) {
  case IF_CMD_PRINTENV:
    rc = handle_printenv (&if_cmd.printenv_cmd);
    break;
  default:
    rc = -EINVAL;
    break;
  }

#if 0
  if (copy_to_user (utracer_ioctl.bffr, "hello", 6))
    rc = -EFAULT;
#endif
  return rc;
}

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
      else
	return -UTRACER_ETRACING;
    }
    break;
  case CTL_CMD_REGISTER:
    {
      register_cmd_s register_cmd = ctl_cmd.register_cmd;
      long client_pid = register_cmd.client_pid;
      utracing_info_s * utracing_info_found =
        lookup_utracing_info (client_pid);

      if (!utracing_info_found) {  // if non-null, entry already exists
        struct proc_dir_entry * de_utracing_client;
        struct proc_dir_entry * de_utracing_cmd;
        struct proc_dir_entry * de_utracing_resp;
        char * client_pid_dir =
	  kasprintf(GFP_KERNEL, "%ld", client_pid);

	if (!client_pid_dir)
	  return -ENOMEM;

	de_utracing_client = proc_mkdir (client_pid_dir, de_utrace);
	if (!de_utracing_client) {
	  kfree (client_pid_dir);
	  return -ENOMEM;
	}
	
	de_utracing_cmd = create_proc_entry(UTRACER_CMD_FN,
						S_IFREG | 0666,
						de_utracing_client);
	if (!de_utracing_cmd) {
	  remove_proc_entry(client_pid_dir, de_utrace);
	  kfree (client_pid_dir);
	  return -ENOMEM;
	}
	
	de_utracing_cmd->write_proc = if_file_write;
	de_utracing_cmd->read_proc = cf_file_read;
	
	de_utracing_resp = create_proc_entry(UTRACER_RESP_FN,
					     S_IFREG | 0444,
					     de_utracing_client);
	if (!de_utracing_resp) {
	  remove_proc_entry(UTRACER_CMD_FN, de_utracing_client);
	  remove_proc_entry(client_pid_dir, de_utrace);
	  kfree (client_pid_dir);
	  return -ENOMEM;
	}
	de_utracing_resp->read_proc  = if_file_read;
	
	{
          int rc;
	  struct utrace_attached_engine * utracing_engine;
	  struct task_struct * task = get_task (client_pid);
	  
	  if (!task) {
	    remove_proc_entry(UTRACER_RESP_FN, de_utracing_client);
	    remove_proc_entry(UTRACER_CMD_FN, de_utracing_client);
	    remove_proc_entry(client_pid_dir, de_utrace);
	    kfree (client_pid_dir);
	    return -ESRCH;
	  }
	  
	  utracing_engine = utrace_attach (task,
					   UTRACE_ATTACH_CREATE |
					   UTRACE_ATTACH_EXCLUSIVE |
					   UTRACE_ATTACH_MATCH_OPS,
					   &utracing_utrace_ops,
					   0UL);  //fixme -- maybe use?
	  if (IS_ERR (utracing_engine)) {
	    remove_proc_entry(UTRACER_RESP_FN, de_utracing_client);
	    remove_proc_entry(UTRACER_CMD_FN, de_utracing_client);
	    remove_proc_entry(client_pid_dir, de_utrace);
	    kfree (client_pid_dir);
	    return -UTRACER_EENGINE;
	  }

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
          if (0 != rc) {
	    remove_proc_entry(UTRACER_RESP_FN, de_utracing_client);
	    remove_proc_entry(UTRACER_CMD_FN, de_utracing_client);
	    remove_proc_entry(client_pid_dir, de_utrace);
	    kfree (client_pid_dir);
            return rc;
          }

	  memcpy (&utracing_info_top->proc_dir_operations,
		  de_utrace_control->proc_fops,
		  sizeof(struct file_operations));
	  utracing_info_top->proc_dir_operations.ioctl = utracer_ioctl;
	  de_utracing_cmd->proc_fops =
	    &utracing_info_top->proc_dir_operations;
	
	  de_utracing_cmd->data       = utracing_info_top;
	  de_utracing_resp->data      = utracing_info_top;
	}

	return count;
      }
      else 
	return -UTRACER_ETRACING;
    }
    break;
  }
  return count;
}

#if 0
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


