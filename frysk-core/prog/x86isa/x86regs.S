	.global main
main:
#ifdef __i386__
	push %ebp
	mov $1, %eax // exit syscall number
	mov $2, %ebx // first syscall arg
	mov $3, %ecx
	mov $4, %edx
	mov $5, %ebp
	mov $6, %esi
	mov $7, %edi
	mov $8, %esp
// Perform a syscall which will end up being exit (2)
	int $0x80
	ret
#endif
