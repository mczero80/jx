package jx.zero;

public interface FBEmulation extends Portal {
    public DeviceMemory getVideoMemory();
    public int getWidth();
    public int getHeight();
    public int getBytesPerLine();
    public int getBitsPerPixel();
    /** update must be called for video memory changes to become effective */
    public void update();
    /** @return true if emulated input devices are available */
    public boolean inputDevicesAvailable();
    /** @return true if event happend; event object is filled with data */
    public boolean checkEvent(FBEmulationEvent event);
    public boolean open(int mode);
}
