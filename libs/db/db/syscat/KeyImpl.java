package db.syscat;

import jx.db.types.DbConverter;
import db.com.Converter;

import jx.db.*;

/**
 * Title:        SA
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author Ivanich
 * @version 1.0
 */

public class KeyImpl implements Key {

  private DbConverter[] m_cConverters;
  private byte[][] m_baData;

    private byte[] minmax;

  protected KeyImpl( DbConverter[] cConverters, byte[][] baData ){
    m_cConverters = cConverters;
    m_baData = baData;
    minmax = new byte[m_cConverters.length];
  }

  public int getFieldCount(){
    return m_baData.length;
  }

  public void getField( Object cDest, int iPos ) throws CodedException{
      checkPos(iPos);
      m_cConverters[iPos].revert( m_baData[ iPos ], 0, cDest, 0, m_baData[ iPos ].length );
  }

  public void setField( Object cSrc, int iPos ) throws CodedException{
      checkPos(iPos);
      m_cConverters[iPos].convert( cSrc, 0, m_baData[ iPos ], 0, m_baData[ iPos ].length );
  }

  public void setField( int iSrc, int iPos ) throws CodedException {
      checkPos(iPos);
      Converter.intToBytes( iSrc, m_baData[ iPos ] );
  }

  public int getField( int iPos ) throws CodedException {
      checkPos(iPos);
      return Converter.bytesToInt( m_baData[ iPos ] );
  }

  public byte[][] getBytes(){
    return m_baData;
  }

  public void setBytes( byte[] baSrc, int iOffset ){
    int iDynOffset = 0;
    for( int iCnter = 0; iCnter < m_baData.length; iCnter ++ ){
      System.arraycopy( baSrc, iOffset + iDynOffset, m_baData[ iCnter ], 0,
                        m_baData[ iCnter ].length );
      iDynOffset += m_baData[ iCnter ].length;
    }
  }


    public void setFieldToMin(int iPos)throws CodedException {
	checkPos(iPos);
	minmax[iPos] = 1;
    }
    
    public void setFieldToMax(int iPos)throws CodedException {
	checkPos(iPos);
	minmax[iPos] = 2;
    }


    private void checkPos(int iPos)throws CodedException {
	if( iPos < 0 || iPos > m_baData.length )
	    throw new CodedException( this, ERR_INVALID_POS, "invalid position in key!" );
    }
}
