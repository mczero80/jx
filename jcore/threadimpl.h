#include "all.h"

#ifdef TRACESCHEDULER
u4_t trace_sched_data[1024 * 1024];
u4_t *trace_sched_ip = trace_sched_data;
u4_t *last_trace_sched_ip = trace_sched_data + 1024 * 1024;
#endif

#ifdef SMP
ThreadDesc *__current[MAX_NR_CPUS] = { NULL, NULL, NULL, NULL, NULL,
	NULL, NULL, NULL, NULL, NULL,
	NULL, NULL, NULL, NULL, NULL
};
ThreadDesc *__idle_thread[MAX_NR_CPUS] = { NULL, NULL, NULL, NULL, NULL,
	NULL, NULL, NULL, NULL, NULL,
	NULL, NULL, NULL, NULL, NULL
};
#else
ThreadDesc *__current[MAX_NR_CPUS] = { NULL };
ThreadDesc *__idle_thread[MAX_NR_CPUS] = { NULL };
#endif
