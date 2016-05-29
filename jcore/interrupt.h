#ifndef __INTERRUPT_H
#define __INTERRUPT_H

void irq_disable();
void irq_enable();
#ifndef KERNEL
sigset_t irq_store_disable();
void irq_restore(sigset_t mask);
#endif

#endif
