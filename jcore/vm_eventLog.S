#include "config.h"

/*
 * vm_event
 *
 * Parameter:
 * %ebx : eventID
 *
 * Locals:
 * %ecx      : n_event
 * %edi      : events 
 * %esi      : index
 * %eax,%edx : time stamp counter 
 *
 */

#ifdef EVENT_LOG

	.align 16
ENTRY(vm_event_OLD)
        pushfl
        cli

	cmpl n_event_types,%ebx
        jle  event_panic

	movl n_events,%ecx
	cmpl MAX_EVENTS,%ecx 
	je   event_panic

	leal (%ecx,%ecx,4),%esi
	sall $2,%esi
	movl $events,%edi
	movl %ebx,(%esi,%edi)
 	rdtsc
	movl %eax,12(%esi,%edi)
	movl %edx,16(%esi,%edi)
	incl %ecx
	movl %ecx,n_events

        popfl

	ret
ENDENTRY(vm_event_OLD)

event_panic:
       push $0 
       push $0 
       call domain_panic; 
#endif

