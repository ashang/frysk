#include <linux/module.h>
#include <linux/moduleparam.h>
#include <linux/kernel.h>
#include <linux/fs.h>
#include <linux/mm.h>
#include <linux/highmem.h>
#include <linux/tracehook.h>
#include <asm/tracehook.h>
#include <asm/uaccess.h>
#include <asm/unistd.h>

#include "../include/utracer.h"
#include "utracer-private.h"

MODULE_LICENSE("GPL");
MODULE_AUTHOR("Chris Moller");

static int
handle_printexe (printexe_cmd_s * printexe_cmd)
{
  int rc = 0;
  utracing_info_s * utracing_info_found =
    lookup_utracing_info (printexe_cmd->utracing_pid);

  if (utracing_info_found) {
    utraced_info_s * utraced_info_found =
      lookup_utraced_info (utracing_info_found, printexe_cmd->utraced_pid);

    if (utraced_info_found) {
      if (utraced_info_found->filename) {
	long len = 1 + strlen (utraced_info_found->filename);
	if (len > printexe_cmd->filename_len)
	  len = printexe_cmd->filename_len - 1;
	if (copy_to_user (printexe_cmd->filename,
			  utraced_info_found->filename, len))
	  rc = -EFAULT;
      }
      if ((0 == rc) && utraced_info_found->interp) {
	long len = 1 + strlen (utraced_info_found->interp);
	if (len > printexe_cmd->interp_len)
	  len = printexe_cmd->interp_len - 1;
	if (copy_to_user (printexe_cmd->interp,
			  utraced_info_found->interp, len))
	  rc = -EFAULT;
      }
    }
    else rc = -UTRACER_ETRACED;
  }
  else rc = -UTRACER_ETRACING;

  return rc;
}

static void
build_printmmap_resp (printmmap_resp_s * prm,
		      vm_struct_subset_s ** vm_struct_subset_p,
		      long * vm_struct_subset_size_p,
		      char ** string_ptr_p,
		      long * string_ptr_len_p,
		      struct task_struct * task)
{
  struct mm_struct * mm = get_task_mm(task);

  memset (prm, 0, sizeof(prm));
  
