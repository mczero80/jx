#include "all.h"


/*
 * DebugChannel DEP
 */
jint debug_read(ObjectDesc * self)
{
	return 'x';
}

void debug_write(ObjectDesc * self, jint c)
{
#ifndef NO_DEBUG_OUT
	if (c >= 10 && c <= 127) {
#ifndef KERNEL
		printf("%c", c);
#else
		ser_putchar(debug_port, c);
#endif
	} else {
		//         debugl(("DEBUG WRITE %lx %ld (%c)\n",(jint)self,c,(char)c));
	}
#endif				/* NO_DEBUG_OUT */
}

void debug_writeBuf(ObjectDesc * self, ArrayDesc * arr, jint off, jint len)
{
#ifndef NO_DEBUG_OUT
#ifndef SMP
	jint i;
#ifdef ALL_ARRAYS_32BIT
	u4_t *field = (u4_t *) (arr->data);
#else
	u1_t *field = (u1_t *) (arr->data);
#endif

	DISABLE_IRQ;
	for (i = off; i < off + len; i++) {
		debug_write(self, field[i]);
	}
	RESTORE_IRQ;
#else				/* use a spinlock to prevent trouble with other CPUs */
	static spinlock_t printf_lock = SPIN_LOCK_UNLOCKED;
	jint i;
#ifdef ALL_ARRAYS_32BIT
	u4_t *field = (u4_t *) (arr->data);
#else
	u1_t *field = (u1_t *) (arr->data);
#endif

	DISABLE_IRQ;
	spin_lock(&printf_lock);
	for (i = off; i < off + len; i++) {
		debug_write(self, field[i]);
	}
	spin_unlock(&printf_lock);
	RESTORE_IRQ;
#endif
#endif				/* NO_DEBUG_OUT */
}

MethodInfoDesc debugChannelMethods[] = {
	{"read", "", debug_read}
	,
	{"write", "", debug_write}
	,
	{"writeBuf", "", debug_writeBuf}
	,
};

static ClassDesc *debugChannelClass;
static jbyte debugChannelTypeMap[] = { 0 };

void init_debugchannel_portal()
{
	debugChannelClass =
	    init_zero_class("jx/zero/debug/DebugChannel", debugChannelMethods, sizeof(debugChannelMethods), 1,
			    debugChannelTypeMap, "<jx/zero/debug/DebugChannel>");


	/* init debug channels */

	init_zero_dep_without_thread("jx/zero/debug/DebugChannel", "DebugChannel0", debugChannelMethods,
				     sizeof(debugChannelMethods), "<jx/zero/debug/DebugChannel>");

}
