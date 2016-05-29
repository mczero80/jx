package jx.db.parser;

import jx.db.*;
import jx.zero.*;
import java.util.Vector;
import jx.db.types.TypeManager;

public class Parser {
    Database db;

    public Parser(Database db) {
	this.db = db;
    }
    public void executeUpdate(String sqlCommand) throws ParseException, CodedException {
	execute(sqlCommand);
    }
    public JXResultSet executeQuery(String sqlCommand) throws ParseException, CodedException {
	return execute(sqlCommand);
    }
    public JXResultSet execute(String sqlCommand) throws ParseException, CodedException {
	//sqlCommand = sqlCommand.toUpperCase();
	ParseHelper parser = new ParseHelper(sqlCommand.trim());
	String cmd = parser.nextToken();
	
	if (cmd.equals("CREATE")) {
	    String cmd1 = parser.nextToken();
	    if (cmd1.equals("TABLE")) {
		String tablename = parser.nextToken();
		Debug.out.println("create:");
		Debug.out.println("   name:"+tablename);
		parser.expectParens();
		String[] tableargs = parser.splitByComma();
		String[] names = new String[tableargs.length];
		int[] types = new int[tableargs.length];
		int[] sizes = new int[tableargs.length];

		for(int i=0; i<tableargs.length; i++) {
		    Debug.out.println("arg: "+tableargs[i]);
		    ParseHelper p = new ParseHelper(tableargs[i]);
		    names[i] = p.nextToken();
		    Debug.out.println("Name: "+names[i]);
		    String type = p.nextToken().toLowerCase();
		    Debug.out.println("Type: "+type);
		    if (type.equals("int")) {
			types[i] = TypeManager.DATP_DID_INT;
			sizes[i] = 4;
		    } else if (type.equals("char")) {
			throw new ParseException("Unsupported SQL data type \""+type+"\"");
		    } else {
			throw new ParseException("Unknown SQL data type \""+type+"\"");
		    }
		}
		Table cTable = db.createTable(tablename, names, sizes, types);
		return null;
	    } else if (cmd1.equals("INDEX")) {
		throw new ParseException("Unknown SQL-CREATE subcommand \""+cmd1+"\"");
	    } else if (cmd1.equals("UNIQUE")) {
		parser.expectToken("INDEX");
		String indexname = parser.nextToken();
		parser.expectToken("ON");
		String tablename = parser.nextToken();
		parser.expectParens();
		String[] tableargs = parser.splitByComma();
		Table table = db.getTable(tablename);

		int[] aiImap = new int[tableargs.length];
		TupleDescriptor td = table.getTupleDescriptor();
		int tc = td.getCount();
		for(int i=0; i<tableargs.length; i++) {
		    Debug.out.println("index:"+ tableargs[i]);
		    aiImap[i] = -1;
		    for(int j=0; j<tc; j++) {
			if (td.getName(j).equals(tableargs[i])) {
			    aiImap[i] = j;
			    break;
			}
		    }
		    if (aiImap[i]== -1) throw new ParseException("column name unknown: " +tableargs[i]);
		}
		Index cIndex1 = table.createIndex( Index.INDX_TYPE_SINGLE_DIM_MULTI_ATTR, true, aiImap );
		return null;
	    } else {
		throw new ParseException("Unknown SQL-CREATE subcommand \""+cmd1+"\"");
	    }
	} else if (cmd.equals("SELECT")) {
	    /*
	    String colname = parser.nextToken();
	    parser.expectToken("FROM");
	    String tablename = parser.nextToken();
	    parser.expectToken("WHERE");
	    String where = parser.nextToken();
	    Debug.out.println("where: "+where);
	    int c0 = where.indexOf('=');
	    String fieldname = where.substring(0, c0);
	    String fieldvalue = where.substring(c0);
	    // find an index that contains our column
	    Index[] indices = table.getAllTableIndexes();
	    Index index;
	    for(int i=0; i<indices.length; i++) {
		int map[] = indices[i].getIndexInfo().getAttributeMap();

		//index.getIID();
		//db.setRegKeyData(int iKey, int iData);
		// set when creating index
		
	    }
	    Key cStart = index.getKey();
	    Key cEnd   = index.getKey();
	    int value = Integer.parseString(fieldvalue);
	    cStart.setField( value, 0 );
	    
	    index.getIndexInfo().getIID();

	    //cStart.setFieldToMin(1);
	    cEnd.setField( value, 0 );
	    //cEnd.setFieldToMax(1);
	    
	    return new JXResultSet(db.getRelationScan( cTable, index, cStart, cEnd ));
	    */
	    return null;
	} else if (cmd.equals("INSERT")) {
	    parser.expectToken("INTO");
	    String tablename = parser.nextToken();
	    parser.expectToken("VALUES");
	    parser.expectParens();
	    String[] tableargs = parser.splitByComma();
	    Table table = db.getTable(tablename);
	    TupleWriter cTupleWriter = table.createTuple();
	    for(int i=0; i<tableargs.length; i++) {
		cTupleWriter.setField(i, Integer.parseInt(tableargs[i]));
	    }
	    //free resources!
	    cTupleWriter.close();
	    return null;
	} else {
	    throw new ParseException("Unknown SQL command \""+cmd+"\"");
	}
    }
}

class ParseHelper {
    private String stringToParse;
    
    public ParseHelper(String s) {stringToParse=s;}

    void expectToken(String token) throws ParseException {
	int c0 = stringToParse.indexOf('(');
	int c1 = stringToParse.indexOf(')');
	int c2 = stringToParse.indexOf(' ');
	int c;
	if (c0 == -1) c0 = 0x0fffffff;
	if (c1 == -1) c1 = 0x0fffffff;
	if (c2 == -1) c2 = 0x0fffffff;
	c = c0<c1?c0:c1; 
	c = c<c2?c:c2;
	String tok = stringToParse.substring(0, c);
	stringToParse = stringToParse.substring(c).trim();
	if (! tok.equals(token)) throw new ParseException("Error parsing SQL string \""+stringToParse+"\"");
    }
    
    String nextToken() throws ParseException {
	if (stringToParse==null) return null;
	int c1 = stringToParse.indexOf(' ');
	if (c1 == -1){
	    String ret = stringToParse;
	    stringToParse=null;
	    return ret;
	}
	String tok = stringToParse.substring(0, c1);
	stringToParse = stringToParse.substring(c1).trim();
	return tok;
    }
    
    void expectParens() throws ParseException {
	if (!stringToParse.startsWith("(") || ! stringToParse.endsWith(")")){
	    throw new ParseException("Missing paranthesis.");
	}
	stringToParse = stringToParse.substring(1, stringToParse.length()-1);
    }
    
    
    String[] splitByComma() throws ParseException {
	boolean exit = false;
	Vector v = new Vector();
	while(! exit){
	    int c3 = stringToParse.indexOf(',');
	    String s;
	    if (c3==-1) { 
		exit = true;
		s = stringToParse;
	    } else {
		s = stringToParse.substring(0, c3);
		stringToParse = stringToParse.substring(c3+1).trim();
	    }
	    v.addElement(s);
	}
	String ret[] = new String[v.size()];
	v.copyInto(ret);
	return ret;
    }
}
