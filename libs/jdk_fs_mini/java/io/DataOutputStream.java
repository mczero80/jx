package java.io;

public class DataOutputStream extends FilterOutputStream implements DataOutput {
    protected int written;

    public DataOutputStream(OutputStream out) {
	super(out);
    }

    public synchronized void write(int b) throws IOException {
	out.write(b);
	written++;
    }

    public synchronized void write(byte b[], int off, int len)throws IOException {
	out.write(b, off, len);
	written += len;
    }

    public void flush() throws IOException {
	out.flush();
    }

    public final void writeBoolean(boolean v) throws IOException {
	out.write(v ? 1 : 0);
	written++;
    }

    public final void writeByte(int v) throws IOException {
	out.write(v);
	written++;
    }

    public final void writeShort(int v) throws IOException {
	OutputStream out = this.out;
	out.write((v >>> 8) & 0xFF);
	out.write((v >>> 0) & 0xFF);
	written += 2;
    }

    public final void writeChar(int v) throws IOException {
	OutputStream out = this.out;
	out.write((v >>> 8) & 0xFF);
	out.write((v >>> 0) & 0xFF);
	written += 2;
    }

    public final void writeInt(int v) throws IOException {
	OutputStream out = this.out;
	out.write((v >>> 24) & 0xFF);
	out.write((v >>> 16) & 0xFF);
	out.write((v >>>  8) & 0xFF);
	out.write((v >>>  0) & 0xFF);
	written += 4;
    }

    public final void writeLong(long v) throws IOException {
	OutputStream out = this.out;
	out.write((int)(v >>> 56) & 0xFF);
	out.write((int)(v >>> 48) & 0xFF);
	out.write((int)(v >>> 40) & 0xFF);
	out.write((int)(v >>> 32) & 0xFF);
	out.write((int)(v >>> 24) & 0xFF);
	out.write((int)(v >>> 16) & 0xFF);
	out.write((int)(v >>>  8) & 0xFF);
	out.write((int)(v >>>  0) & 0xFF);
	written += 8;
    }

    public final void writeFloat(float v) throws IOException {
	writeInt(Float.floatToIntBits(v));
    }

    public final void writeDouble(double v) throws IOException {
	writeLong(Double.doubleToLongBits(v));
    }

    public final void writeBytes(String s) throws IOException {
	OutputStream out = this.out;
	int len = s.length();
	for (int i = 0 ; i < len ; i++) {
	    out.write((byte)s.charAt(i));
	}
	written += len;
    }

    public final void writeChars(String s) throws IOException {
	OutputStream out = this.out;
	int len = s.length();
	for (int i = 0 ; i < len ; i++) {
	    int v = s.charAt(i);
	    out.write((v >>> 8) & 0xFF);
	    out.write((v >>> 0) & 0xFF);
	}
	written += len * 2;
    }

    public final void writeUTF(String str) throws IOException {
	OutputStream out = this.out;
	int strlen = str.length();
	int utflen = 0;

	for (int i = 0 ; i < strlen ; i++) {
	    int c = str.charAt(i);
	    if ((c >= 0x0001) && (c <= 0x007F)) {
		utflen++;
	    } else if (c > 0x07FF) {
		utflen += 3;
	    } else {
		utflen += 2;
	    }
	}

	if (utflen > 65535)
	    throw new UTFDataFormatException();		  

	out.write((utflen >>> 8) & 0xFF);
	out.write((utflen >>> 0) & 0xFF);
	for (int i = 0 ; i < strlen ; i++) {
	    int c = str.charAt(i);
	    if ((c >= 0x0001) && (c <= 0x007F)) {
		out.write(c);
	    } else if (c > 0x07FF) {
		out.write(0xE0 | ((c >> 12) & 0x0F));
		out.write(0x80 | ((c >>  6) & 0x3F));
		out.write(0x80 | ((c >>  0) & 0x3F));
		written += 2;
	    } else {
		out.write(0xC0 | ((c >>  6) & 0x1F));
		out.write(0x80 | ((c >>  0) & 0x3F));
		written += 1;
	    }
	}
	written += strlen + 2;
    }

    public final int size() {
	return written;
    }
}
