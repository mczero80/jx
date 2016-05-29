package jx.rpcsvc.nfs2;

import jx.rpc.*;

// Beschreibung aus: RFC 1094 - NFS: Network File System - March 1989

//        The "fattr" structure contains the attributes of a file; "type" is
//        the type of the file; "nlink" is the number of hard links to the
//        file (the number of different names for the same file); "uid" is
//        the user identification number of the owner of the file; "gid" is
//        the group identification number of the group of the file; "size"
//        is the size in bytes of the file; "blocksize" is the size in bytes
//        of a block of the file; "rdev" is the device number of the file if
//        it is type NFCHR or NFBLK; "blocks" is the number of blocks the
//        file takes up on disk; "fsid" is the file system identifier for
//        the filesystem containing the file; "fileid" is a number that
//        uniquely identifies the file within its filesystem; "atime" is the
//        time when the file was last accessed for either read or write;
//        "mtime" is the time when the file data was last modified
//        (written); and "ctime" is the time when the status of the file was
//        last changed.  Writing to the file also changes "ctime" if the
//        size of the file changes.

//        "Mode" is the access mode encoded as a set of bits.  Notice that
//        the file type is specified both in the mode bits and in the file
//        type.  This is really a bug in the protocol and will be fixed in
//        future versions.  The descriptions given below specify the bit
//        positions using octal numbers.

//        0040000 This is a directory; "type" field should be NFDIR.
//        0020000 This is a character special file; "type" field should
//                be NFCHR.
//        0060000 This is a block special file; "type" field should be
//                NFBLK.
//        0100000 This is a regular file; "type" field should be NFREG.
//        0120000 This is a symbolic link file;  "type" field should be
//                NFLNK.
//        0140000 This is a named socket; "type" field should be NFNON.
//        0004000 Set user id on execution.
//        0002000 Set group id on execution.
//        0001000 Save swapped text even after use.
//        0000400 Read permission for owner.
//        0000200 Write permission for owner.
//        0000100 Execute and search permission for owner.
//        0000040 Read permission for group.
//        0000020 Write permission for group.
//        0000010 Execute and search permission for group.
//        0000004 Read permission for others.
//        0000002 Write permission for others.
//        0000001 Execute and search permission for others.

//        Notes:  The bits are the same as the mode bits returned by the
//        stat(2) system call in UNIX.  The file type is specified both in
//        the mode bits and in the file type.  This is fixed in future
//        versions.

//        The "rdev" field in the attributes structure is an operating
//        system specific device specifier.  It will be removed and
//        generalized in the next revision of the protocol.


public class FAttr implements RPCData {
    public static final int MODE_ROWNER  = 0000400;  /* Read permission for owner */
    public static final int MODE_WOWNER  = 0000200;  /* Write permission for owner */
    public static final int MODE_RWOWNER = 0000600;  /* Read/Write permission for owner */
    public static final int MODE_XOWNER  = 0000100;  /* Exec permission for owner */
    public static final int MODE_RGROUP  = 0000040;  /* Read permission for group */
    public static final int MODE_WGROUP  = 0000020;  /* Write permission for group */
    public static final int MODE_RWGROU  = 0000060;  /* Read/Write permission for group */
    public static final int MODE_XGROUP  = 0000010;  /* Exec permission for group */
    public static final int MODE_ROTHER  = 0000004;/* Read permission for other */
    public static final int MODE_WOTHER  = 0000002;  /* Write permission for other */
    public static final int MODE_RWOTHER = 0000006;  /* Read/Write permission for other */
    public static final int MODE_XOTHER  = 0000001;  /* Exec permission for other */
    public static final int MODE_DIR     = 0040000; /* directory */
    public static final int MODE_REGFILE = 0100000; /* regular file */
    public static final int MODE_SYMLINK = 0120000; /* symbolic link */
    
  /*
      0000100 Execute and search permission for owner.
      0000040 Read permission for group.
      0000020 Write permission for group.
      0000010 Execute and search permission for group.
      0000004 Read permission for others.
      0000002 Write permission for others.
      0000001 Execute and search permission for others.
      */

  public FType        type;
  public int mode;
  public int nlink;
  public int uid;
  public int gid;
  public int size;
  public int blocksize;
  public int rdev;
  public int blocks;
  public int fsid;
  public int fileid;
  public Timeval      atime;
  public Timeval      mtime;
  public Timeval      ctime;

  public FAttr() {}
}
