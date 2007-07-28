#define _XOPEN_SOURCE 500
#define _GNU_SOURCE
#include <stdio.h>
#include <alloca.h>
#include <unistd.h>
#include <fcntl.h>
#include <search.h>
#include <string.h>
#include <ctype.h>
/*#include <asm/ptrace.h>*/
#include <utracer.h>
#include <asm/unistd.h>
#include <asm/user.h>
#include <asm/ldt.h>

#include "udb.h"
#include "udb-i386.h"


static struct hsearch_data reg_hash_table;
static int reg_hash_table_valid = 0;

void
parse_regspec (char * tok, char ** saveptr, long * regset_p, long *reg_p)
{
  long reg = INVALID_REG;
  long regset = INVALID_REG;
  
  if (!reg_hash_table_valid) {
    int i, rc;
    rc = hcreate_r ((4 * nr_regs)/3, &reg_hash_table);
    if (0 == rc) {
      fprintf (stderr, "\tCreating register hash table failed.\n");
      return;
    }

    for (i = 0; i < nr_regs; i++) {
      ENTRY * entry;
      if (0 == hsearch_r (reg_mapping[i], ENTER, &entry, &reg_hash_table)) {
	perror ("Error building register hash.");
	return;
      }
    }
    reg_hash_table_valid = 1;
  }
  
  if (reg_hash_table_valid) {
    ENTRY * entry;
    ENTRY target;

    if (tok) {
      target.key = tok;
      if (0 != hsearch_r (target, FIND, &entry, &reg_hash_table)) {
	reg    = ((reg_data_s *)(entry->data))->reg;
	regset = ((reg_data_s *)(entry->data))->regset;
      }
      else if (isdigit (*tok) || ('-' == *tok)) {
	regset = 0;
	reg    = atol (tok);
	tok = strtok_r (NULL, ", \t", saveptr);
	if (tok && (isdigit (*tok) || ('-' == *tok))) {
	  regset = reg;
	  reg    = atol (tok);
	}
      }
    }
  }
  
  if (INVALID_REG == reg)    reg    = -1;
  if (INVALID_REG == regset) regset = 0;

  if (reg_p)    *reg_p    = reg;
  if (regset_p) *regset_p = regset;
}

#if 0
        from include/asm-i386/user.h
struct user_i387_struct {
  long    cwd;
  long    swd;
  long    twd;
  long    fip;
  long    fcs;
  long    foo;
  long    fos;
  long    st_space[20];   /* 8*10 bytes for each FP-reg = 80 bytes */
};

struct user_fxsr_struct {
  unsigned short  cwd;
  unsigned short  swd;
  unsigned short  twd;
  unsigned short  fop;
  long    fip;
  long    fcs;
  long    foo;
  long    fos;
  long    mxcsr;
  long    reserved;
  long    st_space[32];   /* 8*16 bytes for each FP-reg = 128 bytes */
  long    xmm_space[32];  /* 8*16 bytes for each XMM-reg = 128 bytes */
  long    padding[56];
};

from include/asm-i386/ldt.h
struct user_desc {
  unsigned int  entry_number;
  unsigned long base_addr;
  unsigned int  limit;
  unsigned int  seg_32bit:1;
  unsigned int  contents:2;
  unsigned int  read_exec_only:1;
  unsigned int  limit_in_pages:1;
  unsigned int  seg_not_present:1;
  unsigned int  useable:1;
};



// from arch/i386/kernel/ptrace.c native_regsets
// 0: gprs:		n =  17, size = sizeof(long), align = sizeof(long)
// 1: fprs:		n =  27, size = sizeof(long), align = sizeof(long)
// 2: fprx:		n = 126, size = sizeof(long), align = sizeof(long)
// 3: descriptors:	n =   3, size = sizeof(user_desc), align = size
//				GDTR, LDTR and IDTR
// 4: debug:		n =   8, size = sizeof(long), align = sizeof(long)


#endif

