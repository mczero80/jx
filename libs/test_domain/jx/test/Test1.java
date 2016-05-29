package jx.test;

import jx.zero.Portal;

public interface Test1 extends Portal {
    public int m();
}

class XYZ {
    static void m() {}
    static int n() { 
	int a = 1;
	Object o = new Object();
	m();
	o.toString();
	return a + 1;
    }
}
