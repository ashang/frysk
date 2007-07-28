#ifndef UDB_H
#define UDB_H

#ifdef DO_UDB_INIT
#define DECL(v,i) v = i
#else
#define DECL(v,i) v
#endif

extern int  exec_cmd(char * iline);
extern void text_ui();
extern void text_ui_init();
extern void utace_attach_if(long pid);
extern void utrace_detach_if (long pid);
extern void utrace_readreg_if (long pid, int regset, int reg);
extern void set_prompt();
extern void register_utracer(pid_t pid);
extern void parse_regspec (char * tok, char ** saveptr, 
			   long * regset_p, long *reg_p);

pid_t udb_pid;

DECL (char * prompt, NULL);

DECL (long current_pid, -1);

DECL (int ctl_file_fd, -1);
DECL (int utracer_cmd_file_fd, -1);
DECL (int utracer_resp_file_fd, -1);

#ifdef ENABLE_MODULE_OPS
DECL (char * module_name, NULL);
#endif

DECL (char ** cl_cmds, NULL);
DECL (int cl_cmds_next, 0);
DECL (int cl_cmds_max, 0);
#define CMDS_INCR	4

DECL (char * ggg, NULL);

#define INVALID_REG	-128

#endif  /* UDB_H */
