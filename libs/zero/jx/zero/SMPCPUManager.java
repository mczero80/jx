package jx.zero;

import jx.zero.scheduler.HighLevelScheduler;

// inlined Portal
public interface SMPCPUManager extends Portal {

    boolean start(CPUState state, CPU cpu);
    boolean unblock(CPUState state, CPU cpu);   

    void swap_HLScheduler(CPU cpu, Domain domain, HighLevelScheduler oldSched, HighLevelScheduler newSched);
    void register_LLScheduler(CPU cpu, LowLevelScheduler sched);  
    
    void sendIPI(CPU dest, int vector);
    CPU getMyCPU();
    int getNumCPUs();
    CPU getCPU(int i); 
    
//    CPUState getCPUState(int cpu_id);
    
    int test(int val);  //ttt
    int test2(int val);  //ttt
    int test_setAPICTimer(int val);  //ttt
    void dump(CPU cpu); //ttt


    /* Syncronisation */
    Mutex createMutex();
}
