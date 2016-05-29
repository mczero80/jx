package jx.rpc;
import jx.rpc.*;
import jx.zero.debug.Dump;
import jx.xdr.Format;
import jx.zero.*;
public class RPCFormatterAuth {
     public static void write(RPCBuffer buf, jx.rpc.Auth obj) {
        Format.writeInt(buf,obj.id);
        switch(obj.id) {
          case Auth.SWITCH_AuthNone: RPCFormatterAuthNone.write(buf, (AuthNone)obj); break;
          case Auth.SWITCH_AuthUnix: RPCFormatterAuthUnix.write(buf, (AuthUnix)obj); break;
          case Auth.SWITCH_AuthShort: RPCFormatterAuthShort.write(buf, (AuthShort)obj); break;
        }
     }
     public static jx.rpc.Auth read(RPCBuffer buf) {
        jx.rpc.Auth obj;
        obj = new jx.rpc.Auth();
        obj.id= Format.readInt(buf);
	//Debug.out.println("RPCFormatterRPCAuth ID: "+obj.id);
	//buf.dumpUnprocessed();
	jx.rpc.Auth _obj = null;
        switch(obj.id) {
          case Auth.SWITCH_AuthNone: _obj = RPCFormatterAuthNone.read(buf); break;
          case Auth.SWITCH_AuthUnix: _obj = RPCFormatterAuthUnix.read(buf); break;
          case Auth.SWITCH_AuthShort: _obj = RPCFormatterAuthShort.read(buf); break;
          default: System.err.println("Unknown switch in RPC message" + obj.id); throw new RuntimeException();
        }
_obj.id= obj.id;
        return _obj;
     }
     public static int length(jx.rpc.Auth obj) {
       int _l = 0;
        _l += Format.lengthInt(obj.id);
       return _l;
     }
  }

