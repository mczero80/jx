package jx.zero;

public interface Naming extends Portal {
    void registerPortal(Portal portal, String name);
    Portal lookup(String name);
    
    Portal lookupOrWait(String name); //THIS ONLY WORKS WHEN A THREAD POOL IS USED FOR SERVICES
}
