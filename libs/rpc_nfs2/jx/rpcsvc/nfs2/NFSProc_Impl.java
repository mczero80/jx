package jx.rpcsvc.nfs2;

import jx.rpc.*;
import jx.zero.*;
import jx.fs.FS;
import jx.fs.*;
import java.util.Vector;





public class NFSProc_Impl implements NFSProc {
    CPUManager cpuManager;
    int event_getattr_in;
    int event_getattr_out;
    FS fs;
    Inode[] inodes = new Inode[1024];
    //byte[][] handles = new byte[1024][];
    //byte[]  rootHandle;
    //int nHandles;
    int epoch;
    private static final boolean production=true; 
    private boolean fireProfiler=true;
    Inode rootInode;
    MemoryManager memMgr = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
    Memory readbuffer = memMgr.alloc(4096);

    private static final boolean debug_nfs = false;

    Profiler profiler;

    // Pseudo-Caches:
    // Attr-Cache
    boolean useAttrCache = true;
    boolean debugAttrCache = false;
    int attrCache_lastFhIdentifier;
    FAttr attrCache_lastFAttr;
    int attrCache_cacheMisses = 0;
    int attrCache_cacheHits = 0;
    // Lookup-Cache
    boolean useLookupCache = false;
    boolean debugLookupCache = false;
    LookupCache lookupCache;


    MappedFHandle helperMFh;
    MappedFHandle helperDirMFh;



    public NFSProc_Impl(FS fs) {

	cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");	
	event_getattr_in = cpuManager.createNewEvent("NFSGetattrIn");
	event_getattr_out = cpuManager.createNewEvent("NFSGetattrOut");
	this.fs = fs;
	Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");
	this.epoch = clock.getTicks_low();
	rootInode=null;
	try {
	    rootInode = fs.lookup("/");
	} catch(Exception e) {
	    throw new Error(); 
	}
	profiler = 
	    ((Profiler)InitialNaming.getInitialNaming().lookup("Profiler")); 

// 	this.lookupCache = new LookupCache_firstImpl(debugLookupCache);
	this.lookupCache = new LookupCache_2ndImpl(debugLookupCache);
	/*
	  rootHandle = getOrCreateHandle(rootInode);
	  rootHandle = 0;
	  handles[0] = new byte[32];
	  handles[rootHandle][0] = (byte)rootHandle;
	  try {
	  inodes[rootHandle] = fs.lookup("/");
	  } catch(Exception e) {throw new Error(); }
	*/
	//nHandles = 1;
	
	helperMFh = new MappedFHandle();
	helperDirMFh = new MappedFHandle();
    }




    public void nullproc() {
    }

    public AttrStat getattr(FHandle fh0) { 
	cpuManager.recordEvent(event_getattr_in);

	if (helperMFh == null) helperMFh = new MappedFHandle();
	if (helperDirMFh == null) helperDirMFh = new MappedFHandle();

	helperMFh.renew(fh0.data);
	if (debug_nfs) 
	    Debug.out.println("GETATTR: fh="+helperMFh.deviceIdentifier+"." + 
			      helperMFh.identifier + "." +
			      helperMFh.generation);

	if (! production) {
	    if (fireProfiler) {
		// ***** START PROFILING *****	
		fireProfiler = false;
		Profiler profiler = 
		    ((Profiler)InitialNaming.getInitialNaming().lookup("Profiler"));
		profiler.startSampling();
	    }
	}


	try{
	    FAttr a = getFAttr(helperMFh);
	    if (debug_nfs) Debug.out.println("size: "+a.size);

	    cpuManager.recordEvent(event_getattr_out);
	    
// 	    Profiler profiler = 
// 		((Profiler)InitialNaming.getInitialNaming().lookup("Profiler"));
// 	    profiler.shell();

	    return new AttrStatOK(a);

	} catch (InodeIOException e) {
	    Debug.out.println("----------------------------> Exception (InodeIOException) in GETATTR");
	    cpuManager.recordEvent(event_getattr_out);
	    return new AttrStatErrIO();
	} catch (InodeNotFoundException e) {
	    Debug.out.println("----------------------------> Exception (InodeNotFoundException) in GETATTR");
	    cpuManager.recordEvent(event_getattr_out);
	    return new AttrStatErrNoEnt();
	} catch (NoDirectoryInodeException e) {
	    Debug.out.println("----------------------------> Exception (NoDirectoryInodeException) in GETATTR");
	    cpuManager.recordEvent(event_getattr_out);
	    return new AttrStatErrNotDir();
	} catch (NotExistException e) {
	    Debug.out.println("----------------------------> Exception (NotExistException) in GETATTR");
	    cpuManager.recordEvent(event_getattr_out);
	    return new AttrStatErrNoEnt();
	} catch (PermissionException e) {
	    Debug.out.println("----------------------------> Exception (PermissionException) in GETATTR");
	    cpuManager.recordEvent(event_getattr_out);
	    return new AttrStatErrAcces();
	} catch (FSException e) {
	    Debug.out.println("----------------------------> Exception (FSException) in GETATTR");
	    cpuManager.recordEvent(event_getattr_out);
	    return new AttrStatErrIO();
	} catch (StaleHandleException e) {
	    Debug.out.println("----------------------------> Exception (StaleHandleException) in GETATTR");
	    cpuManager.recordEvent(event_getattr_out);
	    return new AttrStatErrStale();
	}

    }

    

