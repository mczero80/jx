package jx.db.parser;

import jx.zero.*;
import jx.db.*;

public class JXResultSet {
    RelationScan scan;
    boolean avail=false;

    JXResultSet(RelationScan scan) throws CodedException{
	this.scan = scan;
	avail = scan.moveToFirst();
    }

    TupleReader next() throws CodedException{
	if (! avail) return null;
	TupleReader cTupleReader = scan.getCurrent();
	////                        cTupleReader.dump();
	//cTupleReader.close();
	avail = scan.moveToNext();
	return cTupleReader;
    }

    void close() throws CodedException{
	scan.close();
    }

}
