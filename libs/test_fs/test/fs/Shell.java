package test.fs;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Vector;
import jx.zero.Domain;
import jx.zero.Debug;
import jx.zero.*;
import jx.zero.debug.*;
import jx.bio.BlockIO;
import jx.fs.FS;
import jx.fs.DeviceNaming;
import jx.fs.FileSystem;
import jx.fs.InodeImpl;
import jx.fs.FSException;
import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;

class Parser {
    private String kommando;
    private String[] argv = new String[10];
    private int argc;

    public Parser(String zeile) {
	String tmp = zeile.trim();
	int cut = tmp.indexOf(" ", 0);
	argc = 0;

	if (cut == -1) {
	    kommando = tmp;
	    return;
	}
	kommando = tmp.substring(0, cut);
	tmp = tmp.substring(cut).trim();
	while ((tmp.length() > 0) && (argc < 10)) {
	    cut = tmp.indexOf(" ", 0);
	    if (cut == -1) {
		argv[argc++] = tmp;
		break;
	    } else {
		argv[argc++] = tmp.substring(0, cut);
		tmp = tmp.substring(cut).trim();
	    }
	}
    }

    public boolean isValid() {
	return (kommando.length() > 0);
    }

    public String getKommando() {
	return kommando;
    }

    public String[] getArgumente() {
	String[] retval = new String[argc];
	for (int i = 0; i < argc; i++)
	    retval[i] = argv[i];
	return retval;
    }
}

/**
 * Stellt eine kommandozeilen-orientierte Schnittstelle zum Dateisystem zur
 * Verf&uuml;gung (&auml;hnlich einer Unix-Shell), um &uuml;ber die Kommandos
 * <code>cd</code>, <code>ls</code>, <code>ll</code>, <code>touch</code>,
 * <code>rm</code>, <code>mkdir</code>, <code>rmdir</code>, <code>read</code>
 * und <code>write</code> mit dem Dateisystem in Interaktion zu treten.
 */
public class Shell { // extends Thread {
    private FileSystem filesystem;
    private FS fs;
    private DataInputStream in;
    private Hashtable mountPartitions= new Hashtable();
    private PrintStream uout; // user output


    public Shell(Naming naming) {
	String zeile;
	uout = System.out;
	in = new DataInputStream(System.in);
	//filesystem = (FileSystem)new domain.javafs.FileSystem();
	mountPartitions = new Hashtable();

	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	DebugPrintStream out = new DebugPrintStream(new DebugOutputStream(d));
	Debug.out = out;
	//DebugPrintStream uout = System.out; // user output
	uout.println("los gehts...");

	Portal IDEdep=null;
	do {
	    Thread.yield();
	    IDEdep = naming.lookup("IDE");
	} while (IDEdep == null);

	BlockIO idedevice = (BlockIO)IDEdep;
	out.println("Kapazitaet: " + idedevice.getCapacity());

	Portal FSdep = naming.lookup("FS");
	fs = (FS)FSdep;

	Portal JAVAFSdep = naming.lookup("JavaFS");
	filesystem = (FileSystem)JAVAFSdep;

	uout.println("Welcome to the JavaFS-Shell");
	uout.print("build/shell> ");
	try {
	    zeile = in.readLine();
	} catch (IOException e) { zeile = "shell"; }
	Parser parser = new Parser(zeile);

	if (parser.getKommando().equals("build"))
	Debug.out.print("build started");
	filesystem.build("TestFS", 1024);
	Debug.out.print("build finished");
	//filesystem.check(IDEDevice.nameToDevice("hda8")); //hda8

	fs.mountRoot(filesystem, false); // 2. Parameter = read-only  //hda8
	mountPartitions.put("hda8", filesystem);

	mainLoop();
    }



    //public void run() {
    //mainLoop();
    //}