    // ToDo:
    // - siehe setAttr()
    public AttrStat setattr(FHandle fh0, SAttr attributes) { 

	Inode inode;	


	if (helperMFh == null) helperMFh = new MappedFHandle();
	if (helperDirMFh == null) helperDirMFh = new MappedFHandle();

	helperMFh.renew(fh0.data);
	if (debug_nfs) 
	    Debug.out.println("SETATTR: fh="+helperMFh.deviceIdentifier+"." + 
			      helperMFh.identifier + "." +
			      helperMFh.generation);

	
	try{

	    inode = this.getInode(helperMFh);
	    FAttr a = setAttr(inode, helperMFh, attributes);
	    return new AttrStatOK(a);


	} catch (InodeIOException e) {
	    cpuManager.recordEvent(event_getattr_out);
	    return new AttrStatErrIO();
	} catch (InodeNotFoundException e) {
	    cpuManager.recordEvent(event_getattr_out);
	    return new AttrStatErrNoEnt();
	} catch (NoDirectoryInodeException e) {
	    cpuManager.recordEvent(event_getattr_out);
	    return new AttrStatErrNotDir();
	} catch (NotExistException e) {
	    cpuManager.recordEvent(event_getattr_out);
	    return new AttrStatErrNoEnt();
	} catch (PermissionException e) {
	    cpuManager.recordEvent(event_getattr_out);
	    return new AttrStatErrAcces();
	} catch (FSException e) {
	    cpuManager.recordEvent(event_getattr_out);
	    return new AttrStatErrIO();
	} catch (StaleHandleException e) {
	    cpuManager.recordEvent(event_getattr_out);
	    return new AttrStatErrStale();
	}


    }



    public void root() { 
	throw new Error(); 
    }



    // NFS_LOOKUP
    // ToDo:
    // - 
    public DirOpRes    lookup(FHandle  dir0, Name name) {

	Inode inode;
	Inode dirInode;

	if (helperMFh == null) helperMFh = new MappedFHandle();
	if (helperDirMFh == null) helperDirMFh = new MappedFHandle();

	helperDirMFh.renew(dir0.data);
	if (debug_nfs) 
	    Debug.out.println("LOOKUP in fh="+helperDirMFh.deviceIdentifier+"." + 
			      helperDirMFh.identifier + "." +
			      helperDirMFh.generation + 
			  ", suche nach: " + name.data);

 	try {

	    //FAttr a;

	    LookupCache_Result res;
	    
	    // Falls der LookupCache verwendet werden soll...
	    if (useLookupCache) {

		res = lookupCache.getEntry(name.data, helperDirMFh.identifier);

		// Falls kein Eintrag im Cache gefunden wurde...
		if (res == null) {
		    dirInode = this.getInode(helperDirMFh);
		    if (dirInode == null) return new DirOpResNoSuchFile();
		    inode = dirInode.lookup(name.data);
		    
		    if (inode != null) {
			helperMFh = new MappedFHandle(helperDirMFh.deviceIdentifier, 
					       inode.getIdentifier(), 
					       inode.getVersion());
		    } else {
			helperMFh = null;
		    }
// 		    if (name.data == null) Debug.out.println("name.data ist null!!");
// 		    if (helperMFh == null) Debug.out.println("helperMFh ist null!!");
		    lookupCache.addEntry(name.data, helperDirMFh.identifier, helperMFh);
		    
		} else {
		    helperMFh = res.fh;
		}
		if (debugLookupCache) lookupCache.printStatistics();
	    }
	    // Falls ohne LookupCache gearbeitet werden soll...
	    else {
		dirInode = this.getInode(helperDirMFh);
		inode = dirInode.lookup(name.data);
		
		if (inode != null) {
		    helperMFh = new MappedFHandle(helperDirMFh.deviceIdentifier, 
					   inode.getIdentifier(), 
					   inode.getVersion());
		} else {
		    helperMFh = null;
		}
	    }	
	    
	    if (helperMFh == null) return new DirOpResNoSuchFile();
	    if (debug_nfs) Debug.out.println("gefunden: "+helperMFh.identifier);

	    return new DirOpResOK(helperMFh.getFHandle(), getFAttr(helperMFh));

	    
	} catch (InodeIOException e) {
// 	    Debug.out.println("----------------------------> Exception (InodeIOException) in LOOKUP");
	    return new DirOpResErrIO();
	} catch (InodeNotFoundException e) {
// 	    Debug.out.println("----------------------------> Exception (InodeNotFoundException) in LOOKUP");
	    return new DirOpResNoSuchFile();
	} catch (NoDirectoryInodeException e) {
// 	    Debug.out.println("----------------------------> Exception (NoDirectoryInodeException) in LOOKUP");
	    return new DirOpResErrNotDir();
	} catch (NotExistException e) {
// 	    Debug.out.println("----------------------------> Exception (NotExistException) in LOOKUP");
	    return new DirOpResErrStale();
	} catch (PermissionException e) {
// 	    Debug.out.println("----------------------------> Exception (PermissionException) in LOOKUP");
	    return new DirOpResErrAcces();
	} catch (FSException e) {
// 	    Debug.out.println("----------------------------> Exception (FSException) in LOOKUP");
	    return new DirOpResErrIO();
	} catch (StaleHandleException e) {
// 	    Debug.out.println("----------------------------> Exception (StaleHandleException) in LOOKUP");
	    return new DirOpResErrStale();
	}
	
    }



