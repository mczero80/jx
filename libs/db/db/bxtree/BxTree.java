package db.bxtree;



import db.com.Iterator;
import jx.db.types.DbComparator;
import db.com.Converter;
import db.com.ByteArrayPool;
import db.com.ObjectPool;
import db.com.ObjHolderFactory;
import db.com.ObjHolder;
import db.com.Globals;

import db.tid.Set;

import db.systembuffer.PageBuffer;
import db.systembuffer.PageManager;

import jx.db.CodedException;

/** B+ Tree
 */
public class BxTree {

    public static final int ERR_NOT_LEAF = 0;

    private int m_iKeySize = 4; //integer
    private int m_iDataSize = 4; //irgendwas kleines
    private int m_iRootPage = -1; //am anfang nicht besetzt
    private int m_iPageSize;
    private int m_iMaxNodeKeys;
    private int m_iMaxLeafKeys;
    private PageBuffer	m_cPageBuf;
    private PageManager m_cPageMgr;
    private DbComparator  m_cComparator;
    private ByteArrayPool m_cDataPool;
    private ByteArrayPool m_cKeyPool;
    private ByteArrayPool m_cPageNumPool;
    private ObjectPool    m_cObjHolderPool;

    private final int PAGE_HEADER_SIZE = 2;

    private int m_iCurrentPage = -1;

    private int m_iCurrentOffset = -1;

    //1 bit for the leaf flag & 15 bits for the used bytes count

    /////////////////////////////////////////////////////////////////////////////////////////
    //Public functions

    /** Constructor
     * @param iKeySize maximal key size in bytes
     * @param iDataSize malimal data size in bytes
     * @param cPageBuf object implementing {@link PageBuffer}, used to store BxTree's data
     * @param cPageMgr object implementing {@link PageManager}, used to allocate free pages when the {@link BxTree} grows
     * @param iRootPage id of the root page of the tree (adequate to the used {@link PageBuffer})
     * @param cComparator object implementing the {@link DbComparator} interface, used to compare keys in the BxTree
     */
    public BxTree(int iKeySize, int iDataSize, PageBuffer cPageBuf, int iRootPage, DbComparator cComparator, boolean bFormatRoot) throws CodedException {
        m_iKeySize = iKeySize;
        m_iDataSize = iDataSize;
        m_iRootPage = iRootPage;
        m_cPageBuf = cPageBuf;
        m_cPageMgr = cPageBuf.getPageManager();

        if (iDataSize != iKeySize) {
            m_cDataPool = new ByteArrayPool(Globals.BXTREE_POOL_SIZE, iDataSize);
            m_cKeyPool = new ByteArrayPool(Globals.BXTREE_POOL_SIZE, iKeySize);
        }else {
            m_cDataPool = m_cKeyPool = new ByteArrayPool(Globals.BXTREE_POOL_SIZE * 2, iKeySize);
        }

        m_cPageNumPool   = new ByteArrayPool(Globals.BXTREE_POOL_SIZE * 2, 4);
        m_cObjHolderPool = new ObjectPool(Globals.BXTREE_POOL_SIZE, new ObjHolderFactory());

        //read the page size in bytes of the given buffer
        m_iPageSize = cPageBuf.getPageSize();

        m_cComparator = cComparator;

        //do some global calculations, these values will be used often later
        m_iMaxNodeKeys = (m_iPageSize - PAGE_HEADER_SIZE - 4) / (m_iKeySize + 4);
        m_iMaxLeafKeys = (m_iPageSize - PAGE_HEADER_SIZE - 8) / (m_iKeySize + m_iDataSize);

        if (m_iRootPage == -1) {

            byte[] baRoot = null;

            m_iRootPage = m_cPageMgr.getBlankPage();
            baRoot = m_cPageBuf.fixSync(m_iRootPage);

            //the first btree page is always leaf
            setLeaf(baRoot, true);

            //allocate an integer
            byte[] baTmp = m_cPageNumPool.getArray();

            Converter.intToBytes(-1, baTmp);
            writePageNum(baTmp, baRoot, 0);
            writePageNum(baTmp, baRoot, 2);
            m_cPageNumPool.addArray(baTmp);

            //and is empty
            setUsedCount(baRoot, 0);

            m_cPageBuf.setDirty(m_iRootPage);
            m_cPageBuf.unfix(m_iRootPage);

        }else if (bFormatRoot) {
            formatBlankPage(iRootPage, m_cPageBuf);
        }
    }

    /**
     * @return  */
    public int getKeySize() {
        return m_iKeySize;
    }

    /**
     * @return  */
    public int getDataSize() {
        return m_iDataSize;
    }

    ////////////////////////////////////////////////////////////////////////////                               ************
    /** inserts a new key - data entry in the {@link BxTree}.If the key already exists,
     * the old data will be overwritten by the new data.The two byte arrays must have
     * the exact length, specified in the BxTree constructor for the key and data size.
     * @param baKey byte array containing the key to be inserted
     * @param baData byte array containing the data to be inserted
     * @throws CodedException thrown on error
     */
    public void insert(byte[] baKey, byte[] baData) throws CodedException {

        byte[] baRoot = null;

        baRoot = m_cPageBuf.fixSync(m_iRootPage);

        if (isLeaf(baRoot)) {
            if (getUsedCount(baRoot) < m_iMaxLeafKeys || pageContains(baKey, baRoot, null))
                insertInLeaf(baKey, baData, baRoot);
            else {
                int iNewRootNumber = -1;
                int iNewLeafNumber = -1;

                byte[] baNewRoot = null;
                byte[] baNewLeaf = null;

                iNewRootNumber = m_cPageMgr.getBlankPage();
                iNewLeafNumber = m_cPageMgr.getBlankPage();
                baNewRoot = m_cPageBuf.fixSync(iNewRootNumber);
                baNewLeaf = m_cPageBuf.fixSync(iNewLeafNumber);

                System.arraycopy(baRoot, 0, baNewRoot, 0, baRoot.length);

                setLeaf(baRoot, false);
                setUsedCount(baRoot, 0);

                setUsedCount(baNewLeaf, 0);
                setLeaf(baNewLeaf, true);

                byte[] baRetKey = m_cKeyPool.getArray();

                splitLeaf(baNewRoot, iNewRootNumber, baNewLeaf, iNewLeafNumber, baKey, baData, baRetKey);
                byte[] baNewRootNumber = m_cPageNumPool.getArray();
                byte[] baNewLeafNumber = m_cPageNumPool.getArray();

                Converter.intToBytes(iNewRootNumber, baNewRootNumber);
                Converter.intToBytes(iNewLeafNumber, baNewLeafNumber);
                insertInNode(baRetKey, baNewRootNumber, baNewLeafNumber, baRoot);

                m_cPageNumPool.addArray(baNewRootNumber);
                m_cPageNumPool.addArray(baNewLeafNumber);
                m_cKeyPool.addArray(baRetKey);

                m_cPageBuf.setDirty(iNewLeafNumber);
                m_cPageBuf.unfix(iNewLeafNumber);
                m_cPageBuf.setDirty(iNewRootNumber);
                m_cPageBuf.unfix(iNewRootNumber);
            }
        }else {
            byte[] baSubTree = getSubTree(baKey, baRoot, null); //get the number of the page containing the subtree, not the whole subtree!
            byte[] baSplitKey = m_cKeyPool.getArray();
            byte[] baSplitPageNum = m_cPageNumPool.getArray();

            if (insertInSubTree(baKey, baData, baSplitKey, baSubTree, baSplitPageNum)) { //Split caused!
                if (getUsedCount(baRoot) < m_iMaxNodeKeys) {
                    insertInNode(baSplitKey, baSubTree, baSplitPageNum, baRoot);
                }else {
                    int iNewRootNumber = -1;
                    int iNewNode = -1;
                    byte[] baNewRoot = null;
                    byte[] baNewNode = null;

                    try {
                        iNewRootNumber = m_cPageMgr.getBlankPage();
                        iNewNode = m_cPageMgr.getBlankPage();
                        baNewRoot = m_cPageBuf.fixSync(iNewRootNumber);
                        baNewNode = m_cPageBuf.fixSync(iNewNode);
                    }catch (CodedException cex) {
                        if (cex.getErrorCode() == PageManager.ERR_NO_FREE_PAGE)
                            undoInsert(baKey, baSplitKey, baSubTree, baSplitPageNum);
                        throw cex;
                    }

                    //copy the current root page into the new allocated page
                    System.arraycopy(baRoot, 0, baNewRoot, 0, baRoot.length);

                    setUsedCount(baRoot, 0);

                    byte[] baRetKey = m_cKeyPool.getArray();

                    splitNode(baNewRoot, baNewNode, baSplitKey, baSubTree, baSplitPageNum, baRetKey);

                    byte[] baNewRootNumber = m_cPageNumPool.getArray();
                    byte[] baNewNodeNumber = m_cPageNumPool.getArray();

                    Converter.intToBytes(iNewRootNumber, baNewRootNumber);
                    Converter.intToBytes(iNewNode, baNewNodeNumber);
                    insertInNode(baRetKey, baNewRootNumber, baNewNodeNumber, baRoot);

                    m_cPageNumPool.addArray(baNewRootNumber);
                    m_cPageNumPool.addArray(baNewNodeNumber);
                    m_cKeyPool.addArray(baRetKey);

                    m_cPageBuf.setDirty(iNewNode);
                    m_cPageBuf.unfix(iNewNode);
                    m_cPageBuf.setDirty(iNewRootNumber);
                    m_cPageBuf.unfix(iNewRootNumber);
                }
            }

            m_cKeyPool.addArray(baSplitKey);
            m_cPageNumPool.addArray(baSplitPageNum);
            m_cPageNumPool.addArray(baSubTree);

        }
        m_cPageBuf.setDirty(m_iRootPage);
        m_cPageBuf.unfix(m_iRootPage);
    }

