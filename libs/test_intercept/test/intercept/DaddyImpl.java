package test.intercept;

import jx.zero.*;

public class DaddyImpl implements Daddy, Service {
    public void hello() {
	Debug.out.println("HELLO!!");
    }
    public void testPortalParam(Object o){
	Debug.out.println(" testPortalParam exec");
    }
}
