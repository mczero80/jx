/*
 * Benchmark.java
 *
 * Created on 29. Oktober 2001, 18:50
 */

package test.db;

import db.syscat.*;


import db.systembuffer.memorybuf.*;
import db.systembuffer.sortingbuf.*;
import db.systembuffer.firststage.*;
import db.com.*;
import db.com.comparators.*;
import db.bxtree.*;
import db.dbindex.*;
import db.tid.Set;
import db.tid.SetNumber;
import jx.db.BufferList;

import jx.db.types.TypeManager;
import jx.db.CodedException;

import jx.zero.*;
import jx.bio.BlockIO;
import jx.zero.Naming;
import jx.zero.InitialNaming;

import jx.zero.debug.*;
import jx.db.*;




/**
 *
 * @author  ivancho
 * @version
 */
public class Benchmark {
    public static final boolean crash = false;

    /** Creates new Benchmark */
    public static void main(String[] args) throws Exception {
	Naming naming = InitialNaming.getInitialNaming();
	Debug.out.println("Running Benchmark.dbtest()");
	int iGlobalCnter = 0; //for debug
       

	Database db = (Database)LookupHelper.waitUntilPortalAvailable(naming, args[0]);
	
	Debug.out.println("Creating table bench");
	

            String[] aszNames = new String[4];
            int[]    aiTypes  = new int[4];
            int[]    aiSizes  = new int[4];


            Debug.out.println("Attribute 1 is id of int");
            aszNames[0] = "id";
            aiSizes [0] = 4;
            aiTypes [0] = TypeManager.DATP_DID_INT;

            Debug.out.println("Attribute 2 is region of int");
            aszNames[1] = "region";
            aiSizes [1] = 4;
            aiTypes [1] = TypeManager.DATP_DID_INT;

            Debug.out.println("Attribute 3 is grp of int");
            aszNames[2] = "grp";
            aiSizes [2] = 4;
            aiTypes [2] = TypeManager.DATP_DID_INT;

            Debug.out.println("Attribute 4 is rev_id of int");
            aszNames[3] = "rev_id";
            aiSizes [3] = 4;
            aiTypes [3] = TypeManager.DATP_DID_INT;

            Table cTable = db.createTable("bench", aszNames, aiSizes, aiTypes );

            int[] aiImap = new int[2];

            Debug.out.println( "Creating indexes ..." );

            Debug.out.println( "Create unique index < region, id >" );

            aiImap[0] = 1;
            aiImap[1] = 0;
            Index cIndex1 = cTable.createIndex( Index.INDX_TYPE_SINGLE_DIM_MULTI_ATTR, true, aiImap );


            Debug.out.println( "Create unique index < region, rev_id >" );

            aiImap[0] = 1;
            aiImap[1] = 3;
            Index cIndex2 = cTable.createIndex( Index.INDX_TYPE_SINGLE_DIM_MULTI_ATTR, true, aiImap );

            Debug.out.println( "Create unique index < region, grp, id >" );

            aiImap = new int[3];
            aiImap[0] = 1;
            aiImap[1] = 2;
            aiImap[2] = 0;
            Index cIndex3 = cTable.createIndex( Index.INDX_TYPE_SINGLE_DIM_MULTI_ATTR, true, aiImap );

            //insert test

            //the following definitions are the same as in the MySql standard benchmark
            //( see test-select )
            int opt_loop_count = 10000;
            int opt_medium_loop_count = 1000;
            int opt_small_loop_count = 100;
            int opt_regions = 6;
            int opt_groups = 100;
            //end of standard definitions


            //init the clock
            Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");
            CycleTime starttimec = new CycleTime();
            CycleTime endtimec = new CycleTime();
            CycleTime diff = new CycleTime();

            clock.getCycles(starttimec);

            //init the profiler, now disabled
            //Profiler profiler = (Profiler)DomainZeroLookup.getDomainZero().lookup("Profiler");
            //profiler.startSampling();



            Debug.out.println("Inserting Tuples : " + opt_loop_count );

            clock.getCycles(starttimec);


            for (int iCnter = 0; iCnter < opt_loop_count; iCnter++) {
                iGlobalCnter = iCnter;

                TupleWriter cTupleWriter = cTable.createTuple();

                cTupleWriter.setField(0, iCnter);
                cTupleWriter.setField(1, 65 + iCnter % opt_regions);
                cTupleWriter.setField(2, (iCnter * 3) % opt_groups);
                cTupleWriter.setField(3, opt_loop_count - 1 - iCnter);

                //free resources!
                cTupleWriter.close();
            }

            clock.getCycles(endtimec);
            clock.subtract(diff, endtimec, starttimec);

            Debug.out.println("Time for insert: " + clock.toMicroSec(diff));

            RelationScan cOp;
            int iTotalCount = 0;

            Key cStart;
            Key cEnd;

            Debug.out.println( "Starting select test" );

            Debug.out.println( "Execute " + opt_small_loop_count + " times the following:" );

            Debug.out.println( "\nselect idn from bench1 where region=region'");
            Debug.out.println( "select idn from bench1 where region=region and idn=i");
            Debug.out.println( "select idn from bench1 where region=region and rev_idn=i");
            Debug.out.println( "select idn from bench1 where region=region and grp=grp");
            Debug.out.println( "select idn from bench1 where region>=B and region<=C and grp=grp");
            Debug.out.println( "select idn from bench1 where region>=B and region<=E and grp=grp");
            Debug.out.println( "select idn from bench1 where grp=grp"); // This is hard

            Debug.out.println( "\nSee the code for details" );

            clock.getCycles(starttimec);

            for (int iCnter = 0; iCnter < opt_small_loop_count; iCnter++) {
                int grp = iCnter * 11 % opt_groups;
                int region = 65 + iCnter % (opt_regions + 1); //one larger to test misses

		if(! crash) {
                //"select idn from bench1 where region='$region'"
		    //		Debug.out.println( "\nselect idn from bench1 where region = " + region );

                cStart = cIndex2.getKey();
                cEnd   = cIndex2.getKey();

                cStart.setField( region, 0 );
                cStart.setField( 0, 1 );
                cEnd.setField( region, 0 );
                cEnd.setField( opt_loop_count, 1 );

                cOp = db.getRelationScan( cTable, cIndex2, cStart, cEnd );
                if (cOp.moveToFirst())
                    do {
                        TupleReader cTupleReader = cOp.getCurrent();

			//                        cTupleReader.dump();
                        cTupleReader.close();
                        iTotalCount++;
                    }
                    while (cOp.moveToNext());
                cOp.close();

                //"select idn from bench1 where region='$region' and idn=$i"
		//		Debug.out.println( "select idn from bench1 where region= " + region + " and idn= " + iCnter );

                cStart = cIndex1.getKey();
                cEnd   = cIndex1.getKey();

                cStart.setField( region, 0 );
                cStart.setField( iCnter, 1 );
                cEnd.setField( region, 0 );
                cEnd.setField( iCnter, 1 );

                cOp = db.getRelationScan( cTable, cIndex1, cStart, cEnd );
                if (cOp.moveToFirst())
                    do {
                        TupleReader cTupleReader = cOp.getCurrent();

			//    cTupleReader.dump();
                        iTotalCount++;
                        cTupleReader.close();
                    }
                    while (cOp.moveToNext());

                cOp.close();

                //"select idn from bench1 where region='$region' and rev_idn=$i"
		//		Debug.out.println( "select idn from bench1 where region= " + region + "and rev_idn = " + iCnter );
                cStart = cIndex2.getKey();
                cEnd   = cIndex2.getKey();

                cStart.setField( region, 0 );
                cStart.setField( iCnter, 1 );
                cEnd.setField( region, 0 );
                cEnd.setField( iCnter, 1 );

                cOp = db.getRelationScan( cTable, cIndex2, cStart, cEnd );
                if (cOp.moveToFirst())
                    do {
                        TupleReader cTupleReader = cOp.getCurrent();

			//      cTupleReader.dump();
                        iTotalCount++;
                        cTupleReader.close();
                    }
                    while (cOp.moveToNext());

                cOp.close();

                //"select idn from bench1 where region='$region' and grp=$grp"
		//		Debug.out.println( "select idn from bench1 where region = " + region + "and grp = " + grp);
                cStart = cIndex3.getKey();
                cEnd   = cIndex3.getKey();

                cStart.setField( region, 0 );
                cStart.setField( grp,    1 );
                cStart.setField( 0,      2 );

                cEnd.setField( region, 0 );
                cEnd.setField( grp,    1 );
                cEnd.setField( opt_loop_count, 2 );

                cOp = db.getRelationScan( cTable, cIndex3, cStart, cEnd );
                if (cOp.moveToFirst())
                    do {
                        TupleReader cTupleReader = cOp.getCurrent();

			//      cTupleReader.dump();
                        iTotalCount++;
                        cTupleReader.close();
                    }
                    while (cOp.moveToNext());

                cOp.close();

                //"select idn from bench1 where region>='B' and region<='C' and grp=$grp"
		//		Debug.out.println( "select idn from bench1 where region>=B and region<=C and grp = " + grp );

                cStart.setField( 65 + 1,  0 );
                cEnd.setField( 65 + 2,  0 );

                cOp = db.getRelationScan( cTable, cIndex3, cStart, cEnd );
                iGlobalCnter = 0;
                if (cOp.moveToFirst())
                    do {
                        Key cKey = cOp.getCurrentKey();

                        if (grp == cKey.getField( 1 )) {
                            TupleReader cTupleReader = cOp.getCurrent();

			    //          cTupleReader.dump();
                            iTotalCount++;
                            cTupleReader.close();
                        }
                    }
                    while (cOp.moveToNext());
                cOp.close();

                //"select idn from bench1 where region>='B' and region<='E' and grp=$grp"
		//		Debug.out.println( "select idn from bench1 where region>=B and region<=E and grp = " + grp );

                cEnd.setField( 65 + 4,  0 );
                cOp = db.getRelationScan( cTable, cIndex3, cStart, cEnd );

                if (cOp.moveToFirst())
                    do {
                        Key cKey = cOp.getCurrentKey();

                        if (grp == cKey.getField( 1 )) {
                            TupleReader cTupleReader = cOp.getCurrent();

			    //          cTupleReader.dump();
                            iTotalCount++;
                            cTupleReader.close();
                        }
                    }
                    while (cOp.moveToNext());
                cOp.close();
		}
                //"select idn from bench1 where grp=$grp"
		//		Debug.out.println( "select idn from bench1 where grp = " + grp );
                cOp = db.getRelationScan( cTable, cIndex3, null, null );
                if (cOp.moveToFirst())
                    do {
                        Key cKey = cOp.getCurrentKey();

                        if (grp == cKey.getField( 1 )) {
                            TupleReader cTupleReader = cOp.getCurrent();

			    //          cTupleReader.dump();
                            cTupleReader.close();
                            iTotalCount++;
                        }
                    }
                    while (cOp.moveToNext());
                cOp.close();

            }

            clock.getCycles( endtimec );
            clock.subtract( diff, endtimec, starttimec );

            Debug.out.println("Time for scan: " + clock.toMicroSec(diff));
            Debug.out.println("iTotal selected count: >" + iTotalCount);


            Debug.out.println("Starting update test");
            Debug.out.println("update id from bench set id = " + opt_loop_count + " + id");


            BufferList cBufList = db.getTmpStorage( SetNumber.getByteLen() );
            cOp = db.getRelationScan( cTable, cIndex3, null, null );

            clock.getCycles(starttimec);
            //first fetch all tuples
            byte[] baSetNum = new byte[ SetNumber.getByteLen() ];
            if (cOp.moveToFirst())
                do {
                    SetNumberReader cSetNum = cOp.getCurrentSetNum();
                    cSetNum.getSetNumber( baSetNum, 0 );
                    cBufList.addAtEnd( baSetNum, 0 );
                }
                while (cOp.moveToNext());

	    cOp.close();

            clock.getCycles( endtimec );
            clock.subtract( diff, endtimec, starttimec );

            Debug.out.println("Time for fetch all rows for update: " + clock.toMicroSec(diff));


            clock.getCycles(starttimec);

            //do the update
            cBufList.moveToFirst();
            SetNumber cSetNum = new SetNumber();

            do {
                baSetNum = cBufList.getCurrent();

                cSetNum.setSetNumber( baSetNum, 0);
                TupleWriter cTupleWriter = cTable.modifyTuple(cSetNum.getReader());

                cTupleWriter.setField( 0, opt_loop_count + cTupleWriter.getReader().getField(0) );
                cTupleWriter.close();
            }while (cBufList.moveToNext());

            clock.getCycles( endtimec );
            clock.subtract( diff, endtimec, starttimec );

            Debug.out.println("Time for updating all rows" + clock.toMicroSec(diff));

            clock.getCycles(starttimec);

            //delete temporary storage
            cBufList.delete();

            //profiler.stopSampling();
            clock.getCycles( endtimec );
            clock.subtract( diff, endtimec, starttimec );

            Debug.out.println("Time for cleaning up: " + clock.toMicroSec(diff));


            /*	    //dump the modified tuples
            cOp.moveToFirst();
            do {
                Tuple cTuple = cOp.getCurrent();

                cTuple.dump();
                cTuple.unfix();
            }
            while (cOp.moveToNext());*/

    }

}