    // NFS_READLINK
    public ReadLinkRes readlink(FHandle a0) {

	Inode symlinkInode;
       
	if (helperMFh == null) helperMFh = new MappedFHandle();
	if (helperDirMFh == null) helperDirMFh = new MappedFHandle();

	helperMFh.renew(a0.data);
	if (debug_nfs) 
	    Debug.out.println("READLINK: fh="+helperMFh.deviceIdentifier+"." + 
			      helperMFh.identifier + "." +
			      helperMFh.generation);

       
  	try {
	    symlinkInode = this.getInode(helperMFh);
	    if (symlinkInode == null) return new ReadLinkResError(Stat.NFSERR_STALE);
	    
	    //TODO: Wenn es kein Symlink ist, welcher Fehler soll dann zurückgegeben werden?
	    if (!symlinkInode.isSymlink()) return new ReadLinkResError(Stat.NFSERR_PERM);
	    
	   
	    String symlinkPath = symlinkInode.getSymlink();
		
	    
	    return new ReadLinkResOK(new DirPath(symlinkPath));

 	} catch (NotExistException e) {
 	    if (debug_nfs) Debug.out.println("Fehler in NFS_SYMLINK");
 	    return new ReadLinkResError(Stat.NFSERR_NOENT);
 	} catch (FSException e) {
	    return new ReadLinkResError(Stat.NFSERR_IO);
	} catch (StaleHandleException e) {
	    cpuManager.recordEvent(event_getattr_out);
	    return new ReadLinkResErrStale();
	}
	
    }



    // NFS_READ
    public ReadRes read(FHandle fh0, int offset, int count, int totalcount) {

	if (helperMFh == null) helperMFh = new MappedFHandle();
	if (helperDirMFh == null) helperDirMFh = new MappedFHandle();


	helperMFh.renew(fh0.data);
	Inode inode;

	if (debug_nfs) 
	    Debug.out.println("READ (fh=" + helperMFh.deviceIdentifier + "." +
			      helperMFh.identifier  + "." +
			      helperMFh.generation + ") " +
			      "offset="+offset + 
			      ", count="+count+
			      ", totalcount="+totalcount);

	try{

	    inode = this.getInode(helperMFh);

	    if (inode == null) {
		if (debug_nfs) Debug.out.println("NFS_READ ERR");
		return new ReadResError();
	    }
	    
	    if (count > readbuffer.size()) {
		//Debug.out.println("count="+count+",readbuffer.size="+readbuffer.size());
		//throw new Error();
		return new ReadResErrNoEnt();
	    }

	    int nread = inode.read(readbuffer, offset, count);
	    
	    byte[] readresult = new byte[nread];
	    readbuffer.copyToByteArray(readresult, 0, 0, nread);
	    
	    return new ReadResOK(getFAttr(helperMFh),new NFSData(readresult));

	} catch (StaleHandleException e) {
	    cpuManager.recordEvent(event_getattr_out);
	    return new ReadResErrStale();
	} catch (Exception e) {
	    if (debug_nfs) Debug.out.println("Fehler in NFS_READ");
	    return new ReadResError();
	} 
    }



    public void writeCache() {
	throw new Error();
    }



    // NFS_WRITE
    // ToDo:
    // - atomic
    public AttrStat write(FHandle fh0, int beginoffset, int offset, int totalcount, NFSData data) {

	if (helperMFh == null) helperMFh = new MappedFHandle();
	if (helperDirMFh == null) helperDirMFh = new MappedFHandle();

	helperMFh.renew(fh0.data);
	Inode inode;

	if (debug_nfs) 
	    Debug.out.println("WRITE (fh=" + helperMFh.deviceIdentifier + "." +
			      helperMFh.identifier  + "." +
			      helperMFh.generation + ") " +
			      "offset="+offset + 
			      ", beginoffset="+beginoffset+
			      ", totalcount="+totalcount);


	try {
	    inode = this.getInode(helperMFh);

	    if (inode != null) {
		MemoryManager memMgr = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
		Memory buffer = memMgr.alloc(data.data.length); // 100 blocks are 2-indirect
		
		try{
		    byte[] writeArray = data.data;
		    buffer.copyFromByteArray(writeArray, 0, 0, writeArray.length);
		    
		    inode.write(buffer, offset, writeArray.length);
		    inode.setLastAccessed(inode.lastAccessed()+10000);
		    inode.setLastModified(inode.lastModified()+10000);
		    
		} catch (Exception e) {
		    if (debug_nfs) Debug.out.println("Fehler in NFS_WRITE");
		    return new AttrStatError();
		}
	    } else {
		if (debug_nfs) Debug.out.println("NFS_WRITE ERR");
		return new AttrStatError();
	    }

	    // Cache löschen TODO: besser machen!
	    attrCache_lastFhIdentifier = 0;
	    attrCache_lastFAttr = null;

	    return new AttrStatOK(getFAttr(helperMFh));
	    
	} catch (StaleHandleException e) {
	    return new AttrStatErrStale();
	} catch (Exception e) {
	    if (debug_nfs) Debug.out.println("Fehler beim Aufruf von NFSwrite(fh,offset,count,totalcount)");
	    return new AttrStatError();
	}
	
    }



