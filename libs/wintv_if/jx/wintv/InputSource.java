package jx.wintv;

import jx.zero.Portal;

public interface InputSource extends Portal {
   void activate();
   void deactivate();
   boolean isActive();
}
