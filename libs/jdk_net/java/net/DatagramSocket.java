package java.net;

import java.io.IOException;
//import java.io.FileDescriptor;

import jx.net.IPAddress;
import jx.net.NetInit;
import jx.net.UDPSender;
import jx.net.UDPReceiver;

import jx.net.UDPData;

public class DatagramSocket
{ 
    //FileDescriptor fd;
  int port = 0;

  protected void finalize() {
      /*if (fd != null)
	close();*/
  }
  
  public synchronized void close() {
      // fd = null;
  }
  
  public void receive(DatagramPacket p) throws IOException  {
    /*
    Buffer first = null;

    for(;;) {
      Buffer x = p.getFirst();
      if (x != first) {
	first = x;
	byte[] b = first.getData();
	dumpPacket(b);
      }
    }

    //recv(fd, p.buff, p.addr.addr, other);
    p.length = 0;
    p.port = 0;
    */
    /* synchronized(this)  { 
       try { wait(); } catch(InterruptedException e) {}
     }
    */
  }
  
  public void send(DatagramPacket p) throws IOException {
      /*
    int len = UDP.requiresSpace();
    byte[] buf = new byte[len+p.length];
    System.arraycopy(p.buff, 0, buf, len, p.length);

    try {
    UDP.transmit(p.addr.addr, port, p.port, buf);
    } catch(metaxa.os.UnknownIPAddressException e) {
      throw new IOException();
    }
      */
  }
  
  public int getLocalPort()   {
      return port;
    }
  
  public DatagramSocket(int port) throws SocketException   {
    this.port = port;
  }
  
  public DatagramSocket() throws SocketException {
      this(0);
    }
}