  if (mm) {
    down_read(&mm->mmap_sem);
    task_lock(task);
    
    prm->mmap_base = mm->mmap_base;
    prm->task_size = mm->task_size;
    prm->total_vm = mm->total_vm;
    prm->locked_vm = mm->locked_vm;
    prm->shared_vm = mm->shared_vm;
    prm->exec_vm = mm->exec_vm;
    prm->stack_vm = mm->stack_vm;
    prm->reserved_vm = mm->reserved_vm;
    prm->def_flags = mm->def_flags;
    prm->nr_ptes = mm->nr_ptes;
    
    prm->start_code = mm->start_code;
    prm->end_code = mm->end_code;
    
    prm->start_data = mm->start_data;
    prm->end_data = mm->end_data;
    
    prm->start_brk = mm->start_brk;
    prm->brk = mm->brk;
    
    prm->start_stack = mm->start_stack;
    
    prm->arg_start = mm->arg_start;
    prm->arg_end = mm->arg_end;
    prm->env_start = mm->env_start;
    prm->env_end = mm->env_end;

    {
      unsigned long nr_mmaps = 0;
      struct vm_area_struct * mmap;

      vm_struct_subset_s * vm_struct_subset;
      unsigned long mmaps_index = 0;
      unsigned long string_count = 0;
      char * vm_strings;
      char * string_ptr;
      
      mmap = mm->mmap;
      while (mmap) {
	struct file * vm_file;
	nr_mmaps++;

	vm_file = mmap->vm_file;
	if (vm_file) {
	  struct path tpath = vm_file->f_path;
	  struct vfsmount * mnt    = tpath.mnt;
	  struct dentry * dentry   = tpath.dentry;
	  char * buf = kmalloc (PATH_MAX, GFP_KERNEL);
	  char * res = d_path (dentry, mnt, buf, PATH_MAX);
	  string_count += 1 + strlen (res);
	  kfree (buf);
	}

	mmap = mmap->vm_next;
      }

      prm->nr_mmaps = nr_mmaps;
      prm->vm_strings_length = string_count;
      
      vm_struct_subset = kmalloc (nr_mmaps * sizeof(vm_struct_subset_s),
				  GFP_KERNEL);
      if (vm_struct_subset_p) *vm_struct_subset_p = vm_struct_subset;
      if (vm_struct_subset_size_p) *vm_struct_subset_size_p =
	nr_mmaps * sizeof(vm_struct_subset_s);
      
      string_ptr = vm_strings = kmalloc (string_count, GFP_KERNEL);
      if (string_ptr_p) *string_ptr_p = vm_strings;
      if (string_ptr_len_p) *string_ptr_len_p = string_count;
      
      mmap = mm->mmap;
      while (mmap) {
	struct file * vm_file;
	
	// see include/linux/mm.h
	vm_struct_subset[mmaps_index].vm_start = mmap->vm_start;
	vm_struct_subset[mmaps_index].vm_end   = mmap->vm_end;
	vm_struct_subset[mmaps_index].vm_flags = mmap->vm_flags;
	vm_struct_subset[mmaps_index].name_offset = -1;

	vm_file = mmap->vm_file;
	if (vm_file) {
	  struct path tpath = vm_file->f_path;
	  struct vfsmount * mnt    = tpath.mnt;
	  struct dentry * dentry   = tpath.dentry;
	  char * buf = kmalloc (PATH_MAX, GFP_KERNEL);
	  char * res = d_path (dentry, mnt, buf, PATH_MAX);

	  vm_struct_subset[mmaps_index].name_offset =
	    string_ptr - vm_strings;
	  memcpy (string_ptr, res, 1 + strlen (res));
	  string_ptr += 1 + strlen (res);
	  
	  kfree (buf);
	}

	mmap = mmap->vm_next;
	mmaps_index++;
      }
    }
    
    task_unlock(task);
    up_read(&mm->mmap_sem);
    mmput(mm);
  }
}

static int
handle_printmap (printmmap_cmd_s * printmmap_cmd)
{
  int rc = 0;
  utracing_info_s * utracing_info_found =
    lookup_utracing_info (printmmap_cmd->utracing_pid);

  if (utracing_info_found) {
    struct task_struct * task;
    printmmap_resp_s * printmmap_resp =
      kmalloc (sizeof(printmmap_resp_s), GFP_KERNEL);

    task = get_task (printmmap_cmd->utraced_pid);
    if (task) {
      vm_struct_subset_s  * vm_struct_subset = NULL;
      long vm_struct_subset_size = 0;
      char * vm_strings = NULL;
      long vm_strings_length = 0;
	  
      build_printmmap_resp (printmmap_resp,
			    &vm_struct_subset,
			    &vm_struct_subset_size,
			    &vm_strings,
			    &vm_strings_length,
			    task);
      printmmap_resp->utraced_pid = printmmap_cmd->utraced_pid;

      if (copy_to_user (printmmap_cmd->printmmap_resp,
			printmmap_resp,
			sizeof(printmmap_resp_s)))
	rc = -EFAULT;
      if (0 == rc) {
	if (copy_to_user (printmmap_cmd->vm_struct_subset_length,
			  &vm_struct_subset_size,
			  sizeof(long)))
	  rc = -EFAULT;

	if (0 == rc) {
	  if (vm_struct_subset_size < printmmap_cmd->vm_struct_subset_alloc)
	    vm_struct_subset_size = printmmap_cmd->vm_struct_subset_alloc;
	  if (copy_to_user (printmmap_cmd->vm_struct_subset,
			    vm_struct_subset,
			    vm_struct_subset_size))
	    rc = -EFAULT;
	}
      }
      if (0 == rc) {
	if (copy_to_user (printmmap_cmd->vm_strings_length,
			  &vm_strings_length,
			  sizeof(long)))
	  rc = -EFAULT;

	if (0 == rc) {
	  if (vm_strings_length < printmmap_cmd->vm_strings_alloc)
	    vm_strings_length = printmmap_cmd->vm_strings_alloc;
	  if (copy_to_user (printmmap_cmd->vm_strings,
			    vm_strings,
			    vm_strings_length))
	    rc = -EFAULT;
	}
      }

      if (vm_struct_subset) kfree(vm_struct_subset);
      if (vm_strings) kfree(vm_strings);
      if (printmmap_resp) kfree (printmmap_resp);
    }
    else rc = -ESRCH;
  }
  else rc = -UTRACER_ETRACING;

  return rc;
}

