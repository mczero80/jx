package jx.devices;

public class DeviceInfo {
    String name;
    String description;
    String vendor;
    String version;

    public DeviceInfo(String name, String description, String vendor, String version) {
	this.name = name;
	this.description = description;
	this.vendor = vendor;
	this.version = version;
    }
}
