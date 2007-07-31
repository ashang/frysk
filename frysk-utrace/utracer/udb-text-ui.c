#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <readline/readline.h>
#include <readline/history.h>
#include <error.h>
#include <errno.h>
#include <unistd.h>
#include <string.h>
#include <search.h>

#include <utracer.h>
#include "udb.h"
#include "udb-i386.h"


/*
 * TTD
 *
 * print regset info
 * allow regset spec in printreg
 */

static struct hsearch_data cmd_hash_table;
static int cmd_hash_table_valid = 0;

typedef int (*action_fcn)(char ** saveptr);

void
text_ui_terminate()
{
  if (cmd_hash_table_valid) hdestroy_r(&cmd_hash_table);
}

static int
syscall_fcn(char ** saveptr)
{
  int run = 1;
  int got_it = 0;
  char * tok;
  long syscall_nr = SYSCALL_INVALID;
  enum {SY_STATE_A1, SY_STATE_A2, SY_STATE_A3} sy_state = SY_STATE_A1;
  enum {SY_MODE_NULL, SY_MODE_ENTRY, SY_MODE_EXIT} sy_mode = SY_MODE_NULL;
  enum {SY_ENABLE_NULL, SY_ENABLE_ON, SY_ENABLE_OFF}
    sy_enable = SY_ENABLE_NULL;
  enum {SY_ADD_NULL, SY_ADD_ADD, SY_ADD_REMOVE} sy_add = SY_ADD_NULL;
  long sy_nr;

  /*
   * sy[scall] {en[try] | ex[it]} (e(nable) | d(isable))
   *                              (a(dd) | r(emove)) (<nr> | <symbol>)
   *
   */

  while (run && (tok = strtok_r (NULL, " \t", saveptr))) {
    switch(sy_state) {
    case SY_STATE_A1:
      if (0 == strncasecmp (tok, "entry", 2)) {
	sy_mode = SY_MODE_ENTRY;
	sy_state = SY_STATE_A2;
      }
      else if (0 == strncasecmp (tok, "exit", 2)) {
	sy_mode = SY_MODE_EXIT;
	sy_state = SY_STATE_A2;
      }
      else run = 0;
      break;
    case SY_STATE_A2:
      if (0 == strncasecmp (tok, "enable", 1)) {
	sy_enable = SY_ENABLE_ON;
	got_it = 1;
	run = 0;
      }
      else if (0 == strncasecmp (tok, "disable", 1)) {
	sy_enable = SY_ENABLE_OFF;
	got_it = 1;
	run = 0;
      }
      if (0 == strncasecmp (tok, "add", 1)) {
	sy_add = SY_ADD_ADD;
	sy_state = SY_STATE_A3;
      }
      else if (0 == strncasecmp (tok, "remove", 1)) {
	sy_add = SY_ADD_REMOVE;
	sy_state = SY_STATE_A3;
      }
      else run = 0;
      break;
    case SY_STATE_A3:
      syscall_nr = parse_syscall (tok);
      if (SYSCALL_INVALID != syscall_nr) got_it = 1;
      break;
    }
  }
    
  if (got_it) {
    switch (sy_state) {
    case SY_STATE_A2:
      utracer_set_syscall (((SY_MODE_ENTRY == sy_mode) ?
			    SYSCALL_CMD_ENTRY : SYSCALL_CMD_EXIT),
			   ((SY_ENABLE_ON == sy_enable) ?
			    SYSCALL_CMD_ENABLE : SYSCALL_CMD_DISABLE),
			   current_pid,
			   0);
      break;
    case SY_STATE_A3:
      utracer_set_syscall (((SY_MODE_ENTRY == sy_mode) ?
			    SYSCALL_CMD_ENTRY : SYSCALL_CMD_EXIT),
			   ((SY_ADD_ADD == sy_add) ?
			    SYSCALL_CMD_ADD : SYSCALL_CMD_REMOVE),
			   current_pid,
			   syscall_nr);
      break;
    }
  }
  else
    fprintf (stderr, "\tSorry, I've no clue what you want me to do.\n");
  
  return 1;
}

static int
watch_fcn(char ** saveptr)
{
  long pid = -1;
  char * pid_c = strtok_r (NULL, " \t", saveptr);
  pid = pid_c ? atol (pid_c) : current_pid;
  if (-1 != pid) utrace_attach_if (pid, 0, 0);
  else fprintf (stderr, "\twatch requires an argument\n");
  return 1;
}

