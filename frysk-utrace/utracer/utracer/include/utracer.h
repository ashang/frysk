#ifndef UTRACER_H
#define UTRACER_H

#define UTRACER_BASE_DIR	"utrace"
#define UTRACER_CONTROL_FN	"control"
#define UTRACER_CMD_FN		"cmd"
#define UTRACER_RESP_FN		"resp"

typedef enum {
  CTL_CMD_NULL,
  CTL_CMD_REGISTER,
  CTL_CMD_UNREGISTER
} ctl_cmd_e;

typedef struct {
  long cmd;
  long client_pid;
} register_cmd_s;

typedef union {
  long cmd;
  register_cmd_s register_cmd;
} ctl_cmd_u;

typedef enum {
  IF_CMD_NULL,
  IF_CMD_ATTACH,
  IF_CMD_DETACH,
  IF_CMD_SET_REG,
  IF_CMD_READ_REG,
  IF_CMD_RUN,
  IF_CMD_LIST_PIDS,
  IF_CMD_SWITCHPID,
  IF_CMD_PRINTMMAP,
  IF_CMD_PRINTENV,
  IF_CMD_GETMEM,
  IF_CMD_PRINTEXE,
  IF_CMD_SYSCALL,
  IF_CMD_QUIESCE,
  IF_CMD_SYNC,
} if_cmd_e;

typedef enum {
  SYSCALL_CMD_ENTRY,
  SYSCALL_CMD_EXIT,
  SYSCALL_CMD_ENABLE,
  SYSCALL_CMD_DISABLE,
  SYSCALL_CMD_ADD,
  SYSCALL_CMD_REMOVE
} syscall_cmd_e;

typedef struct {
  long cmd;
  long utracing_pid;
  long utraced_pid;
  union {
    long l;
    struct {
      short which;
      short cmd;
    } cmd;
  } syscall_cmd;
  long syscall_nr;
} syscall_cmd_s;
#define syscall_cmd_cmd(s)	(s)->syscall_cmd.cmd.cmd
#define syscall_cmd_which(s)	(s)->syscall_cmd.cmd.which
#define SYSCALL_ALL  -1

typedef struct {
  long cmd;
  long utracing_pid;
  long utraced_pid;
} run_cmd_s;

typedef enum {
  SYNC_INIT,
  SYNC_RESP
} sync_cmd_e;

typedef struct {
  long cmd;
  long utracing_pid;
  long sync_type;
} sync_cmd_s;

typedef struct {
  long cmd;
  long utracing_pid;
  long utraced_pid;
} switchpid_cmd_s;

typedef struct {
  unsigned long vm_start;
  unsigned long vm_end;
  unsigned long vm_flags;
  long name_offset;
} vm_struct_subset_s;

typedef struct {
  long utraced_pid;
  unsigned long mmap_base;
  unsigned long task_size;
  unsigned long total_vm, locked_vm, shared_vm, exec_vm;
  unsigned long stack_vm, reserved_vm, def_flags, nr_ptes;
  unsigned long start_code, end_code, start_data, end_data;
  unsigned long start_brk, brk, start_stack;
  unsigned long arg_start, arg_end, env_start, env_end;
  unsigned long nr_mmaps;
  unsigned long vm_strings_length;
} printmmap_resp_s;

typedef struct {
  long cmd;
  long utracing_pid;
  long utraced_pid;
  unsigned long vm_struct_subset_alloc;
  unsigned long vm_strings_alloc;
  long * vm_struct_subset_length;
  long * vm_strings_length;
  printmmap_resp_s * printmmap_resp;
  vm_struct_subset_s * vm_struct_subset;
  char * vm_strings;
} printmmap_cmd_s;

typedef struct {
  long cmd;
  long utracing_pid;
  long utraced_pid;
  long mem_len;
  long mem_addr;
  void * mem;
  unsigned long * actual;
} getmem_cmd_s;

typedef struct {
  long cmd;
  long utracing_pid;
  long utraced_pid;
  long buffer_len;
  long * length_returned;
  char * buffer;
} printenv_cmd_s;

typedef struct {
  long cmd;
  long utracing_pid;
  long nr_pids_alloced;
  long * nr_pids_actual;
  long * pids;
} listpids_cmd_s;

typedef struct {
  long cmd;
  long utracing_pid;
  long utraced_pid;
  char * filename;
  long filename_len;
  char * interp;
  long interp_len;
} printexe_cmd_s;

