#ifdef NEW_SCHED
#include "all.h"

/************** GENERIC FUNCTIONS ***********/




void sched_init()
{
#ifdef SCHED_GLOBAL_RR
	sched_rr_init();
#endif
#ifdef SCHED_GLOBAL_STRIDE
	sched_stride_init();
#endif
}
#endif
