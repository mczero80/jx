/*
 * SetAccess.java
 *
 * Created on 27. Juni 2001, 21:03
 */

package db.tid;


import jx.db.CodedException;
import db.com.Iterator;
import db.com.Converter;

import db.systembuffer.PageBuffer;
import db.bxtree.BxTree;
import db.list.List;


/** A class realizing the TID concept
 *
 * @author Ivan Dedinski
 * @version 1.0
 */
public class SetAccessImpl implements SetAccess  {

    private PageBuffer m_cPageBuf = null;
    private BxTree m_cBxTree = null;

    private Set m_cKey;
    private byte[] m_baKey;
    private SetPage[] m_cTmpSetPage;

    ////////////////////////////////////////////////////////////////////////////
    /** Creates new SetAccess
     * @param cPageBuf page buffer where the data will be stored
     * @param cBxTree BxTree to register the used pages and their free space.
     * Must be created with a key len 6 and SetKeyComparator as params
     */
    public SetAccessImpl(PageBuffer cPageBuf, BxTree cBxTree) {
        m_cPageBuf = cPageBuf;
        m_cBxTree = cBxTree;
        m_cKey = new Set(null, 0, 0);
        m_baKey = new byte[ 8 ];
        m_cTmpSetPage = new SetPage[2];
        m_cTmpSetPage[0] = new SetPageImpl();
        m_cTmpSetPage[1] = new SetPageImpl();
    }

    ////////////////////////////////////////////////////////////////////////////
    /** implemented function, see {@link SetAccess}
     * @param iDataSize
     * @param iSetType
     * @param cSetNumber place to store the generated set number
     * @throws CodedException
     * @return
     */
    public Set addSet(SetNumber cSetNumber, int iDataSize, int iSetType) throws CodedException {
        //byte[] baKey = new byte[ 8 ];
        Converter.shortToBytes(iSetType, m_baKey);
        Converter.shortToBytes(iDataSize, m_baKey, 2);
        Converter.intToBytes(0, m_baKey, 4);

        m_cBxTree.find(m_baKey, null);
        Iterator i = m_cBxTree.getIterator();

        if (i != null) {
            i.open();
            if (!i.isEmpty()) do {
                    i.getCurrent(m_cKey, null);
                    System.arraycopy(m_cKey.getBytes(), m_cKey.getOffset(), m_baKey, 0, m_baKey.length);
                    if (Converter.bytesToShort(m_baKey, 0) == iSetType) {
                        if (Converter.bytesToShort(m_baKey, 2) >= iDataSize) { //found a page to place the set
                            int iPageNum = Converter.bytesToInt(m_baKey, 4);

                            if( iPageNum < 0 )
                                System.out.println("Gatcha! -> SetAccess");

                            SetPage cSetPage = new SetPageImpl(m_cPageBuf.fixSync(iPageNum));

                            Set cSet = cSetPage.addSet(cSetNumber, iDataSize);

                            cSet.setPageInfo(m_cPageBuf, iPageNum);
                            cSet.setDirty();

                            //unfix all iterator pages
                            i.close();

                            //actualize the b* tree with the new free size of the page after the insetrion
                            m_cBxTree.remove(m_baKey);
                            Converter.shortToBytes(cSetPage.getFreeSpace(), m_baKey, 2);

                            m_cBxTree.insert(m_baKey, m_baKey); //the second argument is dummy
                            cSetNumber.setPageNumber(iPageNum);
                            return cSet;
                        }
                    }else
                        break;
                }
                while (i.moveToNext());

                //unfix all iterator pages
            i.close();
        }

        int iPageNum = m_cPageBuf.getPageManager().getBlankPage();

        SetPage cSetPage = new SetPageImpl(m_cPageBuf.fixSync(iPageNum));

        cSetPage.format();
        Set cSet = cSetPage.addSet(cSetNumber, iDataSize);

        cSet.setPageInfo(m_cPageBuf, iPageNum);
        cSet.setDirty();
        cSetNumber.setPageNumber(iPageNum);

        //actualize the b* tree with the new free size of the page after the insetrion
        Converter.shortToBytes(iSetType, m_baKey, 0);
        Converter.shortToBytes(cSetPage.getFreeSpace(), m_baKey, 2);
        Converter.intToBytes(iPageNum, m_baKey, 4);
        m_cBxTree.insert(m_baKey, m_baKey); //the second argument is dummy

        return cSet;
    }

