package jx.db.types;


public interface DbComparator {
    public int compare(byte[] baFirstKey, byte[] baSecondKey);
    public int compare(byte[] baFirstKey, int iFirstOffset, byte[] baSecondKey,
        int iSecondOffset, int iSize);
}
