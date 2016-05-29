package jx.emulation;

import jx.zero.*;
import jx.zero.scheduler.HighLevelScheduler;
import java.util.*;

public class DomainManagerImpl implements DomainManager {
    public Domain createDomain(String name,
				    CPU[] cpus,  HighLevelScheduler[] HLSched,
			     String domainCode, String[] libs,
			     String startClass, int heapSize, Naming naming) { throw new Error();}
    public Domain createDomainArgv(String name,
				    CPU[] cpus,  String[] HLSchedClass,
			     String domainCode, String[] libs,
			     String startClass, int heapSize, String[] argv, Naming naming) { throw new Error();}

    public Domain getDomainZero(){throw new Error();}
    public Domain getCurrentDomain(){throw new Error();}

    public void installInterceptor(Domain domain, DomainBorder border, CPUState thread){throw new Error();}

    public void gc(Domain domain) {throw new Error();}

    public void freeze(Domain domain){throw new Error();}
    public void thaw(Domain domain){throw new Error();}
    public void terminate(Domain domain){throw new Error();}

}
