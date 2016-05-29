package jx.zero;


public interface JAVAScheduler{
    public void add(CPUState newThread);
    public CPUState removeNext();
    public void dump();

    /* called from SMPcpuManager::register_Scheduler
       and is executed on its final CPU */
    public void init(int irq_nr);
   // public String toString();
}