    private void mainLoop() {
	String kommando, zeile = new String();
	String[] param;
	uout.println("'help' lists all commands");

	while (true) {
	    uout.print("" + fs.getCwdPath() + " > ");
	    
	    try {
	    	zeile = in.readLine();
	    } catch (IOException e) { break; }
	    Parser parser = new Parser(zeile);
	    if (parser.isValid() == false)
		continue;

	    kommando = parser.getKommando();
	    param = parser.getArgumente();

	    if (kommando.equals("help")) {
		uout.println("'cd Pfad'                           wechselt das aktuelle Verzeichnis");
		uout.println("'ls', 'll'                          Verzeichnisinhalt ausgeben");
		uout.println("'mkdir Pfad'                        legt ein neues Verzeichnis an");
		uout.println("'rmdir Pfad'                        loescht das angegebene Verzeichnis");
		uout.println("'touch Pfad'                        legt eine neue Datei an");
		uout.println("'rm Pfad'                           loescht die angegebene Datei");
		uout.println("'read Pfad Offset AnzahlBytes'      liest aus der angegebenen Datei und gibt");
		uout.println("                                    die ersten und letzten 50 Byte aus");
		uout.println("'write Pfad Offset AnzahlBytes'     schreibt in die angegebene Datei");
		uout.println("                                    (geschrieben wird der Pfadname)");
		uout.println("'rename AlterPfad NeuerPfad'        benennt die angegebene Datei bzw. das");
		uout.println("                                    Verzeichnis um");
		uout.println("'symlink Pfad SymlinkPfad'          legt einen symbolischen Link an");
		uout.println("'build Name Blockgroesse Partition' legt ein neues Dateisystem an ");
		uout.println("                                    (z.B. build Test 1024 hda2)");
		uout.println("'mount DateiSystem Partition Pfad'  meldet das angegebene Dateisystem an und");
		uout.println("                                    haengt es in den Verzeichnisbaum ein");
		uout.println("'unmount'                           meldet das angegebene Dateisystem ab");
		uout.println("'benchmark1'                        fuehrt einen Performance-Test durch");
		uout.println("'quit', 'exit'                      beendet die Shell");
		continue;
	    }

	    if (kommando.equals("cd")) {
		if (param.length != 1)
		    uout.println("wrong number of arguments");
		else
		    cd(param[0]);
		continue;
	    }


	    if (kommando.equals("ls")) {
		ls();
		continue;
	    }

	    if (kommando.equals("ll")) {
		ll();
		continue;
	    }

	    if (kommando.equals("mkdir")) {
		if (param.length != 1)
		    uout.println("wrong number of arguments");
		else
		    mkdir(param[0], InodeImpl.S_IWUSR|InodeImpl.S_IRUGO|InodeImpl.S_IXUGO);
		continue;
	    }

	    if (kommando.equals("rmdir")) {
		if (param.length != 1)
		    uout.println("wrong number of arguments");
		else
		    rmdir(param[0]);
		continue;
	    }

	    if (kommando.equals("touch")) {
		if (param.length != 1)
		    uout.println("wrong number of arguments");
		else
		    create(param[0], InodeImpl.S_IWUSR|InodeImpl.S_IRUGO);
		continue;
	    }

	    if (kommando.equals("rm")) {
		if (param.length != 1)
		    uout.println("wrong number of arguments");
		else
		    unlink(param[0]);
		continue;
	    }
	    
	    if (kommando.equals("read")) {
		if (param.length != 3)
		    uout.println("wrong number of arguments");
		else
		    read(param[0], Integer.parseInt(param[1]), Integer.parseInt(param[2]));
		continue;
	    }

	    if (kommando.equals("write")) {
		if (param.length != 3)
		    uout.println("wrong number of arguments");
		else
		    write(param[0], Integer.parseInt(param[1]), Integer.parseInt(param[2]));
		continue;
	    }

	    if (kommando.equals("rename")) {
		if (param.length != 2)
		    uout.println("wrong number of arguments");
		else
		    rename(param[0], param[1]);
		continue;
	    }

	    if (kommando.equals("symlink")) {
		if (param.length != 2)
		    uout.println("wrong number of arguments");
		else
		    symlink(param[0], param[1]);
		continue;
	    }

	    if (kommando.equals("build")) {
		if (param.length != 3)
		    uout.println("wrong number of arguments");
		else {
		    FileSystem filesystem = null; // (FileSystem)new domain.javafs.FileSystem();
		    filesystem.build(param[0], Integer.parseInt(param[1]));
		}
		continue;
	    }

	    if (kommando.equals("mount")) {
		if (param.length != 3)
		    uout.println("wrong number of arguments");
		else
		    mount(param[0], param[1], param[2]);
		continue;
	    }

	    if (kommando.equals("unmount")) {
		if (param.length != 1)
		    uout.println("wrong number of arguments");
		else
		    unmount(param[0]);
		continue;
	    }

	    if (kommando.equals("benchmark1")) {
		//Benchmark1 benchmark = new Benchmark1();
		continue;
	    }

	    if (kommando.equals("quit") || kommando.equals("exit"))
		break;

	    if (kommando.equals("flush")) {
		//metaxa.os.fs.javafs.BufferCache.instance().flush();
		continue;
	    }

	    if (kommando.equals("sync")) {
		//metaxa.os.fs.javafs.BufferCache.instance().syncDevice(0);
		continue;
	    }

	    if (kommando.equals("dinfo")) {
		//DirEntryCache.instance().showDirEntries();
		continue;
	    }

	    if (kommando.equals("iinfo")) {
		//metaxa.os.fs.javafs.InodeCache.instance().showInodes();
		continue;
	    }

	    if (kommando.equals("binfo")) {
		//metaxa.os.fs.javafs.BufferCache.instance().showBuffers();
		continue;
	    }

	    uout.println("unknown command '" + kommando + "'");
	}
	try {
	    fs.cleanUp();
	} catch(Exception e) {
	    uout.println("Cleanup Error!"+e);
	}
	//metaxa.os.fs.javafs.BufferCache.instance().showBuffers();
       	//metaxa.os.fs.javafs.InodeCache.instance().showInodes();
    }

    private void cd(String path) {
	fs.cd(path);
    }