    ////////////////////////////////////////////////////////////////////////////
    /** implemented function, see {@link SetAccess}
     * @param iSetType
     * @param cSetNumber number of the set, that identifies its position
     * @throws CodedException
     * @return
     */
    public Set getSet(SetNumber cSetNumber, int iSetType/*will be used for transaction locking*/) throws CodedException {
        Set cRet = null;
        SetNumber cRedirectedSetNumber = null;
        int iPageNum = cSetNumber.getPageNumber();

        m_cTmpSetPage[0].setPage(m_cPageBuf.fixSync(iPageNum));
        if (!m_cTmpSetPage[0].getRedirectionFlag(cSetNumber.getTidNumber())) {
            cRet = m_cTmpSetPage[0].getSet(cSetNumber);
            cRet.setPageInfo(m_cPageBuf, iPageNum);
        }else {
            cRedirectedSetNumber = new SetNumber();
            m_cTmpSetPage[0].getRedirection(cSetNumber, cRedirectedSetNumber);

            m_cTmpSetPage[1].setPage(m_cPageBuf.fixSync(cRedirectedSetNumber.getPageNumber()));

            cRet = m_cTmpSetPage[1].getSet(cRedirectedSetNumber);
            cRet.setPageInfo(m_cPageBuf, cRedirectedSetNumber.getPageNumber());
        }

        if (cRedirectedSetNumber != null)
            m_cPageBuf.unfix(iPageNum);

        return cRet;
    }

    ////////////////////////////////////////////////////////////////////////////
    /** implemented function, see {@link SetAccess}
     * @param cSetNumber number of the set to be removed
     * @throws SetAccessException Exception is thrown, if the set doesn't exist or can't be removed
     */
    public void removeSet(SetNumber cSetNumber, int iSetType/*will be used for transaction locking*/) throws CodedException {
        SetPage cSetPage = new SetPageImpl(m_cPageBuf.fixSync(cSetNumber.getPageNumber()));

        //byte[] baKey = new byte[ 8 ];

        Converter.shortToBytes(iSetType, m_baKey, 0);
        Converter.shortToBytes(cSetPage.getFreeSpace(), m_baKey, 2);
        Converter.intToBytes(cSetNumber.getPageNumber(), m_baKey, 4);

        cSetPage.removeSet(cSetNumber);

        m_cBxTree.remove(m_baKey);

        if (cSetPage.getSetSlotCount() == 0) {
            //free the unused page
            m_cPageBuf.getPageManager().freePage(cSetNumber.getPageNumber());
            m_cPageBuf.unfix(cSetNumber.getPageNumber());
            return;
        }else {
            //update the bxtree
            Converter.shortToBytes(cSetPage.getFreeSpace(), m_baKey, 2);
            m_cBxTree.insert(m_baKey, m_baKey); //the second param is dummy
        }

        m_cPageBuf.setDirty(cSetNumber.getPageNumber());
        m_cPageBuf.unfix(cSetNumber.getPageNumber());
    }

