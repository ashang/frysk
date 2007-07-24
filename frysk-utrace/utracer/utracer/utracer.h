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

typedef struct {
  long cmd;
  long utracing_pid;
} listpids_cmd_s;

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
  long buffer_len;
  long * length_returned;
  char * buffer;
} printenv_cmd_s;

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
  int  which;
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
} if_cmd_u;

typedef enum {
  IF_RESP_NULL,				//  0
  IF_RESP_REG_DATA,			//  1
  IF_RESP_CLONE_DATA,			//  2
  IF_RESP_SIGNAL_DATA,			//  3
  IF_RESP_EXIT_DATA,			//  4
  IF_RESP_PIDS_DATA,			//  5
  IF_RESP_DEATH_DATA,			//  6
  IF_RESP_SWITCHPID_DATA,		//  7
  IF_RESP_SYSCALL_ENTRY_DATA,		//  8
  IF_RESP_SYSCALL_EXIT_DATA,		//  9
  IF_RESP_EXEC_DATA,			// 10
  IF_RESP_ATTACH_DATA,			// 11
  IF_RESP_QUIESCE_DATA,			// 12
  IF_RESP_SYNC_DATA			// 13
} if_resp_e; 

typedef struct {
  long type;
  long utracing_pid;
  long sync_type;
} sync_resp_s;

typedef struct {
  long type;
  long utraced_pid;
  int  regset;
  int  which;
  int  byte_count;
  int  reg_count;
  void * data;
} readreg_resp_s;

typedef struct {
  long type;
  long utracing_pid;
  long new_utraced_pid;
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
  long nr_pids;
} pids_resp_s;

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

typedef struct {
  long type;
  long utraced_pid;
  int  okay;
} attach_resp_s;

typedef struct {
  long type;
  long utraced_pid;
  int  okay;
} switchpid_resp_s;

typedef union {
  long type;
  readreg_resp_s	readreg_resp;
  clone_resp_s		clone_resp;
  signal_resp_s		signal_resp;
  attach_resp_s		attach_resp;
  death_resp_s		death_resp;
  exit_resp_s		exit_resp;
  pids_resp_s		pids_resp;
  switchpid_resp_s	switchpid_resp;
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
  UTRACER_EMAX,
} utracer_errno_e;

typedef struct {
  long cmd;
  long bffr_len;
  char * bffr;
} utracer_ioctl_s;

#endif  /* UTRACER_H */
