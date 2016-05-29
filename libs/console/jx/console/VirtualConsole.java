package jx.console;

import java.io.InputStream;
import java.io.OutputStream;
/**
 * A virtual console.
 */

public interface VirtualConsole {
    public void putc(int c);
    public int getc();
    public void activate();
    public void deactivate();
    public InputStream getInputStream();
    public OutputStream getOutputStream();
    public OutputStream getErrorStream();
}