    private void ls() {
	String[] names = null;
	try {
	    names = fs.getCwdInode().readdirNames();
	} catch (FSException e) {
	    uout.println("Error: "+e.getMessage());
	}
	for (int i = 0; i < names.length; i++)
	    uout.println((String)names[i]);
    }

    private void ll() {
	String[] names = null;
	String name = null;
	InodeImpl inode;
	try {
	    names = ((InodeImpl)fs.getCwdInode()).readdirNames();
	} catch (FSException e) {
	    uout.println("Error: "+e.getMessage());
	    return;
	}
	for (int i = 0; i < names.length; i++) {
	    name = names[i];
	    uout.print(name);
	    try {
		inode = (InodeImpl)(((InodeImpl)fs.getCwdInode()).lookup(name));
		if (inode.isDirectory())
		    uout.print(" D");
		else if (inode.isFile())
		    uout.print(" F");
		else if (inode.isSymlink())
		    uout.print(" -> " + inode.getSymlink());
		uout.println(" " + inode.getLength());

		inode.decUseCount(); // TODO: is this necessary?

	    } catch (FSException e) {
		uout.println("Error");
	    }
	}
    }

    private void mkdir(String name, int mode) {
	try {
	    fs.mkdir(name, mode);
	} catch (FSException e) {
	    uout.println("Error: "+e.getMessage());
	    return;
	}
    }

    private void rmdir(String name) {
	try {
	    fs.rmdir(name);
	} catch (FSException e) {
	    uout.println("Error: "+e.getMessage());
	    return;
	}
    }

    private void create(String name, int mode) {
	try {
	    fs.create(name, mode);
	} catch (FSException e) {
	    uout.println("Error: "+e.getMessage());
	    return;
	}
    }

    private void unlink(String name) {
	try {
	    fs.unlink(name);
	} catch (FSException e) {
	    uout.println("Error: "+e.getMessage());
	    return;
	}
    }

    private void read(String name, int off, int len) {
	/*
	Memory array = memMgr.alloc(len); // TODO
	String dateiinhalt;
	int reallen = 0;

	try {
	    reallen = fs.read(name, array, off, len);
	} catch (FSException e) {
	    uout.println("Error: "+e.getMessage());
	    return;
	}
	uout.println("read " + reallen + " bytes");
	for (int i = 0; i < len; i++)
	    if (array[i] != 0) {
		uout.println("first valid data at " + i);
		break;
	    }
	for (int i = len-1; i > 0; i--)
	    if (array[i] != 0) {
		uout.println("last valid data at " + i);
		break;
	    }
	if (reallen > 100) {
	    uout.println("first 50 byte:");
	    dateiinhalt = new String(array, 0, 50);
	    uout.println(dateiinhalt);
	    uout.println("last 50 byte:");
	    dateiinhalt = new String(array, reallen-50, 50);
	    uout.println(dateiinhalt);
	} else {
	    dateiinhalt = new String(array, 0, array.length);
	    uout.println(dateiinhalt);
	}
	*/
    }

    private void write(String name, int off, int len) {
	/*
	byte[] array = new byte[len];
	byte[] string = name.getBytes();
	int i;

	for (i = 0; i+string.length < array.length; i += string.length)
	    System.arraycopy(string, 0, array, i, string.length);
	for (int j = 0; i < array.length; i++)
	    array[i] = string[j++];

	try {
	    fs.write(name, array, off, len);
	} catch (FSException e) {
	    uout.println("Error: "+e.getMessage());
	    return;
	}
	*/
    }

    private void rename(String path, String pathneu) {
	try {
	    fs.rename(path, pathneu);
	} catch (FSException e) {
	    uout.println("Error: "+e.getMessage());
	    return;
	}
    }

    private void symlink(String path, String pathneu) {
	try {
	    fs.symlink(path, pathneu);
	} catch (FSException e) {
	    uout.println("Error: "+e.getMessage());
	    return;
	}
    }

    private void mount(String fs_name, String dev_name, String path) {
	String filesystem_path = "metaxa.os.fs." + fs_name + ".FileSystem";
	FileSystem filesystem;
	try {
	    Class the_class = Class.forName(filesystem_path);
	    filesystem = (FileSystem)the_class.newInstance();
	} catch (ClassNotFoundException e) {
	    Debug.out.println("Dateisystem existiert nicht");
	    return;
	} catch (InstantiationException e) {
	    Debug.out.println("Interner Fehler");
	    return;
	} catch (IllegalAccessException e) {
	    Debug.out.println("Interner Fehler");
	    return;
	}
	try {
	    fs.mount(filesystem, path, false);
	    mountPartitions.put(dev_name, filesystem);
	} catch (FSException e) {
	    uout.println("Error: "+e.getMessage());
	    return;
	}
    }

    private void unmount(String dev_name) {
	FileSystem filesystem = (FileSystem)mountPartitions.get(dev_name);
	if (filesystem == null) {
	    Debug.out.println("Auf der Partition " + dev_name + " ist kein Dateisystem angemeldet");
	    return;
	}
	try {
	    fs.unmount(filesystem);
	} catch (FSException e) {
	    uout.println("Error: "+e.getMessage());
	    return;
	}
    }
}
