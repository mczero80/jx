package jx.net;

import jx.zero.Debug;

public class IPAddress {
    int addr;
    byte[] bytes;

    public IPAddress(byte[] b) {
	this(b[0], b[1], b[2], b[3]);
    }
    public IPAddress(int a, int b, int c, int d) {
	this(toIPAddr(a,b,c,d));	
    }
    public IPAddress(String a) {
	this(parseIPAddr(a));
    }
    public IPAddress(int a) {
	addr = a;
	bytes = toBytes();
    }
    public int getAddress() {
	return addr;
    }
    private static int toIPAddr(int a, int b, int c, int d) {
	return ((((((d & 0xff) << 8) | (c & 0xff)) << 8) | (b & 0xff)) << 8) | (a & 0xff);
    }
    private static int parseIPAddr(String addr) {
	byte[] ip = new byte[4];
	int cut0 = 0;
	for(int i=0; i<3; i++) {
	    int cut = addr.indexOf(".", cut0);
	    if (cut == -1) {
		return -1;
	    }
	    String num = addr.substring(cut0, cut);
	    if (num == null) {
		Debug.out.println("NO IP ADDRESS at IPAddress");
		return -1;
	    }
	    ip[i] = (byte)Integer.parseInt(num);
	    cut0 = cut+1;
	}
	ip[3] = (byte)Integer.parseInt(addr.substring(cut0, addr.length()));
	return toIPAddr(ip[0], ip[1], ip[2], ip[3]);
    }

    public String getHostName() { return toString();}// TODO
    //public static IPAddress getLocalHost() { return null; } // TODO

    public String toString() {
	StringBuffer buf = new StringBuffer();
	int a = addr;
	for(int i=0; i<4; i++) {
	    buf.append(a&0xff);
	    a >>= 8;
	    if (i+1<4) buf.append(".");
	}
	return buf.toString();
    }

    public boolean equals(Object o) {
	if (! (o instanceof IPAddress)) return false;
	return ((IPAddress)o).addr == addr;
    }

    public byte[] getBytes() {
	return bytes;
    }

    private byte[] toBytes() {
	byte[] ret = new byte[4];
	ret[3] = (byte)( (addr>>24) & 0xff);
	ret[2] = (byte)((addr>>16)  & 0xff);
	ret[1] = (byte)((addr>>8) & 0xff);
	ret[0] = (byte)(addr & 0xff);
	return ret;
    }
}
