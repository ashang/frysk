	.file	"funit-stackframe-static.c"
	.text
	.type	foo, @function
foo:
	pushl	%ebp
	movl	%esp, %ebp
	movl	$0, %eax
	movl	$0, (%eax)
	popl	%ebp
	ret
.globl main
	.type	main, @function
main:
	leal	4(%esp), %ecx
	andl	$-16, %esp
	pushl	-4(%ecx)
	pushl	%ebp
	movl	%esp, %ebp
	pushl	%ecx
	call	foo
	movl	$0, %eax
	popl	%ecx
	popl	%ebp
	leal	-4(%ecx), %esp
	ret
	.size	main, .-main
	.ident	"GCC: (GNU) 4.1.1 20070105 (Red Hat 4.1.1-51)"
	.section	.note.GNU-stack,"",@progbits
