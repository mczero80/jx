package jx.zero.debug;

import java.io.InputStream;
import java.io.IOException;


public class DebugInputStream extends InputStream {
    DebugChannel channel;

    public DebugInputStream(DebugChannel c) {
	this.channel = c;
    }
    
    public int read() throws IOException {
	return channel.read();
    }
}
