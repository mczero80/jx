#define SCHED_BLOCK_USER Sched_block(STATE_BLOCKEDUSER)

/*
 * Interface to portal invocation system
 */

/* sender wants to send but did not find an available receiver thread
 * thread is already in the service wait queue
 *  set this threads state to PORTAL_WAIT_FOR_RCV and switch to next runnable thread */
#define SCHED_BLOCK_PORTAL_WAIT_FOR_RCV Sched_block(STATE_PORTAL_WAIT_FOR_RCV);

/* receiver is idle, the wait queue is empty, wait for sender
 *  set this threads state to PORTAL_WAIT_FOR_SND and switch to next runnable thread */
#define SCHED_BLOCK_PORTAL_WAIT_FOR_SND {Sched_block(STATE_PORTAL_WAIT_FOR_SND);}


