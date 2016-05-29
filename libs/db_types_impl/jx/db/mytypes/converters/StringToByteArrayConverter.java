package jx.db.mytypes.converters;


import jx.db.CodedException;
import jx.db.types.DbConverter;


/**
 * Title:        Studienarbeit
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

public class StringToByteArrayConverter implements DbConverter {

    public StringToByteArrayConverter() {
    }

    public void convert(Object cSrc, int iSrcOffset, Object cDest, int iDestOffset, int iSize) throws CodedException {
        byte[] baString = ((String) cSrc).getBytes();

        if (baString.length < iSize) {
            System.arraycopy(baString, 0, cDest, iDestOffset, baString.length);
            ((byte[]) cDest)[ baString.length + iDestOffset ] = 0;
        }else {
            System.arraycopy(baString, 0, cDest, iDestOffset, iSize);
        }

    }

    public void revert(Object cSrc, int iSrcOffset, Object cDest, int iDestOffset, int iSize) throws CodedException {
        //byte[] baString = ((String)cSrc).getBytes();
        byte[] baString = new byte[ iSize ];

        System.arraycopy(cSrc, iSrcOffset, baString, 0, baString.length);

        String s = new String(baString);

        ((StringBuffer) cDest).append(s);
    }
}
