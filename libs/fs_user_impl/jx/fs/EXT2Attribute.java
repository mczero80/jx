package jx.fs;

public class EXT2Attribute implements FSAttribute {

    private int id;
    private EXT2Permission perm;

    public EXT2Attribute(int user,EXT2Permission perm) {
	this.id = user;
	this.perm = perm;
    }

    public int getUserID() {
	return id;
    }

    public Permission getPermission() {
	return perm;
    }
}
