package jx.rpc;
import jx.rpc.*;
import jx.zero.debug.Dump;
import jx.xdr.Format;
public class RPCFormatterRPCMsgRejectMismatch {
     public static void write(RPCBuffer buf, jx.rpc.RPCMsgRejectMismatch obj) {
        Format.writeInt(buf,obj.low);
        Format.writeInt(buf,obj.high);
     }
     public static jx.rpc.RPCMsgRejectMismatch read(RPCBuffer buf) {
        jx.rpc.RPCMsgRejectMismatch obj;
        obj = new jx.rpc.RPCMsgRejectMismatch();
        obj.low= Format.readInt(buf);
        obj.high= Format.readInt(buf);
        return obj;
     }
     public static int length(jx.rpc.RPCMsgRejectMismatch obj) {
       int _l = 0;
        _l += Format.lengthInt(obj.low);
        _l += Format.lengthInt(obj.high);
       return _l;
     }
  }

