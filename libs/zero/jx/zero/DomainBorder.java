package jx.zero;

public interface DomainBorder extends Portal {
    boolean createPortal(PortalInfo info);
    void destroyPortal(PortalInfo info);
}
