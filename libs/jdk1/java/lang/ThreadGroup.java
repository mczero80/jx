package java.lang;

import java.io.PrintStream;

public class ThreadGroup {
    ThreadGroup parent;
    String name;
    int maxPriority;
    boolean destroyed;
    boolean daemon;
    boolean vmAllowSuspension;

    int nthreads;
    Thread threads[];

    int ngroups;
    ThreadGroup groups[];

    public ThreadGroup(String name) {
    }
    public ThreadGroup(ThreadGroup parent, String name) {
	this.name = name;
	this.maxPriority = parent.maxPriority;
	this.daemon = parent.daemon;
	this.parent = parent;
	parent.add(this);
    }
    public final String getName() {
	return name;
    }
    public final ThreadGroup getParent() {
	return parent;
    }
    public final int getMaxPriority() {
	return maxPriority;
    }
    public final boolean isDaemon() {
	return daemon;
    }
    public synchronized boolean isDestroyed() {
	return destroyed;
    }
    public final void setDaemon(boolean daemon) {
	this.daemon = daemon;
    }
    public final void setMaxPriority(int pri) {
	int ngroupsSnapshot;
	ThreadGroup[] groupsSnapshot;
	synchronized (this) {
	    if (pri < Thread.MIN_PRIORITY) {
		maxPriority = Thread.MIN_PRIORITY;
	    } else if (pri < maxPriority) {
		maxPriority = pri;
	    }
	    ngroupsSnapshot = ngroups;
	    if (groups != null) {
		groupsSnapshot = new ThreadGroup[ngroupsSnapshot];
		System.arraycopy(groups, 0, groupsSnapshot, 0, ngroupsSnapshot);
	    } else {
		groupsSnapshot = null;
	    }
	}
	for (int i = 0 ; i < ngroupsSnapshot ; i++) {
	    groupsSnapshot[i].setMaxPriority(pri);
	}
    }
    public final boolean parentOf(ThreadGroup g) {
	for (; g != null ; g = g.parent) {
	    if (g == this) {
		return true;
	    }
	}
	return false;
    }
    public int activeCount() {
	int result;
	// Snapshot sub-group data so we don't hold this lock
	// while our children are computing.
	int ngroupsSnapshot;
	ThreadGroup[] groupsSnapshot;
	synchronized (this) {
	    if (destroyed) {
		return 0;
	    }
	    result = nthreads;
	    ngroupsSnapshot = ngroups;
	    if (groups != null) {
		groupsSnapshot = new ThreadGroup[ngroupsSnapshot];
		System.arraycopy(groups, 0, groupsSnapshot, 0, ngroupsSnapshot);
	    } else {
		groupsSnapshot = null;
	    }
	}
	for (int i = 0 ; i < ngroupsSnapshot ; i++) {
	    result += groupsSnapshot[i].activeCount();
	}
	return result;
    }
    public int enumerate(Thread list[]) {
	return enumerate(list, 0, true);
    }
    public int enumerate(Thread list[], boolean recurse) {
	return enumerate(list, 0, recurse);
    }
    private int enumerate(Thread list[], int n, boolean recurse) {
	int ngroupsSnapshot = 0;
	ThreadGroup[] groupsSnapshot = null;
	synchronized (this) {
	    if (destroyed) {
		return 0;
	    }
	    int nt = nthreads;
	    if (nt > list.length - n) {
		nt = list.length - n;
	    }
	    if (nt > 0) {
		System.arraycopy(threads, 0, list, n, nt);
		n += nt;
	    }
	    if (recurse) {
		ngroupsSnapshot = ngroups;
		if (groups != null) {
		    groupsSnapshot = new ThreadGroup[ngroupsSnapshot];
		    System.arraycopy(groups, 0, groupsSnapshot, 0, ngroupsSnapshot);
		} else {
		    groupsSnapshot = null;
		}
	    }
	}
	if (recurse) {
	    for (int i = 0 ; i < ngroupsSnapshot ; i++) {
		n = groupsSnapshot[i].enumerate(list, n, true);
	    }
	}
	return n;
    }

