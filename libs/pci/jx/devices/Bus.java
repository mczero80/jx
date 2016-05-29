package jx.devices;

public interface Bus extends Device {
    public abstract Device getChild(int index);
}