void
show_regs (long pid, long regset, long reg, void * regsinfo,
	   unsigned int regs_count, unsigned int reg_size)
{
  long i,mx,k;
  char * rn;
  struct user_i387_struct * i387_regs;
  struct user_desc * ud;
  
  
  fprintf (stdout, "\t[%ld] %s:\n", pid, regset_names[regset]);
  switch (regset) {
    case 0:			// gprs
    case 4:			// gprs
      mx = regs_count;
      break;
    case 3:
      mx = regs_count;
      ud = regsinfo;
      fprintf (stdout,
	       "\t\t\t\tentry\tbase\t  limit 32b cts rx lmt nosg ok\n");
      break;
    case 1:			// fprs
      i387_regs = regsinfo;

      fprintf (stdout, "\t\tcwd      swd      twd      fip\n");
      fprintf (stdout, "\t\t%08x %08x %08x %08x\n",
	       i387_regs->cwd,
	       i387_regs->swd,
	       i387_regs->twd,
	       i387_regs->fip);
      fprintf (stdout, "\t\tfcs      foo      fos\n");
      fprintf (stdout, "\t\t%08x %08x %08x\n\n",
	       i387_regs->fcs,
	       i387_regs->foo,
	       i387_regs->fos);
      mx = 8;
      break;
  }
  
  if (-1 == reg) {
    i = 0;
    k = mx;
  }
  else {
    i = reg;
    k = reg + 1;
  }
  
  for (; i < k; i++) {
    switch (regset) {
    case 0:			// gprs
      rn = (i < mx) ? reg_mapping[i].key : "";
      fprintf (stdout, "\t\t [%d (%s)]: [%#08x] %ld\n",
	       i, rn, ((long *)regsinfo)[i], ((long *)regsinfo)[i]);
      break;
    case 1:			// fprs
      {
	int j;
	char * fp_vec = (char *)&i387_regs->st_space;
	fp_vec += i * 10;
	fprintf (stdout, "\t\t [%d]: ", i);;
	for (j = 0; j < 10; j++) fprintf (stdout, "%02x ", fp_vec[j]);
	fprintf (stdout, "\n");
      }
      break;
    case 2:			// fprx
      break;
    case 3:			// descriptors
      rn = (i < mx) ? descriptor_names[i] : "";
      fprintf (stdout, "\t\t [%d (%s)]:\t", i, rn);
      fprintf (stdout,
	       "%u\t%08lx  %u     %u   %u   %u  %u   %u    %u\n",
	       ud[i].entry_number,
	       ud[i].base_addr,
	       ud[i].limit,
	       ud[i].seg_32bit,
	       ud[i].contents,
	       ud[i].read_exec_only,
	       ud[i].limit_in_pages,
	       ud[i].seg_not_present,
	       ud[i].useable);
      break;
    case 4:			// debug
      fprintf (stdout, "\t\t [%d]: [%#08x] %ld\n",
	       i, ((long *)regsinfo)[i], ((long *)regsinfo)[i]);
      break;
    }
  }
}


void
show_syscall (long type, long pid, struct pt_regs * regs)
{
  void * addr_to_show = NULL;
  fprintf (stdout, "\t[%ld] %s syscall: %s (",
	   pid,
	   ((IF_RESP_SYSCALL_EXIT_DATA == type) ?
	    "exiting" : "entering"),
    ((0 <= regs->orig_eax) && (regs->orig_eax < nr_syscall_names))
	   ? syscall_names[regs->orig_eax].key : "unknown");
	   
  switch (regs->orig_eax) {
  case __NR_read:
    fprintf (stdout, "%ld, 0x%08x, %ld",
	     regs->ebx,
	     regs->ecx,
	     regs->edx);
    if (IF_RESP_SYSCALL_EXIT_DATA == type) addr_to_show = (void *)regs->ecx;
    break;
  case __NR_write:
    fprintf (stdout, "%ld, 0x%08x, %ld",
	     regs->ebx,
	     regs->ecx,
	     regs->edx);
    addr_to_show = (void *)regs->ecx;
    break;
  case __NR_close:
    fprintf (stdout, "%ld", regs->ebx);
    break;
  default:
    fprintf (stdout, "?????????");
    break;
  }
  fprintf (stdout, ")\n");

  if (addr_to_show) {
    int rc;
#define SC_BFFR_LEN 56
    void * bffr = alloca (SC_BFFR_LEN);

    rc = utracer_get_mem (pid, addr_to_show, SC_BFFR_LEN, &bffr);

    if (0 == rc)
      fprintf (stdout, "\t\tat addr %08x: \"%.*s\"\n",
	       addr_to_show, SC_BFFR_LEN, (char *)bffr);
    else uerror ("stscall getmem.");
  }
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

