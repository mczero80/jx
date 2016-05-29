package jx.db;


/**
 * Title:        SA
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author Ivanich
 * @version 1.0
 */

public interface TupleWriter extends jx.zero.Portal {
    /** sets the contents of the attribute on the given iPos
     * @param iPos position of the attribute in the tuple
     * @param cSrc source object
     * @param iSrcOffset offset in the source object ( usefull for byte arrays )
     * @throws CodedException thrown on error
     */
    public void setField(int iPos, Object cSrc, int iSrcOffset) throws CodedException;
    /** the same as setField( int, Object, int ), but only for integers
     * @param iPos position of the attribute in the tuple( not offset, but number )
     * @param iSrc source number
     * @throws CodedException thrown on error
     */
    public void setField(int iPos, int iSrc)                    throws CodedException;
    /** closes the tuple and unfixes the page that contains it.Should be called always after using the tuple.
     * @throws CodedException thrown on error
     */

    public TupleReader getReader();

    public void close()                                         throws CodedException;
}
