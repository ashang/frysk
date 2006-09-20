#define _GNU_SOURCE
#include <stdio.h>
#include <libaudit.h>


/**
 * A very simple test to see that we can get a couple
 * of syscalls from the audit libs
 */
int
main (int argc, char **argv)
{
  
  int i = 0;
  const char* syscall_name = audit_syscall_to_name(0, 1);
  while(syscall_name != NULL){
    syscall_name = audit_syscall_to_name(i, 1);
    i++;
  }

  if(i == 0){
    return 1;
  }

  return 0; 
}
