package jx.zero;

public interface Profiler extends Portal {
    public void restart();
    public void shell();
    public void startCalibration();

    /**
     * time1: The spent cycles by a empty Method
     * time2: The spent cycles of a method without inner profiling
     * time3: The spent cycles of the same method with inner profiling
     *
     * Returns false on failure.
     *
     */
    public boolean endCalibration(int time1, int time2, int time3);

    public int getAverageCyclesOfMethod(String methodName);

    public void startSampling();
    public void stopSampling();
    public boolean isSampling();
}
