package jx.zero.scheduler;

/**
this interface defines a Scheduler for the Threads of a Domain
the inlined Portal jx.zero.HLSchedulerSupport contains some methods useful for this Scheduler
*/
public interface HLScheduler_all extends HighLevelScheduler, HLS_switchTo, HLS_preempted, HLS_interrupted, HLS_destroyed, HLS_yielded, HLS_unblocked, HLS_blocked, HLS_blockedInPortalCall, HLS_portalCalled {
};
