#ifndef UTRACER_PRIVATE_H
#define UTRACER_PRIVATE_H

#ifndef DO_INIT
extern
#endif
struct proc_dir_entry * de_utrace
#ifdef DO_INIT
= NULL
#endif
  ;

#ifndef DO_INIT
extern
#endif
struct proc_dir_entry * de_utrace_control
#ifdef DO_INIT
= NULL
#endif
  ;

typedef struct _utraced_info_s {
  long utraced_pid;
  struct utrace_attached_engine * utraced_engine;
  struct _utraced_info_s * next;
  struct _utraced_info_s * prev;
} utraced_info_s;

typedef struct _utracing_info_s {
  long utracing_pid;
  char * utracing_pid_string;
  struct proc_dir_entry * de_utracing_control;
  struct utrace_attached_engine * utracing_engine;
  utraced_info_s * utraced_info;
  void * queued_data;
  long queued_data_length;
  struct _utracing_info_s * next;
  struct _utracing_info_s * prev;
} utracing_info_s;

#ifndef DO_INIT
extern
#endif
utracing_info_s * utracing_info_top
#ifdef DO_INIT
= NULL
#endif
  ;

int control_file_write (struct file *file,
			const char *buffer,
			unsigned long count,
			void *data);

utracing_info_s * lookup_utracing_info (long utracing_pid);

utraced_info_s *
lookup_utraced_info (utracing_info_s * utracing_info_entry, long utraced_pid);

utracing_info_s *
remove_utracing_info_entry (utracing_info_s * utracing_info_entry);

int create_utracing_info_entry (long utracing_pid,
				char * utracing_pid_string,
				struct proc_dir_entry * de_utracing_control,
				struct utrace_attached_engine *
				     utraced_engine);

utraced_info_s *
remove_utraced_info_entry (utracing_info_s * utracing_info_entry,
			   utraced_info_s * utraced_info_entry);

int create_utraced_info_entry (utracing_info_s * utracing_info_entry,
			       long utraced_pid,
			       struct utrace_attached_engine * utraced_engine);

int if_file_read ( char *buffer,
		   char **buffer_location,
		   off_t offset,
		   int buffer_length,
		   int *eof,
		   void *data);

int if_file_write (struct file *file,
                   const char *buffer,
		   unsigned long count,
		   void *data);

struct task_struct * get_task (long utraced_pid);

struct utrace_attached_engine *
locate_engine (long utracing_pid, long utraced_pid);

#endif /* UTRACER_PRIVATE_H */

