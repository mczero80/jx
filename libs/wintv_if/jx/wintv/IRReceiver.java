package jx.wintv;

import jx.zero.Portal;

public interface IRReceiver extends Portal {
   public final static  RCKey lastKeyStillPressed = new RCKey("(dummy)", -1);
   
   /**
    * Poll the IR receiver.
    * 
    * @return Special key "lastKeyStillPressed" in which case you can get
    *     the value of that key with "lastKey()" or "null" if there is no key
    *     pressed or there are errors during transmission.
    * 
    */
   public RCKey poll();
   
   /**
    * Wait for the IR receiver to receive a key.
    * 
    * @return the id of the key, which might be "lastKeyStillPressed".
    */
   public RCKey getKey();
   
   /**
    * Return the last key the IR receiver got during a "poll". This is
    * handy, if you forgot that key, or you received the special key
    * "lastKeyStillPressed".
    */
   public RCKey lastKey();
}
