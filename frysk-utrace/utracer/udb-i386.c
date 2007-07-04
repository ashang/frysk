#include <stdio.h>
/*#include <asm/ptrace.h>*/

#include "utracer/utracer.h"
#include "udb-i386.h"
#include <asm/unistd.h>


void
show_syscall (long type, long pid, struct pt_regs * regs)
{
  fprintf (stdout, "\t[%ld] %s syscall: %s (",
	   pid,
	   ((IF_RESP_SYSCALL_EXIT_DATA == type) ?
	    "exiting" : "entering"),
    ((0 <= regs->orig_eax) && (regs->orig_eax < nr_syscall_names))
	   ? syscall_names[regs->orig_eax].key : "unknown");
	   
  switch (regs->orig_eax) {
  case __NR_read:
    fprintf (stdout, "%ld, 0x%08x, %ld", regs->ebx, regs->ecx, regs->edx);
    break;
  case __NR_write:
    fprintf (stdout, "%ld, 0x%08x, %ld",
	     regs->ebx,
	     regs->ecx,
	     regs->edx);
    break;
  case __NR_close:
    fprintf (stdout, "%ld", regs->ebx);
    break;
  default:
    fprintf (stdout, "?????????");
    break;
  }
  fprintf (stdout, ")\n");
}

void
show_syscall_regs (struct pt_regs * regs)
{
  fprintf (stdout, "\neip (%1$08x) %1$12lu\n",
	   regs->eip);
  
  fprintf (stdout, "orig_eax (%1$08x) %1$12lu\n",
	   regs->orig_eax);

  fprintf (stdout, "\
eax (%1$08x) %1$12ld  ebx (%2$08x) %2$12ld\n\
ecx (%3$08x) %3$12ld  edx (%4$08x) %4$12ld\n",
	   regs->eax, regs->ebx,
	   regs->ecx, regs->edx);

  fprintf (stdout, "\
ebp (%1$08x) %1$12ld  esp (%2$08x) %2$12ld\n\
edi (%3$08x) %3$12ld  esi (%4$08x) %4$12ld\n",
	   regs->ebp, regs->esp,
	   regs->edi, regs->esi);

#if 0
  fprintf (stdout, "\
xds (%1$08x) %1$12d  xes (%2$08x) %2$12d\n\
xcs (%3$08x) %3$12d  xss (%4$08x) %4$12d\n",
	   regs->xds, regs->xes,
	   regs->xcs, regs->xss);
#endif
}

