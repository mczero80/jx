#include "timer8254.h"

#include "misc.h"


unsigned int get_8254_timer_count(void)
{
	unsigned int count;

	outb(0x00, 0x43);
	count = inb(0x40);
	count |= inb(0x40) << 8;

	return count;
}

void wait_8254_wraparound(void)
{
	unsigned int curr_count, prev_count = ~0;
	int delta;

	curr_count = get_8254_timer_count();

	do {
		prev_count = curr_count;
		curr_count = get_8254_timer_count();
		delta = curr_count - prev_count;

		/*
		 * This limit for delta seems arbitrary, but it isn't, it's
		 * slightly above the level of error a buggy Mercury/Neptune
		 * chipset timer can cause.
		 */

	} while (delta < 300);
}
