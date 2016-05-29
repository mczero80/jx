package jx.emulation;

import jx.zero.*;

class ProfilerImpl implements Profiler  {
    public void restart() {}
    public void shell() {}
    public void startCalibration() {}
    public boolean endCalibration(int time1, int time2, int time3) {return true;}
    public int getAverageCyclesOfMethod(String methodName) {return -1;}
    public void startSampling() {}
    public void stopSampling() {}
    public boolean isSampling() {return false;}
}
