package jx.rpc;
import jx.rpc.*;
import jx.zero.debug.Dump;
import jx.xdr.Format;
public class RPCFormatterRPCMsgSuccess {
     public static void write(RPCBuffer buf, jx.rpc.RPCMsgSuccess obj) {
        RPCFormatterRPCMsgAccepted.write(buf, obj);	 
     }

     public static jx.rpc.RPCMsgSuccess read(RPCBuffer buf) {
        jx.rpc.RPCMsgSuccess obj;
        obj = new jx.rpc.RPCMsgSuccess();
        return obj;
     }
     public static int length(jx.rpc.RPCMsgSuccess obj) {
       int _l = 0;
       return _l;
     }
  }

