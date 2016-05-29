package jx.console;

/**
 * A console with virtual screens.
 */

public interface Console {
    public VirtualConsole createVirtualConsole();
    public void switchTo(VirtualConsole cons);
}

