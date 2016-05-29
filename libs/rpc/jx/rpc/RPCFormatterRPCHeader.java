package jx.rpc;
import jx.rpc.*;
import jx.zero.debug.Dump;
import jx.xdr.Format;
public class RPCFormatterRPCHeader {
     public static void write(RPCBuffer buf, jx.rpc.RPCHeader obj) {
        Format.writeInt(buf,obj.xid);
        RPCFormatterRPCMessage.write(buf, obj.msg);
     }
     public static jx.rpc.RPCHeader read(RPCBuffer buf) {
        jx.rpc.RPCHeader obj;
        obj = new jx.rpc.RPCHeader();
        obj.xid= Format.readInt(buf);
        obj.msg = (jx.rpc.RPCMessage)RPCFormatterRPCMessage.read(buf);
        return obj;
     }
     public static int length(jx.rpc.RPCHeader obj) {
       int _l = 0;
        _l += Format.lengthInt(obj.xid);
        _l += RPCFormatterRPCMessage.length(obj.msg);
       return _l;
     }
  }

