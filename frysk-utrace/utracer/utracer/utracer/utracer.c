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

#define _GNU_SOURCE
#include <stdio.h>
#include <malloc.h>
#include <sys/ioctl.h>
#include <sys/user.h>
#include <sys/types.h>
#include <errno.h>
#include <limits.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>

#include <utracer.h>
#include <utracer-errmsgs.h>

//fixme -- does this need to be reentrant?
int cmd_file_fd;
int resp_file_fd;
int ctl_file_fd;

static int
utracer_unregister (long pid)
{
  int irc;
  register_cmd_s register_cmd = {CTL_CMD_UNREGISTER, pid};

  irc = ioctl (ctl_file_fd, sizeof(register_cmd_s), &register_cmd);

  return irc;
}


ssize_t
utracer_read (if_resp_u * if_resp, void ** extra)
{
  ssize_t sz;

  sz = pread (resp_file_fd, if_resp, sizeof(if_resp_u), 0);

  if (extra) {
    switch (if_resp->type) {
    case IF_RESP_EXEC_DATA:
      {
	exec_resp_s exec_resp = if_resp->exec_resp;
	long bytes_to_get = exec_resp.data_length;
	long bytes_gotten = 0;
	char * cstr = malloc (bytes_to_get + 2);

	if (sz > sizeof(exec_resp)) {
	  long bytes_avail =  sz - sizeof(exec_resp);
	  memcpy ((void *)cstr, (void *)(if_resp) + sizeof (exec_resp),
		  bytes_avail);
	  bytes_to_get -= bytes_avail;
	  bytes_gotten = bytes_avail;
	}

	if (0 < bytes_to_get) {
	  sz = pread (resp_file_fd,
		      (void *)cstr + bytes_gotten,
		      bytes_to_get, sz);
	  bytes_gotten += sz;
	}
	if (extra) *extra = cstr;
	else free (cstr);
      }
      break;
    case IF_RESP_SYSCALL_ENTRY_DATA:
    case IF_RESP_SYSCALL_EXIT_DATA:
      {
	// fixme -- handle /proc/<pid>/mem to access ptr args
	syscall_resp_s syscall_resp = if_resp->syscall_resp;
	long bytes_to_get = syscall_resp.data_length;
	long bytes_gotten = 0;
	struct pt_regs * regs = malloc (bytes_to_get);

	if (sz > sizeof(syscall_resp)) {
	  long bytes_avail =  sz - sizeof(syscall_resp);
	  memcpy ((void *)regs, (void *)(if_resp) + sizeof (syscall_resp),
		  bytes_avail);
	  bytes_to_get -= bytes_avail;
	  bytes_gotten = bytes_avail;
	}

	if (0 < bytes_to_get) {
	  sz = pread (resp_file_fd,
		      (void *)regs + bytes_gotten,
		      bytes_to_get, sz);
	}
	if (extra) *extra = regs;
	else free (regs);
      }
      break;
    }
  }

  return sz;
}

void
utracer_uerror(const char * s)
{
  if ((UTRACER_EBASE <= errno) && (errno < UTRACER_EMAX)) {
    fprintf (stderr, "%s: %s\n", s, utrace_emsg[errno - UTRACER_EBASE]);
  }
  else perror (s);
}

int
utracer_wait(pid_t client_pid)
{
  int i;
  int rc = 0;
#define CHECKS_NR	3

  for (i = 0; i < CHECKS_NR; i++) {
    if_resp_u if_resp;
    ssize_t sz;
      
    rc = utracer_sync (client_pid, SYNC_WAIT);

    if (0 == rc) {
      LOGIT ("starting utracer_wait pread pass %d fd %d\n",
	     i, resp_file_fd);
      sz = pread (resp_file_fd, &if_resp,
		  sizeof(if_resp), 0);
      LOGIT ("got utracer_wait pread, sz = %d\n", sz);
      if (-1 != sz) {
	if (IF_RESP_SYNC_DATA == if_resp.type){
	  sync_resp_s sync_resp = if_resp.sync_resp;
	  if (SYNC_WAIT == sync_resp.sync_type) break;
	}
      }
      else {
	rc = -1;
	break;
      }
    }
  }

  if ((CHECKS_NR == i) && (0 == rc)) {
    errno = UTRACER_EWAIT;
    rc = -1;
  }

  return rc;
}

static void
utracer_close_ctl_file()
{
  if (-1 != ctl_file_fd) {
    close (ctl_file_fd);
    ctl_file_fd = -1;
  }
}

static void
utracer_cleanup()
{
  if (-1 != cmd_file_fd) {
    close (cmd_file_fd);
    cmd_file_fd = -1;
  }
  if (-1 != resp_file_fd) {
    close (resp_file_fd);
    resp_file_fd = -1;
  }
}

