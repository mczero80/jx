package jx.db.parser;

import jx.zero.*;
import jx.db.*;

public class Main {
    public static void main(String[] args) throws ParseException,CodedException {	
	Naming naming = InitialNaming.getInitialNaming();
	Database db = (Database)LookupHelper.waitUntilPortalAvailable(naming, args[0]);
	Parser parser = new Parser(db);
	parser.executeUpdate("CREATE TABLE cTable (id INT, region INT, grp INT, rev_id INT)");

	parser.executeUpdate("CREATE UNIQUE INDEX cIndex1 ON cTable (region, id)");
	parser.executeUpdate("CREATE UNIQUE INDEX cIndex2 ON cTable (region, rev_id)");
	parser.executeUpdate("CREATE UNIQUE INDEX cIndex3 ON cTable (region, grp, id)");

	//the following definitions are the same as in the MySql standard benchmark
	//( see test-select )
	int opt_loop_count = 10000;
	int opt_medium_loop_count = 1000;
	int opt_small_loop_count = 100;
	int opt_regions = 6;
	int opt_groups = 100;
	//end of standard definitions

	for (int iCnter = 0; iCnter < opt_loop_count; iCnter++) {
	    parser.executeUpdate("INSERT INTO cTable VALUES("
				 +iCnter+", "
				 +(65 + iCnter % opt_regions)+", "
				 +((iCnter * 3) % opt_groups)+", "
				 +(opt_loop_count - 1 - iCnter)
				 +")");
	}
	
	for (int iCnter = 0; iCnter < opt_small_loop_count; iCnter++) {
	    int grp = iCnter * 11 % opt_groups;
	    int region = 65 + iCnter % (opt_regions + 1); //one larger to test misses
	    
	    JXResultSet res = parser.executeQuery("SELECT idn FROM bench1 WHERE region="+region);
	}
    }
}
