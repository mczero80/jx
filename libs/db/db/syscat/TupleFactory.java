package db.syscat;

import db.com.PoolableFactory;

/**
 * Title:        SA
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author Ivanich
 * @version 1.0
 */

public class TupleFactory implements PoolableFactory {

  protected TupleFactory() {
  }
  public Object createPoolable() {
    return new Tuple();
  }
}