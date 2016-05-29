package test.portal.perf;

import jx.zero.Portal;

public interface TargetDomain extends Portal {
    void noparam();
    void intparam(int p);
    void objparam(NullObject p);
    void listparam(ListObject p);
    void arrparam(NullObject[] p);
    int test(TestObject o, int v);
    void gc();
}
