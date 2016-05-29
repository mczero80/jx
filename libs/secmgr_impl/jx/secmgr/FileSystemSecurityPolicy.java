package jx.secmgr;

import jx.zero.*;
import java.util.*;
import jx.fs.*;

public class FileSystemSecurityPolicy implements DomainBorderIn {
    
    private static final boolean doNothing = false;
    private static final boolean doOpenOnly = false;
    private static final boolean debug = false;
    private static final boolean assert = false;

    /** the trusted computing base */
    Principal tcb;

    Naming naming;
    CPUManager cpuManager;
    CentralSecurityManager secMgr;

    VMClass refFilesystemImpl;
    VMClass refDirectoryImpl;
    VMClass refRegularFileImpl;
    VMMethod read_RegularFile;
    VMMethod write_RegularFile;
    VMMethod openRW_Directory;
    VMMethod create_Directory;
    VMMethod getDefaultPermission_FilesystemInterface;
    VMMethod openRootDirectoryRW_FilesystemInterface;
    Principal_impl curPrincipal;  

   FileSystemSecurityPolicy() {
	this.tcb = new Principal_impl("TCB",0);
	this.naming = InitialNaming.getInitialNaming();
	this.secMgr = (CentralSecurityManager)naming.lookup("SecurityManager");
	if ( this.secMgr == null ) throw new Error("CentralSecurityManager not found");
	cpuManager =  (CPUManager)naming.lookup("CPUManager");
 	if (debug) Debug.out.println("FileSystemSecurityPolicy initialized");
	// create reference classes
	refFilesystemImpl  = cpuManager.getClass("jx.fs.FilesystemImpl");
	refDirectoryImpl   = cpuManager.getClass("jx.fs.DirectoryImpl");
	refRegularFileImpl = cpuManager.getClass("jx.fs.RegularFileImpl");
	VMMethod methods[] = refRegularFileImpl.getMethods();
	for (int i = 0 ; i < methods.length; i++)
	    if (methods[i].getName().equals("read"))
		read_RegularFile = methods[i];
 	    else if (methods[i].getName().equals("write"))
		write_RegularFile = methods[i];
	methods = refDirectoryImpl.getMethods();
	for (int i = 0 ; i < methods.length; i++)
	    if (methods[i].getName().equals("openRW"))
		 openRW_Directory = methods[i];
	    else if (methods[i].getName().equals("create"))
		 create_Directory = methods[i];
	methods = refFilesystemImpl.getMethods();
	for (int i = 0 ; i < methods.length; i++)
	    if (methods[i].getName().equals("getDefaultPermission"))
		getDefaultPermission_FilesystemInterface = methods[i];
 	    else if (methods[i].getName().equals("openRootDirectoryRW"))
		 openRootDirectoryRW_FilesystemInterface = methods[i];
     }

   
//    public boolean outBound(InterceptOutboundInfo info) {
//	return true;
//    }
	
    public boolean inBound(InterceptInboundInfo info) {
	if (assert) cpuManager.assertInterruptEnabled();
	if (debug) Debug.out.println("INBOUND CALL");
	if (doNothing) return true;
	Object o = info.getServiceObject();
	curPrincipal = (Principal_impl)secMgr.getPrincipal(info.getSourceDomain());
	if (cpuManager.getVMClass(o).equals(refRegularFileImpl))
	    if (doOpenOnly) return true;
	    else return check_RegularFile((RegularFile) o, info.getMethod());
	else if (cpuManager.getVMClass(o).equals(refFilesystemImpl))
	    return check_FilesystemInterface((FilesystemInterface) o, info.getMethod());
	else if (cpuManager.getVMClass(o).equals(refDirectoryImpl))
	    return check_Directory((Directory) o, info);
	return false;
    }

    private boolean check_FilesystemInterface(FilesystemInterface fs, VMMethod method){
	if (method.equals(getDefaultPermission_FilesystemInterface)) { 
	    return true;
	} else if (method.equals(openRootDirectoryRW_FilesystemInterface)) {
	    if (debug) Debug.out.println("openRootDirectoryRW call intercepted:");
	    try{
		Directory d =	(Directory)fs.openRootDirectoryRW();
		return checkPerm(d, EXT2Permission.RWX);
	    } catch (Exception e) { Debug.out.println("Exception: "); }
	} 
	return false;
    }
    
    private boolean check_Directory(Directory dir, InterceptInboundInfo info){
	VMMethod method = info.getMethod();
	if (method.equals(openRW_Directory)) {
	    if (debug) Debug.out.println("openRW call intercepted:");
	    VMObject obj = cpuManager.getVMObject();
	    info.getFirstParameter(obj);
	    try{
		FSObject fsobj = dir.openRW(obj.getString());
		if (fsobj==null) return true;
		return checkPerm(fsobj, EXT2Permission.RW);
	    } catch (Exception e) { Debug.out.println("Exception: "); }
	} else if (method.equals(create_Directory)) { 
	    if (debug) Debug.out.println("create call intercepted:");
	    return checkPerm(dir, EXT2Permission.RWX);
	}
	return true;
    }

    private boolean check_RegularFile(RegularFile fo, VMMethod method){
	if (method.equals(write_RegularFile)) { 
	    if (debug) Debug.out.println("write call intercepted:");
	    return checkPerm(fo, EXT2Permission.WRITE);
	} else if (method.equals(read_RegularFile)) {
	    if (debug) Debug.out.println("read call intercepted:");
	    return checkPerm(fo, EXT2Permission.READ);
	}
	return false;
    }

    private boolean checkPerm(FSObject fsobj, int right){
	try {
	EXT2Attribute a = (EXT2Attribute)fsobj.getAttribute();
	EXT2Permission p = (EXT2Permission)fsobj.getPermission();
	if (debug) Debug.out.println("UID: "+a.getUserID()+" perm:"+p.getPermission(EXT2Permission.USER)+
				     " vs. uid"+ curPrincipal.uid);
	if ((p.getPermission(EXT2Permission.USER) & right) == right
	    && a.getUserID() == curPrincipal.uid)
	    return true;
	} catch (Exception e) { Debug.out.println("Exception: "); }
	return false;
   }
    
    public boolean createPortal(PortalInfo info) {
	if (debug) Debug.out.println("createPortal");
	return true;
    }

    public void destroyPortal(PortalInfo info) {
    }

 }
