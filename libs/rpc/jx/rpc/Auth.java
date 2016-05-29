package jx.rpc;

public class Auth implements RPCData {
  public static final int SWITCH_AuthNone       = 0;
  public static final int SWITCH_AuthUnix       = 1;
  public static final int SWITCH_AuthShort      = 2;

  public int id;

  public Auth() {} 
  public Auth(int id) { this.id = id; } 

}
