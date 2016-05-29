package jx.rpc;

public class RPCMsgRejectAuth extends RPCMsgDenied {
  /*
   * failed at remote end
   */
  public static final int AUTH_BADCRED      = 1;  /* bad credential (seal broken)     */
  public static final int AUTH_REJECTEDCRED = 2;  /* client must begin new session    */
  public static final int AUTH_BADVERF      = 3;  /* bad verifier (seal broken)       */
  public static final int AUTH_REJECTEDVERF = 4;  /* verifier expired or replayed     */
  public static final int AUTH_TOOWEAK      = 5;  /* rejected for security reasons    */
  /*
   * failed locally
   */
  public static final int AUTH_INVALIDRESP  = 6;  /* bogus response verifier          */
  public static final int AUTH_FAILED       = 7;   /* reason unknown                   */

  public int authStatus;

  public String toString() {
    switch(authStatus) {
    case AUTH_BADCRED: return "bad credential (seal broken)";
    case AUTH_REJECTEDCRED: return "client must begin new session";
    case AUTH_BADVERF: return "bad verifier (seal broken)";
    case AUTH_REJECTEDVERF: return "verifier expired or replayed";
    case AUTH_TOOWEAK: return "rejected for security reasons (too weak)";
    case AUTH_INVALIDRESP: return "bogus response verifier";
    case AUTH_FAILED: return "reason unknown";
    default: return "error: unknown reject/auth status";
    }
  }
}