static int
handle_getmem (getmem_cmd_s * getmem_cmd)
{
  int rc = 0;
  utracing_info_s * utracing_info_found =
    lookup_utracing_info (getmem_cmd->utracing_pid);

  if (utracing_info_found) {
    struct task_struct * task = get_task (getmem_cmd->utraced_pid);
    if (task) {
      struct mm_struct * mm = get_task_mm(task);

      if (mm) {
	int ret;
	struct vm_area_struct * vma;
	struct page *page;
	char * buffer;
	unsigned int nr_pages =
	  (getmem_cmd->mem_len + (PAGE_SIZE - 1))/PAGE_SIZE;

	down_read(&mm->mmap_sem);
	
	buffer = kmalloc (getmem_cmd->mem_len, GFP_KERNEL);
	ret = get_user_pages (task,
			      mm,
			      getmem_cmd->mem_addr,
			      nr_pages,
			      0,				// int write
			      0,				// int force
			      &page,
			      &vma);
	if (0 < ret) {
	  void * maddr = kmap (page);
	  copy_from_user_page (vma,
			       page,
			       getmem_cmd->mem_addr,
			       buffer,
			       maddr + (getmem_cmd->mem_addr & (PAGE_SIZE-1)),
			       getmem_cmd->mem_len);
	  if (copy_to_user (getmem_cmd->mem,
			    buffer, getmem_cmd->mem_len)) {
	    kfree (buffer);
	    rc = -EFAULT;
	  }
#if 0
	  // fixme -- fails, but i'm not really using it at the moment
	  else {
	    if (copy_to_user (getmem_cmd->actual,
			      &getmem_cmd->mem_len, sizeof (unsigned long))) {
	      rc = -EFAULT;
	    }
	  }
#endif
	}
	else rc = -UTRACER_EPAGES;
	
	up_read(&mm->mmap_sem);
      }
      else rc = -UTRACER_EMM;
    }
    else rc = -ESRCH;
  }
  else rc = -UTRACER_ETRACING;

  return rc;
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
	unsigned int nr_pages = (len + (PAGE_SIZE - 1))/PAGE_SIZE;

	buffer = kmalloc (len, GFP_KERNEL);

	ret = get_user_pages (task,
			      mm,
			      mm->env_start,
			      nr_pages,
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
	  if (copy_to_user (printenv_cmd->length_returned, &llen,
			    sizeof(long))) {
	    kfree (buffer);
	    rc = -EFAULT;
	  }
	  else {
	    if (copy_to_user (printenv_cmd->buffer,
			      buffer, len)) {
	      kfree (buffer);
	      rc = -EFAULT;
	    }
	  }
	}
	else rc = -UTRACER_EPAGES;

	kfree (buffer);
      }
      else rc = -UTRACER_EPAGES;
    }
    else rc = -ESRCH;
  }
  else rc = -UTRACER_ETRACING;

  return rc;
}