    ////////////////////////////////////////////////////////////////////////////                              *************
    /** This function searches for an element in the B* Tree
     * @return the function returns true, if the key is found, false otherwise
     * @param baKey contains the key to search for
     * @param baData a buffer to store the found data
     * @throws CodedException  */
    public boolean find(byte[] baKey, byte[] baData) throws CodedException {

        int iPageNum = m_iRootPage;
        int iSavedPageNum = -1;
        boolean bRet = false;

        do {
            byte[] baPage = null;

            baPage = m_cPageBuf.fixSync(iPageNum);

            if (isLeaf(baPage)) {
                ObjHolder o = (ObjHolder) m_cObjHolderPool.getObject();

                if (pageContains(baKey, baPage, o)) {
                    if (baData != null)
                        readData(baData, baPage, ((Integer) o.getObj()).intValue());
                    bRet = true;
                }else {
                    bRet = false;
                }

                //store the position of the record for iterating
                m_iCurrentPage = iPageNum;
                m_iCurrentOffset = ((Integer) o.getObj()).intValue();

                if (m_iCurrentOffset >= getUsedCount(baPage))
                    m_iCurrentOffset--;

                m_cPageBuf.unfix(iPageNum);
                m_cObjHolderPool.addObject(o);

                return bRet;
            }else {
                iSavedPageNum = iPageNum;
                byte[] baSubTree = getSubTree(baKey, baPage, null);

                iPageNum = Converter.bytesToInt(baSubTree);
                m_cPageNumPool.addArray(baSubTree);
            }
            m_cPageBuf.unfix(iSavedPageNum);
        }
        while (true);
    }

    ////////////////////////////////////////////////////////////////////////////                              *************
    /** This function removes a key from the B* Tree
     * @param baKey key to be removed
     * @throws CodedException  */
    public void remove(byte[] baKey) throws CodedException {
        byte[] baRoot = null;

        baRoot = m_cPageBuf.fixSync(m_iRootPage);

        boolean bRet = removeFromUndertree(baKey, baRoot);

        if (!isLeaf(baRoot)) {
            if (getUsedCount(baRoot) == 0 && bRet == true) { //underflow caused && root empty
                int iPageNum = readPageNum(baRoot, 0);

                byte[] baNewRoot = null;

                baNewRoot = m_cPageBuf.fixSync(iPageNum);

                //copy the new root page into the old root, to avoid changes of the m_iRootPage number
                System.arraycopy(baNewRoot, 0, baRoot, 0, baRoot.length);

                m_cPageBuf.unfix(iPageNum);

                //free the unused page
                m_cPageMgr.freePage(iPageNum);
            }
        }
        m_cPageBuf.setDirty(m_iRootPage);
        m_cPageBuf.unfix(m_iRootPage);
    }

    //*********************************************************************************************************************
    //Private helper functions