static int
attach_fcn(char ** saveptr)
{
  long pid = -1;
  char * pid_c = strtok_r (NULL, " \t", saveptr);
  pid = pid_c ? atol (pid_c) : current_pid;
  if (-1 != pid) utrace_attach_if (pid, 1, 0);
  else fprintf (stderr, "\tattach requires an argument\n");
  return 1;
}

static int
run_fcn(char ** saveptr)
{
  long pid = -1;
  char * pid_c = strtok_r (NULL, " \t", saveptr);
  pid = pid_c ? atol (pid_c) : current_pid;
  if (-1 != pid) utrace_run_if (pid);
  else fprintf (stderr, "\trun requires an argument\n");
  return 1;
}

static int
quiesce_fcn(char ** saveptr)
{
  long pid = -1;
  char * pid_c = strtok_r (NULL, " \t", saveptr);
  pid = pid_c ? atol (pid_c) : current_pid;
  if (-1 != pid) utrace_quiesce_if (pid);
  else fprintf (stderr, "\tquiesce requires an argument\n");
  return 1;
}

static int
detach_fcn(char ** saveptr)
{
  long pid = -1;
  char * pid_c = strtok_r (NULL, " \t", saveptr);
  pid = pid_c ? atol (pid_c) : current_pid;
  if (-1 != pid) utrace_detach_if (pid);
  else fprintf (stderr, "\tdetach requires an argument\n");
  return 1;
}

static int
step_fcn (char ** saveptr)
{
  long sc = 1;
  char * sc_c = strtok_r (NULL, " \t", saveptr);
  if (sc_c) sc = atol (sc_c);
  fprintf (stderr, "step_fcn %ld\n", sc);
  return 1;
}

static int
switchpid_fcn (char ** saveptr)
{
  long pid;
  char * pid_c = strtok_r (NULL, " \t", saveptr);
  if (pid_c) {
    pid = atol (pid_c);
    utrace_switchpid_if (pid, 0);
  }
  else fprintf (stderr, "\tswitchpid requires an argument\n");
  return 1;
}

static int
printreg_fcn (char ** saveptr)
{
  int rc;
  char * tok;
  unsigned int nr_regs;
  unsigned int reg_size;
  long pid = current_pid;
  long reg = INVALID_REG;
  long regset = 0;
  void * regsinfo = NULL;

  if (parse_regspec (saveptr, &pid, &regset, &reg)) {
    if (INVALID_REG == reg) {
      reg = -1;	// turn into pra
      regset = 0;
    }

    rc = utracer_get_regs (pid, regset, &regsinfo, &nr_regs, &reg_size);
    if (0 == rc) show_regs (pid, regset, reg, regsinfo, nr_regs, reg_size);
    else uerror ("printreg");

    if (regsinfo) free (regsinfo);
  }

  return 1;
}

static int
printregall_fcn (char ** saveptr)
{
  int rc;
  unsigned int nr_regs;
  unsigned int reg_size;
  void * regsinfo = NULL;
  long pid = current_pid;
  char * tok = strtok_r (NULL, " \t", saveptr);

  if (tok && ('[' == *tok)) pid = atol (tok+1);

  // fixme allow selection of regset
  rc = utracer_get_regs (pid, 0, &regsinfo, &nr_regs, &reg_size);
  if (0 == rc) show_regs (pid, 0, -1, regsinfo, nr_regs, reg_size);
  else uerror ("printreg");

  if (regsinfo) free (regsinfo);

  return 1;
}