int
handle_listpids (listpids_cmd_s * listpids_cmd)
{
  int rc = 0;
  utracing_info_s * utracing_info_found =
    lookup_utracing_info (listpids_cmd->utracing_pid);

  if (utracing_info_found) {
    utraced_info_s * utraced_info;
    long * pids;
    int nr_pids;

    for (nr_pids = 0,utraced_info = utracing_info_found->utraced_info;
	 utraced_info;
	 utraced_info = utraced_info->next) nr_pids++;
    if (0 < nr_pids) {
      int i;

      pids = kmalloc (listpids_cmd->nr_pids_alloced * sizeof(long),
		      GFP_KERNEL);
      for (i = 0, utraced_info = utracing_info_found->utraced_info;
	 utraced_info;
	   i++, utraced_info = utraced_info->next) {
	pids[i] = utraced_info->utraced_pid;
      }

      if (copy_to_user (listpids_cmd->nr_pids_actual, &nr_pids, sizeof(long)))
	rc = -EFAULT;
      else {
	long len = nr_pids * sizeof(long);
	if (len > listpids_cmd->nr_pids_alloced * sizeof(long))
	  len = listpids_cmd->nr_pids_alloced * sizeof(long);
	if (copy_to_user (listpids_cmd->pids, pids, len))
	  rc = -EFAULT;
      }
    }
  }
  else rc = -UTRACER_ETRACING;

  return rc;
}

int
handle_getregs (readreg_cmd_s * readreg_cmd)
{
  int rc = 0;
  utracing_info_s * utracing_info_found =
    lookup_utracing_info (readreg_cmd->utracing_pid);

  if (utracing_info_found) {
    struct task_struct * task = get_task (readreg_cmd->utraced_pid);
    if (task) {
      if ((current == task) ||
	  (task->state & (TASK_TRACED | TASK_STOPPED))) {
	struct utrace_attached_engine * engine =
	  locate_engine (readreg_cmd->utracing_pid, readreg_cmd->utraced_pid);
	
	if (engine) {
	  const struct utrace_regset_view * rv = utrace_native_view(task);

	  if ((0 <= readreg_cmd->regset) && (readreg_cmd->regset < rv->n)) {
	    const struct utrace_regset * regset =
	      utrace_regset(task, engine, rv, readreg_cmd->regset);

	    if (unlikely(regset == NULL))
	      rc = -EIO;
	    else {
	      int lrc;

	      lrc = regset->get(task,
				regset,
				0,		// pos
				regset->n * regset->size,	// count
				NULL,		// kbuf
				readreg_cmd->regsinfo);		// ubuf
	    
	      if (copy_to_user (readreg_cmd->nr_regs,
				&regset->n, sizeof(unsigned int)))
		rc = -EFAULT;
	      else {
		if (copy_to_user (readreg_cmd->reg_size,
				  &regset->size, sizeof(unsigned int)))
		  rc = -EFAULT;
		{
		  long actual_size = (long)(regset->n * regset->size);
		  if (copy_to_user (readreg_cmd->actual_size,
				    &actual_size, sizeof(long)))
		    rc = -EFAULT;
		}
	      }
	    }
	  }
	  else rc = -UTRACER_EREGSET;
	}
	else rc = -UTRACER_EENGINE;
      }
      else rc = -UTRACER_ESTATE;
    }
    else rc = -ESRCH;
  }
  else rc = -UTRACER_ETRACING;

  return rc;
}

extern const struct utrace_engine_ops utraced_utrace_ops;

