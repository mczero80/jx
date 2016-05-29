package db.syscat;


/**
 * Title:        SA
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author Ivanich
 * @version 1.0
 */

import db.list.List;
import jx.db.*;


class TableImpl implements Table {

    public static final int ERR_NO_INDEX_DEFINED = 0;

    private SystemCatalogImpl   m_cSysCat;
    private TupleDescriptor     m_cTupleDescriptor;
    private int                 m_iRelID;
    private String              m_szName;

    ////////////////////////////////////////////////////////////////////////////
    protected TableImpl(SystemCatalogImpl cSysCat, String szTableName)
        throws CodedException {
        m_cSysCat = cSysCat;
        m_szName = szTableName;
        m_iRelID = cSysCat.getRelId(szTableName);
        m_cTupleDescriptor = cSysCat.getTupleDescriptor(m_iRelID);
    }

    ////////////////////////////////////////////////////////////////////////////
    /** creates a new index for a table
     * @param iType specifies the type of the index
     * @param bUnique unique or non unique index
     * @param iAttrMap positions of the attributes of the table which are indexed
     * @param cTD tuple descriptor of the table which should contain the index
     * @throws CodedException thrown on error ( wrong TD, etc.)
     */
    public Index createIndex(int iType, boolean bUnique, int[] iAttrMap)
        throws CodedException {
        return new IndexImpl( m_cSysCat, this, m_cSysCat.createIndex(iType, bUnique, iAttrMap, m_cTupleDescriptor));
    }

    ////////////////////////////////////////////////////////////////////////////
    /** returns an array containing all indexes of the table
     * @throws CodedException thrown on error ( no indexes defined, etc. )
     * @return Index[] array containing all indexes of the table
     */
    public Index[] getAllTableIndexes() throws CodedException {
        List cIndexInfos = m_cSysCat.getIndexInfos(m_iRelID);

        Index[] acRet = new Index[ cIndexInfos.getCount() ];

        if (!cIndexInfos.moveToFirst())
            throw new CodedException(this, ERR_NO_INDEX_DEFINED,
                    "No index defined in the table");
        int iCnter = 0;

        do {
            IndexInfo cIndexInfo = (IndexInfo) cIndexInfos.getCurrent();

            acRet[ iCnter ] = new IndexImpl(m_cSysCat, this, cIndexInfo.getIID());
            iCnter++;
        }
        while (cIndexInfos.moveToNext());

        return acRet;

    }

    ////////////////////////////////////////////////////////////////////////////
    /** creates a new tuple and returns a TupleWriter referencing it. The caller
     *   must call TupleWriter.close after modifying the tuple
     * @throws CodedException thrown on error ( wrong TD, write error, etc. )
     * @return TupleWriter object allowing modifications of the new tuple
     */
    public TupleWriter createTuple() throws CodedException {
        return m_cSysCat.createTuple(m_cTupleDescriptor);
    }

    ////////////////////////////////////////////////////////////////////////////
    /** returns a TupleWriter object referencing a tuple for modification
     * @param cSetNumReader SetNumberReader object containing the address of the
     *  tuple
     * @throws CodedException thrown on error ( wrong SetNumber, etc. )
     * @return returns a TupleWriter object, allowing modification of the tuple
     */
    public TupleWriter modifyTuple(SetNumberReader cSetNumReader)
        throws CodedException {
        return m_cSysCat.modifyTuple(cSetNumReader, m_cTupleDescriptor);
    }

    ////////////////////////////////////////////////////////////////////////////
    /** deletes a tuple from the persistent storage
     * @param cSetNumReader address of the tuple
     * @throws CodedException thrown on error ( wrong tuple address, etc )
     */
    public void deleteTuple(SetNumberReader cSetNumReader)
        throws CodedException {
        m_cSysCat.deleteTuple(cSetNumReader, m_cTupleDescriptor );
    }

    ////////////////////////////////////////////////////////////////////////////
    /** returns a TupleReader reference to a tuple. The caller can use this
     *  reference in order to read the contents of the tuple
     * @param cSetNumReader address of the tuple
     * @throws CodedException thrown on error ( wrong address, etc. )
     * @return returns a TupleReader object pointing to the tuple
     */
    public TupleReader readTuple(SetNumberReader cSetNumReader)
        throws CodedException {
        return m_cSysCat.readTuple(m_cTupleDescriptor, cSetNumReader);
    }

    ////////////////////////////////////////////////////////////////////////////
    /** returns a TupleDescriptor describing the structure of the table
     * @return returns a TupleDescriptor describing the structure of the table
     */
    public TupleDescriptor getTupleDescriptor() {
        return m_cTupleDescriptor;
    }

    public String getName(){
      return m_szName;
    }

    protected int getSetType(){
      return m_iRelID;
    }
}
