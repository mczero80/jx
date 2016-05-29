package db.syscat;

import jx.zero.Naming;
import jx.zero.InitialNaming;
import jx.zero.CPUManager;
import jx.zero.Debug;
import jx.zero.LookupHelper;
import jx.bio.BlockIO;

import jx.db.types.DbComparator;
import jx.db.types.TypeManager;
import jx.db.mytypes.TypeManagerImpl;
import jx.db.mytypes.comparators.IntegerComparator;
import jx.db.mytypes.comparators.StringComparator;
import jx.db.CodedException;
import db.com.Converter;
import db.com.ObjHolder;
import db.com.ObjectPool;
import db.com.Iterator;
import db.com.comparators.ByteComparator;
import db.com.comparators.CompoundComparator;
import db.tid.SetKeyComparator;
import jx.db.types.DbConverter;
import db.tid.SetAccess;
import db.tid.SetAccessImpl;
import db.tid.SetArray;
import db.tid.SetArrayImpl;
import db.tid.SetNumberFactory;
import db.tid.Set;
import db.tid.SetNumber;
import db.systembuffer.PageBuffer;
import db.systembuffer.sortingbuf.SortingBuffer;
import db.systembuffer.sortingbuf.SortingBufferImpl;
import db.systembuffer.memorybuf.MemoryBuffer;
import db.systembuffer.firststage.FirstStageBuf;
import db.dbindex.DbIndex;
import db.dbindex.DbSingleAttrIndex;
import db.dbindex.DbSingleDimIndex;
import db.dbindex.DbIndexSingleDim;
import db.bxtree.BxTree;
import db.list.List;
import db.list.BiList;
import db.collections.DBHashtable;
import db.systembuffer.collections.BufferListImpl;

import jx.db.*;


//import java.util.*;


public class SystemCatalogImpl implements Database{

    public static final int ERR_INVALID_INDEX = 0;
    public static final int ERR_INCONSISTENT_INDEX = 1;
    public static final int ERR_UNSUPPORTED_DID = 2;
    public static final int ERR_UNSUPPORTED_ITYPE = 3;
    public static final int ERR_WRONG_CARDINALITY = 4;
    public static final int ERR_NO_INDEX_DEFINED = 5;
    public static final int ERR_SEGMENT_NOT_SUPPORTED = 6;
    public static final int ERR_REG_KEY_NOT_FOUND = 7;
    public static final int ERR_SEGMENT_DIRTY = 8;
    public static final int ERR_INVALID_RID = 9;
    public static final int ERR_RNAME_EXISTS = 10;
    public static final int ERR_CANT_FIND_ROOT = 11;
    public static final int ERR_PROZESSING_TUPLE_STARTED = 12;
    public static final int ERR_PROZESSING_TUPLE_NOT_STARTED = 13;

    private final int INTSIZE = 4;

    public static final int INDEX_SEGMENT = 0;
    public static final int SYSCAT_SEGMENT = 1;
    public static final int USER_SEGMENT = 2;

    /* Die index-Relation */

    /* 1. Spalte: IID */
    public  static final int INDX_IID_NO_INDEX = 0;
    public  static final int INDX_IID_INDX_IID = 1;
    public  static final int INDX_IID_RELA_RID = 2;
    public  static final int INDX_IID_RELA_RNAME = 3;
    public  static final int INDX_IID_ATTR_AID = 4;
    public  static final int INDX_IID_ATTR_RID = 5;
    public  static final int INDX_IID_DATP_DID = 6;
    public  static final int INDX_IID_IMAP_RID = 7;
    public  static final int INDX_IID_IMAP_IID = 8;

    /* 2. Spalte: wird generiert */

    /* Pagenumber des Index-Index IID_INDX_ID */
    private final int INDX_INDNO_INDX_IID = 1;

    /*Index Types, currently only BxTree( single attr, single dim ) supported, later Grid File maybe */
    public static final int INDX_TYPE_SINGLE_ATTR = 0;
    public static final int INDX_TYPE_SINGLE_DIM_MULTI_ATTR = 1;
    public static final int INDX_TYPE_MULTI_DIM_MULTI_ATTR = 2;

    /* Die relation-Relation */

    /* 1. Spalte: RID */
    private final int RELA_RID_INDX = 0;
    private final int RELA_RID_RELA = 1;
    private final int RELA_RID_ATTR = 2;
    private final int RELA_RID_DATP = 3;
    private final int RELA_RID_IMAP = 4;

    /* 2. Spalte: RNAME */
    private final String RELA_RNAME_INDX = "index";
    private final String RELA_RNAME_RELA = "relation";
    private final String RELA_RNAME_ATTR = "attribute";
    private final String RELA_RNAME_DATP = "datatype";
    private final String RELA_RNAME_IMAP = "indexmap";

    /* Die attribute-Relation */

    /* 1. Spalte: AID */
    private final int ATTR_AID_INDX_IID = 0;
    private final int ATTR_AID_INDX_INDNO = 1;
    private final int ATTR_AID_INDX_INDTYPE = 2;
    private final int ATTR_AID_INDX_INDCARD = 3;
    private final int ATTR_AID_RELA_RID = 4;
    private final int ATTR_AID_RELA_RNAME = 5;
    private final int ATTR_AID_ATTR_AID = 6;
    private final int ATTR_AID_ATTR_ANAME = 7;
    private final int ATTR_AID_ATTR_RID = 8;
    private final int ATTR_AID_ATTR_POS = 9;
    private final int ATTR_AID_ATTR_DID = 10;
    private final int ATTR_AID_ATTR_SIZE = 11;
    private final int ATTR_AID_DATP_DID = 12;
    private final int ATTR_AID_DATP_DNAME = 13;
    private final int ATTR_AID_IMAP_RID = 14;
    private final int ATTR_AID_IMAP_IID = 15;
    private final int ATTR_AID_IMAP_APOS = 16;

    /* Die attribute-Relation */

    /* 2. Spalte: ANAME */
    private final String ATTR_ANAME_INDX_IID = "iid";
    private final String ATTR_ANAME_INDX_INDNO = "indno";
    private final String ATTR_ANAME_INDX_INDTYPE = "indtype";
    private final String ATTR_ANAME_INDX_INDCARD = "indcard";
    private final String ATTR_ANAME_RELA_RID = "rid";
    private final String ATTR_ANAME_RELA_RNAME = "rname";
    private final String ATTR_ANAME_ATTR_AID = "aid";
    private final String ATTR_ANAME_ATTR_ANAME = "aname";
    private final String ATTR_ANAME_ATTR_RID = "rid";
    private final String ATTR_ANAME_ATTR_POS = "pos";
    private final String ATTR_ANAME_ATTR_DID = "did";
    private final String ATTR_ANAME_ATTR_SIZE = "size";
    private final String ATTR_ANAME_DATP_DID = "did";
    private final String ATTR_ANAME_DATP_DNAME = "dname";
    private final String ATTR_ANAME_IMAP_RID = "rid";
    private final String ATTR_ANAME_IMAP_IID = "iid";
    private final String ATTR_ANAME_IMAP_APOS = "attr_pos";

    /* 3. Spalte: RID */
    private final int ATTR_RID_INDX_IID = RELA_RID_INDX;
    private final int ATTR_RID_INDX_INDNO = RELA_RID_INDX;
    private final int ATTR_RID_INDX_INDTYPE = RELA_RID_INDX;
    private final int ATTR_RID_INDX_INDCARD = RELA_RID_INDX;
    private final int ATTR_RID_RELA_RID = RELA_RID_RELA;
    private final int ATTR_RID_RELA_RNAME = RELA_RID_RELA;
    private final int ATTR_RID_ATTR_AID = RELA_RID_ATTR;
    private final int ATTR_RID_ATTR_ANAME = RELA_RID_ATTR;
    private final int ATTR_RID_ATTR_RID = RELA_RID_ATTR;
    private final int ATTR_RID_ATTR_POS = RELA_RID_ATTR;
    private final int ATTR_RID_ATTR_DID = RELA_RID_ATTR;
    private final int ATTR_RID_ATTR_SIZE = RELA_RID_ATTR;
    private final int ATTR_RID_DATP_DID = RELA_RID_DATP;
    private final int ATTR_RID_DATP_DNAME = RELA_RID_DATP;
    private final int ATTR_RID_IMAP_RID = RELA_RID_IMAP;
    private final int ATTR_RID_IMAP_IID = RELA_RID_IMAP;
    private final int ATTR_RID_IMAP_APOS = RELA_RID_IMAP;

    /* Die datatype-Relation */

    /* Die attribute-Relation */

    /* 4. Spalte: POS */
    private final int ATTR_POS_INDX_IID = 0;
    private final int ATTR_POS_INDX_INDNO = 1;
    private final int ATTR_POS_INDX_INDTYPE = 2;
    private final int ATTR_POS_INDX_INDCARD = 3;
    private final int ATTR_POS_RELA_RID = 0;
    private final int ATTR_POS_RELA_RNAME = 1;
    private final int ATTR_POS_ATTR_AID = 0;
    private final int ATTR_POS_ATTR_ANAME = 1;
    private final int ATTR_POS_ATTR_RID = 2;
    private final int ATTR_POS_ATTR_POS = 3;
    private final int ATTR_POS_ATTR_DID = 4;
    private final int ATTR_POS_ATTR_SIZE = 5;
    private final int ATTR_POS_DATP_DID = 0;
    private final int ATTR_POS_DATP_DNAME = 1;
    private final int ATTR_POS_IMAP_RID = 0;
    private final int ATTR_POS_IMAP_IID = 1;
    private final int ATTR_POS_IMAP_APOS = 2;

    /* 5. Spalte: DID */
    private final int ATTR_DID_INDX_IID = TypeManager.DATP_DID_INT;
    private final int ATTR_DID_INDX_INDNO = TypeManager.DATP_DID_INT;
    private final int ATTR_DID_INDX_INDTYPE = TypeManager.DATP_DID_INT;
    private final int ATTR_DID_INDX_INDCARD = TypeManager.DATP_DID_INT;
    private final int ATTR_DID_RELA_RID = TypeManager.DATP_DID_INT;
    private final int ATTR_DID_RELA_RNAME = TypeManager.DATP_DID_STR;
    private final int ATTR_DID_RELA_AID = TypeManager.DATP_DID_INT;
    private final int ATTR_DID_RELA_MAXID = TypeManager.DATP_DID_INT;
    private final int ATTR_DID_ATTR_AID = TypeManager.DATP_DID_INT;
    private final int ATTR_DID_ATTR_ANAME = TypeManager.DATP_DID_STR;
    private final int ATTR_DID_ATTR_RID = TypeManager.DATP_DID_INT;
    private final int ATTR_DID_ATTR_POS = TypeManager.DATP_DID_INT;
    private final int ATTR_DID_ATTR_DID = TypeManager.DATP_DID_INT;
    private final int ATTR_DID_ATTR_SIZE = TypeManager.DATP_DID_INT;
    private final int ATTR_DID_DATP_DID = TypeManager.DATP_DID_INT;
    private final int ATTR_DID_DATP_DNAME = TypeManager.DATP_DID_STR;
    private final int ATTR_DID_IMAP_RID = TypeManager.DATP_DID_INT;
    private final int ATTR_DID_IMAP_IID = TypeManager.DATP_DID_INT;
    private final int ATTR_DID_IMAP_APOS = TypeManager.DATP_DID_INT;