static int
attach_cmd_fcn (long utracing_pid, long utraced_pid,
		long quiesce, long exec_quiesce)
{
  int rc;
  
  utracing_info_s * utracing_info_found =
    lookup_utracing_info (utracing_pid);

  DB_PRINTK (KERN_ALERT "attach_cmd_fcn\n");
    
  if (utracing_info_found) {
    struct task_struct * task = get_task (utraced_pid);
    
    DB_PRINTK (KERN_ALERT "attach_cmd_fcn -- ui found\n");
    if (task) {
      struct utrace_attached_engine * engine;
      engine = utrace_attach (task,
			      UTRACE_ATTACH_CREATE |
			      UTRACE_ATTACH_EXCLUSIVE |
			      UTRACE_ATTACH_MATCH_OPS,
			      &utraced_utrace_ops,
			      0UL);  //fixme -- maybe use?
      DB_PRINTK (KERN_ALERT "attach_cmd_fcn -- task found\n");
      if (!IS_ERR (engine)) {
	unsigned long flags =
	  UTRACE_EVENT (EXEC)	    |
	  UTRACE_EVENT (SIGNAL)	    |
	  UTRACE_EVENT (VFORK_DONE) |
	  UTRACE_EVENT (CLONE)	    |
	  UTRACE_EVENT (EXIT)	    |
	  UTRACE_EVENT (QUIESCE)    |
	  UTRACE_EVENT (SYSCALL_EXIT)    |
	  UTRACE_EVENT (SYSCALL_ENTRY)    |
	  UTRACE_EVENT (REAP)    |
	  UTRACE_EVENT (DEATH);

	if (quiesce) flags |= UTRACE_ACTION_QUIESCE;
	
	DB_PRINTK (KERN_ALERT "attach_cmd_fcn -- engine ok\n");
	
	//fixme -- do something with rc?
	rc = utrace_set_flags (task,engine, flags);

	engine->data = (unsigned long)utracing_info_found;
	rc = create_utraced_info_entry (utracing_info_found,
					utraced_pid,
					engine,
					exec_quiesce);
	DB_PRINTK (KERN_ALERT "attach_cmd_fcn -- ui create rc = %d\n", rc);
	if (0 != rc) utrace_detach(task, engine);
      }
      else rc = -UTRACER_EENGINE;
    }
    else rc = -ESRCH;
  }
  else rc = -UTRACER_ETRACING;

  return rc;
}

static int
handle_attach (attach_cmd_s * attach_cmd)
{
  int rc = attach_cmd_fcn (attach_cmd->utracing_pid,
			   attach_cmd->utraced_pid,
			   attach_cmd->quiesce,
			   attach_cmd->exec_quiesce);
  return rc;
}


static int
handle_switchpid (switchpid_cmd_s * switchpid_cmd)
{
  int rc = 0;
  struct task_struct * task;

  task = get_task (switchpid_cmd->utraced_pid);
  if (task) {
    utracing_info_s * utracing_info_found =
      lookup_utracing_info (switchpid_cmd->utracing_pid);
    if (utracing_info_found) {
      utraced_info_s * utraced_info_found =
	lookup_utraced_info (utracing_info_found, switchpid_cmd->utraced_pid);
      if (!utraced_info_found) rc = -UTRACER_ETRACED;
    }
    else rc = -UTRACER_ETRACING;
  }
  else rc = -ESRCH;

  return rc;
}

