package jx.rpc;
import jx.rpc.*;
import jx.zero.debug.Dump;
import jx.zero.Debug;
import jx.xdr.Format;
public class RPCFormatterAuthUnix {
     public static void write(RPCBuffer buf, jx.rpc.AuthUnix obj) {
        Format.writeInt(buf,length(obj));
        Format.writeInt(buf,obj.stamp);
        Format.writeString(buf,obj.machinename);
        Format.writeInt(buf,obj.uid);
        Format.writeInt(buf,obj.gid);
        Format.writeIntArray(buf,obj.gids);
     }
     public static jx.rpc.AuthUnix read(RPCBuffer buf) {
        jx.rpc.AuthUnix obj;
        obj = new jx.rpc.AuthUnix();
        int len = Format.readInt(buf); 
        obj.stamp= Format.readInt(buf);
	//Debug.out.println("RPCFormatterAuthUnix STAMP: "+obj.stamp);
        obj.machinename= Format.readString(buf);
	//Debug.out.println("RPCFormatterAuthUnix MACHINE: "+obj.machinename);
        obj.uid= Format.readInt(buf);
	//Debug.out.println("RPCFormatterAuthUnix UID: "+obj.uid);
        obj.gid= Format.readInt(buf);
	//Debug.out.println("RPCFormatterAuthUnix GID: "+obj.gid);
        obj.gids= Format.readIntArray(buf);
	//Debug.out.println("RPCFormatterAuthUnix GIDS.length: "+obj.gids.length);
        return obj;
     }
     public static int length(jx.rpc.AuthUnix obj) {
       int _l = 0;
        _l += Format.lengthInt(obj.stamp);
        _l += Format.lengthString(obj.machinename);
        _l += Format.lengthInt(obj.uid);
        _l += Format.lengthInt(obj.gid);
        _l += Format.lengthIntArray(obj.gids);
       return _l;
     }
  }

