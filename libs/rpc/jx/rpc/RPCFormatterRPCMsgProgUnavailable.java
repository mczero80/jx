package jx.rpc;
import jx.rpc.*;
import jx.zero.debug.Dump;
import jx.xdr.Format;
public class RPCFormatterRPCMsgProgUnavailable {
     public static void write(RPCBuffer buf, jx.rpc.RPCMsgProgUnavailable obj) {
        RPCFormatterRPCMsgAccepted.write(buf, obj);	 
     }
     public static jx.rpc.RPCMsgProgUnavailable read(RPCBuffer buf) {
        jx.rpc.RPCMsgProgUnavailable obj;
        obj = new jx.rpc.RPCMsgProgUnavailable();
        return obj;
     }
     public static int length(jx.rpc.RPCMsgProgUnavailable obj) {
       int _l = 0;
       return _l;
     }
  }

