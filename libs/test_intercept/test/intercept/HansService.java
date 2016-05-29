package test.intercept;

import jx.zero.*;
import jx.zero.debug.*;

public interface HansService extends Portal {
    void test(String str);
    void test2(String str, int i, Object o);
}
