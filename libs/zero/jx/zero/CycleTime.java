package jx.zero;

public class CycleTime {
    public int low;
    public int high;

    public CycleTime subtract(CycleTime t) {
	CycleTime diff = new CycleTime();
	int l = low >>> 1;
	l = l - (t.low >>> 1);
	diff.low = l << 1;
	if (l >= 0) {
	    diff.high = high - t.high;
	} else {
	    diff.high = high - t.high - 1;
	}
	return diff;
    }
}
