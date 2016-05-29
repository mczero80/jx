package test.gc;

import jx.zero.*;

public class Cycle {
    class X {
	int a;
	int b;
	int c;
	X x;
    }
    
    class Y {
	int a;
	int b;
	int c;
	X x;
    }
    
    int y0;
    int y;
    public static void main(String[] args) {
	new Cycle(args);
    }
    public Cycle(String[] args) {
	Naming naming=InitialNaming.getInitialNaming();
	// cycle one: allocate much mem
	X root;
	X x;
	for(;;) {
	    x = new X();
	    root = x;
	    for(int i=0;i<4000;i++){
		X y = new X();
		for(int j=0; j<40; j++) {
		    new Y(); // garbage
		}
		x.x = y;
		x = y;
	    }
	    // cycle two: do some heavy computation
	    int z= 42;
	    for(int i=0; i<100000; i++) {
		y0 = y * z;
		y = y + 2;
		z = z - 1;
	    }
	    // cycle three: free this mem
	    root = null;
	    x = null;
	}
	
    }
}


