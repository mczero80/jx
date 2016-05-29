package jx.streams;

import jx.zero.Portal;
import java.io.IOException;

public interface InputStreamPortal extends Portal {
    int read() throws IOException;
}