void
utracer_close(long pid)
{
  utracer_cleanup();
  utracer_unregister (pid);
#if 0 // probably shouldn't
  utracer_close_ctl_file();
#endif
}

long
utracer_open (void)
{
  long rc_pid;
  char * cfn;
  int irc;
  
  rc_pid = (long)getpid();

  asprintf (&cfn, "/proc/%s/%s", UTRACER_BASE_DIR, UTRACER_CONTROL_FN);
  LOGIT ("opening %s\n", cfn);
  ctl_file_fd = open (cfn, O_RDWR);
  LOGIT ("   fd = %d\n", ctl_file_fd);
  free (cfn);
  if (-1 == ctl_file_fd) {
    perror ("Error opening control file");
    return -1;
  }
  
  LOGIT ("calling utracer_register()\n");
  irc = utracer_register (rc_pid);
  LOGIT ("returning from utracer_register(), irc = %d\n", irc);
  if (0 > irc) {
    utracer_uerror ("Initial registration");
    close (ctl_file_fd);
    return -1;
  }

  asprintf (&cfn, "/proc/%s/%ld/%s", UTRACER_BASE_DIR,
	    rc_pid, UTRACER_CMD_FN);
  LOGIT ("opening %s\n", cfn);
  cmd_file_fd = open (cfn, O_RDWR);
  LOGIT ("   fd = %d\n", cmd_file_fd);
  free (cfn);
  if (-1 == cmd_file_fd) {
    utracer_unregister (rc_pid);
    close (ctl_file_fd);
    utracer_uerror ("Error opening command file");
    return -1;
  }
    
  asprintf (&cfn, "/proc/%s/%ld/%s", UTRACER_BASE_DIR,
	    rc_pid, UTRACER_RESP_FN);
  LOGIT ("opening %s\n", cfn);
  resp_file_fd = open (cfn, O_RDONLY);
  LOGIT ("   fd = %d\n", resp_file_fd);
  free (cfn);
  if (-1 == resp_file_fd) {
    utracer_unregister (rc_pid);
    close (ctl_file_fd);
    close (cmd_file_fd);
    utracer_uerror ("Error opening command file");
    return -1;
  }

  LOGIT ("leaving utracer_open()\n");

  return rc_pid;
}


/************************** printmmap ********************/

static int
do_get_mmap (long client_pid,
	     long pid,
	     printmmap_resp_s ** pr,
	     vm_struct_subset_s ** vss,
	     char ** vs,
	     long vssl_req,
	     long vsl_req,
	     long * vssl_actual,
	     long * vsl_actual)
{
  int irc;
  long vm_struct_subset_length;
  long vm_strings_length;

  if (!pr || ! vss || !vs) return -EINVAL;

  *pr  = malloc (sizeof(printmmap_resp_s));
  *vss = malloc (vssl_req);
  *vs  = malloc (vsl_req);
  
  printmmap_cmd_s printmmap_cmd = {IF_CMD_PRINTMMAP,
				   client_pid,
				   pid,
				   vssl_req,
				   vsl_req,
				   &vm_struct_subset_length,
				   &vm_strings_length,
				   *pr,
				   *vss,
				   *vs};
  irc = ioctl (cmd_file_fd,
	       sizeof(printmmap_cmd),
	       &printmmap_cmd);

  if (vssl_actual) *vssl_actual = vm_struct_subset_length;
  if (vsl_actual)  *vsl_actual = vm_strings_length;

  return irc;
}

int
utracer_get_mmap (long client_pid,
		  long pid,
		  printmmap_resp_s ** printmmap_resp_p,
		  vm_struct_subset_s ** vm_struct_subset_p,
		  char ** vm_strings_p)
{
  int irc;
  printmmap_resp_s * pr = NULL;
  vm_struct_subset_s * vss = NULL;
  char * vs = NULL;
  long vssl;
  long vsl;

  irc = do_get_mmap (client_pid,
		     pid,
		     &pr,
		     &vss,
		     &vs,
		     PAGE_SIZE,
		     PAGE_SIZE,
		     &vssl,
		     &vsl);

  if (0 == irc) {
    if ((vssl > PAGE_SIZE) ||
	(vsl > PAGE_SIZE)) {
      if (pr)  free (pr);
      if (vss) free (vss);
      if (vs)  free (vs);
      irc = do_get_mmap (client_pid, pid, &pr, &vss, &vs,
			 vssl, vsl, NULL, NULL);
    }
  }
  
  if  (0 != irc) {
    if (pr)  free (pr);
    if (vss) free (vss);
    if (vs)  free (vs);
    if (printmmap_resp_p)       *printmmap_resp_p	= NULL;
    if (vm_struct_subset_p)	*vm_struct_subset_p	= NULL;
    if (vm_strings_p)		*vm_strings_p		= NULL;
  }
  else {
    if (printmmap_resp_p)	*printmmap_resp_p	= pr;
    if (vm_struct_subset_p)	*vm_struct_subset_p	= vss;
    if (vm_strings_p)		*vm_strings_p		= vs;
  }

  return irc;
}


