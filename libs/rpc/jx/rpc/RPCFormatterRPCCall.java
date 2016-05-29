package jx.rpc;
import jx.rpc.*;
import jx.zero.debug.Dump;
import jx.zero.Debug;
import jx.xdr.Format;
public class RPCFormatterRPCCall {
     public static void write(RPCBuffer buf, jx.rpc.RPCCall obj) {
        Format.writeInt(buf,obj.version);
        Format.writeInt(buf,obj.prog);
        Format.writeInt(buf,obj.progVersion);
        Format.writeInt(buf,obj.proc);
        RPCFormatterAuth.write(buf, obj.a);
        RPCFormatterAuth.write(buf, obj.c);
     }
     public static jx.rpc.RPCCall read(RPCBuffer buf) {
        jx.rpc.RPCCall obj;
        obj = new jx.rpc.RPCCall();
	if (buf.buf.size() <= 18) {
	    Dump.xdump1(buf.buf, 0, 128);
	}
        obj.version= Format.readInt(buf);
        obj.prog= Format.readInt(buf);
        obj.progVersion= Format.readInt(buf);
        obj.proc= Format.readInt(buf);
	//Debug.out.println("RPCFormatterRPCCall PROG: "+obj.prog);
	//Debug.out.println("RPCFormatterRPCCall PROC: "+obj.proc);
        obj.a = (jx.rpc.Auth)RPCFormatterAuth.read(buf);
        obj.c = (jx.rpc.Auth)RPCFormatterAuth.read(buf);
        return obj;
     }
     public static int length(jx.rpc.RPCCall obj) {
       int _l = 0;
        _l += Format.lengthInt(obj.version);
        _l += Format.lengthInt(obj.prog);
        _l += Format.lengthInt(obj.progVersion);
        _l += Format.lengthInt(obj.proc);
        _l += RPCFormatterAuth.length(obj.a);
        _l += RPCFormatterAuth.length(obj.c);
       return _l;
     }
  }

