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
			   struct utrace_attached_engine * utraced_engine)
{
  if (utracing_info_entry) {
    utraced_info_s  * utraced_info_new =
      kmalloc (sizeof(utraced_info_s), GFP_KERNEL);
    if (!utraced_info_new) return -ENOMEM;

    utraced_info_new->utraced_pid = utraced_pid;
    utraced_info_new->utraced_engine = utraced_engine;

    if (utracing_info_entry->utraced_info)
      (utracing_info_entry->utraced_info)->prev = utraced_info_new;
    utraced_info_new->next = utracing_info_entry->utraced_info;
    utracing_info_entry->utraced_info = utraced_info_new;
    utraced_info_new->prev = NULL;

    return 0;
  }
  else return -UTRACER_ETRACING;
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
			    char * utracing_cmd_pid_string,
			    char * utracing_resp_pid_string,
			    struct proc_dir_entry * de_utracing_control,
			    struct proc_dir_entry * de_utracing_resp,
			    struct utrace_attached_engine * utracing_engine)
{
  utracing_info_s * utracing_info_new =
    kmalloc (sizeof(utracing_info_s), GFP_KERNEL);
  if (!utracing_info_new) return -ENOMEM;

  if (utracing_info_top) {
    utracing_info_top->prev = utracing_info_new;
    utracing_info_new->next = utracing_info_top;
  }
  else utracing_info_new->next = NULL;
  utracing_info_top = utracing_info_new;
  utracing_info_new->prev = NULL;
    
  utracing_info_new->utracing_pid		= utracing_pid;
  utracing_info_new->utracing_cmd_pid_string	= utracing_cmd_pid_string;
  utracing_info_new->utracing_resp_pid_string	= utracing_resp_pid_string;
  utracing_info_new->de_utracing_control	= de_utracing_control;
  utracing_info_new->de_utracing_resp		= de_utracing_resp;
  utracing_info_new->utracing_engine		= utracing_engine;
  utracing_info_new->utraced_info		= NULL;
  init_waitqueue_head (&(utracing_info_new->ifr_wait));
  init_waitqueue_head (&(utracing_info_new->ifw_wait));
  utracing_info_new->queued_data		= NULL;
  utracing_info_new->queued_data_length		= 0;

  return 0;
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
    
    if (task && utracing_info_entry->utracing_engine)
      utrace_detach (task, utracing_info_entry->utracing_engine);
    
    if (de_utrace && utracing_info_entry->utracing_cmd_pid_string)
      remove_proc_entry (utracing_info_entry->utracing_cmd_pid_string,
			 de_utrace);
    
    if (de_utrace && utracing_info_entry->utracing_resp_pid_string)
      remove_proc_entry (utracing_info_entry->utracing_resp_pid_string,
			 de_utrace);
    
    if (utracing_info_entry->utracing_cmd_pid_string)
      kfree (utracing_info_entry->utracing_cmd_pid_string);
    if (utracing_info_entry->utracing_resp_pid_string)
      kfree (utracing_info_entry->utracing_resp_pid_string);
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

static int __init utracer_init(void)
{

  de_utrace = create_proc_entry(BASE_DIR, S_IFDIR | 0777, NULL);

  if (de_utrace == NULL) {
    remove_proc_entry(BASE_DIR, &proc_root);
    return -ENOMEM;
  }

  de_utrace->owner     = THIS_MODULE;
  de_utrace->mode      = S_IFDIR | 0777;
  de_utrace->uid       = 0;
  de_utrace->gid       = 0;
  de_utrace->size      = 0;

  de_utrace_control = create_proc_entry(CONTROL_FN,
                                        S_IFREG | 0666, de_utrace);


  if (de_utrace_control == NULL) {
    remove_proc_entry(CONTROL_FN, de_utrace);
    return -ENOMEM;
  }

  de_utrace_control->write_proc = control_file_write;
#if 0
  de_utrace_control->read_proc  = control_file_read;
#else
  de_utrace_control->read_proc  = NULL;
#endif
  de_utrace_control->owner     = THIS_MODULE;
  de_utrace_control->mode      = S_IFREG | 0666;
  de_utrace_control->uid       = 0;
  de_utrace_control->gid       = 0;
  de_utrace_control->size      = 0;

  return 0;
}

static void __exit utracer_exit (void)
{
  utracing_info_s * utracing_info_entry;

  for (utracing_info_entry = utracing_info_top;
       utracing_info_entry;
       utracing_info_entry = remove_utracing_info_entry (utracing_info_entry));

  remove_proc_entry (CONTROL_FN, de_utrace);
  remove_proc_entry (BASE_DIR, &proc_root);
}

module_init (utracer_init);
module_exit (utracer_exit);