    // NFS_CREATE
    // ToDo:
    // - SAttr muss noch ausgewertet werden (z.B. Verzeichnis oder Datei anlegen?)
    // - die Groesse der Datei festlegen in SAttr???
    public DirOpRes create(FHandle  dir0, Name name, SAttr attributes) {

	if (helperMFh == null) helperMFh = new MappedFHandle();
	if (helperDirMFh == null) helperDirMFh = new MappedFHandle();

	helperDirMFh.renew(dir0.data);
	Inode dirInode;

 	if (debug_nfs) 
	    Debug.out.println("CREATE in fh="+helperDirMFh.deviceIdentifier + "." +
			      helperDirMFh.identifier + "." +
			      helperDirMFh.generation + 
			      ", name="+name.data);

 	try {
	    dirInode = this.getInode(helperDirMFh);

	    if (dirInode == null) return new DirOpResError();
	    
	    if (dirInode.isDirectory()) {
		Inode inode;
		try {
		    inode = dirInode.create(name.data, InodeImpl.S_IWUSR|InodeImpl.S_IRUGO);
		} catch(FileExistsException e) {
		    inode = dirInode.lookup(name.data);
		}
		if (debug_nfs) 
		    Debug.out.println("... new inode number: " + 
				      inode.getIdentifier());

		helperMFh.renew(helperDirMFh.deviceIdentifier, 
				inode.getIdentifier(), 
				inode.getVersion());

		lookupCache.addEntry(name.data, helperDirMFh.identifier,helperMFh);
		FAttr a = getFAttr(helperMFh);

		return new DirOpResOK(helperMFh.getFHandle(), a);
	    } else {
		return new DirOpResError();
	    }
	} catch (StaleHandleException e) {
	    cpuManager.recordEvent(event_getattr_out);
	    return new DirOpResErrStale();
	} catch (FileExistsException e) {
	    return new DirOpResErrExist();
	} catch (NotExistException e) {
	    Debug.out.println("NFS_CREATE: Inode existiert nicht.");
	    return new DirOpResError();
	} catch (InodeIOException e) {
	    return new DirOpResErrIO();
	} catch (NoDirectoryInodeException e) {
	    Debug.out.println("NFS_CREATE: Verzeichnis-Inode existiert nicht.");
	    return new DirOpResError();
	} catch (InodeNotFoundException e) {
	    Debug.out.println("NFS_CREATE: Inode nicht gefunden.");
	    return new DirOpResError();
	} catch (FSException e) {
	    return new DirOpResError();
	}
// 	catch (Exception e) {
// 	    if (debug_nfs) Debug.out.println("Fehler in NFS_CREATE");
// 	    e.printStackTrace();
// 	    return new DirOpResError();
// 	}
	
    }



    // NFS_REMOVE
    // ToDo:
    // - Rueckgabewerte im Fehlerfall!
    public Stat remove(FHandle  dir0, Name name) {

	if (helperMFh == null) helperMFh = new MappedFHandle();
	if (helperDirMFh == null) helperDirMFh = new MappedFHandle();

	helperDirMFh.renew(dir0.data);
	Inode dirInode;

	if (debug_nfs) 
	    Debug.out.println("REMOVE fh="+helperDirMFh.deviceIdentifier + "." +
			      helperDirMFh.identifier + "." +
			      helperDirMFh.generation + 
			      ", name="+name.data);

	
 	try {
	    dirInode = this.getInode(helperDirMFh);

	    if (dirInode == null) {
		return new Stat(Stat.NFSERR_PERM);
	    }
	    
	    if (!dirInode.isDirectory()) return new Stat(Stat.NFSERR_PERM);
	    
	   
	    dirInode.unlink(name.data);
		
	    
	    return new Stat(Stat.NFS_OK);

	} catch (StaleHandleException e) {
	    return new Stat(Stat.NFSERR_STALE);
	} catch (Exception e) {
	    if (debug_nfs) Debug.out.println("Fehler in NFS_REMOVE");
	    return new Stat(Stat.NFSERR_PERM);    
	}

    }


