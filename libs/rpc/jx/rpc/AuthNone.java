package jx.rpc;

public class AuthNone extends Auth implements RPCData {
  public int length = 0;
    public AuthNone() {
	super(Auth.SWITCH_AuthNone);
    }
}
