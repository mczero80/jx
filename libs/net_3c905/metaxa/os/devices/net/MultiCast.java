package metaxa.os.devices.net;

class MultiCast {
    private short flags;
    private byte[] addr;
          
    public MultiCast(short f, byte[] a) {
	flags = f;
	for (int i=0; i<6; i++)
	    addr[i] = a[i];
    }
    
    public short flags() {
	return flags;
    }
    public byte addr(int i) {
	return addr[i];
    }
}
