package db.list;


/**
 * Title:        Studienarbeit
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */
import db.com.*;
import jx.db.CodedException;

public class BiList {

    public static final int ERR_NOT_VALID_POS = 0;
    private static class BiListEntry {
        Object        m_cData;
        BiListEntry   m_cNext;
        BiListEntry   m_cPrev;
    }

    private BiListEntry m_cStart;
    private BiListEntry m_cCurr;
    private BiListEntry m_cEnd;

    public BiList() {               //creates an empty bilinked list
        m_cCurr = m_cStart = m_cEnd = null;
    }

    public boolean isEmpty() {
        return m_cStart == null;
    } // true, wenn Liste leer

    public void moveToFirst() {
        m_cCurr = m_cStart;
    }       // rueckt an den Anfang der Liste

    public void moveToEnd() {
        m_cCurr = m_cEnd;
    }       // rueckt an den Anfang der Liste

    public boolean moveToNext() {                             // rueckt in Liste vor
        if (m_cCurr == null || m_cCurr.m_cNext == null) return false;
        m_cCurr = m_cCurr.m_cNext;
        return true;
    }

    public boolean moveToPrev() {                             // rueckt in Liste vor
        if (m_cCurr == null || m_cCurr.m_cPrev == null) return false;
        m_cCurr = m_cCurr.m_cPrev;
        return true;
    }

    public Object getCurrent() throws CodedException {         // liefert Inhalt des aktuellen Eintrags
        if (m_cCurr == null) throw new CodedException(this, ERR_NOT_VALID_POS, "Position in list not valid!");
        return m_cCurr.m_cData;
    }

    public Object insertAfterCurrent(Object x) {             // fuegt ListenEintrag ein
        BiListEntry cNew = new BiListEntry();  // Das neue Listenelement

        cNew.m_cData = x;                    // kommt vor das aktuelle

        if (m_cCurr != null) {
            cNew.m_cNext = m_cCurr.m_cNext;
            m_cCurr.m_cNext = cNew;
            cNew.m_cPrev = m_cCurr;
            if (cNew.m_cNext != null)
                cNew.m_cNext.m_cPrev = cNew;
        }else {
            m_cCurr = cNew;
            cNew.m_cNext = null;
            cNew.m_cPrev = null;
        }

        if (m_cStart == null)
            m_cStart = m_cCurr;

        if (m_cEnd == null)
            m_cStart = m_cCurr;

        return cNew;
    }

    public Object insertAtStart(Object x) {
        BiListEntry cNew = new BiListEntry();  // Das neue Listenelement

        cNew.m_cData = x;                    // kommt vor das aktuelle

        cNew.m_cPrev = null;
        cNew.m_cNext = m_cStart;
        if (m_cStart == null) //empty
            m_cEnd = m_cCurr = cNew;
        else
            m_cStart.m_cPrev = cNew;

        m_cStart = cNew;

        return cNew;
    }

    public boolean removeCurrent() {                         // entfernt aktuelles Element
        if (m_cCurr == null) return false; //list empty

        BiListEntry cNextCurr = null;

        if (m_cCurr.m_cPrev != null) {
            cNextCurr = m_cCurr.m_cPrev;
            m_cCurr.m_cPrev.m_cNext = m_cCurr.m_cNext;
            if (m_cEnd == m_cCurr)
                m_cEnd = cNextCurr;
        }

        if (m_cCurr.m_cNext != null) {
            cNextCurr = m_cCurr.m_cNext;
            m_cCurr.m_cNext.m_cPrev = m_cCurr.m_cPrev;
            if (m_cStart == m_cCurr)
                m_cStart = cNextCurr;
        }

        m_cCurr = cNextCurr;

        if (m_cCurr == null) // only one elem was in list
            m_cStart = m_cEnd = null;

        return true;
    }

    public void setPos(Object cDirectRef) {
        m_cCurr = (BiListEntry) cDirectRef;
    }

    public Object getAtPos(Object cDirectRef) {
        return ((BiListEntry) cDirectRef).m_cData;
    }
}
