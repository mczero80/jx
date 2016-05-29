package jx.compiler.execenv;

import java.io.OutputStream;
import java.io.IOException;

public interface IOSystem {
    public OutputStream getOutputStream(String filename) throws IOException;
    public void set(String path);
}
