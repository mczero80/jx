package db.com;


public interface Comparator {
    public int compare(byte[] baFirstKey, byte[] baSecondKey);
    public int compare(byte[] baFirstKey, int iFirstOffset, byte[] baSecondKey, 
        int iSecondOffset, int iSize);
}
