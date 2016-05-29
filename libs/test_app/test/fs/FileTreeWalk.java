package test.fs;

import jx.zero.*;
import jx.zero.debug.*;
import bioide.IDEDeviceImpl;
import bioram.BlockIORAM;
import jx.bio.BlockIO;
import javafs.*;
import jx.fs.FS;
import jx.fs.Inode;
import vfs.FSImpl;
import jx.fs.FSException;
import jx.zero.debug.*;
import java.util.*;
import test.ide.*;
import bioide.Drive;
import bioide.Partition;
import bioide.PartitionEntry;
import jx.timer.*;
import timerpc.SleepManagerImpl;
import timerpc.TimerManagerImpl;

public class FileTreeWalk {

    Naming naming;

    public static void init(Naming naming) {
	try {
	    if (Debug.out==null) {
		DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
		Debug.out = new DebugPrintStream(new DebugOutputStream(d));
	    }
	    new FileTreeWalk(naming);
	} catch(Exception e) {
	    e.printStackTrace();
	    throw new Error();
	}
    }

    public FileTreeWalk(Naming naming) throws FSException {
	this.naming = naming;

	FS fs = (FS) naming.lookup("FS");

	if (fs==null) {
	    if (Debug.out!=null) Debug.out.println("FS not found!");	    
	    return;
	}

	printDir(" ", fs.getCwdInode());

    }

    private void printDir(String space, Inode dirInode) throws FSException {
	String name = null;
	Inode inode;
	String[] names = dirInode.readdirNames();


	for (int i = 0; i < names.length; i++) {
	    name = names[i];
	    inode = dirInode.lookup(name);
	    Debug.out.print(space);
	    if (inode.isDirectory())
		Debug.out.print(" D");
	    else if (inode.isFile())
		Debug.out.print(" F");
	    else if (inode.isSymlink())
		Debug.out.print(" L");

	    Debug.out.print(" " + inode.getLength());
	    
	    inode.decUseCount(); 
	    
	    Debug.out.print("  \t\t"+name);

	    if (inode.isSymlink())
		Debug.out.print(" -> " + inode.getSymlink());
	    
	    Debug.out.println();
	}
	for (int i = 0; i < names.length; i++) {
	    name = names[i];
	    inode = dirInode.lookup(name);
	    if (! name.equals(".") && ! name.equals("..") && inode.isDirectory()) {
		Debug.out.println(space+"-------------");
		Debug.out.println(space+name+":");
		printDir(space + "  ", inode);
	    }
	    inode.decUseCount(); 
	}
    }

}
