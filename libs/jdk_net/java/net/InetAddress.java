// Implementation of the Inetaddress class 
// for metaxa OS

// currently only a static HACK

package java.net;


public final class InetAddress
{
  final static InetAddress INADDR_ANY = new InetAddress(-1);

  final static byte[] faui48lIP = {(byte)131, (byte)188, 34, (byte)240 };
  final static byte[] faui45IP = {(byte)131, (byte)188, 34, (byte)45 };
  final static byte[] faui40pIP = {(byte)131, (byte)188, 34, (byte)77 };
  final static byte[] localIP = faui48lIP;
  final static String localName = "faui48l";

  public static InetAddress[] getAllByName(String host)  throws UnknownHostException  {
      InetAddress[] dst = new InetAddress[1];
      dst[0] = getByName(host);
      return dst;    
  }
  
  public static InetAddress getLocalHost()  throws UnknownHostException  {
    return new InetAddress(localIP);
  }
  
  public static InetAddress getByName(String host) throws UnknownHostException   {
    if (host == null) throw new UnknownHostException("hostname is null");
    if (host.equals("faui45")) return new InetAddress(faui45IP);
    if (host.equals("faui40p")) return new InetAddress(faui40pIP);
    if (host.equals(localName)) return new InetAddress(localIP);
    throw new UnknownHostException("unknown host " + host); 
  }


  private static int i(byte b)  {
    return b & 0xFF;
  }
  
  private static byte b(int i) {
      return (byte) i;
  }
  
  
  String hostname = null;
  byte[] addr;
  
  public boolean equals(Object obj)
    {
      if (!(obj instanceof InetAddress))
	return false;
      
      InetAddress a = (InetAddress) obj;
      
      if (a.addr.length != addr.length)
	return false;
      
      for (int i = 0; i < addr.length; i++)
	if (a.addr[i] != addr[i])
	  return false;
      
      return true;
    }
  
  public byte[] getAddress()  {
    return addr;
  }
  
  public String getHostName()  {
    return hostname;
  }
  
  public int hashCode()
    {
      byte[] v = new byte[4];
      
      for (int i = 0; i < addr.length; i++)
	v[i & 3] += addr[i];
      
      return	(i(v[0]) <<  0) +
	(i(v[1]) <<  8) +
	(i(v[2]) << 16) +
	(i(v[3]) << 24);
    }
  
  public String toString()
	{
		return	"" + i(addr[0]) +
			"." + i(addr[1]) +
			"." + i(addr[2]) +
			"." + i(addr[3]);
	}
  
  InetAddress(byte[] addr)
    {
      this.addr = addr;
    }
  
  InetAddress(int addr)
    {
      byte[] b = new byte[4];
      b[0] = b(addr);	addr >>>= 8;
      b[1] = b(addr);	addr >>>= 8;
      b[2] = b(addr);	addr >>>= 8;
      b[3] = b(addr);
      
      this.addr = b;
    }
}

