/*
 * SetKeyComparator.java
 *
 * Created on 5. Juli 2001, 22:54
 */

package db.com.comparators;


import db.com.*;


/**
 *
 * @author  ivanich
 * @version
 */
public class SetKeyComparator implements db.com.Comparator {

    /** Creates new SetKeyComparator */
    public SetKeyComparator() {
    }

    public int compare(byte[] baFirstKey, byte[] baSecondKey) {
        return compare(baFirstKey, 0, baSecondKey, 0, 0/*irrelevant*/);
    }

    public int compare(byte[] baFirstKey, int iFirstOffset, byte[] baSecondKey, int iSecondOffset, int iSize) {

        int iType1 = Converter.bytesToShort(baFirstKey, iFirstOffset);
        int iType2 = Converter.bytesToShort(baSecondKey, iSecondOffset);

        if (iType1 < iType2) {
            return -1;
        }else if (iType1 > iType2) {
            return 1;
        }else {
            int iLen1 = Converter.bytesToShort(baFirstKey, 2 + iFirstOffset);
            int iLen2 = Converter.bytesToShort(baSecondKey, 2 + iSecondOffset);

            if (iLen1 < iLen2) {
                return -1;
            }else if (iLen1 > iLen2) {
                return 1;
            }else {
                int iPage1 = Converter.bytesToInt(baFirstKey, 4 + iFirstOffset);
                int iPage2 = Converter.bytesToInt(baSecondKey, 4 + iSecondOffset);

                if (iPage1 < iPage2)
                    return -1;
                else if (iPage1 > iPage2)
                    return 1;

                return 0; //equal!
            }
        }
    }

}
