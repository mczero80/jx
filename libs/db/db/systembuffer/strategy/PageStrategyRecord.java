package db.systembuffer.strategy;

/** this interface is used by the PageBuffer objects to obtain information about the paged in pages
 */
public interface PageStrategyRecord {    
    /** returns the fix counter of the requested page
     * @return fix counter of the requested page
     */    
    public int getFixCnt(); 
    /** returns the dirty flag of the requested page
     * @return dirty flag of the requested page
     */    
    public boolean getDirtyFlag();
    /** returns the page number of the page described in this {@link PageStrategyRecord} object
     * @return page number of the page described in this {@link PageStrategyRecord} object
     */    
    public int getPageNum();
    /** returns a byte array containing the page data
     * @return byte array containing the page data
     */    
    public byte[] getPage();
}
