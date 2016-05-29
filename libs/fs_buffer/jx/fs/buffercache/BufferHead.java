package jx.fs.buffercache;

import jx.zero.Debug;
import jx.zero.Memory;
import jx.zero.*;

public abstract class BufferHead extends jx.buffer.BufferHead {
    public BufferHead(Memory buf) {super(buf); }

    public abstract int getBlock();
    public abstract int getSize();
    public abstract void endIo(boolean error, boolean synchronous);

    public abstract boolean isDirty();
    public abstract void markDirty();
    public abstract void markClean();

    public abstract boolean isUptodate();
    public abstract void markUptodate();

    public abstract boolean isLocked();
    public abstract void lock();
    public abstract void unlock();
    public abstract void waitUntilUnlocked();
    public abstract void waitOn();



  public abstract void clear();
  public abstract void clear(int from, int to);

    public abstract boolean isUsed();
   public abstract boolean isUsedOnlyByMe();

}
