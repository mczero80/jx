package java.io;

public class ByteArrayOutputStream extends OutputStream {
    private byte[] buffer;
    private int count;

    public ByteArrayOutputStream() { this(32); }
    public ByteArrayOutputStream(int size) { buffer = new byte[size]; }

    public synchronized void write(int b) {
	int newcount = count + 1;
	if (newcount > buffer.length) {
	    byte newbuffer[] = new byte[Math.max(buffer.length << 1, newcount)];
	    System.arraycopy(buffer, 0, newbuffer, 0, count);
	    buffer = newbuffer;
	}
	buffer[count] = (byte)b;
	count = newcount;
    }

    public synchronized void write(byte b[], int off, int len) {
	int newcount = count + len;
	if (newcount > buffer.length) {
	    byte newbuffer[] = new byte[Math.max(buffer.length << 1, newcount)];
	    System.arraycopy(buffer, 0, newbuffer, 0, count);
	    buffer = newbuffer;
	}
	System.arraycopy(b, off, buffer, count, len);
	count = newcount;
    }

    public synchronized void writeTo(OutputStream out) throws IOException {
	out.write(buffer, 0, count);
    }

    public synchronized void reset() {
	count = 0;
    }

    public synchronized byte[] toByteArray() {
	byte newbuffer[] = new byte[count];
	System.arraycopy(buffer, 0, newbuffer, 0, count);
	return newbuffer;
    }

    public int size() {
	return count;
    }


    public String toString() {
	return new String(buffer, 0, count);
    }

    public String toString(String enc) throws UnsupportedEncodingException {
	return new String(buffer, 0, count, enc);
    }

    public String toString(int hibyte) {
	return new String(buffer, hibyte, 0, count);
    }

}
