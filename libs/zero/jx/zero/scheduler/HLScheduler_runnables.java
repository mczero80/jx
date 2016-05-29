package jx.zero.scheduler;

/*
 * this interface accumulates all Methods with runnable Threads as arguments
*/
public interface HLScheduler_runnables extends HighLevelScheduler, HLS_preempted, HLS_interrupted, HLS_yielded, HLS_unblocked {
};
