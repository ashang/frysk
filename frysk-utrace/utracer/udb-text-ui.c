#define _GNU_SOURCE
#include <stdio.h>
#include <readline/readline.h>
#include <readline/history.h>
#include <search.h>
#include <error.h>
#include <errno.h>
#include <unistd.h>
#include <string.h>

#include "udb.h"


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
static int reg_hash_table_valid = 0;

typedef int (*action_fcn)(char ** saveptr);

static int
attach_fcn(char ** saveptr)
{
  long pid;
  char * pid_c = strtok_r (NULL, " \t", saveptr);
  if (pid_c) {
    pid = atol (pid_c);
    utrace_attach_if (pid);
  }
  else fprintf (stderr, "\tattach requires an argument\n");
  return 1;
}

static int
run_fcn(char ** saveptr)
{
  long pid;
  char * pid_c = strtok_r (NULL, " \t", saveptr);
  if (pid_c) {
    pid = atol (pid_c);
    utrace_run_if (pid);
  }
  else fprintf (stderr, "\trun requires an argument\n");
  return 1;
}

static int
quiesce_fcn(char ** saveptr)
{
  long pid;
  char * pid_c = strtok_r (NULL, " \t", saveptr);
  if (pid_c) {
    pid = atol (pid_c);
    utrace_quiesce_if (pid);
  }
  else fprintf (stderr, "\tquiesce requires an argument\n");
  return 1;
}

static int
testsig_fcn(char ** saveptr)	// fixme -- diagnostic
{
  utrace_testsig_if ();
  return 1;
}

static int
testcfread_fcn(char ** saveptr)	// fixme -- diagnostic
{
  utrace_testcfread_if ();
  return 1;
}

static int
dettach_fcn(char ** saveptr)
{
  long pid;
  char * pid_c = strtok_r (NULL, " \t", saveptr);
  if (pid_c) {
    pid = atol (pid_c);
    utrace_detach_if (pid);
  }
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
    fprintf (stderr, "switchpid_fcn %ld\n", pid);
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

static ENTRY cmds[] = {
  {"at",		attach_fcn},
  {"attach",		attach_fcn},
  {"bye",		quit_fcn},
  {"det",		dettach_fcn},
  {"dettach",		dettach_fcn},
  {"pr",		printreg_fcn},
  {"printreg",		printreg_fcn},
  {"q",			quit_fcn},
  {"quiesce",		quiesce_fcn},
  {"halt",		quiesce_fcn},
  {"quit",		quit_fcn},
  {"run",		run_fcn},
  {"r",			run_fcn},
  {"s",			step_fcn},
  {"step",		step_fcn},
  {"sw",		switchpid_fcn},
  {"switchpid",		switchpid_fcn},
  {"testsig",		testsig_fcn},		// fixme -- diagnostic
  {"testcfread",	testcfread_fcn},	// fixme -- diagnostic
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

static ENTRY reg_mapping[] = {
  {"ebx",	 (void *)0},
  {"ecx",	 (void *)1},
  {"edx",	 (void *)2},
  {"esi",	 (void *)3},
  {"edi",	 (void *)4},
  {"ebp",	 (void *)5},
  {"eax",	 (void *)6},
  {"ds",	 (void *)7},
  {"es",	 (void *)8},
  {"fs",	 (void *)9},
  {"gs",	(void *)10},
  {"cs",	(void *)13},
  {"orig_eax",	(void *)11},
  {"eip",	(void *)12},
  {"efl",	(void *)14},
  {"esp",	(void *)15},
  {"ss",	(void *)16}
};
static int nr_regs = sizeof(reg_mapping)/sizeof(ENTRY);

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

void
text_ui_init()
{
#define HISTORY_LIMIT	16
  stifle_history (HISTORY_LIMIT);	// fixme--make settable

  create_cmd_hash_table();
  create_reg_hash_table();	//fixme -- talk roland into putting in utrace

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
      if (*iline) {
	ENTRY * entry;
	ENTRY target;
	char * iline_copy;
	char * saveptr;

	iline_copy = strdup (iline);
	target.key = strtok_r (iline_copy, " \t", &saveptr);
      
	if (0 != hsearch_r (target, FIND, &entry, &cmd_hash_table)) {
	  run = (*(action_fcn)(entry->data))(&saveptr);
	  add_history (iline);
	}
	else {
	  fprintf (stderr, "\tCommand %s not recognised\n", iline);
	}

	free (iline_copy);
      }
      free (iline);
    }
  }
}
