package javafs;

import jx.fs.FSException;

public class BufferIOException extends FSException {
    public BufferIOException() {
	super();
    }

    public BufferIOException(String msg) {
	super(msg);
    }
}
