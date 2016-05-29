package db.syscat;

import jx.db.types.DbConverter;
import jx.db.*;

import db.dbindex.DbIndex;

/**
 * Title:        SA
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author Ivanich
 * @version 1.0
 */

public class IndexImpl implements Index {

    private int m_iIID = -1;
    private TableImpl m_cTableImpl;
    private SystemCatalogImpl m_cSysCat;


    ////////////////////////////////////////////////////////////////////////////
    protected IndexImpl( SystemCatalogImpl cSysCat, TableImpl cTableImpl,
                         int iIID ){
      m_cSysCat = cSysCat;
      m_iIID = iIID;
      m_cTableImpl = cTableImpl;
    }


    ////////////////////////////////////////////////////////////////////////////
    /** deletes an index with all its structures
     * @throws CodedException thrown on error ( write error, etc. )
     */
    public void dropIndex() throws CodedException{
      m_cSysCat.dropIndex( m_iIID );
    }


    ////////////////////////////////////////////////////////////////////////////
    /** returns an IndexInfo object for an index
     * @throws CodedException thrown on error ( no valid index, etc )
     * @return returns an IndexInfo object, containing information about the
     * index ( attribute map, unique flag, etc )
     */
    public IndexInfo getIndexInfo() throws CodedException{
      return m_cSysCat.getIndexInfo( m_iIID );
    }


    ////////////////////////////////////////////////////////////////////////////
    /** returns the id of the table, containing the index, which id is iIID
     * @throws CodedException thrown on error ( no valid index, etc )
     * @return id of the table containing the index
     */
    public Table getTable() throws CodedException{
      return m_cTableImpl;
    }

    ////////////////////////////////////////////////////////////////////////////
    /** creates a Key object that makes the key manipulations easier
     * @throws CodedException thrown on error ( no valid index, etc. )
     * @return the created key object
     */
    public Key getKey() throws CodedException{
      int[] aiAttrMap = m_cSysCat.getIndexInfo( m_iIID ).getAttributeMap();
      DbConverter[] acConverters = new DbConverter[ aiAttrMap.length ];
      byte[][] baData = new byte[ aiAttrMap.length ][];

      for( int iCnter = 0; iCnter < aiAttrMap.length; iCnter ++ ){
        acConverters[ iCnter ] = m_cTableImpl.getTupleDescriptor().getConverter(
                                       aiAttrMap[ iCnter ] );
        baData[ iCnter ] = new byte[ m_cTableImpl.getTupleDescriptor().getAttrSize(
                                       aiAttrMap[ iCnter ] ) ];
      }
      return new KeyImpl( acConverters, baData );
    }

    protected DbIndex getIndex() throws CodedException{
      return m_cSysCat.getIndex( m_iIID );
    }
}