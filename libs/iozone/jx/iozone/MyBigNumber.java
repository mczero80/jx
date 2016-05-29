package jx.iozone;

// use to avoid overflows of ints and not depend on BigInt
// the implementation is not efficient but exact
// it uses the long division algorithm I learned in school
class MyBigNumber {
    int[] v = new int[30];

    MyBigNumber(int[] n) {
	v = n;
    }
    MyBigNumber(int n) {
	int i=0;
	for(;;) {
	    v[i] = n % 10;
	    i++;
	    n /= 10;
	    if (n == 0) return;
	}
    }

    MyBigNumber mul(int n) {
	MyBigNumber r = new MyBigNumber(0);
	int overflow = 0;
	for(int i=0; i<v.length; i++) {
	    r.v[i] = n * v[i] + overflow;
	    overflow = r.v[i] / 10;
	    r.v[i] %= 10;
	}	
	return r;
    }

    MyBigNumber mul(int n, int e) {
	MyBigNumber r = new MyBigNumber(0);
	int overflow = 0;
	for(int i=0; i<v.length; i++) {
	    r.v[i] = n * v[i] + overflow;
	    overflow = r.v[i] / 10;
	    r.v[i] %= 10;
	}	
	int ra[] = new int[v.length];
	for(int i=0; i<v.length-e; i++) {
	    ra[i+e] = r.v[i];
	}
	r.v = ra;
	return r;
    }

    MyBigNumber mul(MyBigNumber n) {
	for(int i=0; i<v.length; i++) {
	    n = n.mul(v[i], i);
	}
	return n;
    }

    MyBigNumber div(MyBigNumber n) {
	return null;
    }

    MyBigNumber div(int n) {
	int i;
	int z=0;
	int y=0;
	int [] r = new int[v.length];
	for(i=v.length-1; i>=0; i--) if (v[i] != 0) break;
	for(;;) {
	    for(; i>=0; i--) {
		z = z * 10 + v[i];
		if ((z / n) > 0) {
		    break;
		}
	    }
	    if (z / n == 0) break; // z is final remainder
	    r[i] = z / n;
	    y = r[i] * n;
	    z = z - y; // compute remainder
	    i--;
	}
	return new MyBigNumber(r); // z is remainder
    }

    MyBigNumber add(int n) {
	throw new Error();
    }


    public String toString() {
	String ret="";
	int i;
	for(i=v.length-1; i>=0; i--) if (v[i] != 0) break;
	for(; i>=0; i--) {
	    ret = ret + v[i];
	}
	return ret;
    }

    public int toInt() {
	int ret=0;
	int i;
	for(i=v.length-1; i>=0; i--) if (v[i] != 0) break;
	for(; i>=0; i--) {
	    ret = ret*10 + v[i];
	}
	return ret;
    }

}
