package jx.devices;

import jx.zero.*;

public interface Screen extends Portal {
    public int getWidth();
    public int getHeight();
    public void moveCursorTo(int x, int y);
    public void putAt(int x, int y, char c);
    public void clear();
    public DeviceMemory getVideoMemory();
}
