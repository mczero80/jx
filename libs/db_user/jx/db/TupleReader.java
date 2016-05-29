package jx.db;


/**
 * Title:        SA
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author Ivanich
 * @version 1.0
 */

public interface TupleReader extends jx.zero.Portal {

    /** returns a tuple descriptor for the tuple
     * @return tuple descriptor for the tuple
     */
    public TupleDescriptor getTd();
    /** returns the adress of the tuple
     * @return the adress of the tuple
     */
    public SetNumberReader getSetNumReader();
    /** reads the attribute on the iPos into the cDest Object. This function uses DbConverter and its subclasses to convert the bytes of the attribute into the proper form
     * @param iPos position of the attribute in the tuple
     * @param cDest destination object to save the data
     * @param iDestOffset offset in the destination object ( usefull for byte[] objects )
     * @throws CodedException thrown on error ( wrong datatype and casting )
     */
    public void getField(int iPos, Object cDest, int iDestOffset) throws CodedException;
    /** same as getField( int, Object, int ), but only for integers. The developer should add such methods for all used standard types.
     * @param iPos position of the attribute in the tuple ( number, not offset! )
     * @throws CodedException thrown on error
     * @return integer value of the attribute
     */
    public int getField(int iPos) throws CodedException;
    /** receives a byte array containing the whole tuple
     * @param baDest destination buffer to write the bytes
     * @param iOffset offset in the destination buffer
     * @throws CodedException thrown on error
     */
    public void getBytes( byte[] baDest, int iOffset ) throws CodedException;
    /** dumps the contents of the tuple on the console ( use for testing )
     * @throws CodedException thrown on error
     */
    public void dump() throws CodedException;
    /** closes the tuple and unfixes all fixed pages. Must be called always after use!
     * @throws CodedException thrown on error
     */
    public void close() throws CodedException;

}
