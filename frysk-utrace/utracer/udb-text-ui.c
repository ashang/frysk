#define _GNU_SOURCE
#include <stdio.h>
#include <readline/readline.h>
#include <readline/history.h>
#include <error.h>
#include <errno.h>
#include <unistd.h>
#include <string.h>
#include <search.h>

#include "udb.h"
#include "udb-i386.h"
#include "utracer/utracer.h"


/*
 * TTD
 *
 * list regs
 * print all regs
 * list attached pids
 * reset prompt on detach
 * print regset info
 * allow regset spec in printreg
 */

static struct hsearch_data cmd_hash_table;
static struct hsearch_data reg_hash_table;
static struct hsearch_data sys_hash_table;
static int reg_hash_table_valid = 0;
static int sys_hash_table_valid = 0;

typedef int (*action_fcn)(char ** saveptr);

static int
syscall_fcn(char ** saveptr)
{
  int run = 1;
  int got_it = 0;
  int em_sent = 0;
  char * tok;
#define SYSCALL_INVALID		-5
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
      if (0 == strcasecmp (tok, "all")) {
	syscall_nr = SYSCALL_ALL;
	got_it = 1;
      }
      else if (sys_hash_table_valid) {
	ENTRY * entry;
	ENTRY target;
	
	target.key = tok;
	if (0 != hsearch_r (target, FIND, &entry, &sys_hash_table)) {
	  syscall_nr = (long)(entry->data);
	  got_it = 1;
	}
      }
      if (SYSCALL_INVALID == syscall_nr) {
	char * ep;
	
	syscall_nr = strtol (tok, &ep, 0);

	if (0 == *ep) {
	  if ((0 <= syscall_nr) && (syscall_nr < nr_syscall_names))
	    got_it = 1;
	  else {
	    em_sent = 1;
	    fprintf (stderr, "\tSorry, syscall number %ld is out of range.\n",
		     syscall_nr);
	  }
	}
	else {
	  em_sent = 1;
	  fprintf (stderr, "\tSorry, I don't recognise syscall %s\n", tok);
	}
      }
      break;
    }
  }
    
  if (got_it) {
    switch (sy_state) {
    case SY_STATE_A2:
      utrace_syscall_if (((SY_MODE_ENTRY == sy_mode) ?
			  SYSCALL_CMD_ENTRY : SYSCALL_CMD_EXIT),
			 ((SY_ENABLE_ON == sy_enable) ?
			  SYSCALL_CMD_ENABLE : SYSCALL_CMD_DISABLE),
			 current_pid,
			 0);
      break;
    case SY_STATE_A3:
      utrace_syscall_if (((SY_MODE_ENTRY == sy_mode) ?
			  SYSCALL_CMD_ENTRY : SYSCALL_CMD_EXIT),
			 ((SY_ADD_ADD == sy_add) ?
			  SYSCALL_CMD_ADD : SYSCALL_CMD_REMOVE),
			 current_pid,
			 syscall_nr);
      break;
    }
  }
  else
    if (!em_sent)
      fprintf (stderr, "\tSorry, I've no clue what you want me to do.\n");
  
  return 1;
}

static int
listpids_fcn(char ** saveptr)
{
  utrace_listpids_if ();
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
#define INVALID_REG	-128
  int reg = INVALID_REG;
  
  char * reg_c = strtok_r (NULL, " \t", saveptr);
  if (reg_c) {
    if (reg_hash_table_valid) {
      ENTRY * entry;
      ENTRY target;
      
      target.key = reg_c;
      if (0 != hsearch_r (target, FIND, &entry, &reg_hash_table)) 
	reg = (int)(entry->data);
    }
    if (INVALID_REG == reg) reg = atoi (reg_c);
    utrace_readreg_if (current_pid, 0, reg);  // fixme--first arg == regset
  }
  else fprintf (stderr, "\tprintreg requires an argument\n");
  return 1;
}

static int
printregall_fcn (char ** saveptr)
{
  utrace_readreg_if (current_pid, 0, -1);  // fixme--first arg == regset
  return 1;
}

static int
quit_fcn(char ** saveptr)
{
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
  
  {"pra",		&printregall_info_brief},
  {"printregall",	&printregall_info},
  
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
    unload_utracer();
    fprintf (stderr, "\tCreating command hash table failed.\n");
    _exit (1);
  }

  for (i = 0; i < nr_cmds; i++) {
    ENTRY * entry;
    if (0 == hsearch_r (cmds[i], ENTER, &entry, &cmd_hash_table))
      error (1, errno, "Error building commands hash.");
  }
}

static void
create_reg_hash_table()
{
  int i, rc;
  rc = hcreate_r ((4 * nr_regs)/3, &reg_hash_table);
  if (0 == rc) {
    fprintf (stderr, "\tCreating register hash table failed.\n");
    _exit (1);
  }

  for (i = 0; i < nr_regs; i++) {
    ENTRY * entry;
    if (0 == hsearch_r (reg_mapping[i], ENTER, &entry, &reg_hash_table))
      error (1, errno, "Error building register hash.");
  }
  reg_hash_table_valid = 1;
}

static void
create_sys_hash_table()
{
  int i, rc;
  rc = hcreate_r ((4 * nr_syscall_names)/3, &sys_hash_table);
  if (0 == rc) {
    fprintf (stderr, "\tCreating syscall hash table failed.\n");
    _exit (1);
  }

  for (i = 0; i < nr_syscall_names; i++) {
    ENTRY * entry;
    if (0 == hsearch_r (syscall_names[i], ENTER, &entry, &sys_hash_table))
      error (1, errno, "Error building syscall hash.");
  }
  sys_hash_table_valid = 1;
}

void
text_ui_init()
{
#define HISTORY_LIMIT	16
  stifle_history (HISTORY_LIMIT);	// fixme--make settable

  create_cmd_hash_table();
  create_reg_hash_table();
  create_sys_hash_table();

  set_prompt();
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
	    add_history (iline);
	  }
	  else {
	    fprintf (stderr, "\tCommand %s not recognised\n", iline);
	  }

	  free (iline_copy);
	}
	break;
      }
      free (iline);
    }
  }
}