    //*********************************************************************************************************************
    private boolean insertInSubTree(byte[] baKey, byte[] baData, byte[] baSplitKey, byte[] baSubTreeNum, byte[] baSplitPageNum) throws CodedException {
        byte[] baSubTree = null;
        int iSubTreeNum = Converter.bytesToInt(baSubTreeNum);

        baSubTree = m_cPageBuf.fixSync(iSubTreeNum);

        if (isLeaf(baSubTree)) {
            if (getUsedCount(baSubTree) < m_iMaxLeafKeys || pageContains(baKey, baSubTree, null)) {
                insertInLeaf(baKey, baData, baSubTree);

                //unfix the fixed page
                m_cPageBuf.setDirty(iSubTreeNum);
                m_cPageBuf.unfix(iSubTreeNum);

                return false; //no split needed!
            }else {
                int iNewPageNumber = -1;

                iNewPageNumber = m_cPageMgr.getBlankPage();

                byte[] baNewPage = null;

                baNewPage = m_cPageBuf.fixSync(iNewPageNumber);

                Converter.intToBytes(iNewPageNumber, baSplitPageNum);

                setLeaf(baNewPage, true);
                setUsedCount(baNewPage, 0);

                splitLeaf(baSubTree, Converter.bytesToInt(baSubTreeNum), baNewPage, iNewPageNumber, baKey, baData, baSplitKey);

                m_cPageBuf.setDirty(iNewPageNumber);
                m_cPageBuf.unfix(iNewPageNumber);
                m_cPageBuf.setDirty(iSubTreeNum);
                m_cPageBuf.unfix(iSubTreeNum);

                return true; //split indicated!
            }
        }else { //not a leaf
            ObjHolder o = (ObjHolder) m_cObjHolderPool.getObject();
            byte[] baSubSubTree = getSubTree(baKey, baSubTree, o); //get the number of the page containing the subtree, not the whole subtree!

            m_cObjHolderPool.addObject(o);

            //some balancing code here!
            /*if( true ){
             byte[] baRightNeighbourPageNumber = new byte[ 4 ];
             byte[] baLeftNeighbourPageNumber  = new byte[ 4 ];
             byte[] baRightNeighbourPage = null;
             byte[] baLeftNeighbourPage  = null;

             boolean bLeftFixed = false;
             boolean bRightFixed = false;

             int iMinKeyCount = m_iMaxNodeKeys + 1;
             boolean bLeftChosen = true;

             if( ((Integer)o.getObj()).intValue() < getUsedCount(baSubTree)){
             readPageNum( baRightNeighbourPageNumber, baSubTree, ((Integer)o.getObj()).intValue() + 1 );
             e.resetEvent();
             try{ baRightNeighbourPage = m_cPageBuf.fix(Converter.bytesToInt(baRightNeighbourPageNumber), e );
             }catch( PageException ex ){ ex.printStackTrace(); }
             *
             *      if( e.getNeedToWait())
             e.waitForEvent();
             if( iMinKeyCount > getUsedCount( baRightNeighbourPage )){
             iMinKeyCount = getUsedCount( baRightNeighbourPage );
             bLeftChosen = false;
             }

             bRightFixed = true;
             }

             if( ((Integer)o.getObj()).intValue() > 0){
             readPageNum( baLeftNeighbourPageNumber, baSubTree, ((Integer)o.getObj()).intValue() - 1 );
             e.resetEvent();
             try{ baLeftNeighbourPage = m_cPageBuf.fix(Converter.bytesToInt(baLeftNeighbourPageNumber), e );
             }catch( PageException ex ){ ex.printStackTrace(); }
             *  if( e.getNeedToWait())
             e.waitForEvent();
             if( iMinKeyCount > getUsedCount( baLeftNeighbourPage )){
             iMinKeyCount = getUsedCount( baLeftNeighbourPage );
             bLeftChosen = true;
             }

             bLeftFixed = true;
             }

             //error mistake here! if( getUsedCount( baSubTree ) - iMinKeyCount > ( m_iMaxNodeKeys / 4 )){

             byte[] baMergeKey = new byte[m_iKeySize];

             byte[] baLeftMerge  = null;
             byte[] baRightMerge = null;
             int iReplaceKey = -1;

             if( bLeftChosen ){
             baLeftMerge = baLeftNeighbourPage;
             e.resetEvent();
             try{ baRightMerge = m_cPageBuf.fix(Converter.bytesToInt(baSubSubTree), e );
             }catch( PageException ex ){ ex.printStackTrace(); }
             *if( e.getNeedToWait())
             e.waitForEvent();

             iReplaceKey = ((Integer)o.getObj()).intValue() - 1;

             }else{
             baRightMerge = baLeftNeighbourPage;
             e.resetEvent();
             try{ baLeftMerge = m_cPageBuf.fix(Converter.bytesToInt(baSubSubTree), e );
             }catch( PageException ex ){ ex.printStackTrace(); }
             *if( e.getNeedToWait())
             e.waitForEvent();

             iReplaceKey = ((Integer)o.getObj()).intValue();
             }

             readKey( baMergeKey, baSubTree, iReplaceKey );

             //balance
             //mergePages( baLeftMerge, baRightMerge, baMergeKey );
             //writeKey( baMergeKey, baSubTree, iReplaceKey );

             try{
             if(bLeftChosen)
             m_cPageBuf.setDirty(Converter.bytesToInt(baLeftNeighbourPageNumber));
             else
             m_cPageBuf.setDirty(Converter.bytesToInt(baRightNeighbourPageNumber));

             m_cPageBuf.setDirty(Converter.bytesToInt(baSubSubTree));
             m_cPageBuf.unfix(Converter.bytesToInt(baSubSubTree));
             }catch(Exception ex){ ex.printStackTrace();}
             }

             try{
             if( bLeftFixed )
             m_cPageBuf.unfix(Converter.bytesToInt(baLeftNeighbourPageNumber));

             if( bRightFixed )
             m_cPageBuf.unfix(Converter.bytesToInt(baRightNeighbourPageNumber));

             }catch(Exception ex){ ex.printStackTrace();}
             }*/

            if (insertInSubTree(baKey, baData, baSplitKey, baSubSubTree, baSplitPageNum)) { //Split caused!
                if (getUsedCount(baSubTree) < m_iMaxNodeKeys) {
                    insertInNode(baSplitKey, baSubSubTree, baSplitPageNum, baSubTree);

                    m_cPageNumPool.addArray(baSubSubTree);

                    //unfix the fixed subtree page
                    m_cPageBuf.setDirty(iSubTreeNum);
                    m_cPageBuf.unfix(iSubTreeNum);

                    return false;
                }else {
                    int iNewPageNumber = -1;

                    try {
                        iNewPageNumber = m_cPageMgr.getBlankPage();
                    }catch (CodedException cex) {
                        if (cex.getErrorCode() == PageManager.ERR_NO_FREE_PAGE)
                            undoInsert(baKey, baSplitKey, baSubSubTree, baSplitPageNum);

                        throw cex;
                    }

                    byte[] baNewPage = null;

                    baNewPage = m_cPageBuf.fixSync(iNewPageNumber);

                    setLeaf(baNewPage, false);
                    setUsedCount(baNewPage, 0);

                    byte[] baRetKey = m_cKeyPool.getArray();

                    splitNode(baSubTree, baNewPage, baSplitKey, baSubSubTree, baSplitPageNum, baRetKey);

                    Converter.intToBytes(iNewPageNumber, baSplitPageNum);
                    Converter.moveBytes(baSplitKey, 0, baRetKey, 0, m_iKeySize);

                    m_cKeyPool.addArray(baRetKey);
                    m_cPageNumPool.addArray(baSubSubTree);

                    m_cPageBuf.setDirty(iNewPageNumber);
                    m_cPageBuf.unfix(iNewPageNumber);
                    m_cPageBuf.setDirty(iSubTreeNum);
                    m_cPageBuf.unfix(iSubTreeNum);

                    return true; //split indicated!
                }
            }

            //always unfix!
            m_cPageBuf.setDirty(iSubTreeNum);
            m_cPageBuf.unfix(iSubTreeNum);

            return false; //no split
        }
    }

    //*********************************************************************************************************************
    private boolean removeFromUndertree(byte[] baKey, byte[] baPage) throws CodedException {

        if (isLeaf(baPage)) {
            removeFromLeaf(baKey, baPage);
            if (getUsedCount(baPage) < m_iMaxLeafKeys / 2)
                return true; //indicate underflow
            else
                return false; //no underflow
        }else {
            ObjHolder o = (ObjHolder) m_cObjHolderPool.getObject();
            byte[] baSubTreeNum = getSubTree(baKey, baPage, o);
            int iSubTreeNumber = Converter.bytesToInt(baSubTreeNum);

            m_cPageNumPool.addArray(baSubTreeNum);

            byte[] baSubTree = null;

            baSubTree = m_cPageBuf.fixSync(iSubTreeNumber);

            if (!removeFromUndertree(baKey, baSubTree)) {//no underflow
                m_cPageBuf.setDirty(iSubTreeNumber);
                m_cPageBuf.unfix(iSubTreeNumber);
                m_cObjHolderPool.addObject(o);
                return false;
            }else { //underflow
                int iLeftMergeNumber;
                int iRightMergeNumber;
                byte[] baLeftMerge = null;
                byte[] baRightMerge = null;
                int iReplaceKey = -1;

                if (((Integer) o.getObj()).intValue() == getUsedCount(baPage)) { //the last element in the node
                    iLeftMergeNumber = readPageNum(baPage, getUsedCount(baPage) - 1);
                    iRightMergeNumber = readPageNum(baPage, getUsedCount(baPage));

                    baRightMerge = baSubTree;
                    baLeftMerge = m_cPageBuf.fixSync(iLeftMergeNumber);

                    iReplaceKey = getUsedCount(baPage) - 1;
                }else {
                    iLeftMergeNumber = readPageNum(baPage, ((Integer) o.getObj()).intValue());
                    iRightMergeNumber = readPageNum(baPage, ((Integer) o.getObj()).intValue() + 1);
                    baLeftMerge = baSubTree;
                    baRightMerge = m_cPageBuf.fixSync(iRightMergeNumber);
                    iReplaceKey = ((Integer) o.getObj()).intValue();
                }

                boolean bRet;

                byte[] baMergeKey = m_cKeyPool.getArray();

                readKey(baMergeKey, baPage, iReplaceKey);

                if (mergePages(baLeftMerge, baRightMerge, baMergeKey)) { //real merge
                    //release the empty page
                    m_cPageMgr.freePage(iRightMergeNumber);

                    replaceInNode(iLeftMergeNumber, baPage, iReplaceKey);
                    if (getUsedCount(baPage) < m_iMaxNodeKeys / 2)
                        bRet = true;  //underflow
                    else
                        bRet = false; //no underflow
                }else { //only balancing
                    writeKey(baMergeKey, baPage, iReplaceKey);
                    bRet = false; //no underflow
                }

                m_cKeyPool.addArray(baMergeKey);

                //free the fixed pages
                m_cPageBuf.setDirty(iRightMergeNumber);
                m_cPageBuf.unfix(iRightMergeNumber);
                m_cPageBuf.setDirty(iLeftMergeNumber);
                m_cPageBuf.unfix(iLeftMergeNumber);

                m_cObjHolderPool.addObject(o);
                return bRet;
            }
        }
    }

