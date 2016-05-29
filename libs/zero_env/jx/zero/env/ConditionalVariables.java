package jx.zero.env;

public class ConditionalVariables {
    static final public void impl_wait(Object obj) {
	throw new Error("wait not implemented");
    }
    static final public void impl_notify(Object obj) {
	throw new Error("notify not implemented");
    }
    static final public void impl_notifyAll(Object obj) {
	throw new Error("notifyAll not implemented");
    }
}
