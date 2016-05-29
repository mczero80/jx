package jx.rpcsvc.bind2;
import jx.rpc.*;
import jx.zero.debug.Dump;
import jx.xdr.Format;
import jx.zero.*;
import java.util.Vector;
public class RPCBind_Impl implements RPCBind {
    RPC rpc;
    Vector services=new Vector();
    private static final boolean debug = true;
  public RPCBind_Impl(jx.rpc.RPC r)  { 
    this.rpc = r;
  }
  public void nullproc() {
  }
  public void nullproc1() {
  }
  public void nullproc2() {
  }
  public int getaddr(jx.rpcsvc.bind2.RPCB b) {
      if (debug) Debug.out.println("getaddr:"+b.prog+","+b.vers+","+b.protocol+","+b.args);
      for(int i=0; i<services.size(); i++) {
	  Service s = (Service)services.elementAt(i);
	  if (s.prog == b.prog && s.vers == b.vers && s.protocol == b.protocol) {
	      Debug.out.println("getaddr result:"+s.port);
	      return s.port;
	  }
      }
      Debug.out.println("getaddr NO result");
      return 0;
  }

    // additional methods

    public void registerService(int prog, int vers, int protocol, int port) {
      if (debug) Debug.out.println("BIND: register:"+prog+","+vers+","+protocol+","+port);
	services.addElement(new Service(prog, vers, protocol, port));
    }

}
