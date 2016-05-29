package jx.rdp;

public class DataBlob {
    
    private byte[] data = null;
    private int size = 0;

    public DataBlob(int size, byte[] data) {
	this.size = size;
	this.data = data;
    }

    public int getSize() {
	return this.size;
    }

    public byte[] getData() {
	return this.data;
    }
}
