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
  IF_CMD_TESTSIG,		// fixme -- diagnostic
  IF_CMD_ATTACH,
  IF_CMD_DETACH,
  IF_CMD_SET_REG,
  IF_CMD_READ_REG,
  IF_CMD_RUN,
  IF_CMD_QUIESCE
} if_cmd_e;

typedef struct {
  long cmd;
  long utracing_pid;
  long utraced_pid;
} run_cmd_s;

typedef struct {
  long cmd;
  long utracing_pid;
  long utraced_pid;
} attach_cmd_s;

typedef struct {		// fixme -- diagnostic
  long cmd;
  long utracing_pid;
} testsig_cmd_s;

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
  testsig_cmd_s testsig_cmd;	// fixme -- diagnostic
} if_cmd_u;

typedef enum {
  IF_RESP_NULL,
  IF_RESP_REG_DATA
} if_resp_e; 

typedef struct {
  long type;
  long utracing_pid;
  long utraced_pid;
  int  regset;
  int  which;
  int  byte_count;
  int  reg_count;
  void * data;
} readreg_resp_s;

typedef union {
  long type;
  readreg_resp_s	readreg_resp;
} if_resp_u;

typedef enum {
  UTRACER_EBASE		=	1024,
  UTRACER_EENGINE,
  UTRACER_ETRACING,
  UTRACER_ETRACED,
  UTRACER_EREG,
  UTRACER_EMAX,
} utracer_errno_e;


#endif  /* UTRACER_H */
