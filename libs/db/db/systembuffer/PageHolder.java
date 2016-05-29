package db.systembuffer;


public class PageHolder {
    byte[] m_baPage = null;
    public PageHolder(byte[] page) {
        m_baPage = page;
    }

    public byte[] getBytes() {
        return m_baPage;
    }
}
