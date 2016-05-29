package jx.zero;

public interface DomainBorderOut extends DomainBorder,Portal {
    /** @return true=allow call; false=block call */
    boolean outBound(InterceptOutboundInfo info);
}