    //*********************************************************************************************************************
    private boolean mergePages(byte[] baLeftMerge, byte[] baRightMerge, byte[] baMergeKey) throws CodedException {
        int iLeftUsedCount = getUsedCount(baLeftMerge);
        int iRightUsedCount = getUsedCount(baRightMerge);
        int iKeySum = iLeftUsedCount + iRightUsedCount;

        if (isLeaf(baLeftMerge)) { //leaf merge
            byte[] baReadKey = m_cKeyPool.getArray();
            byte[] baReadData = m_cDataPool.getArray();
            byte[] baPageNum = m_cPageNumPool.getArray();

            if (iKeySum < m_iMaxLeafKeys) { //merge
                setUsedCount(baLeftMerge, iKeySum);
                for (int cnter = 0; cnter < iRightUsedCount; cnter++) {
                    readKey(baReadKey, baRightMerge, cnter);
                    readData(baReadData, baRightMerge, cnter);
                    writeKey(baReadKey, baLeftMerge, cnter + iLeftUsedCount);
                    writeData(baReadData, baLeftMerge, cnter + iLeftUsedCount);
                }
                readPageNum(baPageNum, baRightMerge, 1);
                writePageNum(baPageNum, baLeftMerge, 1);

                m_cKeyPool.addArray(baReadKey);
                m_cDataPool.addArray(baReadData);
                m_cPageNumPool.addArray(baPageNum);

                return true; //merge indicated

            }else { //balancing
                if (iLeftUsedCount < iRightUsedCount) { //balance to left
                    setUsedCount(baLeftMerge, iKeySum / 2);
                    int iNewRightUsedCount = iKeySum - iKeySum / 2;

                    for (int cnter = 0; cnter < iRightUsedCount - iNewRightUsedCount; cnter++) {
                        readKey(baReadKey, baRightMerge, cnter);
                        readData(baReadData, baRightMerge, cnter);
                        writeKey(baReadKey, baLeftMerge, cnter + iLeftUsedCount);
                        writeData(baReadData, baLeftMerge, cnter + iLeftUsedCount);
                    }

                    for (int cnter = iRightUsedCount - iNewRightUsedCount; cnter < iRightUsedCount; cnter++) {
                        readKey(baReadKey, baRightMerge, cnter);
                        readData(baReadData, baRightMerge, cnter);
                        writeKey(baReadKey, baRightMerge, cnter - iRightUsedCount + iNewRightUsedCount);
                        writeData(baReadData, baRightMerge, cnter - iRightUsedCount + iNewRightUsedCount);
                    }

                    setUsedCount(baRightMerge, iNewRightUsedCount);
                    readKey(baMergeKey, baLeftMerge, iKeySum / 2 - 1);

                }else { //balance to right

                    int iNewRightUsedCount = iKeySum - iKeySum / 2;

                    setUsedCount(baRightMerge, iNewRightUsedCount);

                    for (int cnter = iRightUsedCount - 1; cnter >= 0; cnter--) {
                        readKey(baReadKey, baRightMerge, cnter);
                        readData(baReadData, baRightMerge, cnter);
                        writeKey(baReadKey, baRightMerge, cnter - iRightUsedCount + iNewRightUsedCount);
                        writeData(baReadData, baRightMerge, cnter - iRightUsedCount + iNewRightUsedCount);
                    }

                    for (int cnter = iLeftUsedCount - 1; cnter >= iKeySum / 2; cnter--) {
                        readKey(baReadKey, baLeftMerge, cnter);
                        readData(baReadData, baLeftMerge, cnter);
                        writeKey(baReadKey, baRightMerge, cnter - iLeftUsedCount - iRightUsedCount + iNewRightUsedCount);
                        writeData(baReadData, baRightMerge, cnter - iLeftUsedCount - iRightUsedCount + iNewRightUsedCount);
                    }

                    setUsedCount(baLeftMerge, iKeySum / 2);

                    readKey(baMergeKey, baLeftMerge, iKeySum / 2 - 1);
                }

                m_cKeyPool.addArray(baReadKey);
                m_cDataPool.addArray(baReadData);
                m_cPageNumPool.addArray(baPageNum);

                return false; //only balancing
            }
        }else {//node merge
            byte[] baReadKey = m_cKeyPool.getArray();
            byte[] baPageNum = m_cPageNumPool.getArray();

            if (iKeySum + 1 < m_iMaxNodeKeys) { //merge
                setUsedCount(baLeftMerge, iKeySum + 1);

                //add the merge key
                writeKey(baMergeKey, baLeftMerge, iLeftUsedCount);

                //
                for (int cnter = 0; cnter < iRightUsedCount; cnter++) {
                    readKey(baReadKey, baRightMerge, cnter);
                    readPageNum(baPageNum, baRightMerge, cnter);
                    writeKey(baReadKey, baLeftMerge, cnter + iLeftUsedCount + 1);
                    writePageNum(baPageNum, baLeftMerge, cnter + iLeftUsedCount + 1);
                }

                readPageNum(baPageNum, baRightMerge, iRightUsedCount);
                writePageNum(baPageNum, baLeftMerge, iRightUsedCount + iLeftUsedCount + 1);

                m_cKeyPool.addArray(baReadKey);
                m_cPageNumPool.addArray(baPageNum);

                return true; //merge indicated

            }else { //balancing
                if (iLeftUsedCount < iRightUsedCount) { //balance to left
                    setUsedCount(baLeftMerge, iKeySum / 2);
                    int iNewRightUsedCount = iKeySum - iKeySum / 2;

                    //add the merge key
                    writeKey(baMergeKey, baLeftMerge, iLeftUsedCount);

                    for (int cnter = 0; cnter < iRightUsedCount - iNewRightUsedCount; cnter++) {
                        readKey(baReadKey, baRightMerge, cnter);
                        readPageNum(baPageNum, baRightMerge, cnter);
                        writeKey(baReadKey, baLeftMerge, cnter + iLeftUsedCount + 1);
                        writePageNum(baPageNum, baLeftMerge, cnter + iLeftUsedCount + 1);
                    }

                    for (int cnter = iRightUsedCount - iNewRightUsedCount; cnter < iRightUsedCount; cnter++) {
                        readKey(baReadKey, baRightMerge, cnter);
                        readPageNum(baPageNum, baRightMerge, cnter);
                        writeKey(baReadKey, baRightMerge, cnter - iRightUsedCount + iNewRightUsedCount);
                        writePageNum(baPageNum, baRightMerge, cnter - iRightUsedCount + iNewRightUsedCount);
                    }

                    //copy the last page number
                    readPageNum(baPageNum, baRightMerge, iRightUsedCount);
                    writePageNum(baPageNum, baRightMerge, iNewRightUsedCount);

                    setUsedCount(baRightMerge, iNewRightUsedCount);
                    readKey(baMergeKey, baLeftMerge, iKeySum / 2);

                }else { //balance to right

                    int iNewRightUsedCount = iKeySum - iKeySum / 2;

                    setUsedCount(baRightMerge, iNewRightUsedCount);

                    //copy the last page number
                    readPageNum(baPageNum, baRightMerge, iRightUsedCount);
                    writePageNum(baPageNum, baRightMerge, iNewRightUsedCount);

                    //move the right merge page to the right
                    for (int cnter = iRightUsedCount - 1; cnter >= 0; cnter--) {
                        readKey(baReadKey, baRightMerge, cnter);
                        readPageNum(baPageNum, baRightMerge, cnter);
                        writeKey(baReadKey, baRightMerge, cnter - iRightUsedCount + iNewRightUsedCount);
                        writePageNum(baPageNum, baRightMerge, cnter - iRightUsedCount + iNewRightUsedCount);
                    }

                    //add the merge key
                    writeKey(baMergeKey, baRightMerge, iNewRightUsedCount - iRightUsedCount - 1);
                    readPageNum(baPageNum, baLeftMerge, iLeftUsedCount);
                    writePageNum(baPageNum, baRightMerge, iNewRightUsedCount - iRightUsedCount - 1);

                    for (int cnter = iKeySum / 2 + 1; cnter < iLeftUsedCount; cnter++) {
                        readKey(baReadKey, baLeftMerge, cnter);
                        readPageNum(baPageNum, baLeftMerge, cnter);
                        writeKey(baReadKey, baRightMerge, cnter - (iKeySum / 2 + 1));
                        writePageNum(baPageNum, baRightMerge, cnter - (iKeySum / 2 + 1));
                    }

                    setUsedCount(baLeftMerge, iKeySum / 2);

                    readKey(baMergeKey, baLeftMerge, iKeySum / 2);
                }
                m_cKeyPool.addArray(baReadKey);
                m_cPageNumPool.addArray(baPageNum);

                return false; //only balancing
            }
        }
    }

