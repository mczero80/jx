package jx.zero;

public interface DomainBorderIn extends DomainBorder, Portal {
    /** @return true=allow call; false=block call */
    boolean inBound(InterceptInboundInfo info);
}
