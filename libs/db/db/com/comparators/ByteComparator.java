/*
 * ByteComparator.java
 *
 * Created on 30. Juni 2001, 17:29
 */

package db.com.comparators;


/**
 *
 * @author  ivanich
 * @version
 */
public class ByteComparator implements jx.db.types.DbComparator {

    /** Creates new ByteComparator */
    public ByteComparator() {
    }

    /** compares two byte arrays
     * @param baFirstKey first key to compare
     * @param baSecondKey second key to compare
     * @return x < 0 if key1 < key2, x > 0 if key1 > 2, x = 0 if key1 == key2
     */
    public int compare(byte[] baFirstKey, byte[] baSecondKey) {
        int iLen = baFirstKey.length;

        if (iLen > baSecondKey.length)
            iLen = baSecondKey.length;

        return compare(baFirstKey, 0, baSecondKey, 0, iLen);
    }

    public int compare(byte[] baFirstKey, int iFirstOffset, byte[] baSecondKey, int iSecondOffset, int iSize) {
        for (int iCnter = 0; iCnter < iSize; iCnter++) {
            if (baFirstKey[ iCnter + iFirstOffset ] < baSecondKey[ iCnter + iSecondOffset ])
                return -1;
            else if (baFirstKey[ iCnter + iFirstOffset ] > baSecondKey[ iCnter + iSecondOffset ])
                return  1;
        }
        return 0;
    }
}
