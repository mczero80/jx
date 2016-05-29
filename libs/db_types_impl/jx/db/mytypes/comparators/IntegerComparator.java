/*
 * IntegerComparator.java
 *
 * Created on 19. Juli 2001, 00:08
 */

package jx.db.mytypes.comparators;


/**
 *
 * @author  ivanich
 * @version
 */
public class IntegerComparator implements jx.db.types.DbComparator {

    /** Creates new IntegerComparator */
    public IntegerComparator() {
    }

    public int compare(byte[] baFirstKey, byte[] baSecondKey) {
        return compare(baFirstKey, 0, baSecondKey, 0, 4);
    }

    public int compare(byte[] baFirstKey, int iFirstOffset, byte[] baSecondKey, int iSecondOffset, int iSize) {
        int iKey1 = jx.db.mytypes.Converter.bytesToInt(baFirstKey, iFirstOffset);
        int iKey2 = jx.db.mytypes.Converter.bytesToInt(baSecondKey, iSecondOffset);

        if (iKey1 < iKey2)
            return -1;
        else if (iKey1 > iKey2)
            return 1;
        else
            return 0;

    }

}
