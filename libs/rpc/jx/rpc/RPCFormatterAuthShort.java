package jx.rpc;
import jx.rpc.*;
import jx.zero.debug.Dump;
import jx.xdr.Format;
public class RPCFormatterAuthShort {
     public static void write(RPCBuffer buf, jx.rpc.AuthShort obj) {
        Format.writeInt(buf,obj.length);
     }
     public static jx.rpc.AuthShort read(RPCBuffer buf) {
        jx.rpc.AuthShort obj;
        obj = new jx.rpc.AuthShort();
        obj.length= Format.readInt(buf);
        return obj;
     }
     public static int length(jx.rpc.AuthShort obj) {
       int _l = 0;
        _l += Format.lengthInt(obj.length);
       return _l;
     }
  }