    // NFS_RENAME
    public Stat rename(FHandle fromDir0, Name fromName, FHandle  toDir0, Name toName) {

	if (helperMFh == null) helperMFh = new MappedFHandle();
	if (helperDirMFh == null) helperDirMFh = new MappedFHandle();

	helperMFh.renew(fromDir0.data);
	helperDirMFh.renew(toDir0.data);
	MappedFHandle fromDir = helperMFh;
	MappedFHandle toDir = helperDirMFh;

	Inode fromDirInode;
	Inode toDirInode;

 	if (debug_nfs) 
	    Debug.out.println("RENAME fh="+fromDir.deviceIdentifier + "." +
			      fromDir.identifier + "." +
			      fromDir.generation + 
			      ", name="+fromName.data);



 	try {
	    fromDirInode = this.getInode(fromDir);
	    toDirInode = this.getInode(toDir);

	    if (fromDirInode == null || toDirInode == null) return new Stat(Stat.NFSERR_PERM);
	    
	    if (fromDirInode.isDirectory() && toDirInode.isDirectory()) {
		
		fromDirInode.rename(fromName.data, toDirInode, toName.data);
				
		return new Stat(Stat.NFS_OK);
	    } else {
		return new Stat(Stat.NFSERR_PERM);
	    }
	} catch (StaleHandleException e) {
	    return new Stat(Stat.NFSERR_STALE);
	} catch (Exception e) {
	    if (debug_nfs) Debug.out.println("Fehler in NFS_RENAME");
	    return new Stat(Stat.NFSERR_PERM);
	}	
    }



    // NFS_LINK
    public Stat link(FHandle from, FHandle  toDir, Name toName) {
	throw new Error(); 
    }



    // NFS_SYMLINK
    public Stat symlink(FHandle fromDir0, Name fromName, DirPath to, SAttr attributes) {

	if (helperMFh == null) helperMFh = new MappedFHandle();
	if (helperDirMFh == null) helperDirMFh = new MappedFHandle();

	helperDirMFh.renew(fromDir0.data);
	MappedFHandle fromDir = helperDirMFh;

	Inode dirInode;

 	if (debug_nfs) Debug.out.println("SYMLINK fh="+fromDir.identifier+", name="+fromName.data);

 	try {
	    dirInode = this.getInode(fromDir);

	    if (dirInode == null) return new Stat(Stat.NFSERR_PERM);

	    if (!dirInode.isDirectory()) return new Stat(Stat.NFSERR_PERM);

	    
	    Inode inode = dirInode.symlink(to.data, fromName.data);
		
	    helperMFh.renew(fromDir.deviceIdentifier, 
			    inode.getIdentifier(), 
			    inode.getVersion());
	    MappedFHandle fh = helperMFh;
	    FAttr a = getFAttr(fh);
		
	    return new Stat(Stat.NFS_OK);
	} catch (StaleHandleException e) {
	    return new Stat(Stat.NFSERR_STALE);
	} catch (Exception e) {
	    if (debug_nfs) Debug.out.println("Fehler in NFS_SYMLINK");
	    return new Stat(Stat.NFSERR_PERM);
	}
    }



    // NFS_MKDIR
    // ToDo:
    // - Zugriffsrechte
    public DirOpRes mkdir(FHandle  dir0, Name name, SAttr attributes) {

	if (helperMFh == null) helperMFh = new MappedFHandle();
	if (helperDirMFh == null) helperDirMFh = new MappedFHandle();

	helperDirMFh.renew(dir0.data);
	MappedFHandle dir = helperDirMFh;
	Inode dirInode;

 	if (debug_nfs) Debug.out.println("MKDIR in fh="+dir.identifier+", name="+name.data);

 	try {
	    dirInode = this.getInode(dir);

	    if (dirInode == null) return new DirOpResError();

	    if (!dirInode.isDirectory()) return new DirOpResError();

	    
	    Inode inode = dirInode.mkdir(name.data, InodeImpl.S_IWUSR|InodeImpl.S_IRUGO|InodeImpl.S_IXUGO);
	    if (debug_nfs) Debug.out.println("... new inode number: " + inode.getIdentifier());

		
	    helperMFh.renew(dir.deviceIdentifier, 
						 inode.getIdentifier(), 
						 inode.getVersion());
	    MappedFHandle fh = helperMFh;

	    lookupCache.addEntry(name.data, dir.identifier,fh);

	    FAttr a = getFAttr(fh);
		
	    return new DirOpResOK(fh.getFHandle(), a);
	} catch (StaleHandleException e) {
	    return new DirOpResErrStale();
	} catch (FileExistsException e) {
	    return new DirOpResErrExist();
	} catch (Exception e) {
	    if (debug_nfs) Debug.out.println("Fehler in NFS_MKDIR");
	    return new DirOpResError();
	}
    }



