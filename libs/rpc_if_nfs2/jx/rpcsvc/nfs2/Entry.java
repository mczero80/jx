package jx.rpcsvc.nfs2;

public class Entry implements jx.rpc.RPCData {
  public int fileid;
  public Name name;
  public NFSCookie cookie;
  public Entries next;
    
    public Entry() {}
    public Entry(int fileid, Name name, NFSCookie cookie) {
	this.fileid = fileid;
	this.name = name;
	this.cookie = cookie;
    }

}


