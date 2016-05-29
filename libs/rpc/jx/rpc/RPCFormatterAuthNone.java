package jx.rpc;
import jx.rpc.*;
import jx.zero.debug.Dump;
import jx.xdr.Format;
public class RPCFormatterAuthNone {
     public static void write(RPCBuffer buf, jx.rpc.AuthNone obj) {
        Format.writeInt(buf,obj.length);
     }
     public static jx.rpc.AuthNone read(RPCBuffer buf) {
        jx.rpc.AuthNone obj;
        obj = new jx.rpc.AuthNone();
        obj.length= Format.readInt(buf);
        return obj;
     }
     public static int length(jx.rpc.AuthNone obj) {
       int _l = 0;
        _l += Format.lengthInt(obj.length);
       return _l;
     }
  }