    // NFS_RMDIR
    public Stat rmdir(FHandle  dir0, Name name) {

	if (helperMFh == null) helperMFh = new MappedFHandle();
	if (helperDirMFh == null) helperDirMFh = new MappedFHandle();

	helperDirMFh.renew(dir0.data);
	MappedFHandle dir = helperDirMFh;
	Inode dirInode;

	if (debug_nfs) Debug.out.println("RMDIR fh="+dir.identifier+", name="+name.data);
	
 	try {
	    dirInode = this.getInode(dir);

	    if (dirInode == null) return new Stat(Stat.NFSERR_PERM);	    
	    
	    if (!dirInode.isDirectory()) return new Stat(Stat.NFSERR_PERM);
	    
	   
	    dirInode.rmdir(name.data);
		
	    
	    return new Stat(Stat.NFS_OK);

	} catch (StaleHandleException e) {
	    return new Stat(Stat.NFSERR_STALE);
	} catch (Exception e) {
	    if (debug_nfs) Debug.out.println("Fehler in NFS_RMDIR");
	    return new Stat(Stat.NFSERR_PERM);    
	}

	
    }



    // NFS_READDIR
    public ReadDirRes readdir(FHandle fh0, NFSCookie cookie, int count) {

	if (helperMFh == null) helperMFh = new MappedFHandle();
	if (helperDirMFh == null) helperDirMFh = new MappedFHandle();

	helperMFh.renew(fh0.data);
	MappedFHandle fh = helperMFh;
	Inode dirInode;

	if (debug_nfs) Debug.out.println("READDIR: fh="+fh.identifier);
	
 	try {
	    dirInode = this.getInode(fh);

	    if (dirInode == null) return new ReadDirResError();
	    
	    
	    if (!dirInode.isDirectory()) return new ReadDirResError();
	    
	   
	    String [] dirNames = dirInode.readdirNames();


	    Entry startEntry = null;

	    for (int i=0; i<dirNames.length; i++) {
		if (debug_nfs) Debug.out.println("DIRENTRY:  "+dirNames[i]);
		int fileid = dirInode.getInode(dirNames[i]).getIdentifier();
		Entry newEntry = new Entry(
					   fileid,
					   new Name(dirNames[i]),
					   new NFSCookie(fileid)
					       );
		if (startEntry != null) {
		    newEntry.next = new Entries(startEntry);
		}
		startEntry = newEntry;
	    }

	    Entries dirEntries = new Entries(startEntry);
	    return new ReadDirResOK(dirEntries,1);
		
	 } catch (StaleHandleException e) {
	    return new ReadDirResErrStale();
	} catch (Exception e) {
	    if (debug_nfs) Debug.out.println("Fehler in NFS_RMDIR");
	    return new ReadDirResError();
	}
    }    



    // NFS_STATFS
    public StatFSRes statfs(FHandle fh0) {
	if (debug_nfs) Debug.out.println("STATFS (get file system attributes)");
	
	Inode inode;

	if (helperMFh == null) helperMFh = new MappedFHandle();
	if (helperDirMFh == null) helperDirMFh = new MappedFHandle();

	helperMFh.renew(fh0.data);
	MappedFHandle fh = helperMFh;
	if (debug_nfs) 
	    Debug.out.println("STATFS: fh="+fh.deviceIdentifier+"." + 
			      fh.identifier + "." +
			      fh.generation);


	try{
	    inode = this.getInode(fh);
	    StatFS sfs = inode.getStatFS();
	    
	    StatFSResOK result = new StatFSResOK();
	    result.tsize = sfs.tsize;
	    result.bsize = sfs.bsize;
	    result.blocks = sfs.blocks;
	    result.bfree = sfs.bfree;
	    result.bavail = sfs.bavail;

	    // TODO: tsize: optimum transfer size of the server in bytes. This is the number 
	    // of bytes the server would like to have in the data part of READ and WRITE requests. 
	    result.tsize = 8192;
	    // TODO: bavail: the number of "bsize" blocks available to non-privileged users
	    result.bavail = 60057800;

	    return result;

	} catch (InodeIOException e) {
	    cpuManager.recordEvent(event_getattr_out);
	    return new StatFSResErrIO();
	} catch (InodeNotFoundException e) {
	    cpuManager.recordEvent(event_getattr_out);
	    return new StatFSResErrNoEnt();
	} catch (NoDirectoryInodeException e) {
	    cpuManager.recordEvent(event_getattr_out);
	    return new StatFSResErrNotDir();
	} catch (NotExistException e) {
	    cpuManager.recordEvent(event_getattr_out);
	    return new StatFSResErrNoEnt();
	} catch (PermissionException e) {
	    cpuManager.recordEvent(event_getattr_out);
	    return new StatFSResErrAcces();
	} catch (FSException e) {
	    cpuManager.recordEvent(event_getattr_out);
	    return new StatFSResErrIO();
	} catch (StaleHandleException e) {
	    cpuManager.recordEvent(event_getattr_out);
	    return new StatFSResErrStale();
	}


    }







    // -----------------------------------

//     private static int fileid = 1;
//     private Entry createEntry(String name, Entry next) {
// 	Entry e = new Entry();
// 	e.fileid = fileid++;
// 	e.name = new Name(name);
// 	e.cookie = new NFSCookie(e.fileid);
// 	if (next != null) e.next = new Entries(next);	
// 	return e;
//     }


