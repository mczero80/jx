package db.list;


/*****************************  Liste.java  ***********************************/
//import AlgoTools.IO;
/** ADT Liste mit empty, endpos, reset, advance, elem, insert, delete         */
import db.com.*;
import jx.db.CodedException;


public class List {
    public static final int ERR_NOT_VALID_POS = 0;
    private static class ListenEintrag {
        Object        m_cInhalt;      // Inhalt des ListenEintrags
        ListenEintrag m_cNext;        // zeigt auf naechsten ListenEintrag
    }

    private ListenEintrag m_cStart;    // zeigt auf nullten ListenEintrag
    private ListenEintrag m_cCurr;     // zeigt auf ListenEintrag vor dem aktuellen

    public List() {               // kreiert eine leere Liste
        m_cCurr = m_cStart = null;
    }

    public boolean isEmpty() {
        return m_cStart == null;
    } // true, wenn Liste leer

    public boolean moveToFirst() {
        m_cCurr = m_cStart;
        return !isEmpty();
    }       // rueckt an den Anfang der Liste

    public boolean moveToNext() {                             // rueckt in Liste vor
        if (isEmpty() || m_cCurr.m_cNext == null) return false;

        m_cCurr = m_cCurr.m_cNext;

        return true;
    }

    public Object getCurrent() throws CodedException {         // liefert Inhalt des aktuellen Eintrags
        if (m_cCurr == null) throw new CodedException(this, ERR_NOT_VALID_POS, "Position in list not valid!");
        return m_cCurr.m_cInhalt;
    }

    public Object insert(Object x) {             // fuegt ListenEintrag ein
        ListenEintrag cNew = new ListenEintrag();  // Das neue Listenelement

        cNew.m_cInhalt = x;                    // kommt vor das aktuelle

        if (m_cCurr != null) {
            cNew.m_cNext = m_cCurr.m_cNext;
            m_cCurr.m_cNext = cNew;
            m_cCurr = cNew;
        }else {
            m_cCurr = cNew;
            cNew.m_cNext = null;
        }

        if (m_cStart == null)
            m_cStart = m_cCurr;

        return cNew;
    }

    /*public boolean removeCurrent() {                         // entfernt aktuelles Element
        if (m_cCurr == null) return false; //list empty
        if (m_cStart == m_cCurr)
            m_cStart = m_cCurr.m_cNext;
        m_cCurr = m_cCurr.m_cNext;

        return true;
    }*/

    public void setPos(Object cDirectRef) {
        m_cCurr = (ListenEintrag) cDirectRef;
    }

    public Object getAtPos(Object cDirectRef) {
        return ((ListenEintrag) cDirectRef).m_cInhalt;
    }

    public int getCount(){
      if( !moveToFirst())
          return 0;

      int iCount = 1;

      while( moveToNext())
        iCount ++;

      return iCount;

    }
}
