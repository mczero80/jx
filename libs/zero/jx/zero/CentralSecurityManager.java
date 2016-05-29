package jx.zero;

public interface CentralSecurityManager extends Portal {
    public void addDomainAndPrincipal(Domain d, Principal p);
    public Principal getPrincipal(Domain d);
    public void installInterceptor(Domain domain, String interceptorClass);
    public void inheritPrincipal(Domain source, Domain destination);
    public void inheritInterceptor(Domain source, Domain destination);
}
