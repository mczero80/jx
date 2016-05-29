package jx.rpcsvc.nfs2;

import jx.rpc.*;


/*       
         NFSERR_PERM
            Not owner.  The caller does not have correct ownership to perform
            the requested operation.
       
         NFSERR_NOENT
            No such file or directory.  The file or directory specified does
            not exist.
       
         NFSERR_IO
            Some sort of hard error occurred when the operation was in
            progress.  This could be a disk error, for example.
       
         NFSERR_NXIO
            No such device or address.
       
         NFSERR_ACCES
            Permission denied.  The caller does not have the correct
            permission to perform the requested operation.
       
         NFSERR_EXIST
            File exists.  The file specified already exists.
       
         NFSERR_NODEV
            No such device.
       
         NFSERR_NOTDIR
            Not a directory.  The caller specified a non-directory in a
            directory operation.
       
         NFSERR_ISDIR
            Is a directory.  The caller specified a directory in a non-
            directory operation.
       
         NFSERR_FBIG
            File too large.  The operation caused a file to grow beyond the
            server's limit.
       
         NFSERR_NOSPC
            No space left on device.  The operation caused the server's
            filesystem to reach its limit.
       
         NFSERR_ROFS
            Read-only filesystem.  Write attempted on a read-only filesystem.
       
         NFSERR_NAMETOOLONG
            File name too long.  The file name in an operation was too long.
       
         NFSERR_NOTEMPTY
            Directory not empty.  Attempted to remove a directory that was not
            empty.
       
         NFSERR_DQUOT
            Disk quota exceeded.  The client's disk quota on the server has
            been exceeded.
       
         NFSERR_STALE
            The "fhandle" given in the arguments was invalid.  That is, the
            file referred to by that file handle no longer exists, or access
            to it has been revoked.
       
         NFSERR_WFLUSH
            The server's write cache used in the "WRITECACHE" call got flushed
            to disk.
*/

public class Stat implements RPCData,RPCEnum {
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
  
  public int status;

    public Stat() {
	status = NFS_OK;
    }
    public Stat(int status) {
	this.status = status;
    }
}
