package jx.wintv;

public interface Tuner extends InputSource {
   FreqRange[] getFrequRanges();
   void setFrequ(int hz);
   boolean isFrequLocked();
   void waitForLock();
}
