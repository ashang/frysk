#include <linux/module.h>
#include <linux/moduleparam.h>
#include <linux/kernel.h>
#include <linux/fs.h>
#include <linux/mm.h>
#include <linux/highmem.h>
#include <asm/uaccess.h>

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

	if (len > PAGE_SIZE) len = (len + (PAGE_SIZE - 1))/PAGE_SIZE;
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
	  if (copy_to_user (printenv_cmd->length_returned, &llen,
			    sizeof(long)))
	    rc = -EFAULT;
	  else {
	    if (copy_to_user (printenv_cmd->buffer,
			      buffer, len))
	      rc = -EFAULT;
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
    rc = handle_listpids (&if_cmd.listpids_cmd);
    break;
  case IF_CMD_PRINTMMAP:
    rc = handle_printmap (&if_cmd.printmmap_cmd);
    break;
  case IF_CMD_PRINTENV:
    rc = handle_printenv (&if_cmd.printenv_cmd);
    break;
  case IF_CMD_PRINTEXE:
    rc = handle_printexe (&if_cmd.printexe_cmd);
    break;
  default:
    rc = -EINVAL;
    break;
  }
  return rc;
}
