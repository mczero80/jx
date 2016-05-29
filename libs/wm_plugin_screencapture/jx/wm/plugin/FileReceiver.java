package jx.wm.plugin;

import java.io.*;
import java.net.*;
import jx.formats.*;

class FileReceiver {
    public static void main(String[] args) throws Exception {
	final int port = Integer.parseInt(args[1]);
	ServerSocket ssock = new ServerSocket(port);
	System.out.println("Accepting connections");
	Socket sock = ssock.accept();
	System.out.println("Connection established.");
	InputStream in = new BufferedInputStream(sock.getInputStream());
	LittleEndianInputStream lin = new LittleEndianInputStream(in);
	int width = lin.readInt();
	int height = lin.readInt();
	int size = lin.readInt();
	OutputStream out = new BufferedOutputStream(new FileOutputStream(args[0]));
	System.out.println("Size: "+size);
	System.out.println("Width: "+width);
	System.out.println("Height: "+height);
	for(int i=0;i<size;i++) {
	    int c = in.read();
	    out.write(c);
	}
	out.close();
	System.out.println("Done.");
    }
}