    //*********************************************************************************************************************
    private byte[] getSubTree(byte[] baKey, byte[] baNode, ObjHolder cOffset) {

        byte[] baRet = m_cPageNumPool.getArray();
        ObjHolder o = (ObjHolder) m_cObjHolderPool.getObject();
        int iUsedCount = getUsedCount(baNode);

        int iIndex = searchKey(baKey, baNode, o);

        if (iIndex >= iUsedCount) {
            readPageNum(baRet, baNode, iUsedCount);
            if (cOffset != null)
                cOffset.setObj(new Integer(iUsedCount));
        }else {
            readPageNum(baRet, baNode, iIndex);
            if (cOffset != null)
                cOffset.setObj(new Integer(iIndex));
        }

        m_cObjHolderPool.addObject(o);
        return baRet;
    }

    //*********************************************************************************************************************
    //fixit -- bFound could be member, to avoid memory garbage
    private void insertInLeaf(byte[] baKey, byte[] baData, byte[] baPage)  throws CodedException {
        //that function assumes that enough space is available!
        ObjHolder bFound = (ObjHolder) m_cObjHolderPool.getObject();

        int iPlugIndex = searchKey(baKey, baPage, bFound);

        if (!((Boolean) bFound.getObj()).booleanValue()) {
            Converter.moveBytes(baPage, PAGE_HEADER_SIZE + 4 + iPlugIndex * (m_iKeySize + m_iDataSize),
                PAGE_HEADER_SIZE + 4 + getUsedCount(baPage) * (m_iKeySize + m_iDataSize),
                m_iKeySize + m_iDataSize);
            setUsedCount(baPage, getUsedCount(baPage) + 1);
        }

        writeKey(baKey, baPage, iPlugIndex);
        writeData(baData, baPage, iPlugIndex);

        m_cObjHolderPool.addObject(bFound);
    }

    //*********************************************************************************************************************
    //fixit -- bFound could be member, to avoid memory garbage
    private void removeFromLeaf(byte[] baKey, byte[] baPage) {

        ObjHolder bFound = (ObjHolder) m_cObjHolderPool.getObject();
        int iRemoveIndex = searchKey(baKey, baPage, bFound);

        if (((Boolean) bFound.getObj()).booleanValue()) {
            Converter.moveBytes(baPage, PAGE_HEADER_SIZE + 4 + (iRemoveIndex + 1) * (m_iKeySize + m_iDataSize),
                PAGE_HEADER_SIZE + 4 + getUsedCount(baPage) * (m_iKeySize + m_iDataSize),
                -m_iKeySize - m_iDataSize);

            setUsedCount(baPage, getUsedCount(baPage) - 1);
        }

        m_cObjHolderPool.addObject(bFound);
    }

    //*********************************************************************************************************************
    //fixit -- bFound could be member, to avoid memory garbage
    private void insertInNode(byte[] baKey, byte[] baFirstChild, byte[] baSecondChild, byte[] baPage) {
        int cnter;

        //try to find a place in the node to plug in the key
        ObjHolder bFound = (ObjHolder) m_cObjHolderPool.getObject();

        int iPlugIndex = searchKey(baKey, baPage, bFound);

        if (!((Boolean) bFound.getObj()).booleanValue()) {
            Converter.moveBytes(baPage, PAGE_HEADER_SIZE + 4 + iPlugIndex * (m_iKeySize + 4), PAGE_HEADER_SIZE + 4 + getUsedCount(baPage) * (m_iKeySize + 4),
                (m_iKeySize + 4));
            setUsedCount(baPage, getUsedCount(baPage) + 1);
        }

        //insert the new key at the right place in the node
        writePageNum(baFirstChild, baPage, iPlugIndex);
        writePageNum(baSecondChild, baPage, iPlugIndex + 1);
        writeKey(baKey, baPage, iPlugIndex);

        m_cObjHolderPool.addObject(bFound);

        return;
    }

    //*********************************************************************************************************************
    private void replaceInNode(int iNewPageNum, byte[] baPage, int iOffset) {

        //shrink the node
        Converter.moveBytes(baPage, PAGE_HEADER_SIZE + 4 + (iOffset + 1) * (m_iKeySize + 4),
            PAGE_HEADER_SIZE + 4 + getUsedCount(baPage) * (m_iKeySize + 4),
            -(m_iKeySize + 4));

        //update the page number, left by the shrinking
        writePageNum(iNewPageNum, baPage, iOffset);

        //decrease the used count
        setUsedCount(baPage, getUsedCount(baPage) - 1);

        return;
    }

    //*********************************************************************************************************************
    private void replaceInNode(byte[] baNewPageNum, byte[] baPage, int iOffset) {

        //shrink the node
        Converter.moveBytes(baPage, PAGE_HEADER_SIZE + 4 + (iOffset + 1) * (m_iKeySize + 4),
            PAGE_HEADER_SIZE + 4 + getUsedCount(baPage) * (m_iKeySize + 4),
            -(m_iKeySize + 4));

        //update the page number, left by the shrinking
        writePageNum(baNewPageNum, baPage, iOffset);

        //decrease the used count
        setUsedCount(baPage, getUsedCount(baPage) - 1);

        return;
    }

