/*
 * Converter.java
 *
 * Created on 27. Juni 2001, 20:00
 */

package jx.db.mytypes;


/**
 *
 * @author  ivanich
 * @version
 */
public class Converter extends java.lang.Object {

    /** Creates new Converter - not allowed*/
    private Converter() {
    }

    public static int bytesToInt(byte[] baValue) {
        return bytesToInt(baValue, 0);
    }

    public static int bytesToInt(byte[] baValue, int iOffset) {
        int ret = ((int) baValue[ 0 + iOffset ] & 0xff) |
            ((int) (baValue[ 1 + iOffset ] << 8) & 0xff00) |
            ((int) (baValue[ 2 + iOffset ] << 16) & 0xff0000) |
            ((int) (baValue[ 3 + iOffset ] << 24) & 0xff000000);

        return ret;
    }

    public static void intToBytes(int iValue, byte[] baValue) {
        intToBytes(iValue, baValue, 0);
    }

    public static void intToBytes(int iValue, byte[] baValue, int iOffset) {
        baValue[ 0 + iOffset ] = (byte) (iValue & 0xff);
        baValue[ 1 + iOffset ] = (byte) ((iValue & 0xff00) >> 8);
        baValue[ 2 + iOffset ] = (byte) ((iValue & 0xff0000) >> 16);
        baValue[ 3 + iOffset ] = (byte) ((iValue & 0xff000000) >> 24);
    }

    public static int bytesToShort(byte[] baValue) {
        return bytesToShort(baValue, 0);
    }

    public static int bytesToShort(byte[] baValue, int iOffset) {
        int ret = ((int) baValue[ 0 + iOffset ] & 0xff) |
            ((int) (baValue[ 1 + iOffset ] << 8) & 0xff00);

        return ret;
    }

    public static void shortToBytes(int iValue, byte[] baValue) {
        shortToBytes(iValue, baValue, 0);
    }

    public static void shortToBytes(int iValue, byte[] baValue, int iOffset) {
        baValue[ 0 + iOffset ] = (byte) (iValue & 0xff);
        baValue[ 1 + iOffset ] = (byte) ((iValue & 0xff00) >> 8);
    }

    public static void moveBytes(byte[] baDest, byte[] baSrc) {
        int iLen = baDest.length;

        if (iLen > baSrc.length)
            iLen = baSrc.length;

        moveBytes(baDest, 0, baSrc, 0, iLen);
    }

    public static void moveBytes(byte[] baDest, int iDestOffset, byte[] baSrc, int iSrcOffset, int iSize) {
        System.arraycopy(baSrc, iSrcOffset, baDest, iDestOffset, iSize);
    }

    public static void moveBytes(byte[] baPage, int iStartByte, int iEndByte, int iOffset) {
        System.arraycopy(baPage, iStartByte, baPage, iStartByte + iOffset, iEndByte - iStartByte);
    }

    public static void stringToBytes(java.lang.String szString, byte[] baDest) {
        stringToBytes(szString, baDest, 0, baDest.length);
    }

    /*public static void stringToBytes(java.lang.String szString,byte[] baDest, int iOffset ) {
     byte[] baName = null;
     try{ baName = szString.getBytes("US-ASCII");}catch(Exception ex){ex.printStackTrace();}
     int iMinLen = baName.length;
     int iMaxLen = baName.length;

     if( iMinLen > baDest.length - iOffset )
     iMinLen = baDest.length - iOffset ;

     if( iMaxLen < baDest.length - iOffset )
     iMaxLen = baDest.length - iOffset;

     for( int iCnter = 0; iCnter < iMinLen; iCnter ++ )
     baDest[ iCnter + iOffset ] = baName[ iCnter ];

     for( int iCnter = iMinLen; iCnter < iMaxLen; iCnter ++ ){
     baDest[ iCnter + iOffset ] = (byte)(' ');
     }
     }*/

    public static void stringToBytes(java.lang.String szString, byte[] baDest, int iOffset, int iSize) {
        byte[] baName = null;

        try {
            baName = szString.getBytes();
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        int iMinLen = baName.length;
        int iMaxLen = baName.length;

        if (iMinLen > iSize)
            iMinLen = iSize;

        if (iMaxLen < iSize)
            iMaxLen = iSize;

        for (int iCnter = 0; iCnter < iMinLen; iCnter++)
            baDest[ iCnter + iOffset ] = baName[ iCnter ];

        for (int iCnter = iMinLen; iCnter < iMaxLen; iCnter++) {
            baDest[ iCnter + iOffset ] = (byte) (' ');
        }
    }

    public static String bytesToString(byte[] baValue) {
        return bytesToString(baValue, 0);
    }

    public static String bytesToString(byte[] baValue, int iOffset) {

        int iLen = iOffset;

        for (; (iLen < baValue.length) && (baValue[ iLen ] != (byte) ' '); iLen++);

        byte[] baShortenValue = new byte[ iLen - iOffset ];

        moveBytes(baShortenValue, 0, baValue, iOffset, baShortenValue.length);
        String szRet = null;

        try {
            szRet = new String(baShortenValue);
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        return szRet;
    }

}
