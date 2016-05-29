/*
 * SetPage.java
 *
 * Created on 2. Juli 2001, 19:26
 */

package db.tid;


import db.com.*;

import jx.db.CodedException;


/** A class that implements the SetPage interface
 *
 * @author Ivan Dedinski
 * @version 1.0
 */
public class SetPageImpl implements SetPage {

     /** error code, thrown when a too big page is associated with the SetPage object
     * pages can not be bigger than 37000 bytes
     */
    public static final int ERR_PAGE_TOO_BIG   = 0;
    /** erroro code, thrown if a too small set is created. A set can not be smaller than 8 bytes
     */
    public static final int ERR_SET_TOO_SMALL  = 1;

    private byte[] m_baPage = null;
    private final int SLOT_FREE = 0;

    //*********************************************************************************************************************
    /** Creates new SetPage */
    public SetPageImpl(byte[] baPage) throws CodedException {
        m_baPage = baPage;
        if (baPage.length > 37000) throw new CodedException(this, ERR_PAGE_TOO_BIG, "Page too big");

    }

    public SetPageImpl() { // only called by the factory
        m_baPage = null;
    }

    //*********************************************************************************************************************
    public void format() {
        setSetSlotCount(0); //mark page as empty
    }

    //*********************************************************************************************************************
    public void setPage(byte[] baPage) throws CodedException {
        m_baPage = baPage;
        if (baPage.length > 37000) throw new CodedException(this, ERR_PAGE_TOO_BIG, "Page too big");
    }

    //*********************************************************************************************************************
    public Set addSet(SetNumber cSetNumber, int iDataSize) throws CodedException {

        int iEndOfLast;

        if (iDataSize < 8) //sets can't be smaller than the redirection size!
            throw new CodedException(this, ERR_SET_TOO_SMALL, "The set is not big enough");

        iEndOfLast = getEndOfLast();

        int iSetSlot = getFreeSetSlot();

        setSetSlotCount(iSetSlot + 1);

        setSetEnd(iSetSlot, iEndOfLast - iDataSize);
        setRedirectionFlag(iSetSlot, false);

        cSetNumber.setTidNumber(iSetSlot);

        return new Set(m_baPage, iEndOfLast - iDataSize, iDataSize);
    }

    //*********************************************************************************************************************
    public Set getSet(SetNumber cSetNumber) throws CodedException {

        //        if( isValidSetNumber( cSetNumber ) == false )
        //            throw new CodedException( this, ERR_INVALID_SET_NO, "The set number is not valid" );

        int iEndOfPrev;

        if (getPrevSet(cSetNumber.getTidNumber()) == -1)
            iEndOfPrev = m_baPage.length;
        else
            iEndOfPrev = getSetEnd(getPrevSet(cSetNumber.getTidNumber()));

        int iEndOfThis = getSetEnd(cSetNumber.getTidNumber());
        int iLen = iEndOfPrev - iEndOfThis;

        return new Set(m_baPage, iEndOfPrev - iLen, iLen);
    }

    //*********************************************************************************************************************
    public void removeSet(SetNumber cSetNumber) throws CodedException {

        //        if( isValidSetNumber( cSetNumber ) == false )
        //            throw new CodedException( this, ERR_INVALID_SET_NO, "The set number is not valid" );

        int iEndOfPrev;

        if (getPrevSet(cSetNumber.getTidNumber()) == -1)
            iEndOfPrev = m_baPage.length;
        else
            iEndOfPrev = getSetEnd(getPrevSet(cSetNumber.getTidNumber()));

        int iEndOfThis = getSetEnd(cSetNumber.getTidNumber());
        int iEndOfData = getEndOfData();

        //we have to move some data that lies after the record
        if (iEndOfData < iEndOfThis) {
            Converter.moveBytes(m_baPage, iEndOfData,
                iEndOfThis, iEndOfPrev - iEndOfThis);

            //update all moved slots
            for (int iCnter = 0; iCnter < getSetSlotCount(); iCnter++) {
                int iTmp = getSetEnd(iCnter);

                if (iTmp != SLOT_FREE && iTmp < iEndOfThis) {
                    setSetEnd(iCnter, iTmp + iEndOfPrev - iEndOfThis);
                }
            }
        }

        setSetEnd(cSetNumber.getTidNumber(), SLOT_FREE);

        return;
    }