    //*************************************************************************
    private void splitLeaf(byte[] baPage, int iOldPageNum,
        byte[] baNewPage, int iNewPageNum, byte[] baKey,
        byte[] baData, byte[] baRetKey)  throws CodedException {

        int iUsedCount = getUsedCount(baPage);
        byte[] baWorkKey = m_cKeyPool.getArray();
        byte[] baWorkData = m_cDataPool.getArray();

        readKey(baWorkKey, baPage, iUsedCount / 2 - 1);

        int iLeftCount;
        int iRightCount;
        boolean bInsertLeft;

        iLeftCount = iUsedCount / 2;
        iRightCount = iUsedCount - iUsedCount / 2;

        if (m_cComparator.compare(baWorkKey, baKey) >= 0) {
            bInsertLeft = true;
        }else {
            bInsertLeft = false;
        }

        int iRightPos = 0;

        setUsedCount(baNewPage, iRightCount);

        for (int cnter = iLeftCount; cnter < iUsedCount; cnter++) {
            readKey(baWorkKey, baPage, cnter);
            writeKey(baWorkKey, baNewPage, iRightPos);
            readData(baWorkData, baPage, cnter);
            writeData(baWorkData, baNewPage, iRightPos);
            iRightPos++;
        }

        //link the two pages bidirectionally
        Converter.moveBytes(baNewPage, m_iPageSize - 4, baPage, m_iPageSize - 4, 4);
        Converter.intToBytes(iNewPageNum, baPage, m_iPageSize - 4);
        Converter.intToBytes(iOldPageNum, baNewPage, PAGE_HEADER_SIZE);

        setUsedCount(baPage, iLeftCount);

        if (bInsertLeft) {
            insertInLeaf(baKey, baData, baPage);
        }else {
            insertInLeaf(baKey, baData, baNewPage);
        }

        //store the last key here (the caller function needs it)
        readKey(baRetKey, baPage, getUsedCount(baPage) - 1);

        m_cKeyPool.addArray(baWorkKey);
        m_cDataPool.addArray(baWorkData);

        return;

    }

