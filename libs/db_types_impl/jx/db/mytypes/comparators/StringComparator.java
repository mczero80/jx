/*
 * StringComparator.java
 *
 * Created on 30. Juni 2001, 17:37
 */

package jx.db.mytypes.comparators;


/**
 *
 * @author  ivanich
 * @version
 */
public class StringComparator implements jx.db.types.DbComparator {

    /** Creates new StringComparator */
    public StringComparator() {
    }

    public int compare(byte[] baFirstKey, byte[] baSecondKey) {
        int iLen = baFirstKey.length;

        if (iLen > baSecondKey.length)
            iLen = baSecondKey.length;

        return compare(baFirstKey, 0, baSecondKey, 0, iLen);
    }

    public int compare(byte[] baFirstKey, int iFirstOffset, byte[] baSecondKey, int iSecondOffset, int iSize) {
        try {
            String s1 = new String(baFirstKey, iFirstOffset, iSize);
            String s2 = new String(baSecondKey, iSecondOffset, iSize);

            return s1.compareTo(s2);
        }catch (Exception ex) {
            ex.printStackTrace();
        }

        return 0;
    }

}
