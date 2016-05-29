package db.com;


/**
 * Title:        Studienarbeit
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

public class ObjHolderFactory implements PoolableFactory {

    public ObjHolderFactory() {
    }

    public Object createPoolable() {
        return new ObjHolder();
    }
}
