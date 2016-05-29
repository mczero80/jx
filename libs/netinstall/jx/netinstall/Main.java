package jx.netinstall;

import java.io.*;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;

import jx.rexec.*;

class Main {
    int localPort;
    DatagramSocket sock;

    static void writeInt(byte[] buf, int o, int d) { 
	buf[o+0] = (byte)( (d>>24) & 0xff);
	buf[o+1] = (byte)((d>>16)  & 0xff);
	buf[o+2] = (byte)((d>>8) & 0xff);
	buf[o+3] = (byte)(d & 0xff);
    }
    public static void usage() throws Exception {
	System.out.println("Parameters:  hostname componentfile");
    }

    public static void main(String[] args) throws Exception {
	new Main(args);
    }
	
    public Main(String[] args) throws Exception {
	tcp(args);
    }

    void tcp(String[] args) throws Exception {
	if (args.length != 2) { usage(); return ;}
	String filename = args[1];

	RandomAccessFile file = new RandomAccessFile(filename, "r");
	byte [] data = new byte[(int)file.length()];
	file.readFully(data);
	file.close();

	InetAddress addr = InetAddress.getByName(args[0]);
	Socket socket = new Socket(args[0], 6666);
	DataOutputStream out = new DataOutputStream(socket.getOutputStream());
	out.writeInt(Encoder.CMD_INSTALLZIP);
	out.writeUTF("MyDomain");
	out.writeUTF("test/Main");
	out.writeInt(data.length);
	out.write(data, 0, data.length);
	out.flush();
	for(;;);
    }

    public void udp(String[] args) throws Exception {

	if (args.length != 2) { usage(); return ;}
	String filename = args[1];

	RandomAccessFile file = new RandomAccessFile(filename, "r");
	byte [] data = new byte[(int)file.length()];
	file.readFully(data);
	file.close();

	byte buf[] = new byte[1300];

	sock = new DatagramSocket();
	localPort = sock.getLocalPort();
	DatagramPacket packet ;
	InetAddress addr = InetAddress.getByName(args[0]);

	int xid = 1;
	Encoder e = new Encoder(buf);
	e.writeInt(Encoder.MAGIC);
	e.writeInt(xid);
	e.writeInt(Encoder.CMD_CREATEDOMAIN);
	e.writeString("MyDomain");
	e.writeString("test/Main");
	packet = new DatagramPacket(buf, e.getLength(), addr, 6666);
	sock.send(packet);
	waitReply(xid++);
	System.out.println("Sent!");
	
	/*
	int chunks = (data.length+999) / 1000;
	for (int i=0; i<chunks; i++) {
	    e = new Encoder(buf);
	    e.writeInt(Encoder.MAGIC);
	    e.writeInt(xid);
	    e.writeInt(Encoder.CMD_DATA);
	    e.writeInt(i);
	    e.writeData(data, i*1000, 1000);
	    packet = new DatagramPacket(buf, e.getLength(), addr, 6666);
	    sock.send(packet);
	    waitReply(xid++);
	    System.out.println("Sent!");
	}
	*/
    }

    void waitReply(int waitforxid) throws Exception {
	DatagramPacket packet ;
	byte[]buf = new byte[1600];
	packet = new DatagramPacket(buf, buf.length);
	sock.receive(packet);
	Decoder d = new Decoder(buf, 0, packet.getLength());
	int magic = d.readInt();
	int xid = d.readInt();
	int cmd = d.readInt();
	if (xid == waitforxid) return;
	System.out.println("waitforxid="+waitforxid);
	System.out.println("xid="+xid);
	System.out.println("cmd="+cmd);
	throw new Error("wrong xid");
    }
}
