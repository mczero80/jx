package jx.zero;

public interface InterceptInboundInfo extends Portal {
    Domain getSourceDomain();
    Domain getTargetDomain();
    VMMethod getMethod();
    VMObject getServiceObject();
    /**
     * returns the first parameter of the intercepted method-call
     * (with succsessive calls of getNextParameter all parameters
     * can be observed )
     * @param obj is filled with the corresponding information
     */
    boolean getFirstParameter(VMObject result);
    boolean getNextParameter(VMObject result);
}