    //*********************************************************************************************************************
    public Set modifySet(SetNumber cSetNumber, int iDataSize) throws CodedException {

        if (iDataSize < 8) //sets can't be smaller than the redirection size!
            throw new CodedException(this, ERR_SET_TOO_SMALL, "The set is not big enough");

            //        if( isValidSetNumber( cSetNumber ) == false )
            //            throw new CodedException( this, ERR_INVALID_SET_NO, "The set number is not valid" );

        int iEndOfPrev;

        if (getPrevSet(cSetNumber.getTidNumber()) == -1)
            iEndOfPrev = m_baPage.length;
        else
            iEndOfPrev = getSetEnd(getPrevSet(cSetNumber.getTidNumber()));

        int iEndOfThis = getSetEnd(cSetNumber.getTidNumber());

        int iEndOfData = getEndOfData();

        //we have to move some data that lies after the record
        if (iEndOfData < iEndOfThis) {
            Converter.moveBytes(m_baPage, iEndOfData,
                iEndOfThis, iEndOfPrev - iEndOfThis - iDataSize);

            //update all moved slots
            for (int iCnter = cSetNumber.getTidNumber() + 1; iCnter < getSetSlotCount(); iCnter++) {
                int iTmp = getSetEnd(iCnter);

                if (iTmp != SLOT_FREE) {
                    setSetEnd(iCnter, iTmp + iEndOfPrev - iEndOfThis - iDataSize);
                }
            }
        }

        setSetEnd(cSetNumber.getTidNumber(), iEndOfPrev - iDataSize);

        return new Set(m_baPage, iEndOfPrev - iDataSize, iDataSize);
    }

    //*********************************************************************************************************************
    public void setRedirection(SetNumber cSetNumber, SetNumber cRedirectionSetNumber, int iDataLen) throws CodedException {

        //        if( isValidSetNumber( cSetNumber ) == false )
        //            throw new CodedException( this, ERR_INVALID_SET_NO, "The set number is not valid" );

        modifySet(cSetNumber, 8);

        int iEndOfPrev;

        if (getPrevSet(cSetNumber.getTidNumber()) == -1)
            iEndOfPrev = m_baPage.length;
        else
            iEndOfPrev = getSetEnd(getPrevSet(cSetNumber.getTidNumber()));

        Converter.moveBytes(m_baPage, iEndOfPrev - 8, cRedirectionSetNumber.getSetNumber(), 0, 6);
        Converter.shortToBytes(iDataLen, m_baPage, iEndOfPrev - 2);

        setRedirectionFlag(cSetNumber.getTidNumber(), true);
    }

    //*********************************************************************************************************************
    public int getRedirection(SetNumber cSetNumber, SetNumber cRedirectionSetNumber) throws CodedException {
        Set cSet = getSet(cSetNumber);
        SetNumber cSetNumTmp = new SetNumber(cSet.getBytes(), cSet.getOffset());

        cRedirectionSetNumber.copy(cSetNumTmp);
        return Converter.bytesToShort(cSet.getBytes(), 6 + cSet.getOffset());
    }

    //*********************************************************************************************************************
    private int getEndOfData() {
        for (int iCnter = getSetSlotCount() - 1; iCnter >= 0; iCnter--) {
            int iTmp = getSetEnd(iCnter);

            if (iTmp != SLOT_FREE)
                return iTmp;
        }

        return m_baPage.length;
    }

    //*********************************************************************************************************************
    public int getSetLength(SetNumber cSetNumber) throws CodedException {
        int iEndOfPrev;

        if (getPrevSet(cSetNumber.getTidNumber()) == -1)
            iEndOfPrev = m_baPage.length;
        else
            iEndOfPrev = getSetEnd(getPrevSet(cSetNumber.getTidNumber()));

        return iEndOfPrev - getSetEnd(cSetNumber.getTidNumber());
    }

