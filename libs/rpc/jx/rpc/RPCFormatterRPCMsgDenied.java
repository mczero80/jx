package jx.rpc;
import jx.rpc.*;
import jx.zero.debug.Dump;
import jx.xdr.Format;
public class RPCFormatterRPCMsgDenied {
     public static void write(RPCBuffer buf, jx.rpc.RPCMsgDenied obj) {
        Format.writeInt(buf,obj.status);
        switch(obj.status) {
          case RPCMsgDenied.SWITCH_RPCMsgRejectMismatch: RPCFormatterRPCMsgRejectMismatch.write(buf, (RPCMsgRejectMismatch)obj); break;
          case RPCMsgDenied.SWITCH_RPCMsgRejectAuth: RPCFormatterRPCMsgRejectAuth.write(buf, (RPCMsgRejectAuth)obj); break;
        }
     }
     public static jx.rpc.RPCMsgDenied read(RPCBuffer buf) {
        jx.rpc.RPCMsgDenied obj;
        obj = new jx.rpc.RPCMsgDenied();
        obj.status= Format.readInt(buf);
        jx.rpc.RPCMsgDenied _obj = null;
        switch(obj.status) {
          case RPCMsgDenied.SWITCH_RPCMsgRejectMismatch: _obj = RPCFormatterRPCMsgRejectMismatch.read(buf); break;
          case RPCMsgDenied.SWITCH_RPCMsgRejectAuth: _obj = RPCFormatterRPCMsgRejectAuth.read(buf); break;
          default: System.err.println("Unknown switch in RPC message" + obj.status); throw new RuntimeException();
        }
_obj.status= obj.status;
        return _obj;
     }
     public static int length(jx.rpc.RPCMsgDenied obj) {
       int _l = 0;
        _l += Format.lengthInt(obj.status);
       return _l;
     }
  }