    //*********************************************************************************************************************
    private void splitNode(byte[] baPage, byte[] baNewPage, byte[] baKey, byte[] baFirstChild, byte[] baSecondChild, byte[] baRetKey) {

        int iUsedCount = getUsedCount(baPage);
        byte[] baWorkKey = m_cKeyPool.getArray();
        byte[] baWorkPageNum = m_cPageNumPool.getArray();

        readKey(baWorkKey, baPage, iUsedCount / 2 - 1);

        int iLeftCount;
        int iRightCount;
        boolean bInsertLeft;

        iLeftCount = iUsedCount / 2;
        iRightCount = iUsedCount - iUsedCount / 2;

        if (m_cComparator.compare(baWorkKey, baKey) >= 0) {
            bInsertLeft = true;
        }else {
            bInsertLeft = false;
        }

        int iRightPos = 0;
        boolean bWritten = false;

        setUsedCount(baNewPage, iRightCount);

        int cnter;

        for (cnter = iLeftCount; cnter < iUsedCount; cnter++) {
            readPageNum(baWorkPageNum, baPage, cnter);
            writePageNum(baWorkPageNum, baNewPage, iRightPos);
            readKey(baWorkKey, baPage, cnter);
            writeKey(baWorkKey, baNewPage, iRightPos);
            iRightPos++;
        }

        readPageNum(baWorkPageNum, baPage, cnter);
        writePageNum(baWorkPageNum, baNewPage, iRightPos);

        setUsedCount(baPage, iLeftCount);

        if (bInsertLeft) {
            insertInNode(baKey, baFirstChild, baSecondChild, baPage);
        }else {
            insertInNode(baKey, baFirstChild, baSecondChild, baNewPage);
        }

        //store the last key here (the caller function needs it)
        readKey(baRetKey, baPage, getUsedCount(baPage) - 1);

        //throw away the last key
        setUsedCount(baPage, getUsedCount(baPage) - 1);

        m_cKeyPool.addArray(baWorkKey);
        m_cPageNumPool.addArray(baWorkPageNum);

        return;

    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /** this function performs binary search of a key in a page
     * @param baKey key to search for
     * @param baPage page to search in
     * @param bFound contains a boolean value after the search, indicating whether the key was found
     * @return the index of the key in the page if found, the place where it should be inserted
     * else
     */
    private int searchKey(byte[] baKey, byte[] baPage, ObjHolder bFound) {
        int iUsedCount = getUsedCount(baPage);
        int iStartIdx = 0;
        int iEndIdx = iUsedCount - 1;

        boolean bStartChk = true;
        boolean bEndChk = true;

        int iMiddleIdx = (iStartIdx + iEndIdx) / 2;

        //the page is empty, return 0 and false
        if (iStartIdx > iEndIdx) {
            bFound.setObj(Boolean.FALSE);
            return 0;
        }
        //check if the key is smaller than all in the page
        if (m_cComparator.compare(baKey, 0, baPage, getKeyOffset(baPage, iStartIdx), baKey.length) < 0) {
            bFound.setObj(Boolean.FALSE);
            return 0;
        }
        //check if the key is greater than all in the page
        if (m_cComparator.compare(baKey, 0, baPage, getKeyOffset(baPage, iEndIdx), baKey.length) > 0) {
            bFound.setObj(Boolean.FALSE);
            return iUsedCount;
        }

        do {
            if (bStartChk) {
                if (m_cComparator.compare(baKey, 0, baPage, getKeyOffset(baPage, iStartIdx), baKey.length) == 0) {
                    bFound.setObj(Boolean.TRUE);
                    return iStartIdx;
                }
                bStartChk = false;
            }

            if (bEndChk) {
                if (m_cComparator.compare(baKey, 0, baPage, getKeyOffset(baPage, iEndIdx), baKey.length) == 0) {
                    bFound.setObj(Boolean.TRUE);
                    return iEndIdx;
                }
                bEndChk = false;
            }

            int iCmpRes = m_cComparator.compare(baKey, 0, baPage, getKeyOffset(baPage, iMiddleIdx), baKey.length);

            if (iCmpRes == 0) {
                bFound.setObj(Boolean.TRUE);
                return iMiddleIdx;
            }else {
                if (iCmpRes < 0) {
                    iEndIdx = iMiddleIdx;
                    bEndChk = true;
                }else {
                    iStartIdx = iMiddleIdx;
                    bStartChk = true;
                }

                if (iMiddleIdx != (iStartIdx + iEndIdx) / 2)
                    iMiddleIdx = (iStartIdx + iEndIdx) / 2;
                else
                    break;
            }
        }
        while (true);

        bFound.setObj(Boolean.FALSE);
        return iEndIdx;
    }

    //*********************************************************************************************************************
    private boolean pageContains(byte[] baKey, byte[] baPage, ObjHolder cOffset) {
        ObjHolder cObj = (ObjHolder) m_cObjHolderPool.getObject();
        int iIndex = searchKey(baKey, baPage, cObj);

        if (cOffset != null)
            cOffset.setObj(new Integer(iIndex));

        boolean bRet = ((Boolean) cObj.getObj()).booleanValue();

        m_cObjHolderPool.addObject(cObj);

        return bRet;
    }

    //*********************************************************************************************************************
    private void writeKey(byte[] baKey, byte[] baPage, int iOffset) {

        int iByteOffset;

        if (isLeaf(baPage)) { //leaf
            iByteOffset = PAGE_HEADER_SIZE + 4 + (m_iDataSize + m_iKeySize) * iOffset;
        }else {
            iByteOffset = PAGE_HEADER_SIZE + 4 + (4 + m_iKeySize) * iOffset;
        }
        System.arraycopy(baKey, 0, baPage, iByteOffset, m_iKeySize);
    }

    //*********************************************************************************************************************
    private int getKeyOffset(byte[] baPage, int iIndex) {
        if (isLeaf(baPage)) { //leaf
            return PAGE_HEADER_SIZE + 4 + (m_iDataSize + m_iKeySize) * iIndex;
        }else {
            return PAGE_HEADER_SIZE + 4 + (4 + m_iKeySize) * iIndex;
        }
    }

    //*********************************************************************************************************************
    private void readKey(byte[] baKey, byte[] baPage, int iOffset) {

        int iByteOffset;

        if (isLeaf(baPage)) { //leaf
            iByteOffset = PAGE_HEADER_SIZE + 4 + (m_iDataSize + m_iKeySize) * iOffset;
        }else {
            iByteOffset = PAGE_HEADER_SIZE + 4 + (4 + m_iKeySize) * iOffset;
        }

        System.arraycopy(baPage, iByteOffset, baKey, 0, m_iKeySize);
    }

    //*********************************************************************************************************************
    private int getDataOffset(byte[] baPage, int iIndex) throws CodedException {
        if (isLeaf(baPage)) { //leaf
            return PAGE_HEADER_SIZE + 4 + (m_iDataSize + m_iKeySize) * iIndex + m_iKeySize;
        }else
            throw new CodedException(this, ERR_NOT_LEAF, "only leafs can be applied!");
    }

    //*********************************************************************************************************************
    private void writeData(byte[] baData, byte[] baPage, int iOffset) throws CodedException {

        int iByteOffset;

        if (isLeaf(baPage)) { //leaf
            iByteOffset = PAGE_HEADER_SIZE + 4 + (m_iDataSize + m_iKeySize) * iOffset + m_iKeySize;
        }else
            throw new CodedException(this, ERR_NOT_LEAF, "only leafs can be applied!");

        System.arraycopy(baData, 0, baPage, iByteOffset, m_iDataSize);
    }

    //*********************************************************************************************************************
    private void readData(byte[] baData, byte[] baPage, int iOffset) {

        int iByteOffset;

        if (isLeaf(baPage)) { //leaf
            iByteOffset = PAGE_HEADER_SIZE + 4 + (m_iDataSize + m_iKeySize) * iOffset + m_iKeySize;
        }else {
            return; //only leafs can be applied!
        }

        System.arraycopy(baPage, iByteOffset, baData, 0, baData.length);
    }

    //*********************************************************************************************************************
    private void writePageNum(int iPageNum, byte[] baPage, int iOffset) {
        int iByteOffset;

        if (isLeaf(baPage)) { //leaf
            if (iOffset == 0)
                iByteOffset = PAGE_HEADER_SIZE;
            else
                iByteOffset = m_iPageSize - 4;
        } else {
            iByteOffset = PAGE_HEADER_SIZE + (4 + m_iKeySize) * iOffset;
        }
        Converter.intToBytes(iPageNum, baPage, iByteOffset);
    }

    //*********************************************************************************************************************
    private void writePageNum(byte[] baPageNum, byte[] baPage, int iOffset) {
        int iByteOffset;

        int iPageNum = Converter.bytesToInt( baPageNum );

        if (isLeaf(baPage)) { //leaf
            if (iOffset == 0)
                iByteOffset = PAGE_HEADER_SIZE;
            else
                iByteOffset = m_iPageSize - 4;
        } else {
            iByteOffset = PAGE_HEADER_SIZE + (4 + m_iKeySize) * iOffset;
        }

        baPage[ iByteOffset	] = baPageNum[ 0 ];
        baPage[ iByteOffset + 1 ] = baPageNum[ 1 ];
        baPage[ iByteOffset + 2 ] = baPageNum[ 2 ];
        baPage[ iByteOffset + 3 ] = baPageNum[ 3 ];
    }

    //*********************************************************************************************************************
    private int readPageNum(byte[] baPage, int iOffset) {
        int iByteOffset;

        if (isLeaf(baPage)) { //leaf
            if (iOffset == 0)
                iByteOffset = PAGE_HEADER_SIZE;
            else
                iByteOffset = m_iPageSize - 4;
        } else {
            iByteOffset = PAGE_HEADER_SIZE + (4 + m_iKeySize) * iOffset;
        }

        return Converter.bytesToInt(baPage, iByteOffset);
    }

    //*********************************************************************************************************************
    private void readPageNum(byte[] baPageNum, byte[] baPage, int iOffset) {
        int iByteOffset;

        if (isLeaf(baPage)) { //leaf
            if (iOffset == 0)
                iByteOffset = PAGE_HEADER_SIZE;
            else
                iByteOffset = m_iPageSize - 4;
        } else {
            iByteOffset = PAGE_HEADER_SIZE + (4 + m_iKeySize) * iOffset;
        }

        baPageNum[ 0 ] = baPage[ iByteOffset	 ];
        baPageNum[ 1 ] = baPage[ iByteOffset + 1 ];
        baPageNum[ 2 ] = baPage[ iByteOffset + 2 ];
        baPageNum[ 3 ] = baPage[ iByteOffset + 3 ];
    }

    //*********************************************************************************************************************
    private boolean isLeaf(byte[] baPage) {
        if ((baPage[ 1 ] & (0x1 << 7)) != 0) //bit 1 set
            return true;
        else
            return false;
    }

    //*********************************************************************************************************************
    private void setLeaf(byte[] baPage, boolean bValue) {
        if (bValue) {
            baPage[ 1 ] = (byte) ((0x1 << 7) | baPage[ 1 ]);
        }else {
            baPage[ 1 ] = (byte) (~(0x1 << 7) & baPage[ 1 ]);
        }

    }

    //*********************************************************************************************************************
    private int getUsedCount(byte[] baPage) {

        //return only the least significent 15 bits
        return Converter.bytesToShort(baPage, 0) & 0x7fff;
    }

    //*********************************************************************************************************************
    private void setUsedCount(byte[] baPage, int iValue) {

        //change only the least significent 15 bits
        boolean bLeaf = isLeaf(baPage);

        Converter.shortToBytes(iValue, baPage, 0);
        setLeaf(baPage, bLeaf);
    }

    //*********************************************************************************************************************
    private void dumpPage(byte[] baPage) {
        for (int cnter = 0; cnter < m_iPageSize; cnter += 8) {
            System.out.println(":>" + baPage[ cnter     ] + " " + baPage[ cnter + 1 ] + " " + baPage[ cnter + 2 ] + " " + baPage[ cnter + 3 ]
                + " " + baPage[ cnter + 4 ] + " " + baPage[ cnter + 5 ] + " " + baPage[ cnter + 6 ] + " " + baPage[ cnter + 7 ]);
        }
    }

    //*********************************************************************************************************************
    /** returns a Iterator object positioned at the last found element
     * @return Iterator
     * @throws CodedException  */
    public Iterator getIterator() throws CodedException {
        if ((m_iCurrentOffset == -1) || (m_iCurrentPage == -1)) {
            goToFirstKey();
        }

        return new IteratorBxImpl(m_iCurrentPage, m_iCurrentOffset);
    }

    //*********************************************************************************************************************
    private void formatBlankPage(int iPageNum, PageBuffer cPageBuf) throws CodedException {
        byte[] baRoot = null;

        baRoot = cPageBuf.fixSync(iPageNum);

        //the first btree page is always leaf
        setLeaf(baRoot, true);

        writePageNum(-1, baRoot, 0);
        writePageNum(-1, baRoot, 2);

        //and is empty
        setUsedCount(baRoot, 0);

        cPageBuf.setDirty(iPageNum);
        cPageBuf.unfix(iPageNum);
    }

    //*********************************************************************************************************************
    /**
     * @param bLastFirst
     * @throws CodedException  */
    public void goToKey(boolean bLastFirst) throws CodedException {
        int iPageNum = m_iRootPage;
        int iSavedPageNum = -1;
        boolean bRet = false;

        do {
            byte[] baPage = null;

            baPage = m_cPageBuf.fixSync(iPageNum);

            if (isLeaf(baPage)) {

                //store the position of the record for iterating
                m_iCurrentPage = iPageNum;
                if (bLastFirst) {
                    m_iCurrentOffset = getUsedCount(baPage) - 1;
                }else {
                    m_iCurrentOffset = 0;
                }
                m_cPageBuf.unfix(iPageNum);
                return;
            }else {
                iSavedPageNum = iPageNum;
                if (bLastFirst) {
                    iPageNum = readPageNum(baPage, getUsedCount(baPage) - 1);
                }else {
                    iPageNum = readPageNum(baPage, 0);
                }
            }
            m_cPageBuf.unfix(iSavedPageNum);
        }
        while (true);
    }

    //*********************************************************************************************************************
    private void goToFirstKey() throws CodedException {
        goToKey(false);
    }

    //*********************************************************************************************************************
    private void goToLastKey() throws CodedException {
        goToKey(true);
    }

    //*********************************************************************************************************************
    /**
     * @param baKey
     * @param baSplitKey
     * @param baFirstBranchNum
     * @param baSecondBranchNum
     * @throws CodedException  */
    public void undoInsert(final byte[] baKey, final byte[] baSplitKey, byte[] baFirstBranchNum, byte[] baSecondBranchNum) throws CodedException {

        byte[] baFirstBranch = m_cPageBuf.fixSync(Converter.bytesToInt(baFirstBranchNum));
        byte[] baSecondBranch = m_cPageBuf.fixSync(Converter.bytesToInt(baSecondBranchNum));

        removeFromUndertree(baKey, baFirstBranch);
        removeFromUndertree(baKey, baSecondBranch);

        mergePages(baFirstBranch, baSecondBranch, baSplitKey);

        return;

    }

    //*********************************************************************************************************************
    /**
     * @throws CodedException  */
    public void removeAll() throws CodedException {
        removeUndertree(m_iRootPage);
        m_iRootPage = -1;
    }

    //*********************************************************************************************************************
    private void removeUndertree(int iPageNum) throws CodedException {
        byte[] baRoot = m_cPageBuf.fixSync(iPageNum);

        if (!isLeaf(baRoot)) {
            for (int iCnter = 0; iCnter < getUsedCount(baRoot); iCnter++) {

                int iSubPageNum = readPageNum(baRoot, iCnter);

                removeUndertree(iSubPageNum);
            }
        }
        m_cPageBuf.unfix(iPageNum);
        m_cPageBuf.getPageManager().freePage(iPageNum);
        return;
    }

    //*********************************************************************************************************************

    /**
     * @return  */
    public DbComparator getComparator() {
        return m_cComparator;
    }

    /*
     * IteratorBxImpl.java
     *
     * Created on 25. Juni 2001, 12:02
     */

    private class IteratorBxImpl implements db.com.Iterator {

        private int        m_iPagePos = -1;
        private int        m_iOffsetPos = -1;
        private int        m_iLastPagePos = -1;
        private int        m_iLastOffsetPos = -1;
        private boolean    m_bOpened = false;
        private byte[]     m_baCurrentPage = null;

        /** Creates new IteratorBxImpl
         * @param iPagePos
         * @param iOffsetPos  */
        public IteratorBxImpl(int iPagePos, int iOffsetPos) {
            m_iPagePos = iPagePos;
            m_iOffsetPos = iOffsetPos;
        }

        //********************************************************************************************************
        /**
         */
        public void open() {
            if (m_bOpened == false) {
                try {
                    m_baCurrentPage = m_cPageBuf.fixSync(m_iPagePos);
                    m_bOpened = true;

                }catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        //********************************************************************************************************
        /**
         */
        public void close() {
            if (m_bOpened == true) {
                try {
                    m_cPageBuf.unfix(m_iPagePos);
                    m_bOpened = false;
                }catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        //********************************************************************************************************
        /**
         */
        public void finalize() {
            close();
        }

        //********************************************************************************************************
        /**
         * @return  */
        public boolean isEmpty() {
            if (m_iCurrentPage != -1 && m_iCurrentOffset != -1)
                return false;
            else
                return true;
        }

        //********************************************************************************************************
        /**
         * @throws CodedException
         * @return  */
        public boolean moveToFirst() throws CodedException {
            close();
            goToFirstKey();

            m_iPagePos = m_iCurrentPage;
            m_iOffsetPos = m_iCurrentOffset;

            open();
            return !isEmpty();
        }

        //********************************************************************************************************
        /**
         * @param cKey
         * @param cData
         * @throws CodedException  */
        public void getCurrent(Set cKey, Set cData) throws CodedException {
            if (!m_bOpened)
                open();

            if (isEmpty())
                throw new CodedException(this, ERR_EMPTY_ITERATOR, "The iterator is empty, you can't get the current!");

            if (cKey != null) {
                cKey.setBytes(m_baCurrentPage);
                cKey.setOffset(getKeyOffset(m_baCurrentPage, m_iOffsetPos));
                cKey.setLength(m_iKeySize);
            }

            if (cData != null) {
                cData.setBytes(m_baCurrentPage);
                cData.setOffset(getDataOffset(m_baCurrentPage, m_iOffsetPos));
                cData.setLength(m_iDataSize);
            }
        }

        //********************************************************************************************************
        /**
         * @throws CodedException
         * @return  */
        public boolean moveToNext() throws CodedException {

            if (!m_bOpened)
                open();

            if (isEmpty())
                throw new CodedException(this, ERR_EMPTY_ITERATOR, "The iterator is empty, you can't move to next!");

            int iNewOffsetPos = m_iOffsetPos + 1;

            if (iNewOffsetPos >= getUsedCount(m_baCurrentPage)) {
                int iNextPageNum = readPageNum(m_baCurrentPage, 1);

                if (iNextPageNum != -1) {

                    //unfix the old page
                    m_cPageBuf.unfix(m_iPagePos);

                    m_iPagePos = iNextPageNum;
                    m_iOffsetPos = 0;

                    //fix the next page
                    m_baCurrentPage = m_cPageBuf.fixSync(m_iPagePos);

                    return true;

                }else {
                    //end reached
                    return false;
                }
            }else {
                m_iOffsetPos = iNewOffsetPos;
                return true;
            }
        }

        //********************************************************************************************************
        /**
         * @throws CodedException
         * @return  */
        public boolean moveToPrev() throws CodedException {
            if (!m_bOpened)
                open();

            if (isEmpty())
                throw new CodedException(this, ERR_EMPTY_ITERATOR, "The iterator is empty, you can't move to prev!");

            int iNewOffsetPos = m_iOffsetPos - 1;

            if (iNewOffsetPos < 0) {
                int iPrevPageNum = readPageNum(m_baCurrentPage, 0);

                if (iPrevPageNum != -1) {

                    //unfix the old page
                    m_cPageBuf.unfix(m_iPagePos);

                    m_iPagePos = iPrevPageNum;

                    //fix the next page
                    m_baCurrentPage = m_cPageBuf.fixSync(m_iPagePos);

                    m_iOffsetPos = getUsedCount(m_baCurrentPage);

                    return true;

                }else {
                    //end reached
                    return false;
                }
            }else {
                m_iOffsetPos = iNewOffsetPos;
                return true;
            }
        }
        //********************************************************************************************************
    }

}