    ////////////////////////////////////////////////////////////////////////////
    /** implemented function, see {@link SetAccess}
     * @param cSetNumber number of the set to be modified
     * @param baData the new data of the set
     * @throws SetAccessException Exception is thrown, if the set doesn't exist, the new length is too big, or the
     * set can't be modifies
     */
    public Set modifySet(SetNumber cSetNumber, int iDataSize, int iSetType) throws CodedException {

        SetPage cSetPage = new SetPageImpl(m_cPageBuf.fixSync(cSetNumber.getPageNumber()));
        Set cRet = null;
        boolean bRedirected = false;
        int iPrevFreeSpace = cSetPage.getFreeSpace();

        if (!cSetPage.getRedirectionFlag(cSetNumber.getTidNumber())) {
            // if enough space available ...
            if (cSetPage.getFreeSpace() >= (iDataSize - cSetPage.getSetLength(cSetNumber))) { //yes!
                cRet = cSetPage.modifySet(cSetNumber, iDataSize);
                cRet.setPageInfo(m_cPageBuf, cSetNumber.getPageNumber());

            }else {

                //add the set somewhere else
                SetNumber cRedirectedSetNumber = new SetNumber();

                cRet = addSet(cRedirectedSetNumber, iDataSize, iSetType);

                //add redirection note at the old set place
                cSetPage.setRedirection(cSetNumber, cRedirectedSetNumber, iDataSize);

                cRet.setPageInfo(m_cPageBuf, cRedirectedSetNumber.getPageNumber());

                bRedirected = true;
            }
        }else {
            bRedirected = true;

            SetNumber cRedirectedSetNumber = new SetNumber();

            cSetPage.getRedirection(cSetNumber, cRedirectedSetNumber);

            SetPage cRedirectedSetPage = new SetPageImpl(m_cPageBuf.fixSync(cRedirectedSetNumber.getPageNumber()));

            //check if enough space available ...
            if (cRedirectedSetPage.getFreeSpace() >=
                iDataSize - cRedirectedSetPage.getSetLength(cRedirectedSetNumber)) { //yes!

                int iPrevRedFreeSpace = cRedirectedSetPage.getFreeSpace();

                cRet = cRedirectedSetPage.modifySet(cRedirectedSetNumber, iDataSize);
                cRet.setPageInfo(m_cPageBuf, cRedirectedSetNumber.getPageNumber());

                Converter.shortToBytes(iSetType, m_baKey, 0);
                Converter.shortToBytes(iPrevRedFreeSpace, m_baKey, 2);
                Converter.intToBytes(cRedirectedSetNumber.getPageNumber(), m_baKey, 4);
                m_cBxTree.remove(m_baKey);
                Converter.shortToBytes(cRedirectedSetPage.getFreeSpace(), m_baKey, 2);
                m_cBxTree.insert(m_baKey, m_baKey);

                //optimizeSetPage( cRedirectedSetPage ); //remove unused redirections

            }else { //migrate again, but use the old redirection note, don't create a new one!
                //  m_cPageBuf.setDirty(  cRedirectedSetNumber.getPageNumber() );
                m_cPageBuf.unfix(cRedirectedSetNumber.getPageNumber());
                removeSet(cRedirectedSetNumber, iSetType); //that calls optimizeSetPage
                cRet = addSet(cRedirectedSetNumber, iDataSize, iSetType);
                cRet.setPageInfo(m_cPageBuf, cRedirectedSetNumber.getPageNumber());

            }
            //update the redirection note
            cSetPage.setRedirection(cSetNumber, cRedirectedSetNumber, iDataSize);
        }

        //optimizeSetPage( cSetPage ); //remove unused redirections


        if (iPrevFreeSpace != cSetPage.getFreeSpace()) { //free space has changed!
            //update the bxtree

            Converter.shortToBytes(iSetType, m_baKey, 0);
            Converter.shortToBytes(iPrevFreeSpace, m_baKey, 2);
            Converter.intToBytes(cSetNumber.getPageNumber(), m_baKey, 4);
            m_cBxTree.remove(m_baKey);

            Converter.shortToBytes(cSetPage.getFreeSpace(), m_baKey, 2);
            m_cBxTree.insert(m_baKey, m_baKey); //the second param is dummy
        }

        if (bRedirected) {
            m_cPageBuf.setDirty(cSetNumber.getPageNumber());
            m_cPageBuf.unfix(cSetNumber.getPageNumber());
        }

        cRet.setDirty();

        return cRet;
    }

    ////////////////////////////////////////////////////////////////////////////
    /** implemented function, see {@link SetAccess}
     * @param cSetPage
     */
    public void optimizeSetPage(SetPage cSetPage) {
        return; //currently no optimizations
    }

    ////////////////////////////////////////////////////////////////////////////
    /** implemented function, see {@link SetAccess}
     * @return
     */
    public int getMaxSetLen() {
        try {
            return m_cPageBuf.getPageSize() - 4;
        }  catch (Exception ex) {
            ex.printStackTrace();
        }

        return -1;
    }

    ////////////////////////////////////////////////////////////////////////////
    /** implemented function, see {@link SetAccess}
     * @return
     */
    public int getMinSetLen() {
        return 8;
    }

    /*
     public void format(){
     Converter.shortToBytes()
     }*/

    ////////////////////////////////////////////////////////////////////////////
    /** implemented function, see {@link SetAccess}
     */
    public void dropSetType(int iSetType) throws CodedException {
        byte[] baKey;// = new byte[ 8 ];

        Converter.shortToBytes(iSetType, m_baKey);
        Converter.shortToBytes(0, m_baKey, 2);
        Converter.intToBytes(0, m_baKey, 4);

        m_cBxTree.find(m_baKey, null);
        Iterator i = m_cBxTree.getIterator();
        List cList = new List();

        if (i != null) {
            i.open();
            if (!i.isEmpty()) do {
                    i.getCurrent(m_cKey, null);
                    if (Converter.bytesToShort(m_cKey.getBytes(), m_cKey.getOffset()) == iSetType) {
                        baKey = new byte[ 8 ];
                        System.arraycopy(m_cKey.getBytes(), m_cKey.getOffset(), baKey, 0, 8);
                        cList.insert(baKey);
                    }else
                        break;
                }
                while (i.moveToNext());

                //unfix all iterator pages
            i.close();
        }

        if (cList.moveToFirst()) { //list not empty, so prozess
            do {
                baKey = (byte[]) cList.getCurrent();
                //free the used page
                m_cPageBuf.getPageManager().freePage(Converter.bytesToInt(baKey, 4));

                //and remove the key
                m_cBxTree.remove(baKey);
            }
            while (cList.moveToNext());
        }
    }
    ////////////////////////////////////////////////////////////////////////////
}

