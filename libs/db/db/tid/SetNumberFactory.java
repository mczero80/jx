package db.tid;

import db.com.PoolableFactory;

/**
 * Title:        SA
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author Ivanich
 * @version 1.0
 */

public class SetNumberFactory implements PoolableFactory {

  public SetNumberFactory() {
  }

  public Object createPoolable() {
    return new SetNumber();
  }
}