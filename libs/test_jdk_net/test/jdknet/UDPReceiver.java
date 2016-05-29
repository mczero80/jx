package test.jdknet;

import java.net.*;
import java.io.*;
import java.lang.*;
import jx.zero.*;

public class UDPReceiver {
  public static void main(String [] args) throws IOException {
    String hostname;
    InetAddress address = null;
    DatagramPacket packet = null;
    DatagramSocket socket = null;
    int j = 0;
    int port;
    Object o = new Object();
    
    if (args.length > 0) {
      Debug.out.println("Setting port");
      port = new Integer(args[0]).intValue();
    }
    else {
      port = 2500;
    }

    Debug.out.println("Port: " + port);

    try {
      socket = new DatagramSocket(port);
    }
    catch (Exception e) {
      Debug.out.println("Socket konnte nicht erzeugt werden!");
      System.exit(0);
    }
    while (true) {
      byte[] buffer = new byte[2000];
      DatagramPacket toReceive = new DatagramPacket(buffer, 2000);
      Debug.out.println("Waiting to receive ... ");
      try {
	socket.receive(toReceive);
	Debug.out.println("Packet received");
      }
      catch (IOException e) {
	Debug.out.println("IOException!!");
      }
      Debug.out.println("Empfangen von: " + toReceive.getAddress());
      Debug.out.println("Länge: " + toReceive.getLength());
      Debug.out.println("Port: " + toReceive.getPort());
    }
  }
}









