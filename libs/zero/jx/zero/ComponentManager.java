package jx.zero;

public interface ComponentManager extends Portal {
    void registerLib(String name,Memory libcode);

    /** @return componentID */
    int load(String name);

    /** if this class is used as a service object its service thread is inherited
     * to all services it creates */
    void setInheritThread(String classname);
}
