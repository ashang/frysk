#ifndef UTRACER_H
#define UTRACER_H

#define BASE_DIR	"utrace"
#define CONTROL_FN	"control"

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
  IF_CMD_SYSCALL,
  IF_CMD_QUIESCE
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
#define syscall_cmd_cmd(s)	(s).syscall_cmd.cmd.cmd
#define syscall_cmd_which(s)	(s).syscall_cmd.cmd.which
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

typedef struct {
  long cmd;
  long utracing_pid;
  long utraced_pid;
} switchpid_cmd_s;

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
  attach_cmd_s	attach_cmd;
  readreg_cmd_s	readreg_cmd;
  run_cmd_s	run_cmd;
  listpids_cmd_s listpids_cmd;
  switchpid_cmd_s switchpid_cmd;
  syscall_cmd_s syscall_cmd;
} if_cmd_u;

typedef enum {
  IF_RESP_NULL,
  IF_RESP_REG_DATA,
  IF_RESP_CLONE_DATA,
  IF_RESP_SIGNAL_DATA,
  IF_RESP_EXIT_DATA,
  IF_RESP_PIDS_DATA,
  IF_RESP_DEATH_DATA,
  IF_RESP_SWITCHPID_DATA,
  IF_RESP_SYSCALL_ENTRY_DATA,
  IF_RESP_SYSCALL_EXIT_DATA,
  IF_RESP_EXEC_DATA,
  IF_RESP_ATTACH_DATA
} if_resp_e; 

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
  long data_length;
} syscall_resp_s;

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
} if_resp_u;

typedef enum {
  UTRACER_EBASE		=	1024,
  UTRACER_EENGINE,
  UTRACER_ETRACING,
  UTRACER_ETRACED,
  UTRACER_EREG,
  UTRACER_ESYSRANGE,
  UTRACER_ESTATE,
  UTRACER_EMAX,
} utracer_errno_e;


#endif  /* UTRACER_H */
