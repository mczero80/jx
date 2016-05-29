package db.com.comparators;

import jx.db.types.DbComparator;

/**
 * Title:        Studienarbeit
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

public class CompoundComparator implements DbComparator {

    private int[]        m_aiSizes;
    private DbComparator[] m_acComparators;
    private int m_iKeySize = 0;

    public CompoundComparator(int[] aiSizes, DbComparator[] acComparators) {
        m_aiSizes = aiSizes;
        m_acComparators = acComparators;

        for (int iCnter = 0; iCnter < m_aiSizes.length; iCnter++)
            m_iKeySize += m_aiSizes[ iCnter ];
    }

    public int compare(byte[] baFirstKey, byte[] baSecondKey) {
        return compare(baFirstKey, 0, baSecondKey, 0, baFirstKey.length);
    }

    public int compare(byte[] baFirstKey, int iFirstOffset, byte[] baSecondKey, int iSecondOffset, int iSize/* iSize is not used! */) {
        int iOffset = 0;

        for (int iCnter = 0; iCnter < m_aiSizes.length; iCnter++) {
            int iRes = m_acComparators[ iCnter ].compare(baFirstKey, iOffset + iFirstOffset,
                    baSecondKey, iOffset + iSecondOffset,
                    m_aiSizes[ iCnter ]);

            if (iRes != 0)
                return iRes;

            iOffset += m_aiSizes[ iCnter ];
        }
        return 0;
    }

    public int getKeySize() {
        return m_iKeySize;
    }

    public int[] getSizes() {
        return m_aiSizes;
    }

    public DbComparator[] getComparators() {
        return m_acComparators;
    }
}
