package jx.rpc;

public class AuthUnix extends Auth implements RPCOpaque {
    { id = SWITCH_AuthUnix; }

  public int stamp; /* random number */
  public String machinename; /* max len 255 */
  public int uid;
  public int gid;
  public int[] gids; /* max 16 */


  public AuthUnix() {
  }
  public AuthUnix(String machinename, int uid, int gid, int[] gids) {
    this.stamp = (int)(System.currentTimeMillis() / 1000);
    this.machinename = machinename; /* max len 255 */
    this.uid = uid;
    this.gid = gid;
    this.gids = gids;
  }
}
