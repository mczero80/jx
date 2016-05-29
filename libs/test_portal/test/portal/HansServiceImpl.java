package test.portal;

import jx.zero.*;
import jx.zero.debug.*;

public class HansServiceImpl implements HansService, Service {
    private Daddy daddy;
    HansServiceImpl(Daddy daddy) {
	this.daddy = daddy;
    }
    public void test(String str) {
	Debug.out.println("GOT: " + str);
	Debug.out.println("Count now: " + daddy.getCount());
    }
}
