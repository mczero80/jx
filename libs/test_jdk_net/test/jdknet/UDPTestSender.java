package test.jdknet;
import java.net.*;
import java.io.*;

public class UDPTestSender {
  public static void main(String [] args) throws IOException {
    String hostname;
    InetAddress address = null;
    DatagramPacket packet = null;
    DatagramSocket socket = null;
    int j = 0;
    Object o = new Object();
    boolean flood = false;

    if (args.length > 0) {
      System.out.println("Setting hostname");
      hostname = args[0];
      if (args.length == 2)
	flood = true;
    }
    else {
      hostname = "faui40c.informatik.uni-erlangen.de";
    }

    System.out.println("Zielrechner: " + hostname);

    byte[] buf = new byte[1000];
    for (int i=0; i<1000; i++) 
      buf[i] = (byte)(i*7);

    try {
      socket = new DatagramSocket(2500);
    }
    catch (Exception e) {
      System.out.println("Socket konnte nicht erzeugt werden!");
      System.exit(0);
    }
    try {
      address = InetAddress.getByName(hostname);
      System.out.println("Die Addresse von " + hostname + " ist " + address);
           }
    catch (UnknownHostException e) {
      System.out.println("Hostaddresse konnte nicht bestimmt werden!");
      System.exit(0);
    }
    while (true) {
      j++;
      System.out.println(".");
      if (j % 200 == 0)
	System.out.println("("+j+")");
      packet = new DatagramPacket(buf, buf.length, address, 6666); 
      if (!flood) {
	try{
	  Thread.sleep(1000);
	}
	catch (Exception e) {
	  System.out.println("Sleep() fehlgeschlagen!");
	}
      }
      try {
	socket.send(packet);
      }
      catch (Exception e) {
        System.out.println("Das UDP-Packet konnte nicht versendet werden!");
      }
    }
    
  }
}













