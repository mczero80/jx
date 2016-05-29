package java.net;

public final class DatagramPacket
{ 
  byte[] buff;
  int length;
  InetAddress addr;
  int port;
  
  public InetAddress getAddress()
    {
      return addr;
    }
  
  public byte[] getData()
    {
      return buff;
    }
  
  public int getLength()
    {
      return length;
    }
  
  public int getPort()
    {
      return port;
    }
  
  public DatagramPacket(byte ibuf[], int ilength)
    {
      buff = ibuf;
      length = ilength;
    }
  
  public DatagramPacket(byte ibuf[], int ilength, InetAddress iaddr, int iport)
    {
      this(ibuf, ilength);
      addr = iaddr;
      port = iport;
    }
}