static int
handle_syscall (syscall_cmd_s * syscall_cmd)
{
  int rc = 0;
  struct task_struct * task;

  task = get_task (syscall_cmd->utraced_pid);

  if (task) {
    utracing_info_s * utracing_info_found =
      lookup_utracing_info (syscall_cmd->utracing_pid);

    if (utracing_info_found) {
      utraced_info_s * utraced_info_found =
	lookup_utraced_info (utracing_info_found, (long)task->pid);
      if (utraced_info_found) {
	struct utrace_attached_engine * engine =
	  locate_engine (syscall_cmd->utracing_pid,
			 syscall_cmd->utraced_pid);
	if (engine) {
	  unsigned long flags = engine->flags;
	  unsigned long ** bv;
	  unsigned long  ef;
	  if (SYSCALL_CMD_ENTRY == syscall_cmd_which(syscall_cmd)) {
	    bv = &(utraced_info_found->entry_bv);
	    ef = UTRACE_EVENT (SYSCALL_ENTRY);
	  }
	  else {
	    bv = &(utraced_info_found->exit_bv);
	    ef = UTRACE_EVENT (SYSCALL_EXIT);
	  }
	  
	  switch (syscall_cmd_cmd(syscall_cmd)) {
	  case SYSCALL_CMD_ENABLE:
	    flags |= ef;
	    break;
	  case SYSCALL_CMD_DISABLE:
	    flags &= ~ef;
	    break;
	  case SYSCALL_CMD_ADD:
	    if (SYSCALL_ALL == syscall_cmd->syscall_nr)
	      memset (*bv, -1, utraced_info_found->bv_len);
	    else if (NR_syscalls > syscall_cmd->syscall_nr)
	      setbit (*bv, syscall_cmd->syscall_nr);
	    else rc = -UTRACER_ESYSRANGE;
	    break;
	  case SYSCALL_CMD_REMOVE:
	    if (SYSCALL_ALL == syscall_cmd->syscall_nr)
	      memset (*bv, 0, utraced_info_found->bv_len);
	    else if (NR_syscalls > syscall_cmd->syscall_nr)
	      clearbit (*bv, syscall_cmd->syscall_nr);
	    else rc = -UTRACER_ESYSRANGE;
	    break;
	  }
	  utrace_set_flags(task, engine, flags);
	}
	else rc = -UTRACER_EENGINE;
      }
      else rc = -UTRACER_ETRACED;
    }
    else rc = -UTRACER_ETRACING;
  }
  else rc = -ESRCH;
  
  return rc;
}

int
utracer_ioctl (struct inode * inode,
	       struct file * file,
	       unsigned int a1,
	       unsigned long a2)
{
  int rc = 0;
  if_cmd_u if_cmd;
  
  if (copy_from_user(&if_cmd, (void *)a2, a1))
    return -EFAULT;

  switch (if_cmd.cmd) {
  case IF_CMD_LIST_PIDS:
    DB_PRINTK (KERN_ALERT "IF_LIST_PIDS--ioctl\n");
    rc = handle_listpids (&if_cmd.listpids_cmd);
    break;
  case IF_CMD_PRINTMMAP:
    DB_PRINTK (KERN_ALERT "IF_CMD_PRINTMAP--ioctl\n");
    rc = handle_printmap (&if_cmd.printmmap_cmd);
    break;
  case IF_CMD_PRINTENV:
    DB_PRINTK (KERN_ALERT "IF_CMD_PRINTENV--ioctl\n");
    rc = handle_printenv (&if_cmd.printenv_cmd);
    break;
  case IF_CMD_PRINTEXE:
    DB_PRINTK (KERN_ALERT "IF_CMD_PRINTEXE--ioctl\n");
    rc = handle_printexe (&if_cmd.printexe_cmd);
    break;
  case IF_CMD_GETMEM:
    DB_PRINTK (KERN_ALERT "IF_CMD_GETMEM--ioctl\n");
    rc = handle_getmem (&if_cmd.getmem_cmd);
    break;
  case IF_CMD_READ_REG:
    DB_PRINTK (KERN_ALERT "IF_CMD_READ_REG--ioctl\n");
    rc = handle_getregs (&if_cmd.readreg_cmd);
    break;
  case IF_CMD_SYSCALL:
    DB_PRINTK (KERN_ALERT "IF_CMD_SYSCALL--ioctl\n");
    rc = handle_syscall (&if_cmd.syscall_cmd);
    break;
  case IF_CMD_SWITCHPID:
    DB_PRINTK (KERN_ALERT "IF_CMD_SWITCHPID--ioctl\n");
    rc = handle_switchpid (&if_cmd.switchpid_cmd);
    break;
  case IF_CMD_ATTACH:
    DB_PRINTK (KERN_ALERT "IF_CMD_ATTACH--ioctl\n");
    rc = handle_attach (&if_cmd.attach_cmd);
    break;
  default:
    rc = -EINVAL;
    break;
  }
  return rc;
}
