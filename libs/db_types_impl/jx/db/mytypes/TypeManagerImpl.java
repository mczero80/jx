package jx.db.mytypes;

import jx.db.types.*;
import jx.db.CodedException;

import jx.db.mytypes.comparators.*;
import jx.db.mytypes.converters.*;
/**
 * Title:        SA
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author Ivanich
 * @version 1.0
 */

public class TypeManagerImpl implements TypeManager {

    private int[][] m_aiTypeInfos;
    private String[] m_aszNames;

    public TypeManagerImpl(){
      m_aiTypeInfos = new int[2][2];
      m_aszNames = new String[2];

      m_aiTypeInfos[0][0] = DATP_DID_INT; //type id
      m_aiTypeInfos[0][1] = 4; //size
      m_aszNames[0] = DATP_DNAME_INT;

      m_aiTypeInfos[1][0] = DATP_DID_STR; //type id
      m_aiTypeInfos[1][1] = -1; //size, -1 for variable size
      m_aszNames[1] = DATP_DNAME_STR;

      //init further types here
    }

    ////////////////////////////////////////////////////////////////////////////
    /** returns an object that implements the DbComparator interface, used to compare byte arrays of iDID type.
     * @param iDID datatype id
     * @throws CodedException thrown, if an error occurs
     * @return Comparator object
     */
    public DbComparator getComparator( int typeID ) throws CodedException {
        switch ( typeID ) {
        case DATP_DID_INT:
            return new IntegerComparator();

        case DATP_DID_STR:
            return new StringComparator();

        //create comparators for further types here

        default:
            throw new CodedException(this, ERR_UNSUPPORTED_DID, "Unsupported datatype!");
        }
    }


    ////////////////////////////////////////////////////////////////////////////
    /** returns a DbConverter objects for the datatype of the attribute on the given iPos
     * @param typeID position of the attribute in the tuple ( number, not offset!)
     * @throws CodedException thrown on error ( wrong iPos )
     * @return DbConverter objects for the datatype of the attribute on the given iPos
     */
    public DbConverter getConverter( int typeID ) throws CodedException {

        switch ( typeID ) {
        case DATP_DID_INT:
            return null;

        case DATP_DID_STR:
            return new StringToByteArrayConverter();

        //create converters for further types here

        default:
            throw new CodedException(this, ERR_UNSUPPORTED_DID, "Unsupported data type! : " + typeID );
        }
    }


    public int[][] getTypeInfos(){
	//fixme!
	//return (int[][])m_aiTypeInfos.clone();
	return m_aiTypeInfos;
    }

    public String getTypeName( int typeID ) throws CodedException {
      for( int iCnter = 0; iCnter < m_aiTypeInfos.length; iCnter ++ )
           if( m_aiTypeInfos[iCnter][0] == typeID )
               return m_aszNames[ iCnter ];

      throw new CodedException( this, ERR_UNSUPPORTED_DID, "Unsupported data type! : " + typeID );
    }
}
