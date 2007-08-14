#include <stdio.h>
#include <malloc.h>
#include <sys/ioctl.h>
#include <sys/user.h>
#include <sys/types.h>
#include <errno.h>
#include <limits.h>

#include <utracer.h>

// temporary
pid_t client_pid;
int utracer_cmd_file_fd;

void
utracer_set_environment (pid_t cp, int cf)
{
  client_pid = cp;
  utracer_cmd_file_fd = cf;
}


/************************** printmmap ********************/

static int
do_get_mmap (long pid,
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
				   (long)client_pid,
				   pid,
				   vssl_req,
				   vsl_req,
				   &vm_struct_subset_length,
				   &vm_strings_length,
				   *pr,
				   *vss,
				   *vs};
  irc = ioctl (utracer_cmd_file_fd,
	       sizeof(printmmap_cmd),
	       &printmmap_cmd);

  if (vssl_actual) *vssl_actual = vm_struct_subset_length;
  if (vsl_actual)  *vsl_actual = vm_strings_length;

  return irc;
}

int
utracer_get_printmmap (long pid,
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

  irc = do_get_mmap (pid, &pr, &vss, &vs, PAGE_SIZE, PAGE_SIZE, &vssl, &vsl);

  if (0 == irc) {
    if ((vssl > PAGE_SIZE) ||
	(vsl > PAGE_SIZE)) {
      if (pr)  free (pr);
      if (vss) free (vss);
      if (vs)  free (vs);
      irc = do_get_mmap (pid, &pr, &vss, &vs, vssl, vsl, NULL, NULL);
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
do_get_pids (long ** pids_p, long nr_pids_req, long *nr_pids_actual_p)
{
  int irc;
  long nr_pids_actual;

  if (!pids_p || !nr_pids_actual_p) return -EINVAL;

  *pids_p  = malloc (nr_pids_req * sizeof(long));

  listpids_cmd_s listpids_cmd = {IF_CMD_LIST_PIDS,
				 (long)client_pid,
				 nr_pids_req,
				 &nr_pids_actual,
				 *pids_p};
  irc = ioctl (utracer_cmd_file_fd,
	       sizeof(listpids_cmd_s),
	       &listpids_cmd);

  if (nr_pids_actual_p) *nr_pids_actual_p = nr_pids_actual;

  return irc;
}


int
utracer_get_pids (long * nr_pids_p, long ** pids_p)
{
  int irc;
  long nr_pids;
  long * pids;
  long nr_pids_actual;
  long nr_pids_req = PAGE_SIZE/sizeof(long);
  
  irc = do_get_pids (&pids, nr_pids_req, &nr_pids_actual);

  if (0 == irc) {
    if (nr_pids_actual > nr_pids_req) {
      if (pids)  free (pids);
      irc = do_get_pids (&pids, nr_pids_actual, NULL);
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
do_get_mem (long pid,
	    void ** mem_p,
	    void * addr,
	    long mem_req,
	    unsigned long * actual)
{
  int irc;

  if (!mem_p) return -EINVAL;

  *mem_p  = malloc (mem_req);

  getmem_cmd_s getmem_cmd = {IF_CMD_GETMEM,
			     (long)client_pid,
			     pid,
			     mem_req,
			     (long)addr,
			     *mem_p,
			     actual};
  irc = ioctl (utracer_cmd_file_fd,
	       sizeof(getmem_cmd_s),
	       &getmem_cmd);

  return irc;
}

int
utracer_get_mem (long pid, void * addr, unsigned long length,
		 void ** mem_p, unsigned long * actual)
{
  int irc;
  void * mem  = NULL;
  
  irc = do_get_mem (pid, &mem, addr, length, actual);

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
do_get_env (long pid,
	    char ** env_p,
	    long env_req,
	    long * env_actual)
{
  int irc;
  long env_length;

  if (!env_p) return -EINVAL;

  *env_p  = malloc (env_req);

  printenv_cmd_s printenv_cmd = {IF_CMD_PRINTENV,
				 (long)client_pid,
				 pid,
				 env_req,
				 &env_length,
				 *env_p};
  irc = ioctl (utracer_cmd_file_fd,
	       sizeof(printenv_cmd_s),
	       &printenv_cmd);

  if (env_actual) *env_actual = env_length;

  return irc;
}

int
utracer_get_env (long pid, char ** env_p)
{
  int irc;
  char * env  = NULL;
  long envl;
  
  irc = do_get_env (pid, &env, PAGE_SIZE, &envl);

  if (0 == irc) {
    if (envl > PAGE_SIZE) {
      if (env)  free (env);
      irc = do_get_env (pid, &env, envl, NULL);
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
utracer_get_regs (long pid, long regset, void ** regsinfo_p,
		  unsigned int * nr_regs_p, unsigned int * reg_size_p)
{
  int irc;
  long actual_size;
  void * regsinfo = malloc (PAGE_SIZE);
  readreg_cmd_s readreg_cmd = {IF_CMD_READ_REG,
			       (long)client_pid,
			       pid,
			       regset,
			       regsinfo,
			       PAGE_SIZE,
			       &actual_size,
			       nr_regs_p,
			       reg_size_p};

  irc = ioctl (utracer_cmd_file_fd, sizeof(readreg_cmd_s), &readreg_cmd);

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
utracer_set_syscall (short which, short cmd, long pid, long syscall)
{
  int irc;
  syscall_cmd_s syscall_cmd;

  syscall_cmd.cmd			= IF_CMD_SYSCALL;
  syscall_cmd.utracing_pid		= (long)client_pid;
  syscall_cmd.utraced_pid		= pid;
  syscall_cmd_which (&syscall_cmd)	= which;
  syscall_cmd_cmd (&syscall_cmd)	= cmd;
  syscall_cmd.syscall_nr		= syscall;

  irc = ioctl (utracer_cmd_file_fd, sizeof(syscall_cmd_s), &syscall_cmd);

  return irc;
}


/************************** sync  ********************/


int
utracer_sync (long type)
{
  int irc;
  sync_cmd_s sync_cmd = {IF_CMD_SYNC,
			 (long)client_pid, type};

  irc = ioctl (utracer_cmd_file_fd, sizeof(sync_cmd_s), &sync_cmd);

  return irc;
}


/************************** detach  ********************/


int
utracer_detach (long pid)
{
  int irc;
  attach_cmd_s attach_cmd = {IF_CMD_DETACH,
			     (long)client_pid, pid, 0, 0};

  irc = ioctl (utracer_cmd_file_fd, sizeof(attach_cmd_s), &attach_cmd);

  return irc;
}



/************************** attach  ********************/


int
utracer_attach (long pid, long quiesce, long exec_quiesce)
{
  int irc;
  attach_cmd_s attach_cmd = {IF_CMD_ATTACH,
			     (long)client_pid, pid, quiesce, exec_quiesce};

  irc = ioctl (utracer_cmd_file_fd, sizeof(attach_cmd_s), &attach_cmd);

  return irc;
}



/************************** quiesce  ********************/


int
utracer_run (long pid)
{
  int irc;
  run_cmd_s run_cmd = {IF_CMD_RUN,
		       (long)client_pid,
		       pid};

  irc = ioctl (utracer_cmd_file_fd, sizeof(run_cmd_s), &run_cmd);

  return irc;
}


int
utracer_quiesce (long pid)
{
  int irc;
  run_cmd_s run_cmd = {IF_CMD_QUIESCE,
		       (long)client_pid,
		       pid};

  irc = ioctl (utracer_cmd_file_fd, sizeof(run_cmd_s), &run_cmd);

  return irc;
}


/************************** switchpid  ********************/


int
utracer_switch_pid (long pid)
{
  int irc;
  switchpid_cmd_s switchpid_cmd = {IF_CMD_SWITCHPID,
				   (long)client_pid,
				   pid};

  irc = ioctl (utracer_cmd_file_fd, sizeof(switchpid_cmd_s), &switchpid_cmd);

  return irc;
}

/************************** printexe  ********************/


int
utracer_get_exe (long pid,
		 char ** filename_p,
		 char ** interp_p)
{
  int irc;
  char * filename = malloc (PATH_MAX);
  char * interp   = malloc (PATH_MAX);
  printexe_cmd_s printexe_cmd = {IF_CMD_PRINTEXE,
				 (long)client_pid,
				 pid,
				 filename,
				 PATH_MAX,
				 interp,
				 PATH_MAX};

  irc = ioctl (utracer_cmd_file_fd, sizeof(printexe_cmd_s), &printexe_cmd);

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
