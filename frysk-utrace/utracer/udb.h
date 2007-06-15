#ifndef UDB_H
#define UDB_H

extern void text_ui();
extern void text_ui_init();
extern void utace_attach_if(long pid);
extern void utrace_detach_if (long pid);
extern void utrace_readreg_if (long pid, int regset, int reg);
extern void set_prompt();
extern void register_utracer(pid_t pid);

char * prompt
#ifdef DO_UDB_INIT
= NULL;
#endif
;

long current_pid
#ifdef DO_UDB_INIT
= -1
#endif
;

int ctl_file_fd
#ifdef DO_UDB_INIT
= -1
#endif
;

int utracer_file_fd
#ifdef DO_UDB_INIT
= -1
#endif
;

#endif  /* UDB_H */