    /* 6. Spalte: SIZE */
    private final int ATTR_SIZE_INDX_IID = INTSIZE;
    private final int ATTR_SIZE_INDX_INDNO = INTSIZE;
    private final int ATTR_SIZE_INDX_INDTYPE = INTSIZE;
    private final int ATTR_SIZE_INDX_INDCARD = INTSIZE;
    private final int ATTR_SIZE_RELA_RID = INTSIZE;
    private final int ATTR_SIZE_RELA_RNAME = 20;
    private final int ATTR_SIZE_ATTR_AID = INTSIZE;
    private final int ATTR_SIZE_ATTR_ANAME = 20;
    private final int ATTR_SIZE_ATTR_RID = INTSIZE;
    private final int ATTR_SIZE_ATTR_POS = INTSIZE;
    private final int ATTR_SIZE_ATTR_DID = INTSIZE;
    private final int ATTR_SIZE_ATTR_SIZE = INTSIZE;
    private final int ATTR_SIZE_DATP_DID = INTSIZE;
    private final int ATTR_SIZE_DATP_DNAME = 20;
    private final int ATTR_SIZE_IMAP_RID = INTSIZE;
    private final int ATTR_SIZE_IMAP_IID = INTSIZE;
    private final int ATTR_SIZE_IMAP_APOS = INTSIZE;

    private static final int REG_SETACC_ROOT = 1;
    private static final int REG_USER_SETACC_ROOT = 2;
    private static final int REG_ROOT_INDX_ROOT = 3;
    private static final int REG_ATTR_MAX_ID = 4;
    private static final int REG_RELA_MAX_ID = 5;
    private static final int REG_INDX_MAX_ID = 6;

    private TypeManager     m_cTypeManager = null;
    private SetAccess       m_cSetAccess = null;
    private SetAccess       m_cUserSetAccess = null;
    private PageBuffer      m_cPageBuf = null;
    private BxTree          m_cRootIndex = null;
    private BxTree          m_cSetAccessBxTree = null;
    private BxTree          m_cUserSetAccessBxTree = null;
    private BxTree          m_cRegistry = null;

    private TupleDescriptorImpl m_cIndexRelTD = null;
    private TupleDescriptorImpl m_cAttributeRelTD = null;
    private TupleDescriptorImpl m_cRelationRelTD = null;
    private TupleDescriptorImpl m_cDatatypeRelTD = null;
    private TupleDescriptorImpl m_cIndexmapRelTD = null;

    private boolean m_bCreateTupleProzess = false;
    private boolean m_bModifyTupleProzess = false;
    private boolean m_bReadTupleProzess = false;

    private ObjectPool m_cTuplePool = null;
    private ObjectPool m_cSetNumPool = null;
    private BiList     m_cTuplesInUse = null;

    //cache arrays:
    private byte[] m_baTmpIntKey = null;
    private byte[] m_baTmpIntData = null;
    private byte[] m_baTmpSetNum = null;
    private byte[] m_baTmpSetNum_user = null;
    private SetNumber m_cTmpSetNum = null;
    private SetNumber m_cTmpSetNum_user = null;
    private Tuple     m_cTmpTuple = null;
    private Tuple     m_cTmpTuple_user = null;
    private SetArray  m_cTmpSetArray = null;
    private ByteComparator  m_cTmpByteComparator = null;
    private byte[]    m_baTmpStrKey = null;
    private Set       m_cTmpSet = null;

    private DBHashtable m_cIndexCache;
    private DBHashtable m_cIndexInfoCache;

    private void initTemps() {
        m_baTmpIntKey = new byte[INTSIZE];
        m_baTmpIntData = new byte[INTSIZE];
        m_baTmpSetNum = new byte[SetNumber.getByteLen()];
        m_baTmpSetNum_user = new byte[SetNumber.getByteLen()];
        m_cTmpSetNum = new SetNumber((byte[]) null);
        m_cTmpSetNum_user = new SetNumber((byte[]) null);
        m_cTmpTuple = new Tuple();
        m_cTmpTuple_user = new Tuple();
        m_cTmpByteComparator = new ByteComparator();
        m_cTmpSetArray = new SetArrayImpl();
        m_baTmpStrKey = new byte[ 20 ];
        m_cTmpSet = new Set(null, -1, -1);
        m_cIndexCache = new DBHashtable(100, null);
        m_cIndexInfoCache = new DBHashtable(100, null);

        m_cTuplePool = new ObjectPool(db.com.Globals.TUPLE_POOL_SIZE, new TupleFactory());
        m_cSetNumPool = new ObjectPool(db.com.Globals.TUPLE_POOL_SIZE, new SetNumberFactory());
        m_cTuplesInUse = new BiList();
    }

