package test.portal.scale;

import java.io.*;
import jx.zero.*;

public interface Connection extends Portal {
    public int read() throws IOException;
    public void write(int c) throws IOException;
}
