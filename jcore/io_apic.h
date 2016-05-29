#ifndef __IO_APIC_H
#define __IO_APIC_H

void setup_IO_APIC(void);
void print_IO_APIC(void);	/* for debug only */
/* sets the destination CPU of an IRQ (-1 = all CPUs) */
void set_irq_destination(unsigned int irq, char new_dest);

#endif
