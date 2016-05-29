package jx.zero.scheduler;

import jx.zero.CPUState;

/**
this interface defines a Notifying-Method of a HighLevel_Scheduler 
*/
public interface HLS_GCThread{

/* y/n *//* specifies wether the IRQs are disabled or not */

             /** the Garbage Collector is runnable. <BR>
	      * This method informs the HLS, that the GCThread schould be activated 
	      *@params <code>interruptedThread</code> this Thread was interrupted<BR>
	      *        <code>GCThread</code> this thread schould be activated
	      * <BR>
              * <B>! IRQ are disabled during execution of this method</B> <BR>*/
/* yes */    public void startGCThread (CPUState interruptedThread, CPUState GCThread);

             /** the Garbage Collector is runnable. <BR>
	      * This method informs the HLS, that the GCThread is runnable,
	      *  but the Domain is not allowed to start it immediately 
	      *@params <code>GCThread</code> the GC-Thread
	      * <BR>
              * <B>! IRQ are disabled during execution of this method</B> <BR>*/
/* yes */    public void unblockedGCThread (CPUState GCThread);

            /** the Garbage Collector has finished. <BR>
	      * This method informs the HLS, that GCThread is destroyed 
	      *@params <code>GCThread</code> Thread of the Garbage Collector
	      * <BR>
              * <B>! IRQ are disabled during execution of this method</B> <BR>*/
/* yes */    public void destroyedGCThread (CPUState GCThread);
}

