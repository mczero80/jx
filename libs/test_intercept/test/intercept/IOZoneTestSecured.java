package test.intercept;

import jx.zero.*;
import jx.zero.debug.*;
import jx.secmgr.Principal_impl;

class IOZoneTestSecured {


    public static void init(Naming naming, String[]  args) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
	new IOZoneTestSecured(naming, args);
    }

    IOZoneTestSecured (Naming naming, String[]  args)   {
	
	CentralSecurityManager secmgr = (CentralSecurityManager)naming.lookup("SecurityManager");
	if ( secmgr == null ) throw new Error("CentralSecurityManager not found");

	Domain bio = DomainStarter.createDomain("BlockIO", "test_fs.jll", "test/fs/BioRAMDomain", null, 2000000, new String[] {"BlockIO"});
//	Domain bio = DomainStarter.createDomain("BlockIO", "test_fs.jll", "test/fs/EmulDiskDomain", null,  5000000 , new String[] {"BlockIO"});
	Domain fs = DomainStarter.createDomain("FS", "fs_user_impl.jll", "jx/fs/FSInterfaceDomain", 6000000, 100000, new String[] {"BlockIO"},naming ,"jx.secmgr.FileSystemSecurityPolicy");	

 	DomainManager domainManager = (DomainManager)naming.lookup("DomainManager");
	secmgr.addDomainAndPrincipal(domainManager.getCurrentDomain(), new Principal_impl("User",1));
	domainManager.getCurrentDomain().clearTCBflag();

//	Domain iozone = DomainStarter.createDomain("IOZONE-JDK", "legacy_starter.jll", "jx/start/ApplicationStarter",
//						   null , 50000000, new String[] { "test/jdkfs/Main", "test_jdk_fs.jll", "IOZONE"});

	Domain iozone = DomainStarter.createDomain("IOZONE-JXFS", "legacy_starter.jll", "jx/start/ApplicationStarter",
						   null , 50000000, new String[] { "test/fsuser/IOZONE", "test_fs_user.jll", "IOZONE"});
    }
}