    ////////////////////////////////////////////////////////////////////////////
    public SystemCatalogImpl(PageBuffer cPageBuf) throws CodedException {

        //init the cache
        initTemps();

        m_cPageBuf = cPageBuf;
        m_cTypeManager = new TypeManagerImpl();

        //initialize common Tuple descriptors
        initIndexRelTD();
        initRelationRelTD();
        initAttributeRelTD();
        initDatatypeRelTD();
        initIndexmapRelTD();

        if (m_cPageBuf.getPageManager().isPageUsed(1)) { //db already initialized

            //create the registy index (contains info about the root pages of the layers)
            m_cRegistry = new BxTree(INTSIZE, INTSIZE, m_cPageBuf, 1, new IntegerComparator(), false);

            initIndexes(m_cRegistry, false);

        }else { //we have to initialize for the first time

            int iRegistryRootPageNum = m_cPageBuf.getPageManager().getBlankPage();

            if (iRegistryRootPageNum != 1)
                throw new CodedException(this, ERR_SEGMENT_DIRTY, "Can't initialize registry index!");

            m_cRegistry = new BxTree(INTSIZE, INTSIZE, m_cPageBuf, 1, new IntegerComparator(), true);

            Converter.intToBytes(REG_SETACC_ROOT, m_baTmpIntKey);
            Converter.intToBytes(m_cPageBuf.getPageManager().getBlankPage(), m_baTmpIntData);

            m_cRegistry.insert(m_baTmpIntKey, m_baTmpIntData);

            Converter.intToBytes(REG_USER_SETACC_ROOT, m_baTmpIntKey);
            Converter.intToBytes(m_cPageBuf.getPageManager().getBlankPage(), m_baTmpIntData);

            m_cRegistry.insert(m_baTmpIntKey, m_baTmpIntData);

            Converter.intToBytes(REG_ROOT_INDX_ROOT, m_baTmpIntKey);
            Converter.intToBytes(m_cPageBuf.getPageManager().getBlankPage(), m_baTmpIntData);

            m_cRegistry.insert(m_baTmpIntKey, m_baTmpIntData);

            initIndexes(m_cRegistry, true);

            initIndexRel();
            initRelaRel();
            initAttrRel();
            initDatpRel();
            initImapRel();

            setRegKeyData(REG_ATTR_MAX_ID, 100);
            setRegKeyData(REG_RELA_MAX_ID, 100);
            setRegKeyData(REG_INDX_MAX_ID, 100);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    private void addIndexInfoToCache( IndexInfo cIndexInfo, int iRID ) throws CodedException{
      List cList = ( List ) m_cIndexInfoCache.get( iRID );
      if( cList == null ){
        cList = new List();
        m_cIndexInfoCache.put( iRID, cList );
      }
      cList.insert( cIndexInfo );
    }
    ////////////////////////////////////////////////////////////////////////////
    private void initIndexes(BxTree cRegistry, boolean bFormat) throws CodedException {

        Converter.intToBytes(REG_SETACC_ROOT, m_baTmpIntKey);
        if (!cRegistry.find(m_baTmpIntKey, m_baTmpIntData))
            throw new CodedException(this, ERR_CANT_FIND_ROOT, "can't find the Set Access layer root page in the Registry!");

            //create the set acess layer
        m_cSetAccessBxTree = new BxTree(8, 0, m_cPageBuf, Converter.bytesToInt(m_baTmpIntData), new SetKeyComparator(), bFormat);
        m_cSetAccess = new SetAccessImpl(m_cPageBuf, m_cSetAccessBxTree);

        Converter.intToBytes(REG_USER_SETACC_ROOT, m_baTmpIntKey);
        if (!cRegistry.find(m_baTmpIntKey, m_baTmpIntData))
            throw new CodedException(this, ERR_CANT_FIND_ROOT, "can't find the User Set Access layer root page in the Registry!");

            //create the user set acess layer
        m_cUserSetAccessBxTree = new BxTree(8, 0, m_cPageBuf, Converter.bytesToInt(m_baTmpIntData), new SetKeyComparator(), bFormat);
        m_cUserSetAccess = new SetAccessImpl(m_cPageBuf, m_cUserSetAccessBxTree);

        Converter.intToBytes(REG_ROOT_INDX_ROOT, m_baTmpIntKey);
        if (!cRegistry.find(m_baTmpIntKey, m_baTmpIntData))
            throw new CodedException(this, ERR_CANT_FIND_ROOT, "can't find the Root Index root page in the Registry!");

            //create the root index
        m_cRootIndex = new BxTree(4, //IID
                    SetNumber.getByteLen(), //TID
                    m_cPageBuf,
                    Converter.bytesToInt(m_baTmpIntData),
                    new IntegerComparator(), bFormat);

        return;
    }

    ////////////////////////////////////////////////////////////////////////////
    private void initIndexRelTD() throws CodedException {
        m_cIndexRelTD = new TupleDescriptorImpl(4, RELA_RID_INDX, SYSCAT_SEGMENT);

        m_cIndexRelTD.setAttrSize(0, ATTR_SIZE_INDX_IID);
        m_cIndexRelTD.setAttrSize(1, ATTR_SIZE_INDX_INDNO);
        m_cIndexRelTD.setAttrSize(2, ATTR_SIZE_INDX_INDTYPE);
        m_cIndexRelTD.setAttrSize(3, ATTR_SIZE_INDX_INDCARD);

        m_cIndexRelTD.setType(0, ATTR_DID_INDX_IID);
        m_cIndexRelTD.setType(1, ATTR_DID_INDX_INDNO);
        m_cIndexRelTD.setType(2, ATTR_DID_INDX_INDTYPE);
        m_cIndexRelTD.setType(3, ATTR_DID_INDX_INDCARD);

        m_cIndexRelTD.setConverter(0, m_cTypeManager.getConverter(ATTR_DID_INDX_IID));
        m_cIndexRelTD.setConverter(1, m_cTypeManager.getConverter(ATTR_DID_INDX_INDNO));
        m_cIndexRelTD.setConverter(2, m_cTypeManager.getConverter(ATTR_DID_INDX_INDTYPE));
        m_cIndexRelTD.setConverter(3, m_cTypeManager.getConverter(ATTR_DID_INDX_INDCARD));

        m_cIndexRelTD.setName(0, ATTR_ANAME_INDX_IID);
        m_cIndexRelTD.setName(1, ATTR_ANAME_INDX_INDNO);
        m_cIndexRelTD.setName(2, ATTR_ANAME_INDX_INDTYPE);
        m_cIndexRelTD.setName(3, ATTR_ANAME_INDX_INDCARD);

        IndexInfoImpl cIndexInfo = new IndexInfoImpl(INDX_IID_INDX_IID);
        int[] iMap = new int[ 1 ];

        iMap[0] = ATTR_POS_INDX_IID;
        cIndexInfo.setAttributeMap(iMap);
        cIndexInfo.setUnique(true);
        addIndexInfoToCache( cIndexInfo, RELA_RID_INDX );
    }

    ////////////////////////////////////////////////////////////////////////////
    private void initRelationRelTD() throws CodedException {
        m_cRelationRelTD = new TupleDescriptorImpl(2, RELA_RID_RELA, SYSCAT_SEGMENT);

        m_cRelationRelTD.setAttrSize(0, ATTR_SIZE_RELA_RID);
        m_cRelationRelTD.setAttrSize(1, ATTR_SIZE_RELA_RNAME);

        m_cRelationRelTD.setType(0, ATTR_DID_RELA_RID);
        m_cRelationRelTD.setType(1, ATTR_DID_RELA_RNAME);

        m_cRelationRelTD.setConverter(0, m_cTypeManager.getConverter(ATTR_DID_RELA_RID));
        m_cRelationRelTD.setConverter(1, m_cTypeManager.getConverter(ATTR_DID_RELA_RNAME));

        m_cRelationRelTD.setName(0, ATTR_ANAME_RELA_RID);
        m_cRelationRelTD.setName(1, ATTR_ANAME_RELA_RNAME);

        IndexInfoImpl cIndexInfo = new IndexInfoImpl(INDX_IID_RELA_RID);
        int[] iMap = new int[ 1 ];

        iMap[0] = ATTR_POS_RELA_RID;
        cIndexInfo.setAttributeMap(iMap);
        cIndexInfo.setUnique(true);
        addIndexInfoToCache( cIndexInfo, RELA_RID_RELA );

        cIndexInfo = new IndexInfoImpl(INDX_IID_RELA_RNAME);
        iMap = new int[ 1 ];
        iMap[0] = ATTR_POS_RELA_RNAME;
        cIndexInfo.setAttributeMap(iMap);
        cIndexInfo.setUnique(true);
        addIndexInfoToCache( cIndexInfo, RELA_RID_RELA );
    }

    ////////////////////////////////////////////////////////////////////////////
    private void initAttributeRelTD() throws CodedException {
        m_cAttributeRelTD = new TupleDescriptorImpl(6, RELA_RID_ATTR, SYSCAT_SEGMENT);

        m_cAttributeRelTD.setAttrSize(0, ATTR_SIZE_ATTR_AID);
        m_cAttributeRelTD.setAttrSize(1, ATTR_SIZE_ATTR_ANAME);
        m_cAttributeRelTD.setAttrSize(2, ATTR_SIZE_ATTR_RID);
        m_cAttributeRelTD.setAttrSize(3, ATTR_SIZE_ATTR_POS);
        m_cAttributeRelTD.setAttrSize(4, ATTR_SIZE_ATTR_DID);
        m_cAttributeRelTD.setAttrSize(5, ATTR_SIZE_ATTR_SIZE);

        m_cAttributeRelTD.setType(0, ATTR_DID_ATTR_AID);
        m_cAttributeRelTD.setType(1, ATTR_DID_ATTR_ANAME);
        m_cAttributeRelTD.setType(2, ATTR_DID_ATTR_RID);
        m_cAttributeRelTD.setType(3, ATTR_DID_ATTR_POS);
        m_cAttributeRelTD.setType(4, ATTR_DID_ATTR_DID);
        m_cAttributeRelTD.setType(5, ATTR_DID_ATTR_SIZE);

        m_cAttributeRelTD.setConverter(0, m_cTypeManager.getConverter(ATTR_DID_ATTR_AID));
        m_cAttributeRelTD.setConverter(1, m_cTypeManager.getConverter(ATTR_DID_ATTR_ANAME));
        m_cAttributeRelTD.setConverter(2, m_cTypeManager.getConverter(ATTR_DID_ATTR_RID));
        m_cAttributeRelTD.setConverter(3, m_cTypeManager.getConverter(ATTR_DID_ATTR_POS));
        m_cAttributeRelTD.setConverter(4, m_cTypeManager.getConverter(ATTR_DID_ATTR_DID));
        m_cAttributeRelTD.setConverter(5, m_cTypeManager.getConverter(ATTR_DID_ATTR_SIZE));

        m_cAttributeRelTD.setName(0, ATTR_ANAME_ATTR_AID);
        m_cAttributeRelTD.setName(1, ATTR_ANAME_ATTR_ANAME);
        m_cAttributeRelTD.setName(2, ATTR_ANAME_ATTR_RID);
        m_cAttributeRelTD.setName(3, ATTR_ANAME_ATTR_POS);
        m_cAttributeRelTD.setName(4, ATTR_ANAME_ATTR_DID);
        m_cAttributeRelTD.setName(5, ATTR_ANAME_ATTR_SIZE);

        IndexInfoImpl cIndexInfo = new IndexInfoImpl(INDX_IID_ATTR_AID);
        int[] iMap = new int[ 1 ];

        iMap[0] = ATTR_POS_ATTR_AID;
        cIndexInfo.setAttributeMap(iMap);
        cIndexInfo.setUnique(true);
        addIndexInfoToCache( cIndexInfo, RELA_RID_ATTR );

        cIndexInfo = new IndexInfoImpl(INDX_IID_ATTR_RID);
        iMap = new int[ 1 ];
        iMap[0] = ATTR_POS_ATTR_RID;
        cIndexInfo.setAttributeMap(iMap);
        cIndexInfo.setUnique(false);
        addIndexInfoToCache( cIndexInfo, RELA_RID_ATTR );
    }

    ////////////////////////////////////////////////////////////////////////////
    private void initDatatypeRelTD() throws CodedException {
        m_cDatatypeRelTD = new TupleDescriptorImpl(2, RELA_RID_INDX, SYSCAT_SEGMENT);

        m_cDatatypeRelTD.setAttrSize(0, ATTR_SIZE_DATP_DID);
        m_cDatatypeRelTD.setAttrSize(1, ATTR_SIZE_DATP_DNAME);

        m_cDatatypeRelTD.setType(0, ATTR_DID_DATP_DID);
        m_cDatatypeRelTD.setType(1, ATTR_DID_DATP_DNAME);

        m_cDatatypeRelTD.setConverter(0, m_cTypeManager.getConverter(ATTR_DID_DATP_DID));
        m_cDatatypeRelTD.setConverter(1, m_cTypeManager.getConverter(ATTR_DID_DATP_DNAME));

        m_cDatatypeRelTD.setName(0, ATTR_ANAME_DATP_DID);
        m_cDatatypeRelTD.setName(1, ATTR_ANAME_DATP_DNAME);

        IndexInfoImpl cIndexInfo = new IndexInfoImpl(INDX_IID_DATP_DID);
        int[] iMap = new int[ 1 ];

        iMap[0] = ATTR_POS_DATP_DID;
        cIndexInfo.setAttributeMap(iMap);
        cIndexInfo.setUnique(true);
        addIndexInfoToCache( cIndexInfo, RELA_RID_DATP );

    }

    ////////////////////////////////////////////////////////////////////////////
    private void initIndexmapRelTD() throws CodedException {
        m_cIndexmapRelTD = new TupleDescriptorImpl(3, RELA_RID_IMAP, SYSCAT_SEGMENT);

        m_cIndexmapRelTD.setAttrSize(0, ATTR_SIZE_IMAP_RID);
        m_cIndexmapRelTD.setAttrSize(1, ATTR_SIZE_IMAP_IID);
        m_cIndexmapRelTD.setAttrSize(2, ATTR_SIZE_IMAP_APOS);

        m_cIndexmapRelTD.setType(0, ATTR_DID_IMAP_RID);
        m_cIndexmapRelTD.setType(1, ATTR_DID_IMAP_IID);
        m_cIndexmapRelTD.setType(2, ATTR_DID_IMAP_APOS);

        m_cIndexmapRelTD.setConverter(0, m_cTypeManager.getConverter(ATTR_DID_IMAP_RID));
        m_cIndexmapRelTD.setConverter(1, m_cTypeManager.getConverter(ATTR_DID_IMAP_IID));
        m_cIndexmapRelTD.setConverter(2, m_cTypeManager.getConverter(ATTR_DID_IMAP_APOS));

        m_cIndexmapRelTD.setName(0, ATTR_ANAME_IMAP_RID);
        m_cIndexmapRelTD.setName(1, ATTR_ANAME_IMAP_IID);
        m_cIndexmapRelTD.setName(2, ATTR_ANAME_IMAP_APOS);

        IndexInfoImpl cIndexInfo = new IndexInfoImpl(INDX_IID_IMAP_RID);
        int[] iMap = new int[ 1 ];

        iMap[0] = ATTR_POS_IMAP_RID;
        cIndexInfo.setAttributeMap(iMap);
        cIndexInfo.setUnique(false);
        addIndexInfoToCache( cIndexInfo, RELA_RID_IMAP );

        cIndexInfo = new IndexInfoImpl(INDX_IID_IMAP_IID);
        iMap = new int[ 1 ];
        iMap[0] = ATTR_POS_IMAP_IID;
        cIndexInfo.setAttributeMap(iMap);
        cIndexInfo.setUnique(false);
        addIndexInfoToCache( cIndexInfo, RELA_RID_IMAP );
    }

    ////////////////////////////////////////////////////////////////////////////
    protected DbIndex getIndex(int iIID) throws CodedException {

        if (iIID == INDX_IID_INDX_IID) {
            return new DbSingleAttrIndex(m_cRootIndex);
        }else {
            m_cTmpTuple.setTd(m_cIndexRelTD);

            Converter.intToBytes(iIID, m_baTmpIntKey);

            if (!m_cRootIndex.find(m_baTmpIntKey, m_baTmpSetNum))
                throw new CodedException(this, ERR_INVALID_INDEX, "Index does not exist!");
            m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
            Set cSet = m_cSetAccess.getSet(m_cTmpSetNum, RELA_RID_INDX);

            m_cTmpTuple.setData(cSet);
            int iIndexPageNum = Converter.bytesToInt(m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(1));

            m_cTmpTuple.unfix();

            switch (iIID) {
            case INDX_IID_RELA_RID:
            case INDX_IID_ATTR_AID:
            case INDX_IID_ATTR_RID:
            case INDX_IID_DATP_DID:
            case INDX_IID_IMAP_RID:
            case INDX_IID_IMAP_IID: {

                    return new DbSingleAttrIndex(new BxTree(4,
                                SetNumber.getByteLen(),
                                m_cPageBuf,
                                iIndexPageNum,
                                new IntegerComparator(), false));
                }

            case INDX_IID_RELA_RNAME: {

                    return new DbSingleAttrIndex(new BxTree(m_cRelationRelTD.getAttrSize(1),
                                SetNumber.getByteLen(),
                                m_cPageBuf,
                                iIndexPageNum,
                                new StringComparator(), false));

                }

            default: {
                    DbIndex cIndex = getIndex(INDX_IID_IMAP_IID);

                    Converter.intToBytes(iIID, m_baTmpIntKey);
                    cIndex.addAttributeToKey(m_baTmpIntKey, 0);
                    if (!cIndex.find(m_baTmpSetNum))
                        throw new CodedException(this, ERR_INVALID_INDEX, "Index does not exist!");

                    m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
                    m_cTmpSetArray.init(m_cSetAccess, m_cTmpSetNum, SetNumber.getByteLen(), RELA_RID_IMAP);
                    if (!m_cTmpSetArray.moveToFirst())
                        throw new CodedException(this, ERR_INCONSISTENT_INDEX, "IMAP_IID Index is not consistent!");

                    cSet = m_cTmpSetArray.getCurrentRecord();
                    m_cTmpSetNum.setSetNumber(cSet.getBytes(), cSet.getOffset());
                    Set cSet1 = m_cSetAccess.getSet(m_cTmpSetNum, RELA_RID_IMAP);

                    cSet.unfix();
                    cSet = cSet1;

                    m_cTmpTuple.setTd(m_cIndexmapRelTD);
                    m_cTmpTuple.setData(cSet);

                    int iRID = Converter.bytesToInt(m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(ATTR_POS_IMAP_RID));

                    m_cTmpTuple.unfix();

                    TupleDescriptor cTD = getTupleDescriptor(iRID);
                    IndexInfo cIndexInfo = getIndexInfo(iIID);

                    switch (cIndexInfo.getType()) {
                    case INDX_TYPE_SINGLE_ATTR: {
                            return new DbSingleAttrIndex(new BxTree(cTD.getAttrSize(cIndexInfo.getAttributeMap()[0]),
                                        SetNumber.getByteLen(),
                                        m_cPageBuf,
                                        iIndexPageNum,
                                        m_cTypeManager.getComparator(cTD.getType(cIndexInfo.getAttributeMap()[0])),
                                        false));
                        }

                    case INDX_TYPE_SINGLE_DIM_MULTI_ATTR: {
                            DbComparator[] acComparators = new DbComparator[ cIndexInfo.getAttributeMap().length ];
                            int[] aiSizes = new int[ cIndexInfo.getAttributeMap().length ];

                            for (int iCnter = 0; iCnter < cIndexInfo.getAttributeMap().length; iCnter++) {
                                acComparators[ iCnter ] = m_cTypeManager.getComparator(cTD.getType(cIndexInfo.getAttributeMap()[ iCnter ]));
                                aiSizes[ iCnter ] = cTD.getAttrSize(cIndexInfo.getAttributeMap()[ iCnter ]);
                            }

                            CompoundComparator cCpComp = new CompoundComparator(aiSizes, acComparators);

                            return new DbSingleDimIndex(new BxTree(cCpComp.getKeySize(),
                                        SetNumber.getByteLen(),
                                        m_cPageBuf,
                                        iIndexPageNum,
                                        cCpComp,
                                        false));
                        }

                    default:
                        throw new CodedException(this, ERR_UNSUPPORTED_ITYPE, "Unsupported index type!");
                    }
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    private void insertInSecondaryIndex(DbIndex cIndex, byte[] baData, int iSetType) throws CodedException {

        if (cIndex.find(m_baTmpSetNum)) {
            m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
            m_cTmpSetArray.init(m_cSetAccess, m_cTmpSetNum, m_baTmpSetNum.length, iSetType);
            m_cTmpSetArray.addAtFront(baData, 0);
        }else {
            m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
            m_cTmpSetArray.createStartSet(m_cSetAccess, m_cTmpSetNum, iSetType);
            m_cTmpSetArray.init(m_cSetAccess, m_cTmpSetNum, baData.length, iSetType);
            m_cTmpSetArray.addAtFront(baData, 0);
            cIndex.insert(m_cTmpSetNum.getSetNumber());
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    private void removeFromSecondaryIndex(DbIndex cIndex, byte[] baData, int iSetType) throws CodedException {

        if (cIndex.find(m_baTmpSetNum)) {
            m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
            m_cTmpSetArray.init(m_cSetAccess, m_cTmpSetNum, m_baTmpSetNum.length, iSetType);
            if (!m_cTmpSetArray.moveToFirst())
                throw new CodedException(this, ERR_INCONSISTENT_INDEX, "Inconsistent index!");

            do {
                Set cSet = m_cTmpSetArray.getCurrentRecord();

                if (m_cTmpByteComparator.compare(baData, 0, cSet.getBytes(), cSet.getOffset(), baData.length) == 0) {
                    cSet.unfix();
                    m_cTmpSetArray.removeCurrentRecord();
                }
            }
            while (m_cTmpSetArray.moveToNext());

            if (!m_cTmpSetArray.moveToFirst())
                cIndex.remove();
        }

    }

    ////////////////////////////////////////////////////////////////////////////
    private void addToIndexRel(int iIID, int iType, int iCardinality/*can be only 0 or 1*/) throws CodedException {

        //change this, if new index types added
        if (!checkIndexType(iType))
            throw new CodedException(this, ERR_UNSUPPORTED_ITYPE, "unsupported index type requested:" + iType);

        m_cTmpTuple.setTd(m_cIndexRelTD);

        int iPageNum = 0;

        if (iIID == INDX_IID_INDX_IID) {
            iPageNum = INDX_INDNO_INDX_IID;
        }else {
            //get a free root page for the new index
            iPageNum = m_cPageBuf.getPageManager().getBlankPage();

            //fixit!
            //this line should be changed, if other index types have to be supported!
            //create this object only to format the root page
            BxTree cTmpTree = new BxTree(10, 10, m_cPageBuf, iPageNum, new ByteComparator(), true);
        }

        Converter.intToBytes(iIID, m_baTmpIntKey);

        m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
        Set cSet = m_cSetAccess.addSet(m_cTmpSetNum, m_cIndexRelTD.getTupleSize(), RELA_RID_INDX);

        m_cTmpTuple.setData(cSet);

        Converter.moveBytes(m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(0), m_baTmpIntKey, 0, INTSIZE);
        Converter.intToBytes(iPageNum, m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(1));
        Converter.intToBytes(iType, m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(2));

        if (iCardinality != 0 && iCardinality != 1)
            throw new CodedException(this, ERR_WRONG_CARDINALITY, "Invalid cardinality specified! : " + iCardinality);
        Converter.intToBytes(iCardinality, m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(3));

        m_cTmpTuple.unfix();

        m_cRootIndex.insert(m_baTmpIntKey, m_cTmpSetNum.getSetNumber());
    }

    ////////////////////////////////////////////////////////////////////////////
    private void initIndexRel() throws CodedException {
        addToIndexRel(INDX_IID_INDX_IID, INDX_TYPE_SINGLE_ATTR, 0);
        addToIndexRel(INDX_IID_RELA_RID, INDX_TYPE_SINGLE_ATTR, 0);
        addToIndexRel(INDX_IID_RELA_RNAME, INDX_TYPE_SINGLE_ATTR, 0);
        addToIndexRel(INDX_IID_ATTR_AID, INDX_TYPE_SINGLE_ATTR, 0);
        addToIndexRel(INDX_IID_ATTR_RID, INDX_TYPE_SINGLE_ATTR, 1);
        addToIndexRel(INDX_IID_DATP_DID, INDX_TYPE_SINGLE_ATTR, 0);
        addToIndexRel(INDX_IID_IMAP_RID, INDX_TYPE_SINGLE_ATTR, 1);
        addToIndexRel(INDX_IID_IMAP_IID, INDX_TYPE_SINGLE_ATTR, 1);
    }

    ////////////////////////////////////////////////////////////////////////////
    private void addToRelaRel(int iRID, String szName) throws CodedException {

        m_cTmpTuple.setTd(m_cRelationRelTD);
        SetNumber cSetNum = new SetNumber();
        Set cSet = m_cSetAccess.addSet(cSetNum, m_cRelationRelTD.getTupleSize(), RELA_RID_RELA);

        m_cTmpTuple.setData(cSet);

        Converter.stringToBytes(szName, m_cTmpTuple.getBytes(),
            m_cTmpTuple.getFieldOffset(ATTR_POS_RELA_RNAME),
            m_cRelationRelTD.getAttrSize(ATTR_POS_RELA_RNAME));
        Converter.intToBytes(iRID, m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(ATTR_POS_RELA_RID));

        m_cTmpTuple.unfix();

        DbIndex cIndex = getIndex(INDX_IID_RELA_RID);

        Converter.intToBytes(iRID, m_baTmpIntKey, 0);
        cIndex.addAttributeToKey(m_baTmpIntKey, 0);
        //insert into the unique index
        cIndex.insert(cSetNum.getSetNumber());

        cIndex = getIndex(INDX_IID_RELA_RNAME);
        Converter.stringToBytes(szName, m_baTmpStrKey, 0, m_baTmpStrKey.length);
        cIndex.addAttributeToKey(m_baTmpStrKey, 0);

        //insert into the unique index
        cIndex.insert(cSetNum.getSetNumber());

        return;
    }

    ////////////////////////////////////////////////////////////////////////////
    private void initRelaRel() throws CodedException {

        addToRelaRel(RELA_RID_INDX, RELA_RNAME_INDX);
        addToRelaRel(RELA_RID_RELA, RELA_RNAME_RELA);
        addToRelaRel(RELA_RID_ATTR, RELA_RNAME_ATTR);
        addToRelaRel(RELA_RID_DATP, RELA_RNAME_DATP);
        addToRelaRel(RELA_RID_IMAP, RELA_RNAME_IMAP);

        return;
    }

    ////////////////////////////////////////////////////////////////////////////
    private void addToAttrRel(int iAID, String szName, int iRID, int iPos, int iDID, int iSize) throws CodedException {

        //add the tuple in the SetAccess
        SetNumber cSetNum = new SetNumber();
        Set cSet = m_cSetAccess.addSet(cSetNum, m_cAttributeRelTD.getTupleSize(), RELA_RID_ATTR);

        m_cTmpTuple.setTd(m_cAttributeRelTD);
        m_cTmpTuple.setData(cSet);

        Converter.stringToBytes(szName, m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(ATTR_POS_ATTR_ANAME),
            m_cAttributeRelTD.getAttrSize(ATTR_POS_ATTR_ANAME));

        Converter.intToBytes(iAID, m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(ATTR_POS_ATTR_AID));
        Converter.intToBytes(iRID, m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(ATTR_POS_ATTR_RID));
        Converter.intToBytes(iPos, m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(ATTR_POS_ATTR_POS));
        Converter.intToBytes(iDID, m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(ATTR_POS_ATTR_DID));
        Converter.intToBytes(iSize, m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(ATTR_POS_ATTR_SIZE));

        m_cTmpTuple.unfix();

        //add the tuple to the AID index
        DbIndex cIndex = getIndex(INDX_IID_ATTR_AID);

        Converter.intToBytes(iAID, m_baTmpIntKey, 0);
        cIndex.addAttributeToKey(m_baTmpIntKey, 0);
        cIndex.insert(cSetNum.getSetNumber());

        //add the tuple to the ARID index
        cIndex = getIndex(INDX_IID_ATTR_RID);
        Converter.intToBytes(iRID, m_baTmpIntKey, 0);
        cIndex.addAttributeToKey(m_baTmpIntKey, 0);

        //RID can appear more than once, so we need a special index!
        insertInSecondaryIndex(cIndex, cSetNum.getSetNumber(), RELA_RID_ATTR);

        return;

    }

    ////////////////////////////////////////////////////////////////////////////
    private void initAttrRel() throws CodedException {

        addToAttrRel(ATTR_AID_INDX_IID, ATTR_ANAME_INDX_IID, RELA_RID_INDX, ATTR_POS_INDX_IID, TypeManager.DATP_DID_INT, INTSIZE);
        addToAttrRel(ATTR_AID_INDX_INDNO, ATTR_ANAME_INDX_INDNO, RELA_RID_INDX, ATTR_POS_INDX_INDNO, TypeManager.DATP_DID_INT, INTSIZE);
        addToAttrRel(ATTR_AID_INDX_INDTYPE, ATTR_ANAME_INDX_INDTYPE, RELA_RID_INDX, ATTR_POS_INDX_INDTYPE, TypeManager.DATP_DID_INT, INTSIZE);
        addToAttrRel(ATTR_AID_INDX_INDCARD, ATTR_ANAME_INDX_INDCARD, RELA_RID_INDX, ATTR_POS_INDX_INDCARD, TypeManager.DATP_DID_INT, INTSIZE);

        addToAttrRel(ATTR_AID_RELA_RID, ATTR_ANAME_RELA_RID, RELA_RID_RELA, ATTR_POS_RELA_RID, TypeManager.DATP_DID_INT, INTSIZE);
        addToAttrRel(ATTR_AID_RELA_RNAME, ATTR_ANAME_RELA_RNAME, RELA_RID_RELA, ATTR_POS_RELA_RNAME, TypeManager.DATP_DID_STR, 20);

        addToAttrRel(ATTR_AID_ATTR_AID, ATTR_ANAME_ATTR_AID, RELA_RID_ATTR, ATTR_POS_ATTR_AID, TypeManager.DATP_DID_INT, INTSIZE);
        addToAttrRel(ATTR_AID_ATTR_ANAME, ATTR_ANAME_ATTR_ANAME, RELA_RID_ATTR, ATTR_POS_ATTR_ANAME, TypeManager.DATP_DID_STR, 20);
        addToAttrRel(ATTR_AID_ATTR_RID, ATTR_ANAME_ATTR_RID, RELA_RID_ATTR, ATTR_POS_ATTR_RID, TypeManager.DATP_DID_INT, INTSIZE);
        addToAttrRel(ATTR_AID_ATTR_POS, ATTR_ANAME_ATTR_POS, RELA_RID_ATTR, ATTR_POS_ATTR_POS, TypeManager.DATP_DID_INT, INTSIZE);
        addToAttrRel(ATTR_AID_ATTR_DID, ATTR_ANAME_ATTR_DID, RELA_RID_ATTR, ATTR_POS_ATTR_DID, TypeManager.DATP_DID_INT, INTSIZE);
        addToAttrRel(ATTR_AID_ATTR_SIZE, ATTR_ANAME_ATTR_SIZE, RELA_RID_ATTR, ATTR_POS_ATTR_SIZE, TypeManager.DATP_DID_INT, INTSIZE);

        addToAttrRel(ATTR_AID_DATP_DID, ATTR_ANAME_DATP_DID, RELA_RID_DATP, ATTR_POS_DATP_DID, TypeManager.DATP_DID_INT, INTSIZE);
        addToAttrRel(ATTR_AID_DATP_DNAME, ATTR_ANAME_DATP_DNAME, RELA_RID_DATP, ATTR_POS_DATP_DNAME, TypeManager.DATP_DID_STR, 20);

        addToAttrRel(ATTR_AID_IMAP_RID, ATTR_ANAME_IMAP_RID, RELA_RID_IMAP, ATTR_POS_IMAP_RID, TypeManager.DATP_DID_INT, INTSIZE);
        addToAttrRel(ATTR_AID_IMAP_IID, ATTR_ANAME_IMAP_IID, RELA_RID_IMAP, ATTR_POS_IMAP_IID, TypeManager.DATP_DID_INT, INTSIZE);
        addToAttrRel(ATTR_AID_IMAP_APOS, ATTR_ANAME_IMAP_APOS, RELA_RID_IMAP, ATTR_POS_IMAP_APOS, TypeManager.DATP_DID_INT, INTSIZE);

        return;

    }

    ////////////////////////////////////////////////////////////////////////////
    private void addToDatpRel(int iDID, String szName) throws CodedException {

        //add the tuple in the SetAccess
        Set cSet = m_cSetAccess.addSet(m_cTmpSetNum, m_cDatatypeRelTD.getTupleSize(), RELA_RID_DATP);

        m_cTmpTuple.setTd(m_cDatatypeRelTD);
        m_cTmpTuple.setData(cSet);

        Converter.stringToBytes(szName, m_cTmpTuple.getBytes(),
            m_cTmpTuple.getFieldOffset(ATTR_POS_DATP_DNAME),
            m_cTmpTuple.getReader().getTd().getAttrSize(ATTR_POS_DATP_DNAME));

        Converter.intToBytes(iDID, m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(ATTR_POS_DATP_DID));
        m_cTmpTuple.unfix();

        //add the tuple to the AID index
        DbIndex cIndex = getIndex(INDX_IID_DATP_DID);

        Converter.intToBytes(iDID, m_baTmpIntKey);

        cIndex.addAttributeToKey(m_baTmpIntKey, 0);
        cIndex.insert(m_cTmpSetNum.getSetNumber());

        return;
    }

    ////////////////////////////////////////////////////////////////////////////
    private void initDatpRel() throws CodedException {
        int[][] aiTypeInfos = m_cTypeManager.getTypeInfos();

        for( int iCnter = 0; iCnter < aiTypeInfos.length; iCnter ++ ){
          addToDatpRel( aiTypeInfos[iCnter][0], m_cTypeManager.getTypeName(aiTypeInfos[iCnter][0]));
        }

        return;
    }

    ////////////////////////////////////////////////////////////////////////////
    private void addToImapRel(int iRID, int iIID, int iPos)throws CodedException {

        //add the tuple in the SetAccess
        SetNumber cSetNum = new SetNumber();
        Set cSet = m_cSetAccess.addSet(cSetNum, m_cIndexmapRelTD.getTupleSize(), RELA_RID_IMAP);

        m_cTmpTuple.setTd(m_cIndexmapRelTD);
        m_cTmpTuple.setData(cSet);

        Converter.intToBytes(iRID, m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(ATTR_POS_IMAP_RID));
        Converter.intToBytes(iIID, m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(ATTR_POS_IMAP_IID));
        Converter.intToBytes(iPos, m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(ATTR_POS_IMAP_APOS));

        m_cTmpTuple.unfix();

        //add the tuple to the AID index
        DbIndex cIndex = getIndex(INDX_IID_IMAP_RID);

        Converter.intToBytes(iRID, m_baTmpIntKey, 0);
        cIndex.addAttributeToKey(m_baTmpIntKey, 0);

        //RID can appear more than once, so we need a special index!
        insertInSecondaryIndex(cIndex, cSetNum.getSetNumber(), RELA_RID_IMAP);

        //add the tuple to the ARID index
        cIndex = getIndex(INDX_IID_IMAP_IID);
        Converter.intToBytes(iIID, m_baTmpIntKey, 0);
        cIndex.addAttributeToKey(m_baTmpIntKey, 0);

        //IID can appear more than once, so we need a special index!
        insertInSecondaryIndex(cIndex, cSetNum.getSetNumber(), RELA_RID_IMAP);

        return;
    }

    ////////////////////////////////////////////////////////////////////////////
    private void initImapRel() throws CodedException {
        addToImapRel(RELA_RID_INDX, INDX_IID_INDX_IID, ATTR_POS_INDX_IID);

        addToImapRel(RELA_RID_RELA, INDX_IID_RELA_RID, ATTR_POS_RELA_RID);
        addToImapRel(RELA_RID_RELA, INDX_IID_RELA_RNAME, ATTR_POS_RELA_RNAME);

        addToImapRel(RELA_RID_ATTR, INDX_IID_ATTR_AID, ATTR_POS_ATTR_AID);
        addToImapRel(RELA_RID_ATTR, INDX_IID_ATTR_RID, ATTR_POS_ATTR_RID);

        addToImapRel(RELA_RID_DATP, INDX_IID_DATP_DID, ATTR_POS_DATP_DID);

        addToImapRel(RELA_RID_IMAP, INDX_IID_IMAP_RID, ATTR_POS_IMAP_RID);
        addToImapRel(RELA_RID_IMAP, INDX_IID_IMAP_IID, ATTR_POS_IMAP_IID);
        return;
    }

    ////////////////////////////////////////////////////////////////////////////
    private int getIndexTypeAndCard(int iIID, ObjHolder cIsUnique) throws CodedException {
        DbIndex cIndex = getIndex(INDX_IID_INDX_IID);

        Converter.intToBytes(iIID, m_baTmpIntKey);
        cIndex.addAttributeToKey(m_baTmpIntKey, 0);
        if (!cIndex.find(m_baTmpSetNum))
            throw new CodedException(this, ERR_INVALID_INDEX, "Index not found!");
        m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
        Set cSet = m_cSetAccess.getSet(m_cTmpSetNum, RELA_RID_INDX);

        m_cTmpTuple.setTd(m_cIndexRelTD);
        m_cTmpTuple.setData(cSet);

        int iCard = Converter.bytesToInt(m_cTmpTuple.getBytes(),
                m_cTmpTuple.getFieldOffset(ATTR_POS_INDX_INDCARD));

        int iType = Converter.bytesToInt(m_cTmpTuple.getBytes(),
                m_cTmpTuple.getFieldOffset(ATTR_POS_INDX_INDTYPE));

        m_cTmpTuple.unfix();

        if (iCard == 0)
            cIsUnique.setObj(Boolean.TRUE);
        else
            cIsUnique.setObj(Boolean.FALSE);

        return iType;
    }

    ////////////////////////////////////////////////////////////////////////////
    protected List getIndexInfos(int iRID) throws CodedException {
        List cRet = (List) m_cIndexInfoCache.get(iRID);

        if (cRet != null)
            return cRet;
        else {

            cRet = new List();

            DbIndex cIndex = getIndex(INDX_IID_IMAP_RID);

            Converter.intToBytes(iRID, m_baTmpIntKey, 0);
            cIndex.addAttributeToKey(m_baTmpIntKey, 0);
            if (!cIndex.find(m_baTmpSetNum))
                return null;

            m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
            m_cTmpSetArray.init(m_cSetAccess, m_cTmpSetNum, m_baTmpSetNum.length, RELA_RID_IMAP);
            m_cTmpSetArray.moveToFirst();

            m_cTmpTuple.setTd(m_cIndexmapRelTD);

            do {
                Set cSet = m_cTmpSetArray.getCurrentRecord();

                m_cTmpSetNum.setSetNumber(cSet.getBytes(), cSet.getOffset());
                Set cSet1 = m_cSetAccess.getSet(m_cTmpSetNum, RELA_RID_IMAP);

                cSet.unfix();
                cSet = cSet1;

                m_cTmpTuple.setData(cSet);
                int iIID = Converter.bytesToInt(m_cTmpTuple.getBytes(),
                        m_cTmpTuple.getFieldOffset(ATTR_POS_IMAP_IID));
                int iPos = Converter.bytesToInt(m_cTmpTuple.getBytes(),
                        m_cTmpTuple.getFieldOffset(ATTR_POS_IMAP_APOS));

                m_cTmpTuple.unfix();

                IndexInfoImpl cIndexInfo = null;

                if (cRet.moveToFirst()) {
                    do {
                        if (((IndexInfo) cRet.getCurrent()).getIID() == iIID) {
                            cIndexInfo = (IndexInfoImpl) cRet.getCurrent();
                            break;
                        }
                    }
                    while (cRet.moveToNext());
                }

                if (cIndexInfo == null) {
                    cIndexInfo = new IndexInfoImpl(iIID);
                    cRet.insert(cIndexInfo);
                }
                cIndexInfo.addAttributeToMap(iPos);
            }
            while (m_cTmpSetArray.moveToNext());

            if (cRet.moveToFirst()) {
                do {
                    ObjHolder o = new ObjHolder();

                    ((IndexInfoImpl) cRet.getCurrent()).setType(getIndexTypeAndCard(((IndexInfo) cRet.getCurrent()).getIID(), o));
                    ((IndexInfoImpl) cRet.getCurrent()).setUnique(((Boolean) o.getObj()).booleanValue());
                }
                while (cRet.moveToNext());
            }

            return cRet;
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    private void manageIndexes(Tuple cTuple, boolean bAttach) throws CodedException {
        List cIndexInfos = (List)m_cIndexInfoCache.get(cTuple.getReader().getTd().getRelId());
        List cIndexCache = (List)m_cIndexCache.get(cTuple.getReader().getTd().getRelId());

        //create index info cache to avoid calling getIndexInfos for all tuples
        if (cIndexInfos == null) {
            cIndexInfos = getIndexInfos(cTuple.getReader().getTd().getRelId());
            m_cIndexInfoCache.put(cTuple.getReader().getTd().getRelId(), cIndexInfos);
        }

        if (cIndexInfos.isEmpty())
            throw new CodedException(this, ERR_NO_INDEX_DEFINED, "No index defined in the table!");
        else
            cIndexInfos.moveToFirst();

        boolean bCreateCache = false;

        //create index cache to avoid calling getIndex (and creating new DbIndex objs!)
        //for all the Tuples

        if (cIndexCache == null) {
            cIndexCache = new List();
            m_cIndexCache.put(cTuple.getReader().getTd().getRelId(), cIndexCache);
            bCreateCache = true;
        }else {
            cIndexCache.moveToFirst();
        }

        do {
            IndexInfo cIndexInfo = (IndexInfo) cIndexInfos.getCurrent();
            DbIndex cIndex;

            if (bCreateCache) {
                cIndex = getIndex(cIndexInfo.getIID());
                cIndexCache.insert(cIndex);
            }else {
                cIndex = (DbIndex) cIndexCache.getCurrent();
                cIndexCache.moveToNext();
            }

            int[] aiAttrMap = cIndexInfo.getAttributeMap();

            cIndex.clearKey();
            for (int iCnter = 0; iCnter < aiAttrMap.length; iCnter++) {
                cIndex.addAttributeToKey(cTuple.getBytes(), cTuple.getFieldOffset(aiAttrMap[ iCnter ]));
            }

            if (bAttach) {  //attach the new tuple to the indexes
                if (cIndexInfo.isUnique())
                    cIndex.insert(cTuple.getSetNum().getSetNumber());
                else
                    insertInSecondaryIndex(cIndex, cTuple.getSetNum().getSetNumber(), cTuple.getReader().getTd().getRelId());
            }else { //detach an existing tuple from the indexes
                if (cIndexInfo.isUnique())
                    cIndex.remove();
                else
                    removeFromSecondaryIndex(cIndex, cTuple.getSetNum().getSetNumber(), cTuple.getReader().getTd().getRelId());
            }
        }
        while (cIndexInfos.moveToNext());
    }

    ////////////////////////////////////////////////////////////////////////////
    public int getRelId(String szRelName) throws CodedException {
        DbIndex cIndex = getIndex(INDX_IID_RELA_RNAME);

        Converter.stringToBytes(szRelName, m_baTmpStrKey, 0, m_baTmpStrKey.length);
        cIndex.addAttributeToKey(m_baTmpStrKey, 0);

        if (!cIndex.find(m_baTmpSetNum)) {
            return -1; //not found
        }

        m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
        Set cSet = m_cSetAccess.getSet(m_cTmpSetNum, RELA_RID_RELA);

        m_cTmpTuple.setTd(m_cRelationRelTD);
        m_cTmpTuple.setData(cSet);

        int iRet = Converter.bytesToInt(m_cTmpTuple.getBytes(),
                m_cTmpTuple.getFieldOffset(ATTR_DID_RELA_RID));

        m_cTmpTuple.unfix();

        return iRet;
    }

    ////////////////////////////////////////////////////////////////////////////
    public TupleDescriptor getTupleDescriptor(int iRID) throws CodedException {

        switch (iRID) {
        case RELA_RID_INDX:
            return m_cIndexRelTD;

        case RELA_RID_RELA:
            return m_cRelationRelTD;

        case RELA_RID_ATTR:
            return m_cAttributeRelTD;

        case RELA_RID_DATP:
            return m_cDatatypeRelTD;

        default: {

                DbIndex cIndex = getIndex(INDX_IID_ATTR_RID);

                Converter.intToBytes(iRID, m_baTmpIntKey, 0);
                cIndex.addAttributeToKey(m_baTmpIntKey, 0);
                if (!cIndex.find(m_baTmpSetNum))
                    throw new CodedException(this, ERR_INVALID_RID, "Relation id is invalid!");

                m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
                m_cTmpSetArray.init(m_cSetAccess, m_cTmpSetNum, SetNumber.getByteLen(), RELA_RID_ATTR);
                if (!m_cTmpSetArray.moveToFirst())
                    throw new CodedException(this, ERR_INCONSISTENT_INDEX, "Attribute relation is not consistent!");

                class AttrInfo {
                    public int m_iPos;
                    public int m_iSize;
                    public int m_iType;
                    public String m_szName;
                }

                int iAttrCount = 0;
                List cList = new List();

                m_cTmpTuple.setTd(m_cAttributeRelTD);
                do {
                    Set cSet = m_cTmpSetArray.getCurrentRecord();

                    m_cTmpSetNum.setSetNumber(cSet.getBytes(), cSet.getOffset());
                    Set cSet1 = m_cSetAccess.getSet(m_cTmpSetNum, RELA_RID_ATTR);

                    cSet.unfix();

                    m_cTmpTuple.setData(cSet1);

                    AttrInfo cAttrInfo = new AttrInfo();

                    cAttrInfo.m_iPos = Converter.bytesToInt(m_cTmpTuple.getBytes(),
                                m_cTmpTuple.getFieldOffset(ATTR_POS_ATTR_POS));

                    cAttrInfo.m_iSize = Converter.bytesToInt(m_cTmpTuple.getBytes(),
                                m_cTmpTuple.getFieldOffset(ATTR_POS_ATTR_SIZE));

                    cAttrInfo.m_iType = Converter.bytesToInt(m_cTmpTuple.getBytes(),
                                m_cTmpTuple.getFieldOffset(ATTR_POS_ATTR_DID));

                    cAttrInfo.m_szName = Converter.bytesToString(m_cTmpTuple.getBytes(),
                                m_cTmpTuple.getFieldOffset(ATTR_POS_ATTR_ANAME));

                    cList.insert(cAttrInfo);
                    iAttrCount++;
                }
                while (m_cTmpSetArray.moveToNext());

                TupleDescriptorImpl cRet = new TupleDescriptorImpl(iAttrCount, iRID, USER_SEGMENT);

                if (cList.moveToFirst()) {
                    do {
                        AttrInfo cAttrInfo = (AttrInfo) cList.getCurrent();

                        cRet.setAttrSize(cAttrInfo.m_iPos, cAttrInfo.m_iSize);
                        cRet.setType(cAttrInfo.m_iPos, cAttrInfo.m_iType);
                        cRet.setConverter(cAttrInfo.m_iPos, m_cTypeManager.getConverter(cAttrInfo.m_iType));
                        cRet.setName(cAttrInfo.m_iPos, cAttrInfo.m_szName);
                    }
                    while (cList.moveToNext());
                }

                return cRet;
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    public /*synchronized*/ TupleWriter createTuple(TupleDescriptor cTD) throws CodedException {

        Tuple cTuple = (Tuple) m_cTuplePool.getObject();
        SetNumber cSetNum = (SetNumber) m_cSetNumPool.getObject();

        cTuple.setTd(cTD);

        Set cSet = getSetAccessForSegment(cTD.getSegNum()).addSet(cSetNum,
                cTD.getTupleSize(), cTD.getRelId());

        cTuple.setData(cSet);
        cTuple.setSetNum(cSetNum);

        cTuple.setUseReference(m_cTuplesInUse.insertAtStart(cTuple));
        cTuple.setSysCatReference(this);

        return cTuple.getWriter();
    }

    ////////////////////////////////////////////////////////////////////////////
    public TupleWriter modifyTuple(SetNumberReader cSetNumReader, TupleDescriptor cTD) throws CodedException {

        SetNumber cSetNumber = (SetNumber) m_cSetNumPool.getObject();

        cSetNumber.copy(cSetNumReader);

        Tuple cTuple = (Tuple) m_cTuplePool.getObject();

        cTuple.setTd(cTD);

        Set cSet = getSetAccessForSegment(cTD.getSegNum()).getSet( cSetNumber, cTD.getRelId());

        cTuple.setData(cSet);
        cTuple.setSetNum(cSetNumber);

        cTuple.setUseReference(m_cTuplesInUse.insertAtStart(cTuple));
        cTuple.setSysCatReference(this);

        manageIndexes(cTuple, false);

        return cTuple.getWriter();
    }

    ////////////////////////////////////////////////////////////////////////////
    protected void endModifyTuple(Object cReference) throws CodedException {

        m_cTuplesInUse.setPos(cReference);
        Tuple cTuple = (Tuple) m_cTuplesInUse.getCurrent();

        m_cTuplesInUse.removeCurrent();

        manageIndexes(cTuple, true);

        m_cSetNumPool.addObject(cTuple.getSetNum());

        cTuple.setDirty();
        cTuple.unfix();

        m_cTuplePool.addObject(cTuple);
    }

    ////////////////////////////////////////////////////////////////////////////
    public TupleReader readTuple(TupleDescriptor cTD, SetNumberReader cSetNumReader) throws CodedException {

        SetNumber cSetNumber = (SetNumber) m_cSetNumPool.getObject();

        cSetNumber.copy(cSetNumReader);

        Tuple cTuple = (Tuple) m_cTuplePool.getObject();

        cTuple.setTd(cTD);

        Set cSet = getSetAccessForSegment(cTD.getSegNum()).getSet( cSetNumber, cTD.getRelId());

        cTuple.setData(cSet);
        cTuple.setSetNum(cSetNumber);

        cTuple.setUseReference(m_cTuplesInUse.insertAtStart(cTuple));
        cTuple.setSysCatReference(this);

        return cTuple.getReader();
    }

    ////////////////////////////////////////////////////////////////////////////
    protected Tuple readTuple(TupleDescriptor cTD, SetNumber cSetNum) throws CodedException {

        SetNumber cSetNumber = (SetNumber) m_cSetNumPool.getObject();

        cSetNumber.copy(cSetNum);

        Tuple cTuple = (Tuple) m_cTuplePool.getObject();

        cTuple.setTd(cTD);

        Set cSet = getSetAccessForSegment(cTD.getSegNum()).getSet( cSetNumber, cTD.getRelId());

        cTuple.setData(cSet);
        cTuple.setSetNum(cSetNumber);

        cTuple.setUseReference(m_cTuplesInUse.insertAtStart(cTuple));
        cTuple.setSysCatReference(this);

        return cTuple;
    }

    ////////////////////////////////////////////////////////////////////////////
    protected void endReadTuple(Object cReference) throws CodedException {
        m_cTuplesInUse.setPos(cReference);
        Tuple cTuple = (Tuple) m_cTuplesInUse.getCurrent();

        m_cTuplesInUse.removeCurrent();

        m_cSetNumPool.addObject(cTuple.getSetNum());

        cTuple.unfix();

        m_cTuplePool.addObject(cTuple);
    }

    ////////////////////////////////////////////////////////////////////////////
    public String describeTable(String szTableName) throws CodedException {
        DbIndex cIndex = getIndex(INDX_IID_ATTR_RID);
        int iRID = getRelId(szTableName);

        Converter.intToBytes(iRID, m_baTmpIntKey);
        cIndex.addAttributeToKey(m_baTmpIntKey, 0);
        if (!cIndex.find(m_baTmpSetNum)) {
            return "Table doesn't exist!";
        }

        m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
        m_cTmpSetArray.init(m_cSetAccess, m_cTmpSetNum, SetNumber.getByteLen(), RELA_RID_ATTR);

        String szRet = "\n|-size-------|-type---------|-name---------------|\n";

        if (!m_cTmpSetArray.moveToFirst())
            throw new CodedException(this, ERR_INCONSISTENT_INDEX, "Attribute RID index not consistent!");
        do {
            Set cSet = m_cTmpSetArray.getCurrentRecord();

            m_cTmpSetNum.setSetNumber(cSet.getBytes(), cSet.getOffset());
            Set cSet1 = m_cSetAccess.getSet(m_cTmpSetNum, RELA_RID_ATTR);

            cSet.unfix();

            m_cTmpTuple.setTd(m_cAttributeRelTD);
            m_cTmpTuple.setData(cSet1);

            String szName = Converter.bytesToString(m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(ATTR_POS_ATTR_ANAME));
            int iSize = Converter.bytesToInt(m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(ATTR_POS_ATTR_SIZE));
            int iDID = Converter.bytesToInt(m_cTmpTuple.getBytes(), m_cTmpTuple.getFieldOffset(ATTR_POS_ATTR_DID));

            m_cTmpTuple.unfix();

            szRet += "| " + iSize;
            szRet += "| " + iDID;
            szRet += "| " + szName;
            szRet += "|\n";

        }
        while (m_cTmpSetArray.moveToNext());

        return szRet;
    }

    ////////////////////////////////////////////////////////////////////////////
    /** creates a new internal table
     * @param szName name of the new table
     * @param aszNames array of attribute names
     * @param aiSizes array of attribute sizes
     * @param aiTypes array of attribute datatypes
     * @throws CodedException thrown on error ( table name exists, wrong array lengths, etc. )
     */
    public Table createTable(String szName, String[] aszNames, int[] aiSizes, int[] aiTypes) throws CodedException {

        TupleDescriptorImpl cTD = new TupleDescriptorImpl(aszNames.length, -1, USER_SEGMENT);

        for (int iCnter = 0; iCnter < aszNames.length; iCnter++) {
            cTD.setName(iCnter, aszNames[iCnter]);
            cTD.setAttrSize(iCnter, aiSizes[ iCnter ]);
            cTD.setType(iCnter, aiTypes[ iCnter ]);
        }

        int iAttrId = getRegKeyData(REG_ATTR_MAX_ID);

        if (getRelId(szName) != -1)
            throw new CodedException(this, ERR_RNAME_EXISTS, "Tablename already used!");

        int iMaxRelID = getRegKeyData(REG_RELA_MAX_ID);

        ((TupleDescriptorImpl) cTD).setRelId(iMaxRelID);

        setRegKeyData(REG_RELA_MAX_ID, iMaxRelID + 1);

        for (int iCnter = 0; iCnter < cTD.getCount(); iCnter++) {
            addToAttrRel(iAttrId, cTD.getName(iCnter), cTD.getRelId(), iCnter,
                cTD.getType(iCnter), cTD.getAttrSize(iCnter));
            iAttrId++;
        }

        setRegKeyData(REG_ATTR_MAX_ID, iAttrId);
        addToRelaRel(cTD.getRelId(), szName);

        return getTable( szName );
    }

    ////////////////////////////////////////////////////////////////////////////
    public void deleteTuple(SetNumberReader cSetNumReader, TupleDescriptor cTD) throws CodedException {
        m_cTmpTuple_user.setTd(cTD);

        SetNumber cSetNum = (SetNumber) m_cSetNumPool.getObject();

        cSetNum.copy(cSetNumReader);

        m_cTmpTuple_user.setSetNum(cSetNum);

        Set cSet = getSetAccessForSegment(cTD.getSegNum()).getSet(cSetNum, cTD.getRelId());

        m_cTmpTuple_user.setData(cSet);
        manageIndexes(m_cTmpTuple_user, false);
        m_cTmpTuple_user.unfix();

        getSetAccessForSegment(cTD.getSegNum()).removeSet(cSetNum, cTD.getRelId());

        m_cSetNumPool.addObject(cSetNum);
    }

    ////////////////////////////////////////////////////////////////////////////
    protected SetAccess getSetAccessForSegment(int iSegNum) throws CodedException {
        switch (iSegNum) {
        case SYSCAT_SEGMENT:
            return m_cSetAccess;

        case USER_SEGMENT:
            return m_cUserSetAccess;

        default: {
                throw new CodedException(this, ERR_SEGMENT_NOT_SUPPORTED, "Segment number not supported yet!");
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    private boolean checkIndexType(int iType) {
        if (iType == INDX_TYPE_SINGLE_ATTR ||
            iType == INDX_TYPE_SINGLE_DIM_MULTI_ATTR/*||    //these are not supported yet
         iType == INDX_TYPE_MULTI_DIM_MULTI_ATTR */)
            return true;

        return false;
    }

    ////////////////////////////////////////////////////////////////////////////
    public int createIndex(int iType, boolean bUnique, int[] iAttrMap, TupleDescriptor cTD) throws CodedException {

        if (!checkIndexType(iType))
            throw new CodedException(this, ERR_UNSUPPORTED_ITYPE, "Invalid index type!");

        int iMaxIID = getRegKeyData(REG_INDX_MAX_ID);
        int iCard = 0;

        if (!bUnique) iCard = 1;

        addToIndexRel(iMaxIID, iType, iCard);
        setRegKeyData(REG_INDX_MAX_ID, iMaxIID + 1);

        for (int iCnter = 0; iCnter < iAttrMap.length; iCnter++) {
            //the cryptic index is to ensure the proper ordering of the index positions
            addToImapRel(cTD.getRelId(), iMaxIID, iAttrMap[ iAttrMap.length - iCnter - 1 ]);
        }
        return iMaxIID;
    }

    ////////////////////////////////////////////////////////////////////////////
    /** deletes a table from the database and all its describing data
     * @param szRelName table name
     * @throws CodedException thrown on error ( wrong name, delete error, etc. )
     */
    public void dropTable(final String szRelName) throws CodedException {
        int iRID = getRelId(szRelName);

        TupleDescriptor cTD = getTupleDescriptor(iRID);

        //drop the data in the table first
        getSetAccessForSegment(cTD.getSegNum()).dropSetType(iRID);

        //remove RelaRel entires
        DbIndex cIndex = getIndex(INDX_IID_RELA_RID);

        Converter.intToBytes(iRID, m_baTmpIntKey);
        cIndex.addAttributeToKey(m_baTmpIntKey, 0);

        if (!cIndex.find(m_baTmpSetNum))
            throw new CodedException(this, ERR_INVALID_INDEX, "Inconsistent index INDX_IID_RELA_RID");

        m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
        deleteTuple(m_cTmpSetNum.getReader(), m_cRelationRelTD);

        //remove AttrRel entires
        cIndex = getIndex(INDX_IID_ATTR_RID);
        Converter.intToBytes(iRID, m_baTmpIntKey);
        cIndex.addAttributeToKey(m_baTmpIntKey, 0);

        if (!cIndex.find(m_baTmpSetNum))
            throw new CodedException(this, ERR_INVALID_INDEX, "Inconsistent index INDX_IID_ATTR_RID");

        m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
        m_cTmpSetArray.init(m_cSetAccess, m_cTmpSetNum, SetNumber.getByteLen(), RELA_RID_ATTR);

        if (!m_cTmpSetArray.moveToFirst())
            throw new CodedException(this, ERR_INVALID_INDEX, "Inconsistent index INDX_IID_ATTR_RID");
        else {
            List cList = new List();

            do {
                Set cSet = m_cTmpSetArray.getCurrentRecord();
                byte[] baSetNum = new byte[ SetNumber.getByteLen() ];

                System.arraycopy(cSet.getBytes(), cSet.getOffset(), baSetNum, 0, baSetNum.length);
                cSet.unfix();
                cList.insert(baSetNum);
            }
            while (m_cTmpSetArray.moveToNext());

            cList.moveToFirst();
            SetNumber cSetNum = new SetNumber();

            do {
                cSetNum.setSetNumber((byte[]) cList.getCurrent());
                deleteTuple(cSetNum.getReader(), m_cAttributeRelTD);
            }
            while (cList.moveToNext());
        }

        //remove ImapRel entires
        cIndex = getIndex(INDX_IID_IMAP_RID);
        cIndex.addAttributeToKey(m_baTmpIntKey, 0);

        if (!cIndex.find(m_baTmpSetNum))
            return; //no indexes defined, so nothing more to do

        m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
        m_cTmpSetArray.init(m_cSetAccess, m_cTmpSetNum, m_cIndexmapRelTD.getTupleSize(), RELA_RID_IMAP);

        if (!m_cTmpSetArray.moveToFirst())
            throw new CodedException(this, ERR_INVALID_INDEX, "Inconsistent index INDX_IID_ATTR_RID");
        else {
            List cList = new List();

            do {
                Set cSet = m_cTmpSetArray.getCurrentRecord();
                byte[] baSetNum = new byte[ SetNumber.getByteLen() ];

                System.arraycopy(cSet.getBytes(), cSet.getOffset(), baSetNum, 0, baSetNum.length);
                cSet.unfix();
                cList.insert(baSetNum);
            }
            while (m_cTmpSetArray.moveToNext());

            cList.moveToFirst();
            do {
                m_cTmpSetNum.setSetNumber((byte[]) cList.getCurrent());
                deleteTuple(m_cTmpSetNum.getReader(), m_cAttributeRelTD);
            }
            while (cList.moveToNext());
        }

        //remove the IndexRel Entries
        List cIndexInfos = getIndexInfos( iRID );

        if (cIndexInfos.moveToFirst()) {
            cIndex = getIndex(INDX_IID_INDX_IID);
            do {
                Converter.intToBytes(((IndexInfo) cIndexInfos.getCurrent()).getIID(),
                    m_baTmpIntKey);
                cIndex.clearKey();
                cIndex.addAttributeToKey(m_baTmpIntKey, 0);
                if (cIndex.find(m_baTmpSetNum)) {
                    m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
                    deleteTuple(m_cTmpSetNum.getReader(), m_cIndexRelTD);
                }
            }
            while (cIndexInfos.moveToNext());
        }

        //clear the cached data
        m_cIndexCache.remove( iRID );
        m_cIndexInfoCache.remove( iRID );
    }

    ////////////////////////////////////////////////////////////////////////////
    public int getRidForIid(int iIID) throws CodedException {

        //search in the imap relation
        DbIndex cIndex = getIndex(INDX_IID_IMAP_IID);

        Converter.intToBytes(iIID, m_baTmpIntKey);
        cIndex.addAttributeToKey(m_baTmpIntKey, 0);

        //get the SetArray start number for the iIID
        if (!cIndex.find(m_baTmpSetNum))
            throw new CodedException(this, ERR_INCONSISTENT_INDEX, "Index INDX_IID_IMAP_IID is not consistent");

        m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
        m_cTmpSetArray.init(getSetAccessForSegment(m_cIndexmapRelTD.getSegNum()),
            m_cTmpSetNum, SetNumber.getByteLen(), RELA_RID_IMAP);

        //take just the first tuple in the array
        Set cSet = m_cTmpSetArray.getCurrentRecord();

        m_cTmpSetNum.setSetNumber(cSet.getBytes(), cSet.getOffset());

        TupleReader cTupleReader = readTuple(m_cIndexmapRelTD, m_cTmpSetNum.getReader());

        cSet.unfix();

        //get the rid
        int iRet = cTupleReader.getField(ATTR_POS_IMAP_RID);

        cTupleReader.close();

        return iRet;
    }

    ////////////////////////////////////////////////////////////////////////////
    public IndexInfo getIndexInfo(int iIID) throws CodedException {
        List cIndexInfos = getIndexInfos(getRidForIid(iIID));

        if (cIndexInfos.moveToFirst()) {
            do {
                IndexInfo cIndexInfo = (IndexInfo) cIndexInfos.getCurrent();

                if (cIndexInfo.getIID() == iIID)
                    return cIndexInfo;
            }
            while (cIndexInfos.moveToNext());
        }
        throw new CodedException(this, ERR_INVALID_INDEX, "Invalid IID");
    }

    ////////////////////////////////////////////////////////////////////////////
    public void dropIndex(int iIID) throws CodedException {
        //remove all keys first

        DbIndex cIndex       = getIndex(iIID);
        IndexInfo cIndexInfo = getIndexInfo(iIID);

        if (cIndexInfo.isUnique()) {
            cIndex.removeAll();
        }else {
            ((DbIndexSingleDim) cIndex).getFirstKey();
            Iterator cIterator = ((DbIndexSingleDim) cIndex).getIterator();

            if (cIterator.moveToFirst()) {
                do {
                    cIterator.getCurrent(null, m_cTmpSet);
                    m_cTmpSetNum.setSetNumber(m_cTmpSet.getBytes(), m_cTmpSet.getOffset());
                    m_cTmpSetArray.deleteArray(m_cTmpSetNum);
                }
                while (cIterator.moveToNext());
            }
            cIndex.removeAll();
        }

        //now delete all info in the syscat about the index
        //index map iid index first
        cIndex = getIndex(INDX_IID_IMAP_IID);
        Converter.intToBytes(iIID, m_baTmpIntKey);
        cIndex.addAttributeToKey(m_baTmpIntKey, 0);

        if (!cIndex.find(m_baTmpSetNum))
            throw new CodedException(this, ERR_INCONSISTENT_INDEX, "INDX_IID_IMAP_IID not consistent!");
        else {
            m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
            m_cTmpSetArray.init(m_cSetAccess, m_cTmpSetNum, SetNumber.getByteLen(), RELA_RID_IMAP);
            if (!m_cTmpSetArray.moveToFirst())
                throw new CodedException(this, ERR_INCONSISTENT_INDEX, "INDX_IID_IMAP_IID not consistent!");
            else {
                do {
                    //remove all tuples here
                    Set cSet = m_cTmpSetArray.getCurrentRecord();

                    m_cTmpSetNum.setSetNumber(cSet.getBytes(), cSet.getOffset());
                    m_cSetAccess.removeSet(m_cTmpSetNum, RELA_RID_IMAP);
                    cSet.unfix();

                }
                while (m_cTmpSetArray.moveToNext());
            }

            m_cTmpSetArray.deleteArray();
            cIndex.remove();
        }

        //index map rid index next, no need to remove tuples!
        cIndex = getIndex(INDX_IID_IMAP_RID);
        Converter.intToBytes(getRidForIid(iIID), m_baTmpIntKey);
        cIndex.addAttributeToKey(m_baTmpIntKey, 0);

        if (!cIndex.find(m_baTmpSetNum))
            throw new CodedException(this, ERR_INCONSISTENT_INDEX, "INDX_IID_IMAP_RID not consistent!");
        else {
            m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
            m_cTmpSetArray.deleteArray(m_cTmpSetNum);
            cIndex.remove();
        }

        //now the index rel
        cIndex = getIndex(INDX_IID_INDX_IID);
        Converter.intToBytes(iIID, m_baTmpIntKey);

        cIndex.addAttributeToKey(m_baTmpIntKey, 0);

        if (!cIndex.find(m_baTmpSetNum))
            throw new CodedException(this, ERR_INCONSISTENT_INDEX, "INDX_IID_INDX_IID not consistent!");

        m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
        m_cSetAccess.removeSet(m_cTmpSetNum, RELA_RID_INDX);
        cIndex.remove();

        //clear the cached data
        int iRID = getRidForIid( iIID );
        m_cIndexCache.remove( iRID );
        m_cIndexInfoCache.remove( iRID );

    }



    ////////////////////////////////////////////////////////////////////////////
    //Registry routines

    public int getRegKeyData(int iKey) throws CodedException {
        Converter.intToBytes(iKey, m_baTmpIntKey);
        if (!m_cRegistry.find(m_baTmpIntKey, m_baTmpIntData))
            throw new CodedException(this, ERR_REG_KEY_NOT_FOUND, "Registry key not found!");

        return Converter.bytesToInt(m_baTmpIntData);
    }

    public boolean regKeyExist(int iKey)  throws CodedException {
        Converter.intToBytes(iKey, m_baTmpIntKey);
        if (!m_cRegistry.find(m_baTmpIntKey, m_baTmpIntData))
            return false;

        return true;
    }

    public void setRegKeyData(int iKey, int iData) throws CodedException {
        Converter.intToBytes(iKey, m_baTmpIntKey);
        Converter.intToBytes(iData, m_baTmpIntData);
        m_cRegistry.insert(m_baTmpIntKey, m_baTmpIntData);
    }

    ////////////////////////////////////////////////////////////////////////////

    /***************************************************************************************/

    /** returns the id of the table for a given table name
     * @param szRelName name of the table
     * @throws CodedException thrown on error ( wrong name, etc. )
     * @return the table
     */
    public Table getTable(String szRelName) throws CodedException {
        return new TableImpl(this, szRelName);
    }

    /***************************************************************************************/

    public TypeManager getTypeManager() {
        return m_cTypeManager;
    }

    /***************************************************************************************/

    /**
     * @param table table to scan
     * @param index index of table
     */
    public RelationScan getRelationScan(Table table, Index index, Key start, Key end) throws CodedException {
      return new RelationScanImpl( this, table, index, start, end );
    }

    ////////////////////////////////////////////////////////////////////////////
    public BufferList getTmpStorage( int iDataSize ) throws CodedException{
      return new BufferListImpl( m_cPageBuf, iDataSize );
    }
    ////////////////////////////////////////////////////////////////////////////


    public void flush() throws CodedException{
	m_cPageBuf.flush();
    }


    public static void main(String [] args) throws Exception {
	Naming naming = InitialNaming.getInitialNaming();
	
            Debug.out.println("Start testing..................!");

            CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	    
            String bioName = args[0];
            String dbName = args[1];

	    Debug.out.println("DB: Waiting for blockdevice "+bioName+" to become available ... ");

	    BlockIO cBio = (BlockIO)LookupHelper.waitUntilPortalAvailable(naming, bioName);


	    Debug.out.println("DB: Found blockdevice with capacity " + cBio.getCapacity());

            SortingBuffer cSb      = new SortingBufferImpl( cBio);
            FirstStageBuf cPageBuf = new FirstStageBuf( cSb, 1024, cBio.getCapacity() / 4 );

            //MemoryBuffer cPageBuf = new MemoryBuffer();

            cPageBuf.getPageManager().formatMedia();



            //only if we use MemoryPageBuffer
            //cPageBuf.getPageManager().getBlankPage();


            //create the system catalog
            Database db = new SystemCatalogImpl(cPageBuf);
	    naming.registerPortal(db, dbName);
    } 

}
