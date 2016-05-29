package test.wintv;

import jx.zero.Portal;
import jx.zero.Memory;

public interface VideoDataSource extends Portal {
   void startTransmission();
   Memory getNextPacket();
   void recyclePacket(Memory oldpacket);
}

