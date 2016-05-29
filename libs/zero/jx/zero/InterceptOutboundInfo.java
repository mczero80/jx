package jx.zero;

public interface InterceptOutboundInfo extends Portal {
    Domain getSourceDomain();
    Domain getTargetDomain();
    VMMethod getMethod();
    VMObject getServiceObject();
    VMObject[] getParameters();
}
