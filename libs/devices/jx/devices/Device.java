package jx.devices;

/**
 * The top-level device interface. All devices must implement this interface.
 * @author Michael Golm
 */
public interface Device {

    /**
     * Return all configurations that are supported by this device.
     */
    DeviceConfigurationTemplate[] getSupportedConfigurations ();

    /**
     * Initialize the device.
     */
    public void open(DeviceConfiguration conf);

    /**
     * Release all resources associated with the physical device.
     */
    public void close();
}
