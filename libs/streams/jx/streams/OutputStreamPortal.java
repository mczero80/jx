package jx.streams;

import jx.zero.Portal;
import java.io.IOException;

public interface OutputStreamPortal extends Portal {
    void write(int c) throws IOException;
    void flush() throws IOException;
}
