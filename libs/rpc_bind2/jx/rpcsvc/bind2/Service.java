package jx.rpcsvc.bind2;
import jx.rpc.*;
import jx.zero.debug.Dump;
import jx.xdr.Format;
import jx.zero.*;
class Service {
    int prog, vers, protocol, port;
    Service(int prog, int vers, int protocol, int port) {
	this.prog = prog;
	this.vers = vers;
	this.protocol = protocol;
	this.port = port;
    }
}
