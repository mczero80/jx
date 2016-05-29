package jx.fs;

public class EXT2Permission implements Permission {

    public static final int RWX   = 7;
    public static final int RX    = 3;
    public static final int RW    = 6;

    public static final int WRITE = 4;
    public static final int READ  = 2;    
    public static final int EXEC  = 1;

    public static final int USER  = 6;
    public static final int GROUP = 3;
    public static final int OTHER = 0;

    private int mode;
    
    public EXT2Permission(int user, int group, int other) {setMode(user,group,other);}
    
    public EXT2Permission(int mode) {setMode(mode);}

    public void addPermission(int field, int perm) {
	mode |= (perm & 0x07) << (field & 0x07);
    }

    public void delPermission(int field, int perm) {
	throw new Error("not impl.");
	//int flags = (perm & 0x07) << (field & 0x07);
	//mode = mode & !flags;
    }

    public int getPermission(int field) {
	return ( mode >> field ) & 0x07;
    }

    public void setMode(int user, int group, int other) {
	mode =  (other & 0x07) | 
	    ((group & 0x07) <<3) | 
	    ((user  & 0x07) <<6);
    }

    public void setMode(int mode) {
	this.mode = ( mode & 0x1ff);
    }
    
    public int getMode() {
	return mode;
    }
}