static void
handle_printmmap (printmmap_resp_s * prm,
		  vm_struct_subset_s * vss,
		  char * estrings)
{
  fprintf (stdout, "\n\t[%ld] mmap\n",prm->utraced_pid);
  fprintf (stdout, "\t\t%08lx mmap base\n", prm->mmap_base);
  fprintf (stdout, "\t\t%08lx task size\n", prm->task_size);
  fprintf (stdout, "\t\t%08lx def flags\n\t\t%8ld nr ptes\n",
	   prm->def_flags, prm->nr_ptes);
	
  fprintf (stdout,
	   "\n\t\tVM:  total    locked   shared   exec     stack\
    reserved\n\t\t     ");
  fprintf (stdout, "%-8ld ",prm->total_vm);
  fprintf (stdout, "%-8ld ",prm->locked_vm);
  fprintf (stdout, "%-8ld ",prm->shared_vm);
  fprintf (stdout, "%-8ld ",prm->exec_vm);
  fprintf (stdout, "%-8ld ",prm->stack_vm);
  fprintf (stdout, "%-8ld ",prm->reserved_vm);
  fprintf (stdout, "\n");

  fprintf (stdout, "\n\t\tCode ranges:\n");
  fprintf (stdout, "\t\t  %08lx - %08lx code (length = %ld) \n",
	   prm->start_code, prm->end_code, prm->end_code - prm->start_code);
  fprintf (stdout, "\t\t  %08lx - %08lx data (length = %ld)\n",
	   prm->start_data, prm->end_data, prm->end_data - prm->start_data);
  fprintf (stdout, "\t\t  %08lx - %08lx brk  (length = %ld) \n",
	   prm->start_brk, prm->brk, prm->brk - prm->start_brk);
  fprintf (stdout, "\t\t  %08lx  stack\n",
	   prm->start_stack);

  if (0 < prm->nr_mmaps) {
    int i;

    fprintf (stdout, "\n\t\tMemory maps:\n\t\t  start\
      end      flags    pathname\n");

    for (i = 0; i < prm->nr_mmaps; i++) {
      char * fn1;

      if (-1 != vss[i].name_offset)
	fn1 = &estrings[vss[i].name_offset];
      else {
	if ((vss[i].vm_start <= prm->start_brk) &&
	    (vss[i].vm_end   >= prm->brk)) fn1 = "[heap]";
	else if ((vss[i].vm_start <= prm->start_stack) &&
		 (vss[i].vm_end   >= prm->start_stack)) fn1 = "[stack]";
	else fn1 = "[vdso]";
      }
	    
      fprintf (stdout, "\t\t  %08x - %08x %08x %s\n",
	       vss[i].vm_start,
	       vss[i].vm_end,
	       vss[i].vm_flags,
	       fn1);
    }
  }
}

static int
printmmap_fcn (char ** saveptr)
{
  printmmap_resp_s * printmmap_resp;
  vm_struct_subset_s * vm_struct_subset;
  char * vm_strings;
  int rc;
  
  long pid = current_pid;
  char * tok = strtok_r (NULL, " \t", saveptr);

  if (tok && ('[' == *tok)) pid = atol (tok+1);
  rc = utracer_get_printmmap (pid,
			      &printmmap_resp, &vm_struct_subset, &vm_strings);
  if (0 == rc) 
    handle_printmmap (printmmap_resp, vm_struct_subset, vm_strings);
  else uerror ("printmmap");

  if (printmmap_resp)	free (printmmap_resp);
  if (vm_struct_subset)	free (vm_struct_subset);
  if (vm_strings)	free (vm_strings);
  
  return 1;
}

static int
printenv_fcn (char ** saveptr)
{
  char * env;
  int rc;
  long pid = current_pid;
  char * tok = strtok_r (NULL, " \t", saveptr);

  if (tok && ('[' == *tok)) pid = atol (tok+1);
  rc = utracer_get_env (pid, &env);
  if (0 == rc) fprintf (stdout, "%s\n", env);
  else uerror ("printenv");
  return 1;
}

static int
listpids_fcn(char ** saveptr)
{
  int rc;
  long nr_pids = 0;
  long * pids = NULL;
  
  rc = utracer_get_pids (&nr_pids, &pids);
  if (pids && (0 == rc)) {
    long i;
    
    for (i = 0; i < nr_pids; i++) fprintf (stdout, "\t%ld\n", pids[i]);
  }
  else uerror ("printenv");

  if (pids) free (pids);
  return 1;
}

static int
printexe_fcn (char ** saveptr)
{
  char * filename = NULL;
  char * interp = NULL;
  int rc;
  long pid = current_pid;
  char * tok = strtok_r (NULL, " \t", saveptr);

  if (tok && ('[' == *tok)) pid = atol (tok+1);
  rc = utracer_get_exe (pid, &filename, &interp);
  if (0 == rc) {
    fprintf (stdout, "\t filename: \"%s\"\n", filename);
    fprintf (stdout, "\t   interp: \"%s\"\n", interp);
  }
  else uerror ("printexe");

  if (filename)	free (filename);
  if (interp)	free (interp);
  
  return 1;
}

