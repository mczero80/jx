package jx.rpc;
import jx.rpc.*;
import jx.zero.debug.Dump;
import jx.xdr.Format;
public class RPCFormatterRPCMsgAccepted {
     public static void write(RPCBuffer buf, jx.rpc.RPCMsgAccepted obj) {
	 RPCFormatterRPCReply.write(buf, obj);
	 RPCFormatterAuth.write(buf, obj.verf);
	 Format.writeInt(buf,obj.stat);
	 /*
	   switch(obj.stat) {
	   case RPCMsgAccepted.SWITCH_RPCMsgSuccess: RPCFormatterRPCMsgSuccess.write(buf, (RPCMsgSuccess)obj); break;
	   case RPCMsgAccepted.SWITCH_RPCMsgProgMismatch: RPCFormatterRPCMsgProgMismatch.write(buf, (RPCMsgProgMismatch)obj); break;
	   }
	 */
     }
     public static jx.rpc.RPCMsgAccepted read(RPCBuffer buf) {
        jx.rpc.RPCMsgAccepted obj;
        obj = new jx.rpc.RPCMsgAccepted();
        obj.verf = (jx.rpc.Auth)RPCFormatterAuth.read(buf);
        obj.stat= Format.readInt(buf);
        jx.rpc.RPCMsgAccepted _obj = null;
        switch(obj.stat) {
          case RPCMsgAccepted.SWITCH_RPCMsgSuccess: _obj = RPCFormatterRPCMsgSuccess.read(buf); break;
          case RPCMsgAccepted.SWITCH_RPCMsgProgMismatch: _obj = RPCFormatterRPCMsgProgMismatch.read(buf); break;
          default: System.err.println("Unknown switch in RPC message" + obj.stat); throw new RuntimeException();
        }
_obj.verf= obj.verf;_obj.stat= obj.stat;
        return _obj;
     }
     public static int length(jx.rpc.RPCMsgAccepted obj) {
       int _l = 0;
        _l += RPCFormatterAuth.length(obj.verf);
        _l += Format.lengthInt(obj.stat);
       return _l;
     }
  }

