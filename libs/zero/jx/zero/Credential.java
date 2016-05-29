package jx.zero;

public interface Credential extends Portal {
    void set(Object value);
    Object get();
    int getSignerDomainID();
}