    //*********************************************************************************************************************
    public int getFreeSpace() {
        int iSlotCount = getSetSlotCount();

        if (iSlotCount <= 0)
            return m_baPage.length - 6; //2 bytes for SlotCount and 2 for EndOfSet

        int iRet = getEndOfLast() - 6 - 2 * iSlotCount;

        if (iRet < 0)
            return 0;

        return iRet;
    }

    //*********************************************************************************************************************
    public int getSetSlotCount() {
        return Converter.bytesToShort(m_baPage, 0);
    }

    //*********************************************************************************************************************
    private void setSetSlotCount(int iCount) {
        Converter.shortToBytes(iCount, m_baPage, 0);
    }

    //*********************************************************************************************************************
    private int getFreeSetSlot() {

        /*for( int iCnter = 0; iCnter < getSetSlotCount(); iCnter ++ ){
         if( getSetEnd( iCnter ) == SLOT_FREE )
         return iCnter;
         }*/

        //if not found, return a new slot
        return getSetSlotCount();
    }

    //*********************************************************************************************************************
    private int getPrevSet(int iSetNum) {
        for (int iCnter = iSetNum - 1; iCnter >= 0; iCnter--) {
            if (getSetEnd(iCnter) != SLOT_FREE)
                return iCnter;
        }

        return -1;

        /*int iClosest = 4096;
         int iClosestNum = -1;
         int iMyEnd = getSetEnd( iSetNum );
         for( int iCnter = 0; iCnter < getSetSlotCount(); iCnter ++ ){
         int iEnd = getSetEnd( iCnter );
         if( iCnter == iSetNum ||  iEnd == SLOT_FREE )
         continue;

         if( iMyEnd < iEnd && iEnd < iClosest ){
         iClosestNum = iCnter;
         iClosest = iEnd;
         }
         }

         return iClosestNum;*/
    }

    //*********************************************************************************************************************
    public boolean getRedirectionFlag(int iIndex) {
        if ((m_baPage[ 3 + iIndex * 2 ] & (0x1 << 7)) != 0) //bit 1 set
            return true;
        else
            return false;
    }

    //*********************************************************************************************************************
    private void setRedirectionFlag(int iIndex, boolean bValue) {

        if (bValue) {
            m_baPage[ 3 + iIndex * 2 ] = (byte) ((0x1 << 7) | m_baPage[ 3 + iIndex * 2 ]);
        }else {
            m_baPage[ 3 + iIndex * 2 ] = (byte) (~(0x1 << 7) & m_baPage[ 3 + iIndex * 2 ]);
        }
    }

    //*********************************************************************************************************************
    private int getSetEnd(int iIndex) {
        return Converter.bytesToShort(m_baPage, 2 + iIndex * 2) & 0x7fff;
    }

    //*********************************************************************************************************************
    private void setSetEnd(int iIndex, int iValue) {

        boolean bRedirectionFlag = getRedirectionFlag(iIndex);

        Converter.shortToBytes(iValue, m_baPage, 2 + iIndex * 2);

        setRedirectionFlag(iIndex, bRedirectionFlag);

    }

    //*********************************************************************************************************************
    public boolean isValidSetNumber(SetNumber cSetNumber) throws CodedException {
        if (cSetNumber.getTidNumber() < 0)
            return false; // invalid

        if (cSetNumber.getTidNumber() > getSetSlotCount() - 1) {
            return false; // freed
        }

        if (getSetEnd(cSetNumber.getTidNumber()) == 0)
            return false; // unused

        return true;
    }

    //*********************************************************************************************************************
    private int getEndOfLast() {

        /*int iEndOfLast = m_baPage.length - 1;
         for( int iCnter = 0; iCnter < getSetSlotCount(); iCnter ++ ){
         if( iEndOfLast > getSetEnd( iCnter ) && getSetEnd( iCnter ) != SLOT_FREE )
         iEndOfLast = getSetEnd( iCnter );
         }
         return iEndOfLast;*/

        for (int iCnter = getSetSlotCount() - 1; iCnter >= 0; iCnter--) {
            if (getSetEnd(iCnter) != SLOT_FREE)
                return getSetEnd(iCnter);
        }
        return m_baPage.length;
    }
}