    private Inode getInode(MappedFHandle fh) throws FSException, StaleHandleException {
	Inode inode;
	
	inode = fs.getInode(new Integer(fh.deviceIdentifier),fh.identifier);
	
	if (inode.getVersion() != fh.generation) { 
	    throw new StaleHandleException();
	}
	return inode;
	
    } 
    

    private FAttr getFAttr(MappedFHandle fh) 
	throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException,
	NotExistException, PermissionException, FSException, StaleHandleException
    {

	Inode inode;
	FAttr a;

	// Falls der AttrCache genutzt werden soll...
	if (useAttrCache) {
	    if (fh.identifier != attrCache_lastFhIdentifier) {
		inode = this.getInode(fh);
		a = getFAttr(inode, fh);
		attrCache_lastFhIdentifier = fh.identifier;
		attrCache_lastFAttr = a;
		if (debugAttrCache) attrCache_cacheMisses++;
	    } else {
		if (debugAttrCache) attrCache_cacheHits++;
		a = attrCache_lastFAttr;
	    }
	    if (debugAttrCache) 
		Debug.out.println("AttrCache: " + 
				  attrCache_cacheHits + "," +attrCache_cacheMisses);
	}
	// Falls der AttrCache nicht genutzt werden soll...
	else {
	    inode = this.getInode(fh);
	    a = getFAttr(inode, fh);		
	}
	
	return a;
    }

    /* ToDo: 
       - 
    */
	private FAttr getFAttr(Inode inode, MappedFHandle fh) {
	    try {
	    FAttr a = new FAttr();
	    
	    // type und mode
	    a.type = new FType();
	    a.mode = 0;

	    if (inode.isDirectory()) {
		a.type.ftype = FType.ftype_NFDIR;
		a.mode |= FAttr.MODE_DIR;
	    } 
	    if (inode.isFile()) {
		a.type.ftype = FType.ftype_NFREG;
		a.mode |= FAttr.MODE_REGFILE;
	    }
	    if (inode.isSymlink()) {
		a.type.ftype = FType.ftype_NFLNK;
		a.mode |= FAttr.MODE_SYMLINK;
	    }
				
	    if (inode.isWritable()) {
		a.mode |= FAttr.MODE_WOWNER;
		a.mode |= FAttr.MODE_WGROUP;
		a.mode |= FAttr.MODE_WOTHER;
	    }
	    if (inode.isReadable()) {
		a.mode |= FAttr.MODE_ROWNER;
		a.mode |= FAttr.MODE_RGROUP;
		a.mode |= FAttr.MODE_ROTHER;
	    }

	    if (inode.isExecutable()) {
		a.mode |= FAttr.MODE_XOWNER;
		a.mode |= FAttr.MODE_XGROUP;
		a.mode |= FAttr.MODE_XOTHER;
	    }

	    a.nlink = inode.i_nlinks();

	    // user id and group id
	    a.uid = 500;
	    a.gid = 600;

	    // size and blocksize
	    a.size = inode.getLength();
	    a.blocksize = inode.getStatFS().bsize;
	    a.blocks = (a.size + 1024) / 1024;


	    // file id und file system id
	    a.fileid = inode.getIdentifier();
	    a.fsid = fh.deviceIdentifier;

	    // rdev nimmt einen beliebigen Wert an
	    a.rdev = 42;

	    // Zeiten
	    a.atime = new Timeval(inode.lastAccessed(),0);
	    a.ctime = new Timeval(inode.lastChanged(),0);
	    a.mtime = new Timeval(inode.lastModified(),0);	
	    return a;
	} catch(Exception e) { throw new Error(); }
    }


    private FAttr setAttr(Inode inode, MappedFHandle fh, SAttr attr) throws FSException{
	if (attr.atime.seconds != -1) inode.setLastAccessed(attr.atime.seconds);
	if (attr.mtime.seconds != -1) inode.setLastModified(attr.mtime.seconds);
	return getFAttr(inode,fh);
	/*
	  try {
	  FAttr a = new FAttr();
	  a.type = new FType();
	  if (inode.isDirectory()) {
	  a.type.ftype = FType.ftype_NFDIR;
	  a.mode |= FAttr.MODE_DIR;
	  } 
	  if (inode.isFile()) {
	  a.type.ftype = FType.ftype_NFREG;
	  a.mode |= FAttr.MODE_REGFILE;
	  }
	  if (inode.isWritable()) {
	  a.mode |= FAttr.MODE_WOWNER;
	  }
	  if (inode.isReadable()) {
	  a.mode |= FAttr.MODE_ROWNER;
	  }
	  if (inode.isExecutable()) {
	  a.mode |= FAttr.MODE_XOWNER;
	  }
	  a.nlink = 1;
	  a.uid = 500;
	  a.gid = 600;
	  a.size = inode.getLength();
	  a.blocksize = 1024;
	  a.atime = new Timeval(1000, 0);
	  a.ctime = new Timeval(1000, 0);
	  a.mtime = new Timeval(1000, 0);	
	  return a;
	  } catch(Exception e) { throw new Error(); }
	*/
    }

