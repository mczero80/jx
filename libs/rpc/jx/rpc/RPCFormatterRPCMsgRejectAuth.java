package jx.rpc;
import jx.rpc.*;
import jx.zero.debug.Dump;
import jx.xdr.Format;
public class RPCFormatterRPCMsgRejectAuth {
     public static void write(RPCBuffer buf, jx.rpc.RPCMsgRejectAuth obj) {
        Format.writeInt(buf,obj.authStatus);
     }
     public static jx.rpc.RPCMsgRejectAuth read(RPCBuffer buf) {
        jx.rpc.RPCMsgRejectAuth obj;
        obj = new jx.rpc.RPCMsgRejectAuth();
        obj.authStatus= Format.readInt(buf);
        return obj;
     }
     public static int length(jx.rpc.RPCMsgRejectAuth obj) {
       int _l = 0;
        _l += Format.lengthInt(obj.authStatus);
       return _l;
     }
  }

