package test.db;

import jx.zero.*;
import jx.zero.debug.*;

public class Main {
    public static void init(Naming naming) throws Exception {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
	Benchmark.main(null);
      //QTest.test(naming);
    }
}
