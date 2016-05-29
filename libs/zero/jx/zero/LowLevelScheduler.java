package jx.zero;


public interface LowLevelScheduler extends FirstLevelIrqHandler{

/* y/n *//* specifies wether the IRQs are disabled or not */
    
/* yes */    public void activate_currDomain();
/* yes */    public void activate_nextDomain();
     
/* yes */    public void registerDomain(Domain domain);
/* ??? */    public void unregisterDomain(Domain domain);
/* ??? */    public void dump();   

/* yes */    public void setTimeSlice(Domain domain, int time);

    /* called from SMPcpuManager::register_LLScheduler
            and is executed on its final CPU 
       Parameter: the IRQ Nr which this Scheduler should use */
/* yes */    public void registered(int irq_nr);
}
