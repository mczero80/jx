package jx.verifier.wcet;

public class TestClass {
    public int testMethod(int k) {
	int res = 0;
	for (int i = 0; i < 100; i++) {
	    res += t2(k); 
	}
	return k;
    }

    public int t2(int k) {
	int ret = 1;
	for (int i = 0; i < k; i++)
	    ret = ret * k;
	return ret;
    }
	
}