    public int activeGroupCount() {
	int ngroupsSnapshot;
	ThreadGroup[] groupsSnapshot;
	synchronized (this) {
	    if (destroyed) {
		return 0;
	    }
	    ngroupsSnapshot = ngroups;
	    if (groups != null) {
		groupsSnapshot = new ThreadGroup[ngroupsSnapshot];
		System.arraycopy(groups, 0, groupsSnapshot, 0, ngroupsSnapshot);
	    } else {
		groupsSnapshot = null;
	    }
	}
	int n = ngroupsSnapshot;
	for (int i = 0 ; i < ngroupsSnapshot ; i++) {
	    n += groupsSnapshot[i].activeGroupCount();
	}
	return n;
    }

    public int enumerate(ThreadGroup list[]) {
	return enumerate(list, 0, true);
    }

    public int enumerate(ThreadGroup list[], boolean recurse) {
	return enumerate(list, 0, recurse);
    }

    private int enumerate(ThreadGroup list[], int n, boolean recurse) {
	int ngroupsSnapshot = 0;
	ThreadGroup[] groupsSnapshot = null;
	synchronized (this) {
	    if (destroyed) {
		return 0;
	    }
	    int ng = ngroups;
	    if (ng > list.length - n) {
		ng = list.length - n;
	    }
	    if (ng > 0) {
		System.arraycopy(groups, 0, list, n, ng);
		n += ng;
	    }
	    if (recurse) {
		ngroupsSnapshot = ngroups;
		if (groups != null) {
		    groupsSnapshot = new ThreadGroup[ngroupsSnapshot];
		    System.arraycopy(groups, 0, groupsSnapshot, 0, ngroupsSnapshot);
		} else {
		    groupsSnapshot = null;
		}
	    }
	}
	if (recurse) {
	    for (int i = 0 ; i < ngroupsSnapshot ; i++) {
		n = groupsSnapshot[i].enumerate(list, n, true);
	    }
	}
	return n;
    }

    public final void stop() {
        if (stopOrSuspend(false))
            Thread.currentThread().stop();
    }

    public final void suspend() {
        if (stopOrSuspend(true))
            Thread.currentThread().suspend();
    }

    private boolean stopOrSuspend(boolean suspend) {
        boolean suicide = false;
        Thread us = Thread.currentThread();
	int ngroupsSnapshot;
	ThreadGroup[] groupsSnapshot = null;
	synchronized (this) {
	    for (int i = 0 ; i < nthreads ; i++) {
                if (threads[i]==us)
                    suicide = true;
                else if (suspend)
                    threads[i].suspend();
                else
                    threads[i].stop();
	    }

	    ngroupsSnapshot = ngroups;
	    if (groups != null) {
		groupsSnapshot = new ThreadGroup[ngroupsSnapshot];
		System.arraycopy(groups, 0, groupsSnapshot, 0, ngroupsSnapshot);
	    }
	}
	for (int i = 0 ; i < ngroupsSnapshot ; i++)
	    suicide = groupsSnapshot[i].stopOrSuspend(suspend) || suicide;

        return suicide;
    }
    public final void resume() {
	int ngroupsSnapshot;
	ThreadGroup[] groupsSnapshot;
	synchronized (this) {
	    for (int i = 0 ; i < nthreads ; i++) {
		threads[i].resume();
	    }
	    ngroupsSnapshot = ngroups;
	    if (groups != null) {
		groupsSnapshot = new ThreadGroup[ngroupsSnapshot];
		System.arraycopy(groups, 0, groupsSnapshot, 0, ngroupsSnapshot);
	    } else {
		groupsSnapshot = null;
	    }
	}
	for (int i = 0 ; i < ngroupsSnapshot ; i++) {
	    groupsSnapshot[i].resume();
	}
    }

    public final void destroy() {
	int ngroupsSnapshot;
	ThreadGroup[] groupsSnapshot;
	synchronized (this) {
	    if (destroyed || (nthreads > 0)) {
		throw new IllegalThreadStateException();
	    }
	    ngroupsSnapshot = ngroups;
	    if (groups != null) {
		groupsSnapshot = new ThreadGroup[ngroupsSnapshot];
		System.arraycopy(groups, 0, groupsSnapshot, 0, ngroupsSnapshot);
	    } else {
		groupsSnapshot = null;
	    }
	    if (parent != null) {
		destroyed = true;
		ngroups = 0;
		groups = null;
		nthreads = 0;
		threads = null;
	    }
	}
	for (int i = 0 ; i < ngroupsSnapshot ; i += 1) {
	    groupsSnapshot[i].destroy();
	}
	if (parent != null) {
	    parent.remove(this);
	}
    }


