package jx.rpc;
import jx.rpc.*;
import jx.zero.debug.Dump;
import jx.xdr.Format;
public class RPCFormatterRPCReply {
     public static void write(RPCBuffer buf, jx.rpc.RPCReply obj) {
	 RPCFormatterRPCMessage.write(buf, obj);
	 Format.writeInt(buf,obj.status);
	/*
        switch(obj.status) {
          case RPCReply.SWITCH_RPCMsgAccepted: RPCFormatterRPCMsgAccepted.write(buf, (RPCMsgAccepted)obj); break;
          case RPCReply.SWITCH_RPCMsgDenied: RPCFormatterRPCMsgDenied.write(buf, (RPCMsgDenied)obj); break;
        }
	*/
     }
     public static jx.rpc.RPCReply read(RPCBuffer buf) {
        jx.rpc.RPCReply obj;
        obj = new jx.rpc.RPCReply();
        obj.status= Format.readInt(buf);
        jx.rpc.RPCReply _obj = null;
        switch(obj.status) {
          case RPCReply.SWITCH_RPCMsgAccepted: _obj = RPCFormatterRPCMsgAccepted.read(buf); break;
          case RPCReply.SWITCH_RPCMsgDenied: _obj = RPCFormatterRPCMsgDenied.read(buf); break;
          default: System.err.println("Unknown switch in RPC message" + obj.status); throw new RuntimeException();
        }
_obj.status= obj.status;
        return _obj;
     }
     public static int length(jx.rpc.RPCReply obj) {
       int _l = 0;
        _l += Format.lengthInt(obj.status);
       return _l;
     }
  }