/************************** listpids  ********************/

static int
do_get_pids (long client_pid, long ** pids_p,
	     long nr_pids_req, long *nr_pids_actual_p)
{
  int irc;
  long nr_pids_actual;

  if (!pids_p || !nr_pids_actual_p) return -EINVAL;

  *pids_p  = malloc (nr_pids_req * sizeof(long));

  listpids_cmd_s listpids_cmd = {IF_CMD_LIST_PIDS,
				 client_pid,
				 nr_pids_req,
				 &nr_pids_actual,
				 *pids_p};
  irc = ioctl (cmd_file_fd,
	       sizeof(listpids_cmd_s),
	       &listpids_cmd);

  if (nr_pids_actual_p) *nr_pids_actual_p = nr_pids_actual;

  return irc;
}


int
utracer_get_pids (long client_pid, long * nr_pids_p, long ** pids_p)
{
  int irc;
  long nr_pids;
  long * pids;
  long nr_pids_actual;
  long nr_pids_req = PAGE_SIZE/sizeof(long);
  
  irc = do_get_pids (client_pid, &pids, nr_pids_req, &nr_pids_actual);

  if (0 == irc) {
    if (nr_pids_actual > nr_pids_req) {
      if (pids)  free (pids);
      irc = do_get_pids (client_pid, &pids, nr_pids_actual, NULL);
    }
  }
  
  if  (0 != irc) {
    if (pids)  free (pids);
    if (pids_p) *pids_p = NULL;
  }
  else {
    if (pids_p)		*pids_p = pids;
    if (nr_pids_p)	*nr_pids_p = nr_pids_actual;
  }

  return irc;
}


/************************** printenv  ********************/


static int
do_get_mem (long client_pid,
	    long pid,
	    void ** mem_p,
	    void * addr,
	    long mem_req,
	    unsigned long * actual)
{
  int irc;

  if (!mem_p) return -EINVAL;

  *mem_p  = malloc (mem_req);

  getmem_cmd_s getmem_cmd = {IF_CMD_GETMEM,
			     client_pid,
			     pid,
			     mem_req,
			     (long)addr,
			     *mem_p,
			     actual};
  irc = ioctl (cmd_file_fd,
	       sizeof(getmem_cmd_s),
	       &getmem_cmd);

  return irc;
}

int
utracer_get_mem (long client_pid, long pid, void * addr, unsigned long length,
		 void ** mem_p, unsigned long * actual)
{
  int irc;
  void * mem  = NULL;
  
  irc = do_get_mem (client_pid, pid, &mem, addr, length, actual);

  if  (0 != irc) {
    if (mem)  free (mem);
    if (mem_p) *mem_p = NULL;
  }
  else {
    if (mem_p) *mem_p = mem;
  }

  return irc;
}



/************************** printenv  ********************/


static int
do_get_env (long client_pid,
	    long pid,
	    char ** env_p,
	    long env_req,
	    long * env_actual)
{
  int irc;
  long env_length;

  if (!env_p) return -EINVAL;

  *env_p  = malloc (env_req);

  printenv_cmd_s printenv_cmd = {IF_CMD_PRINTENV,
				 client_pid,
				 pid,
				 env_req,
				 &env_length,
				 *env_p};
  irc = ioctl (cmd_file_fd,
	       sizeof(printenv_cmd_s),
	       &printenv_cmd);

  if (env_actual) *env_actual = env_length;

  return irc;
}

int
utracer_get_env (long client_pid, long pid, char ** env_p)
{
  int irc;
  char * env  = NULL;
  long envl;
  
  irc = do_get_env (client_pid, pid, &env, PAGE_SIZE, &envl);

  if (0 == irc) {
    if (envl > PAGE_SIZE) {
      if (env)  free (env);
      irc = do_get_env (client_pid, pid, &env, envl, NULL);
    }
  }
  
  if  (0 != irc) {
    if (env)  free (env);
    if (env_p) *env_p = NULL;
  }
  else {
    long i;
    for (i = 0; i < envl; i++) {
      if (0 == env[i]) env[i] = '\n';
    }
    if (env_p) *env_p = env;
  }

  return irc;
}


/************************** get regs  ********************/


