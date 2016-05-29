package jx.db;

/**
 * Title:        SA
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author Ivanich
 * @version 1.0
 */

public interface IndexInfo {
    public int[] getAttributeMap() throws CodedException;
    public boolean isUnique();
    public int getIID();
    public int getType();
}