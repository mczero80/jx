package test.intercept;

import jx.zero.*;
import jx.zero.debug.*;

public class HansServiceImpl implements HansService, Service {
    public void test(String str) {
	Debug.out.println("GOT: " + str);
    }
    public void test2(String str, int i, Object o){
	Debug.out.println("test 2 GOT: " + str);
	Debug.out.println(" int: "+i);
	Debug.out.println(" Object of Class: "+o.getClass().getName());
    }


}
