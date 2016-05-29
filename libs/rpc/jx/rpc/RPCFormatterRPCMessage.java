package jx.rpc;
import jx.rpc.*;
import jx.zero.debug.Dump;
import jx.xdr.Format;
public class RPCFormatterRPCMessage {
     public static void write(RPCBuffer buf, jx.rpc.RPCMessage obj) {
	 if (obj.msgType == RPCMessage.SWITCH_RPCCall) {
	     Format.writeInt(buf,obj.xid);
	     Format.writeInt(buf,obj.msgType);
	     
	     switch(obj.msgType) {
	     case RPCMessage.SWITCH_RPCCall: RPCFormatterRPCCall.write(buf, (RPCCall)obj); break;
	     case RPCMessage.SWITCH_RPCReply: RPCFormatterRPCReply.write(buf, (RPCReply)obj); break;
	     }
	 } else { // TODO: HACK; this else branch should be sufficient for call
	     Format.writeInt(buf,obj.xid);
	     Format.writeInt(buf,obj.msgType);	     
	 }
     }
     public static jx.rpc.RPCMessage read(RPCBuffer buf) {
        jx.rpc.RPCMessage obj;
        obj = new jx.rpc.RPCMessage();
        obj.xid= Format.readInt(buf);
        obj.msgType= Format.readInt(buf);
        jx.rpc.RPCMessage _obj = null;
        switch(obj.msgType) {
          case RPCMessage.SWITCH_RPCCall: _obj = RPCFormatterRPCCall.read(buf); break;
          case RPCMessage.SWITCH_RPCReply: _obj = RPCFormatterRPCReply.read(buf); break;
          default: {
	      jx.zero.Debug.out.println("Unknown switch in RPC message" + obj.msgType);
	      jx.zero.Debug.out.println("          xid=" + obj.xid);
	      buf.xdump();
	      throw new RuntimeException();
	  }
        }
_obj.xid= obj.xid;_obj.msgType= obj.msgType;
        return _obj;
     }
     public static int length(jx.rpc.RPCMessage obj) {
       int _l = 0;
        _l += Format.lengthInt(obj.xid);
        _l += Format.lengthInt(obj.msgType);
       return _l;
     }
  }

