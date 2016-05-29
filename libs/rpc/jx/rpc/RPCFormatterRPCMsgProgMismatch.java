package jx.rpc;
import jx.rpc.*;
import jx.zero.debug.Dump;
import jx.xdr.Format;
public class RPCFormatterRPCMsgProgMismatch {
     public static void write(RPCBuffer buf, jx.rpc.RPCMsgProgMismatch obj) {
        RPCFormatterRPCMsgAccepted.write(buf, obj);	 
	Format.writeInt(buf,obj.low);
        Format.writeInt(buf,obj.high);
     }
     public static jx.rpc.RPCMsgProgMismatch read(RPCBuffer buf) {
        jx.rpc.RPCMsgProgMismatch obj;
        obj = new jx.rpc.RPCMsgProgMismatch();
        obj.low= Format.readInt(buf);
        obj.high= Format.readInt(buf);
        return obj;
     }
     public static int length(jx.rpc.RPCMsgProgMismatch obj) {
       int _l = 0;
        _l += Format.lengthInt(obj.low);
        _l += Format.lengthInt(obj.high);
       return _l;
     }
  }