typedef struct {
  long cmd;
  long utracing_pid;
  long utraced_pid;
  long quiesce;
  long exec_quiesce;
} attach_cmd_s;

typedef struct {
  long cmd;
  long utracing_pid;
  long utraced_pid;
  int  regset;
  void *  regsinfo;
  long alloced_size;
  long * actual_size;
  unsigned int * nr_regs;
  unsigned int * reg_size;
} readreg_cmd_s;

typedef union {
  long cmd;
  attach_cmd_s		attach_cmd;
  readreg_cmd_s		readreg_cmd;
  run_cmd_s		run_cmd;
  listpids_cmd_s	listpids_cmd;
  switchpid_cmd_s	switchpid_cmd;
  printmmap_cmd_s	printmmap_cmd;
  syscall_cmd_s		syscall_cmd;
  sync_cmd_s		sync_cmd;
  printenv_cmd_s	printenv_cmd;
  printexe_cmd_s	printexe_cmd;
  getmem_cmd_s		getmem_cmd;
} if_cmd_u;

typedef enum {
  IF_RESP_NULL,				//  0
  IF_RESP_CLONE_DATA,			//  1
  IF_RESP_SIGNAL_DATA,			//  2
  IF_RESP_EXIT_DATA,			//  3
  IF_RESP_DEATH_DATA,			//  4
  IF_RESP_SYSCALL_ENTRY_DATA,		//  5
  IF_RESP_SYSCALL_EXIT_DATA,		//  6
  IF_RESP_EXEC_DATA,			//  7
  IF_RESP_QUIESCE_DATA,			//  8
  IF_RESP_SYNC_DATA			//  9
} if_resp_e; 

typedef struct {
  long type;
  long utracing_pid;
  long sync_type;
} sync_resp_s;

typedef struct {
  long type;
  long utracing_pid;
  long new_utraced_pid;
  long attach_rc;
} clone_resp_s;

typedef struct {
  long type;
  long utraced_pid;
  long signal;
} signal_resp_s;

typedef struct {
  long type;
  long utraced_pid;
  long code;
} exit_resp_s;

typedef struct {
  long type;
  long utraced_pid;
} death_resp_s;

typedef struct {
  long type;
  long utraced_pid;
  long data_length;
} syscall_resp_s;

typedef struct {
  long type;
  long utraced_pid;
} quiesce_resp_s;

typedef struct {
  long type;
  long utraced_pid;
  long data_length;
} exec_resp_s;

typedef union {
  long type;
  clone_resp_s		clone_resp;
  signal_resp_s		signal_resp;
  death_resp_s		death_resp;
  exit_resp_s		exit_resp;
  syscall_resp_s	syscall_resp;
  exec_resp_s		exec_resp;
  quiesce_resp_s	quiesce_resp;
  sync_resp_s		sync_resp;
} if_resp_u;

typedef enum {
  UTRACER_EBASE		=	1024,
  UTRACER_EENGINE,
  UTRACER_ETRACING,
  UTRACER_ETRACED,
  UTRACER_EREG,
  UTRACER_ESYSRANGE,
  UTRACER_ESTATE,
  UTRACER_EPAGES,
  UTRACER_EMM,
  UTRACER_EREGSET,
  UTRACER_EMAX,       
} utracer_errno_e;

/***************** public i/f ****************/

void utracer_set_environment (pid_t client_pid, int cmd_fd);  //temp
int utracer_get_printmmap (long pid,
			   printmmap_resp_s ** printmmap_resp_p,
			   vm_struct_subset_s ** vm_struct_subset_p,
			   char ** vm_strings_p);
int utracer_get_exe (long pid,
		     char ** filename_p,
		     char ** interp_p);
int utracer_get_env (long pid, char ** env_p);
int utracer_get_mem (long pid,
		     void * addr,
		     unsigned long length,
		     void ** mem_p,
		     unsigned long * actual_length);
int utracer_get_pids (long * nr_pids, long ** pids);
int utracer_switch_pid (long pid);
int utracer_run (long pid);
int utracer_quiesce (long pid);
int utracer_attach (long pid, long quiesce, long exec_quiesce);
int utracer_get_regs (long pid, long regset, void ** regsinfo,
		      unsigned int * nr_regs_p, unsigned int * reg_size_p);
int utracer_set_syscall (short which, short cmd, long pid, long syscall);
  
#endif  /* UTRACER_H */
