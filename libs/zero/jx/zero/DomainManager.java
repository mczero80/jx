package jx.zero;

import jx.zero.scheduler.HighLevelScheduler;

public interface DomainManager extends Portal {
    Domain createDomain(String name,
			CPU[] cpus, String[] HLSchedClass,
			String domainCode, String[] libs,
			String startClass,
			int gcinfo0, 
			int gcinfo1, 
			int gcinfo2, 
			String gcinfo3, 
			int gcinfo4, 
			int codeSize,
			String [] argv,
			Naming naming, Object[] moreArgs,
			int garbageCollector,
			int [] schedinfo);
    Domain getDomainZero();
    Domain getCurrentDomain();

    void installInterceptor(Domain domain, DomainBorder border, CPUState thread);

    void gc(Domain domain);

    void freeze(Domain domain);
    void thaw(Domain domain);
    void terminate(Domain domain);
    void terminateCaller();
}
