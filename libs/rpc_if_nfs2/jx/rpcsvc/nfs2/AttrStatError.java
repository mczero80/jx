package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class AttrStatError extends AttrStat {
    public static final int NFS_OK = 0;
    public static final int NFSERR_PERM=1;
    public static final int NFSERR_NOENT=2;
    public static final int NFSERR_IO=5;
    public static final int NFSERR_NXIO=6;
    public static final int NFSERR_ACCES=13;
    public static final int NFSERR_EXIST=17;
    public static final int NFSERR_NODEV=19;
    public static final int NFSERR_NOTDIR=20;
    public static final int NFSERR_ISDIR=21;
    public static final int NFSERR_FBIG=27;
    public static final int NFSERR_NOSPC=28;
    public static final int NFSERR_ROFS=30;
    public static final int NFSERR_NAMETOOLONG=63;
    public static final int NFSERR_NOTEMPTY=66;
    public static final int NFSERR_DQUOT=69;
    public static final int NFSERR_STALE=70;
    public static final int NFSERR_WFLUSH=99;

    public AttrStatError() { super(AttrStat.SWITCHDEFAULT_AttrStatError); }
    public AttrStatError(int err) { super(err); }
}
