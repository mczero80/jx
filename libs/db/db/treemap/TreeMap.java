/*
 * TreeMap.java
 *
 * Created on 2. August 2001, 12:09
 */

package db.treemap;


import db.avltree.*;


/**
 *
 * @author  ivanich
 * @version
 */
public class TreeMap extends java.lang.Object {

    AVLBaum m_cAVL = null;
    int m_iCount = 0;

    /** Creates new TreeMap */
    public TreeMap() {
        m_cAVL = new AVLBaum();
    }

    public int getCount(){
	return m_iCount;
    }

    public void put(Object cKey, Object cValue) {
        try {
            m_cAVL.insert(new TreeMapEntry(cKey, cValue));
	    m_iCount ++;
        }catch (Exception ex) {
            ex.printStackTrace();
        }
       
    }

    public boolean remove(Object cKey) {
        try { 
	    m_iCount --;
            return m_cAVL.delete(new TreeMapEntry(cKey, null));
	}catch (Exception ex) {
            ex.printStackTrace();
        }

        return false;
    }

    public Object get(Object cKey) {
        try {
            return ((TreeMapEntry) m_cAVL.lookup(new TreeMapEntry(cKey, null))).getValue();
        }catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public Object firstKey() throws Exception {
        Baum b = m_cAVL;

        while (!b.right().empty()) {
            b = b.right();
        }

        return ((TreeMapEntry) (b.value())).getKey();
    }

    public Object lastKey() throws Exception {
        Baum b = m_cAVL;

        while (!b.left().empty()) {
            b = b.left();
        }

        return ((TreeMapEntry) (b.value())).getKey();
    }

    public boolean containsKey(Object cKey) {
        try {
            if (m_cAVL.lookup(new TreeMapEntry(cKey, null)) != null)
                return true;
            else
                return false;
        }catch (Exception ex) {
            ex.printStackTrace();
        }

        return false;
    }

    public boolean isEmpty() {
        try {
            return m_cAVL.empty();
        }catch (Exception ex) {
            ex.printStackTrace();
        }

        return true;
    }

    private class TreeMapEntry implements db.avltree.Comparable {
        Object m_cKey = null;
        Object m_cValue = null;

        public TreeMapEntry(Object cKey, Object cValue) {
            m_cKey = cKey;
            m_cValue = cValue;
        }

        public int hashCode() {
            return m_cKey.hashCode();
        }

        Object getValue() {
            return m_cValue;
        }

        Object getKey() {
            return m_cKey;
        }

        public int compareTo(db.avltree.Comparable a) {
            if (m_cKey.hashCode() < a.hashCode())
                return -1;
            else if (m_cKey.hashCode() > a.hashCode())
                return 1;
            else
                return 0;
        }
    }
}
