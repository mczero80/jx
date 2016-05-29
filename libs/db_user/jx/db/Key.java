package jx.db;

public interface Key {

    public static final int ERR_INVALID_POS = 0;

    public int getFieldCount();
    public void getField( Object cDest, int iPos ) throws CodedException;
    public void setField( Object cSrc, int iPos ) throws CodedException;
    public byte[][] getBytes();
    public void setBytes( byte[] baSrc, int iOffset );

    public void setField( int iSrc, int iPos )throws CodedException;
    public int getField( int iPos ) throws CodedException;

    public void setFieldToMin(int iPos)throws CodedException;
    public void setFieldToMax(int iPos)throws CodedException;

}
