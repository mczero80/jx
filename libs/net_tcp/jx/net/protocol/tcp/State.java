package jx.net.protocol.tcp;

class State {
  /* TCP State */
  /* Note: close-wait state is bypassed by automatically closing a connection
   *       when a FIN is received.  This is easy to undo.
   */
  static final int LISTEN = 0;      /* listening for connection */
  static final int SYNSENT= 1;      /* syn sent, active open */
  static final int SYNREC = 2;      /* syn received, synack+syn sent. */
  static final int ESTAB  = 3;      /* established */
  static final int FINWT1 = 4;      /* sent FIN */
  static final int FINWT2 = 5;      /* sent FIN, received FINACK */
  /*CLOSEWT 6    /* received FIN waiting for close */
  static final int CLOSING =6;      /* sent FIN, received FIN (waiting for FINACK) */
  static final int LASTACK =7;      /* fin received, finack+fin sent */
  static final int TIMEWT  =8;      /* dally after sending final FINACK */
  static final int CLOSED  =9;      /* finack received */
}