int
utracer_get_regs (long client_pid, long pid, long regset, void ** regsinfo_p,
		  unsigned int * nr_regs_p, unsigned int * reg_size_p)
{
  int irc;
  long actual_size;
  void * regsinfo = malloc (PAGE_SIZE);
  readreg_cmd_s readreg_cmd = {IF_CMD_READ_REG,
			       client_pid,
			       pid,
			       regset,
			       regsinfo,
			       PAGE_SIZE,
			       &actual_size,
			       nr_regs_p,
			       reg_size_p};

  irc = ioctl (cmd_file_fd, sizeof(readreg_cmd_s), &readreg_cmd);

  if  (0 != irc) {
    if (regsinfo)	free (regsinfo);
    if (regsinfo_p)	*regsinfo_p	= NULL;
  }
  else {
    if (regsinfo_p)	*regsinfo_p	= regsinfo;
  }

  return irc;
}


/************************** printexe  ********************/


int
utracer_set_syscall (long client_pid, short which, short cmd,
		     long pid, long syscall)
{
  int irc;
  syscall_cmd_s syscall_cmd;

  syscall_cmd.cmd			= IF_CMD_SYSCALL;
  syscall_cmd.utracing_pid		= client_pid;
  syscall_cmd.utraced_pid		= pid;
  syscall_cmd_which (&syscall_cmd)	= which;
  syscall_cmd_cmd (&syscall_cmd)	= cmd;
  syscall_cmd.syscall_nr		= syscall;

  irc = ioctl (cmd_file_fd, sizeof(syscall_cmd_s), &syscall_cmd);

  return irc;
}


/************************** sync  ********************/


int
utracer_sync (long client_pid, long type)
{
  int irc;
  sync_cmd_s sync_cmd = {IF_CMD_SYNC,
			 client_pid, type};

  LOGIT ("in utracer_sync(), pid = %d type = %d\n",
	 client_pid, type);

  irc = ioctl (cmd_file_fd, sizeof(sync_cmd_s), &sync_cmd);

  return irc;
}


/************************** detach  ********************/


int
utracer_detach (long client_pid, long pid)
{
  int irc;
  attach_cmd_s attach_cmd = {IF_CMD_DETACH,
			     client_pid, pid, 0, 0};

  irc = ioctl (cmd_file_fd, sizeof(attach_cmd_s), &attach_cmd);

  return irc;
}



/************************** attach  ********************/


int
utracer_attach (long client_pid, long pid, long quiesce, long exec_quiesce)
{
  int irc;
  attach_cmd_s attach_cmd = {IF_CMD_ATTACH,
			     client_pid, pid, quiesce, exec_quiesce};

  irc = ioctl (cmd_file_fd, sizeof(attach_cmd_s), &attach_cmd);

  return irc;
}



/************************** quiesce  ********************/


int
utracer_run (long client_pid, long pid)
{
  int irc;
  run_cmd_s run_cmd = {IF_CMD_RUN,
		       client_pid,
		       pid};

  irc = ioctl (cmd_file_fd, sizeof(run_cmd_s), &run_cmd);

  return irc;
}


int
utracer_quiesce (long client_pid, long pid)
{
  int irc;
  run_cmd_s run_cmd = {IF_CMD_QUIESCE,
		       client_pid,
		       pid};

  irc = ioctl (cmd_file_fd, sizeof(run_cmd_s), &run_cmd);

  return irc;
}


/************************** register  ********************/


int
utracer_register (long pid)
{
  int irc;
  register_cmd_s register_cmd = {CTL_CMD_REGISTER, pid};

  irc = ioctl (ctl_file_fd, sizeof(register_cmd_s), &register_cmd);

  return irc;
}


/************************** switchpid  ********************/


int
utracer_check_pid (long client_pid, long pid)
{
  int irc;
  checkpid_cmd_s checkpid_cmd = {IF_CMD_CHECKPID,
				 client_pid,
				 pid};

  irc = ioctl (cmd_file_fd, sizeof(checkpid_cmd_s), &checkpid_cmd);

  return irc;
}

/************************** printexe  ********************/


int
utracer_get_exe (long client_pid,
		 long pid,
		 char ** filename_p,
		 char ** interp_p)
{
  int irc;
  char * filename = malloc (PATH_MAX);
  char * interp   = malloc (PATH_MAX);
  printexe_cmd_s printexe_cmd = {IF_CMD_PRINTEXE,
				 client_pid,
				 pid,
				 filename,
				 PATH_MAX,
				 interp,
				 PATH_MAX};

  irc = ioctl (cmd_file_fd, sizeof(printexe_cmd_s), &printexe_cmd);

  if  (0 != irc) {
    if (filename)	free (filename);
    if (interp)		free (interp);
    if (filename_p)	*filename_p	= NULL;
    if (interp_p)	*interp_p	= NULL;
  }
  else {
    if (filename_p)	*filename_p	= filename;
    if (interp_p)	*interp_p	= interp;
  }

  return irc;
}
