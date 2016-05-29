package jx.devices;

import jx.zero.Portal;

public interface Keyboard extends Portal {
    public void addKeyListener(KeyListener listener);
    public int getc();
    public int getcode();
}