    private final void add(ThreadGroup g){
	synchronized (this) {
	    if (destroyed) {
		throw new IllegalThreadStateException();
	    }
	    if (groups == null) {
		groups = new ThreadGroup[4];
	    } else if (ngroups == groups.length) {
		ThreadGroup newgroups[] = new ThreadGroup[ngroups * 2];
		System.arraycopy(groups, 0, newgroups, 0, ngroups);
		groups = newgroups;
	    }
	    groups[ngroups] = g;

	    // This is done last so it doesn't matter in case the
	    // thread is killed
	    ngroups++;
	}
    }

    private void remove(ThreadGroup g) {
	synchronized (this) {
	    if (destroyed) {
		return;
	    }
	    for (int i = 0 ; i < ngroups ; i++) {
		if (groups[i] == g) {
		    ngroups -= 1;
		    System.arraycopy(groups, i + 1, groups, i, ngroups - i);
		    // Zap dangling reference to the dead group so that
		    // the garbage collector will collect it.
		    groups[ngroups] = null;
		    break;
		}
	    }
	    if (nthreads == 0) {
		notifyAll();
	    }
	    if (daemon && (nthreads == 0) && (ngroups == 0)) {
		destroy();
	    }
	}
    }
    
    void add(Thread t) {
	synchronized (this) {
	    if (destroyed) {
		throw new IllegalThreadStateException();
	    }
	    if (threads == null) {
		threads = new Thread[4];
	    } else if (nthreads == threads.length) {
		Thread newthreads[] = new Thread[nthreads * 2];
		System.arraycopy(threads, 0, newthreads, 0, nthreads);
		threads = newthreads;
	    }
	    threads[nthreads] = t;

	    // This is done last so it doesn't matter in case the
	    // thread is killed
	    nthreads++;
	}
    }

    void remove(Thread t) {
	synchronized (this) {
	    if (destroyed) {
		return;
	    }
	    for (int i = 0 ; i < nthreads ; i++) {
		if (threads[i] == t) {
		    System.arraycopy(threads, i + 1, threads, i, --nthreads - i);
		    // Zap dangling reference to the dead thread so that
		    // the garbage collector will collect it.
		    threads[nthreads] = null;
		    break;
		}
	    }
	    if (nthreads == 0) {
		notifyAll();
	    }
	    if (daemon && (nthreads == 0) && (ngroups == 0)) {
		destroy();
	    }
	}
    }

    public void list() {
	list(System.out, 0);
    }
    void list(PrintStream out, int indent) {
	int ngroupsSnapshot;
	ThreadGroup[] groupsSnapshot;
	synchronized (this) {
	    for (int j = 0 ; j < indent ; j++) {
		out.print(" ");
	    }
	    out.println(this);
	    indent += 4;
	    for (int i = 0 ; i < nthreads ; i++) {
		for (int j = 0 ; j < indent ; j++) {
		    out.print(" ");
		}
		out.println(threads[i]);
	    }
	    ngroupsSnapshot = ngroups;
	    if (groups != null) {
		groupsSnapshot = new ThreadGroup[ngroupsSnapshot];
		System.arraycopy(groups, 0, groupsSnapshot, 0, ngroupsSnapshot);
	    } else {
		groupsSnapshot = null;
	    }
	}
	for (int i = 0 ; i < ngroupsSnapshot ; i++) {
	    groupsSnapshot[i].list(out, indent);
	}
    }

    public void uncaughtException(Thread t, Throwable e) {
	if (parent != null) {
	    parent.uncaughtException(t, e);
	} else if (!(e instanceof ThreadDeath)) {
	    e.printStackTrace(System.err);
	}
    }

    public boolean allowThreadSuspension(boolean b) {
	return true;
    }

    public String toString() {
	return getClass().getName() + "[name=" + getName() + ",maxpri=" + maxPriority + "]";
    }
}
