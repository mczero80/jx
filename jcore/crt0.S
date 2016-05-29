	.text
.p2align  2   , 0x90 ; .type       _start   ,@function ; .globl     _start     ;     _start:    
	jmp	boot_entry

	 
	.p2align  2  
boot_hdr:
	.long	0x1BADB002		 

	.long	0x00000000		 
	.long	0-0x1BADB002-0x00000000	 
boot_entry:
	movl	$base_stack_end, %esp
	pushl	$0
	popf
	lea	edata, %edi
	lea	end, %ecx
	subl	%edi,%ecx
	xorl	%eax,%eax
	rep
	stosb

	 
	xorb	%al,%al
	movw	$0x3f2,%dx		 
	outb	%al,%dx
	movw	$0x372,%dx		 
	outb	%al,%dx


	pushl	%ebx

	xorl	%ebp,%ebp
	 
	call	 multiboot_main  

1:      
        int   $3
        jmp   2f
        .ascii "PANIC: main returned!"
2:      
        jmp   1b
