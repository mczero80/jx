package jx.zero;

import jx.zero.scheduler.HighLevelScheduler;

// inlined Portal
public interface TestDZperf extends Portal {

  /** this is an empty Method. 
   *  it returns immediatly
   */
  void emptyMethod();

    /** prepares data-structures for the Method callSchedulerMethod_registered_.
     * @param  worker a Thread 
     * HLS a HighlevelScheduler Object the Method registered of this Object is called by callSchedulerMethod_registered_
     * @return <code>true</code>, if Parameters are ok
     * @return <code>false</code> otherwise
    */
  int prepareSchedulerMethod_registered_(CPUState worker, HighLevelScheduler HLS);
  int callSchedulerMethod_registered_();
}
