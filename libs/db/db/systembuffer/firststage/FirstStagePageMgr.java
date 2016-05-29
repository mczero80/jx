package db.systembuffer.firststage;


import jx.db.CodedException;

import db.systembuffer.PageManager;

    ////////////////////////////////////////////////////////////////////////////
    /**
     * this class represents a manager for free and used pages
     * @author Ivan Dedinski
     * @see db.systembuffer.PageBuffer
     * @see db.systembuffer.PageManager
     * @see db.systembuffer.firststage.FirstStageBuf
     */


class FirstStagePageMgr implements PageManager {

        private int m_iUsefulBits;
        private FirstStageBuf m_cBuffer;
        private int m_iBlocksPerPage;
        private int m_iPagesTotal;

        public FirstStagePageMgr( FirstStageBuf cBuffer, int iBlocksPerPage, int iPagesTotal ) {
            m_iBlocksPerPage = iBlocksPerPage;
            m_iPagesTotal = iPagesTotal;
            m_cBuffer = cBuffer;
            m_iUsefulBits = (512 * m_iBlocksPerPage - 2) * 8;
        }

        /**************************************************************************************************/
        public synchronized int getBlankPage() throws CodedException {

            for (int cnter1 = 0; cnter1 < m_iPagesTotal; cnter1 += m_iUsefulBits) {

                byte[] baPage = m_cBuffer.fixSync(cnter1);

                int iFreeCnt = (int) baPage[0] + ((int) baPage[ 1 ]) * 0xff;

                if (iFreeCnt >= m_iUsefulBits)
                    continue;

                for (int cnter2 = 16; cnter2 < m_iUsefulBits; cnter2++) {
                    if ((baPage[cnter2 / 8] & (byte) ((0x1 << (7 - cnter2 % 8)) & 0xff)) == 0) {
                        baPage[cnter2 / 8] |= (byte) ((0x1 << (7 - cnter2 % 8)) & 0xff);

                        iFreeCnt++;

                        baPage[0] = (byte) (iFreeCnt % 0xff);
                        baPage[1] = (byte) (iFreeCnt / 0xff);

                        m_cBuffer.setDirty(cnter1);
                        m_cBuffer.unfix(cnter1);

                        return cnter1 + cnter2 - 16;
                    }
                }
            }
            throw new CodedException(this, FirstStagePageMgr.ERR_NO_FREE_PAGE, null);
        }

        /*************************************************************************************************/
        public synchronized void freePage(int iPageNum) throws CodedException {

            if (iPageNum > (m_iPagesTotal - 1) || iPageNum < 0) {
                throw new CodedException(this, FirstStageBuf.ERR_INVALID_PAGE_NUM, "Invalid page number");
            }

            byte[] baPage = m_cBuffer.fixSync(iPageNum / m_iUsefulBits);

            int iPageIndex = iPageNum % m_iUsefulBits + 16;

            if ((baPage[iPageIndex / 8] & (byte) ((0x1 << (7 - iPageIndex % 8)) & 0xff)) != 0) {
                baPage[iPageIndex / 8] &= (byte) ((~(0x1 << (7 - iPageIndex % 8))) & 0xff);
                int iFreeCnt = (int) baPage[0] + ((int) baPage[1]) * 0xff;

                iFreeCnt--;
                baPage[0] = (byte) (iFreeCnt % 0xff);
                baPage[1] = (byte) (iFreeCnt / 0xff);
                m_cBuffer.setDirty(iPageNum / m_iUsefulBits);
            }else {
                throw new CodedException(this, FirstStageBuf.ERR_PAGE_FREED, "Trying to free an already freed page!");
            }

            m_cBuffer.unfix(iPageNum / m_iUsefulBits);
        }

        /*************************************************************************************************/
        public synchronized void formatMedia() throws CodedException {

            for (int cnter1 = 0; cnter1 < m_iPagesTotal; cnter1 += m_iUsefulBits) {

                byte[] baPage = m_cBuffer.fixSync(cnter1);

                baPage[ 0 ] = 1;
                baPage[ 1 ] = 0; //we have one allocated page at the beginning (this page)
                baPage[ 2 ] = (byte) 128;

                for (int cnter2 = 3; cnter2 < 512 * m_iBlocksPerPage; cnter2++)
                    baPage[cnter2] = (byte) 0;

                m_cBuffer.setDirty(cnter1);
                m_cBuffer.unfix(cnter1);
            }

        }

        /*************************************************************************************************/
        public synchronized boolean isPageUsed(int iPageNum) throws CodedException {

            if (iPageNum > (m_iPagesTotal - 1) || iPageNum < 0) {
                throw new CodedException(this, FirstStageBuf.ERR_INVALID_PAGE_NUM, "Invalid page number");
            }

            boolean bRet = false;
            byte[] baPage = m_cBuffer.fixSync(iPageNum / m_iUsefulBits);

            int iPageIndex = iPageNum % m_iUsefulBits + 16;

            if ((baPage[iPageIndex / 8] & (byte) ((0x1 << (7 - iPageIndex % 8)) & 0xff)) != 0) {
                bRet = true;
            }else
                bRet = false;

            m_cBuffer.unfix(iPageNum / m_iUsefulBits);

            return bRet;
        }

        /*************************************************************************************************/

}