static int
printmem_fcn (char ** saveptr)
{
  int rc;
  long bffr_length_actual;
  long pid = current_pid;
  void * bffr = NULL;
  unsigned long addr = -1;
  unsigned long bffr_length_req = 0;
  char * tok = strtok_r (NULL, " \t", saveptr);

  if (tok && ('[' == *tok)) {
    pid = atol (tok+1);
    tok = strtok_r (NULL, " \t", saveptr);
  }
  if (tok) {
    addr = strtoul (tok, NULL, 0);
    tok = strtok_r (NULL, " \t", saveptr);
  }
  if (tok) bffr_length_req = strtoul (tok, NULL, 0);

  if ((0 == addr) || (0 == bffr_length_req)) {
    fprintf (stderr,
	     "Either the requested address or the \
requested length are invlaid.\n");
    return 1;
  }

  fprintf (stderr, "addr = %08x, len = %lu\n", addr, bffr_length_req);
  rc = utracer_get_mem (pid, (void *)addr, bffr_length_req, &bffr,
			&bffr_length_actual);
  
  if (0 == rc) {
    int i;
    fprintf (stdout, "str = \"%.*s\"\n", bffr_length_req, (char *)bffr);

    for (i = 0; i < bffr_length_req; i++)
      fprintf (stdout, "%02x ", (int)(((char *)bffr)[i]));
    fprintf (stdout, "\n");
  }
  else uerror ("printmem");

  if (bffr) free (bffr);
  
  return 1;
}

static int
quit_fcn (char ** saveptr)
{
  // fixme -- have quit do more than end the cmd loop when don from -c option
  return 0;
}

void
set_prompt()
{
  if (prompt) free (prompt);
  asprintf (&prompt, "udb [%ld] >", current_pid);
}

typedef struct {
  int (*cmd_fcn)(char ** saveptr);
  char * desc;
} cmd_info_s;

static cmd_info_s printexe_info =
  {printexe_fcn, "(pexe) -- Print the user-supplied and interpreted \
filenames of the binarie of the given PID."};
static cmd_info_s printexe_info_brief =
  {printexe_fcn, NULL};

static cmd_info_s printmem_info =
  {printmem_fcn, "(pm) -- Print the contents of the specified address for \
the specified length of the given PID."};
static cmd_info_s printmem_info_brief =
  {printmem_fcn, NULL};

static cmd_info_s printenv_info =
  {printenv_fcn, "(penv) -- Print the environment the given PID."};
static cmd_info_s printenv_info_brief =
  {printenv_fcn, NULL};

static cmd_info_s attach_info =
  {attach_fcn, "(at) -- Attach and quiesce the given PID."};
static cmd_info_s attach_info_brief =
  {attach_fcn, NULL};

static cmd_info_s quit_info =
  {quit_fcn, "(bye, q) -- Quit the debugger."};
static cmd_info_s quit_info_brief =
  {quit_fcn, NULL};

static cmd_info_s detach_info =
  {detach_fcn, "(det) -- Detach the given PID."};
static cmd_info_s detach_info_brief =
  {detach_fcn, NULL};

static cmd_info_s printreg_info =
  {printreg_fcn, "(pr) -- Print the value of the specified register."};
static cmd_info_s printreg_info_brief =
  {printreg_fcn, NULL};

static cmd_info_s printmmap_info =
  {printmmap_fcn, "(prm) -- Print the memory map of the current task."};
static cmd_info_s printmmap_info_brief =
  {printmmap_fcn, NULL};

static cmd_info_s printregall_info =
  {printregall_fcn, "(pra) -- Print the values of all the registers."};
static cmd_info_s printregall_info_brief =
  {printregall_fcn, NULL};

static cmd_info_s quiesce_info =
  {quiesce_fcn, "(halt) -- Quiesce the specified PID."};
static cmd_info_s quiesce_info_brief =
  {quiesce_fcn, NULL};

static cmd_info_s run_info =
  {run_fcn, "(r, c) -- Continue the specified PID."};
static cmd_info_s run_info_brief =
  {run_fcn, NULL};

static cmd_info_s step_info =
  {step_fcn, "(s) -- Single-step the specified PID."};
static cmd_info_s step_info_brief =
  {step_fcn, NULL};

static cmd_info_s switchpid_info =
  {switchpid_fcn, "(sw) -- Switch to the specified PID."};
static cmd_info_s switchpid_info_brief =
  {switchpid_fcn, NULL};

static cmd_info_s listpids_info =
  {listpids_fcn, "(lp) -- List the attached PIDs."};