    /*
    public byte[] getHandle(String name) {
	return null;
    }

    public Inode getInode(byte[] handle) {
	if (handle[1] != (byte)epoch) {
	    Debug.out.println("OLD HANDLE");
	    return null;//throw new Error("OLD HANDLE");
	}
	Inode i = inodes[handle[0]];
	if (i!=null) return i;	
	throw new Error("Unknown Handle");
    }
    */


//     public byte[] getRootHandle() {
// 	try {
// 	MappedFHandle fh = new MappedFHandle(rootInode.getFileSystem().getDeviceID().intValue(), rootInode.getIdentifier(), epoch);
// 	return fh.getData();
// 	} catch(jx.fs.NotExistException ex) { throw new Error(); }
//     }

    /*
    public byte[] getOrCreateHandle(Inode inode) {
	for(int i=0; i<nHandles; i++) {
	    if (inodes[i] == inode) return handles[i];
	}
	int handle = nHandles;
	handles[handle] = new byte[32];
	//
	try {
	    int inodeID = inode.getIdentifier();
	} catch(FSException ex) {}
	handles[handle][0] = (byte)handle;
	handles[handle][1] = (byte)epoch;

	inodes[handle] = inode;
	nHandles++;
	return handles[handle];
    }
    */


   
}


class StaleHandleException extends Exception {
    public StaleHandleException() {
	super();
    }
}


class LookupCache_Result {
    MappedFHandle fh;

    public LookupCache_Result(MappedFHandle fh) {
	super();
	this.fh = fh;
    }
}


interface LookupCache {
    public void addEntry(String name, int dirIdentifier, MappedFHandle fh);
    public LookupCache_Result getEntry(String name, int dirIdentifier);
    public void delEntry(String name, int dirIdentifier);
    public void flush();
    public void printStatistics();
}

class LookupCache_firstImpl implements LookupCache{
    private boolean debug;
    private int lastDirIdentifier;
    private String lastName;
    private MappedFHandle lastFh;
    private int cacheMisses;
    private int cacheHits;

    public LookupCache_firstImpl() {
	this(false);
    }

    public LookupCache_firstImpl(boolean debug) {
	this.debug = debug;
	this.cacheHits = 0;
	this.cacheMisses = 0;
    }
	

    public void addEntry(String name, int dirIdentifier, MappedFHandle fh) {
	this.lastName = name;
	this.lastDirIdentifier = dirIdentifier;
	this.lastFh = fh;
    }
    
    public LookupCache_Result getEntry(String name, int dirIdentifier) {
	if ((dirIdentifier != lastDirIdentifier) || 
	    (!name.equals(lastName))) {
	    
	    if (debug) cacheMisses++;
	    return null;
	} else {
	    if (debug) cacheHits++;	
	    return new LookupCache_Result(lastFh);
	}
    }
    
    public void delEntry(String name, int dirIdentifier) {
	lastName = null;
	lastDirIdentifier = 0;
	lastFh = null;    
    }
    
    public void flush() {
    }
    
    public void printStatistics() {
	Debug.out.println("LookupCache_firstImpl: " + 
			  cacheHits + "," + 
			  cacheMisses);
	
    }
 
}

class LookupCache_2ndImpl_Entry {
    int dirIdentifier;
    MappedFHandle fh;
    
    public LookupCache_2ndImpl_Entry(int dirIdentifier, MappedFHandle fh) {
	this.dirIdentifier = dirIdentifier;
	this.fh = fh;
    }    
}

class LookupCache_2ndImpl implements LookupCache {
    private boolean debug;
    private java.util.Hashtable nameHt;

    private int lastDirIdentifier;
    private String lastName;
    private MappedFHandle lastFh;
    private int cacheMisses;
    private int cacheHits;

    public LookupCache_2ndImpl() {
	this(false);
    }

    public LookupCache_2ndImpl(boolean debug) {
	this.debug = debug;
	this.cacheHits = 0;
	this.cacheMisses = 0;
	nameHt = new java.util.Hashtable(100);
    }


    public void addEntry(String name, int dirIdentifier, MappedFHandle fh) {
	LookupCache_2ndImpl_Entry newEntry = 
	    new LookupCache_2ndImpl_Entry(dirIdentifier,fh);
	nameHt.put(name,newEntry);
    }

    public LookupCache_Result getEntry(String name, int dirIdentifier) {
	LookupCache_2ndImpl_Entry res;
	res = (LookupCache_2ndImpl_Entry)nameHt.get(name);
	if (res != null) {
	    if (res.dirIdentifier == dirIdentifier) {
		if (debug) cacheHits++;
		return new LookupCache_Result(res.fh);
	    }    
	    else {
		//if (debug) 
		    Debug.out.println("LookupCache_2ndImpl: doch nichts!-----------");
		cacheMisses++;
		return null;
	    }
	}
	else cacheMisses++;
	return null;
    }
    
    
    public void delEntry(String name, int dirIdentifier) {
    }

    public void flush() {
    }
    
    public void printStatistics() {
	Debug.out.println("LookupCache_2ndImpl: " + 
			  cacheHits + "," + 
			  cacheMisses);
	
    }
    
}
