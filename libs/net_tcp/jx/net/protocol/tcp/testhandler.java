package jx.net.protocol.tcp;

import jx.zero.*;

public class testhandler implements DataHandler { 
    public testhandler() {
	Debug.out.println("Testhandler");
    }
    public void rec() {
	Debug.out.println("rec!");
    }
}
