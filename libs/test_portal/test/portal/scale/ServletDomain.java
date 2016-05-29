package test.portal.scale;

import jx.zero.*;
import jx.zero.debug.*;
import test.portal.*;
import java.io.*;

public class ServletDomain {

    public static void init(Naming naming, String[] args, Object[] portals) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));

	// lookup portal
	//Connection con = (Connection) naming.lookup("Connection");
	Connection con = (Connection) portals[0];

	//Debug.out.println("Started first stage");
	try {
	    for(int i=0; i<100; i++) {
		con.read();
	    }
	} catch(IOException e) {
	}
	//Debug.out.println("Completed first stage");

	DomainManager domainManager = (DomainManager) naming.lookup("DomainManager");
	domainManager.terminateCaller();
    }
}