static cmd_info_s listpids_info_brief =
  {listpids_fcn, NULL};

static cmd_info_s watch_info =
  {watch_fcn, "(w) -- Watch the specified PID.  (Attach w/o quiesce.)"};
static cmd_info_s watch_info_brief =
  {watch_fcn, NULL};

static cmd_info_s syscall_info =
  {syscall_fcn, "(sy) -- Control syscall reporting.)"};
static cmd_info_s syscall_info_brief =
  {syscall_fcn, NULL};

static ENTRY cmds[] = {
  {"at",		&attach_info_brief},
  {"attach",		&attach_info},
  
  {"det",		&detach_info_brief},
  {"detach",		&detach_info},
  
  {"q",			&quit_info_brief},
  {"bye",		&quit_info_brief},
  {"quit",		&quit_info},
  
  {"pr",		&printreg_info_brief},
  {"printreg",		&printreg_info},
  
  {"pm",		&printmem_info_brief},
  {"printmem",		&printmem_info},
  
  {"prm",		&printmmap_info_brief},
  {"printmmap",		&printmmap_info},
  
  {"pra",		&printregall_info_brief},
  {"printregall",	&printregall_info},
  
  {"penv",		&printenv_info_brief},
  {"printenv",		&printenv_info},
  
  {"pexe",		&printexe_info_brief},
  {"printexe",		&printexe_info},
  
  {"halt",		&quiesce_info_brief},
  {"quiesce",		&quiesce_info},
  
  {"r",			&run_info_brief},
  {"c",			&run_info_brief},
  {"run",		&run_info},
  
  {"s",			&step_info_brief},
  {"step",		&step_info},
  
  {"sw",		&switchpid_info_brief},
  {"switchpid",		&switchpid_info},
  
  {"sy",		&syscall_info_brief},
  {"syscallpid",	&syscall_info},
  
  {"lp",		&listpids_info_brief},
  {"listpids",		&listpids_info},
  
  {"w",			&watch_info_brief},
  {"watch",		&watch_info},
};
static int nr_cmds = sizeof(cmds)/sizeof(ENTRY);

static void
create_cmd_hash_table()
{
  int i, rc;
  rc = hcreate_r ((4 * nr_cmds)/3, &cmd_hash_table);
  if (0 == rc) {
    cleanup_udb();
    unregister_utracer (udb_pid);
    close_ctl_file();
    fprintf (stderr, "\tCreating command hash table failed.\n");
    _exit (1);
  }

  for (i = 0; i < nr_cmds; i++) {
    ENTRY * entry;
    if (0 == hsearch_r (cmds[i], ENTER, &entry, &cmd_hash_table)) {
      cleanup_udb();
      unregister_utracer (udb_pid);
      close_ctl_file();
      error (1, errno, "Error building commands hash.");
    }
  }
  cmd_hash_table_valid = 1;
}

void
text_ui_init()
{
#define HISTORY_LIMIT	16
  stifle_history (HISTORY_LIMIT);	// fixme--make settable

  create_cmd_hash_table();
  create_sys_hash_table();

  set_prompt();
}

int
exec_cmd (char * iline)
{
  int run = 1;
  
  switch (*iline) {
  case 0:
    break;
  case '?':
    {
      int i;
      for (i = 0; i < nr_cmds; i++) {
	cmd_info_s * cmd_info = (cmd_info_s *)cmds[i].data;
	if (cmd_info->desc)
	  fprintf (stderr, "\t%s %s\n", cmds[i].key, cmd_info->desc);
      }
    }
    break;
  default:
    {
      ENTRY * entry;
      ENTRY target;
      char * iline_copy;
      char * saveptr;
      
      iline_copy = strdup (iline);
      target.key = strtok_r (iline_copy, " \t", &saveptr);
      
      if (0 != hsearch_r (target, FIND, &entry, &cmd_hash_table)) {
	cmd_info_s * cmd_info = (cmd_info_s *)entry->data;
	run = (*(action_fcn)(cmd_info->cmd_fcn))(&saveptr);
      }
      else 
	fprintf (stderr, "\tCommand %s not recognised\n", iline);
      add_history (iline);
      
      free (iline_copy);
    }
    break;
  }

  return run;
}

void
text_ui()
{
  int run;
  char * iline = NULL;

  run = 1;
  while (run) {
    iline = readline (prompt);
    if (iline) {
      run = exec_cmd (iline);
      free (iline);
    }
  }
}
