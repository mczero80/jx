package bioide;

import jx.zero.*;
import jx.zero.Debug;
import jx.timer.SleepManager;
import jx.timer.*;


/**
 * Main entry point for IDE controller driver.
 * @author Michael Golm
 * @author Andreas Weissel
 */
public class IDEDeviceImpl {

    /** max. number drives per controller */
    public  static final int MAX_DRIVES      = 2;

    /** <code>true</code>, if busmaster DMA possible */
    public  static boolean dmaSupported;

    /** <code>true</code>, if UltraDMA possible */
    public  static boolean ultraDmaSupported;


    private static final int MAX_CONTROLLERS = 2;
    private static final int PAGE_SIZE       = 4096;
    private Controller[] controllers;
    private int pci_bus, pci_device, pci_fn;
    private boolean inited = false;
    private Drive[] drives = new Drive[MAX_CONTROLLERS * MAX_DRIVES];
    private PCIBus pcibus;
    private MyTimer myTimer;

    private static final int DEFAULT_BMIBA = 0xe800;
    private static final int PCI_VENDOR_ID_INTEL           = 0x00008086;
    private static final int PCI_DEVICE_ID_INTEL_82371FB_0 = 0x0000122e;
    private static final int PCI_DEVICE_ID_INTEL_82371FB_1 = 0x00001230;
    private static final int PCI_DEVICE_ID_INTEL_82371SB   = 0x00007010;
    private static final int PCI_DEVICE_ID_INTEL_82371AB   = 0x00007111;

    
    /**
     * Init driver. 
     * Detect controllers and drives. 
     * Init them for fast transmission (UDMA, MultiwordDMA, fallback to PIO when no DMA possible)
     */
    public IDEDeviceImpl() {
	if (inited)
	    return;
	inited = true;
	Env.init();

	this.myTimer = new MyTimer();
       
	pcibus = new PCIBus();

	controllers = new Controller[MAX_CONTROLLERS];
	for (int i = 0; i < MAX_CONTROLLERS; i++)
	    controllers[i] = new Controller(this, i);
	    
	ultraDmaSupported = false;

	boolean found = probeForTriton(PCI_VENDOR_ID_INTEL, PCI_DEVICE_ID_INTEL_82371FB_0, 1); // PIIX
	if (found == false)
	    found = probeForTriton(PCI_VENDOR_ID_INTEL, PCI_DEVICE_ID_INTEL_82371FB_1, 0); // PIIX
	if (found == false)
	    found = probeForTriton(PCI_VENDOR_ID_INTEL, PCI_DEVICE_ID_INTEL_82371SB, 0); // PIIX3
	if (found == false)
	    found = probeForTriton(PCI_VENDOR_ID_INTEL, PCI_DEVICE_ID_INTEL_82371AB, 0); // PIIX4
	dmaSupported = found; // found
	

	//	probeCmosForDrives();

	for (int i = 0; i < MAX_CONTROLLERS; i++) {
	    Debug.out.println("Try controller "+i);
	    if (controllers[i].identify() == false)
		continue;
	    Debug.out.println("Check controller "+i);
	    if (controllers[i].isPresent()) {
		Debug.out.println("Setup controller "+i);
		controllers[i].setup();
		for(int j=0; j<MAX_DRIVES; j++) {
		    drives[i*MAX_DRIVES+j] = controllers[i].drives[j];
		}
	    }
	}
	Debug.out.println("IDEDeviceImpl done");

    }

    public Drive[] getDrives() {
	return drives;
    }

    /**
     * Read drive geometry from CMOS.
     */
    private void probeCmosForDrives() {
	byte cmos_disks;
	CmosData cmos_data = new CmosData();

	/* read CMOS address 0x12:
	   inb(0x71) & 0xf0  -> unit 0 present
	   inb(0x71) & 0x0f  -> unit 1 present
	*/
	Env.ports.outb(0x70, (byte)0x12);
	cmos_disks = Env.ports.inb(0x71);

	for (int unit = 0; unit < MAX_DRIVES; ++unit) {
	    Drive drive = controllers[0].drives[unit];
	    if (((cmos_disks & (0xf0 >> (unit*4))) > 0) && !drive.present) {
		/*bh.b_data = new FixedDiskBiosMemory(unit);
		  cmos_data.init(bh, 0);
		  drive.cyl  = drive.bios_cyl  = cmos_data.cyl();
		  drive.head = drive.bios_head = cmos_data.head();
		  drive.sect = drive.bios_sect = cmos_data.sect();
		  drive.ctl  = cmos_data.ctl();
		  drive.present = true;*/
	    }
	}
    }

    /**
     * Check PCI for device
     */
    private boolean probeForTriton(int vendor, int device_id, int func_adj) {
	int max_pci_bus = 1;
	int my_id, num_fun, type, pcicmd;
	int ven_dev = (device_id << 16) | vendor;

	for (int bus = 0; bus < max_pci_bus; bus ++) {
	    for (int device = 0; device < 32; device ++) {
		/* Falls ein Geraet (device) vorhanden ist, muss Funktion 0 eine gueltige
		   vendor-ID aufweisen (!= 0xffff/-1, != 0) */
		my_id = pcibus.readConfig(bus, device, 0, 0);
		if ((my_id == -1) || (my_id == 0))
		    continue;

		//Debug.out.println("PCI: found device at bus="+bus+", device="+device);

		// auf Multifunktionseinheit ueberpruefen
		type = pcibus.readConfig(bus, device, 0, 3); // Headertype
		type >>= 16;
		if ((type & 0x80) > 0)
		    num_fun = 8; // Multifunktionseinheit
		else
		    num_fun = 1; // Einzelfunktionseinheit
		for (int function = 0; function < num_fun; function++) {
		    my_id = pcibus.readConfig(bus, device, function, 2); // Register 2 => Klassencode
		    if ((my_id >> 16) == 0x0604) { // Basis- und Subcode ausfiltern
			// 0x0604 => Basiscode == 06 (Bridge), Subcode == 04 (PCI-PCI)
			max_pci_bus++;  // PCI-PCI-bridge gefunden, bus-count erhoehen
		    }
	
		    my_id = pcibus.readConfig(bus, device, function, 0); // Einheiten-ID
		    if (my_id == ven_dev) { // Hersteller und Einheiten-ID stimmen ueberein
			if (initTriton((byte)bus, (byte)device, (byte)(function + func_adj)) == -1)
			    Debug.out.println("PCI-BIOS-Zugriff fehlgeschlagen");
			pcicmd = pcibus.readConfig(pci_bus, pci_device, pci_fn, 0x12); // UDMACTL - Ultra DMA/33 Control Register
			if ((pcicmd & 1) > 0)   // UDMA Enable
			    ultraDmaSupported = true;

			return true;
		    }
		}
	    }
	}

	return false;
    }

    /**
     * Prepare IDE driver for BM-DMA.
     */
    private int initTriton(int bus, int device, int fn) {
	boolean dma_enabled = false;
	int pcicmd, timings, tmp;
	int bmiba = 0;
	int time;
	pci_bus = bus;
	pci_device = device;
	pci_fn = fn;

	Debug.out.println("ide: i82371 PIIX(3,4) (Triton) am PCI-Bus " + bus + " (function " + fn + ")");

	// Ueberpruefen, ob IDE und Busmaster-DMA aktiviert sind:
	pcicmd = pcibus.readConfig(bus, device, fn, 1);
	pcicmd &= 0xffff;

	if ((pcicmd & 1) == 0)  { // I/O access
	    Debug.out.print("initTriton: Register sind nicht aktiv (BIOS), ");
	    return -1;
	}
	if ((pcicmd & 4) == 0) {  // Busmaster aktiv
	    Debug.out.println("initTriton: BM-DMA ist nicht aktiv (BIOS)");
	} else { // oder: pcibus.isBusmasterCapable(bus, device, fn)
	    // Auslesen der bmiba-Basisadresse
	    for (int i = 0; i < 2; i++) {
		bmiba = pcibus.readConfig(bus, device, fn, 8); // BMIBA - Bus Master Interface Address Register
		bmiba &= 0xfff0;	// Basisadresse der Busmaster-Interface-Register
		if (bmiba != 0) {
		    dma_enabled = true;
		    break;
		} else {
		    Debug.out.println("initTriton: BM-DMA Basisregister ist ungueltig (" + bmiba + ", PnP BIOS Problem)");
		    if ((Env.ports.inb(DEFAULT_BMIBA) != 0xff) || (i == 1))
			break;
		    Debug.out.println("initTriton: setze BM-DMA Basisregister auf " + DEFAULT_BMIBA);
		    Env.sleepManager.mdelay(50);
		    tmp = pcibus.readConfig(bus, device, fn, 1);
		    tmp = (tmp & 0xffff0000) | (pcicmd & ~1);
		    pcibus.writeConfig(bus, device, fn, 1, tmp); // I/O access enable loeschen
		    pcibus.writeConfig(bus, device, fn, 8, DEFAULT_BMIBA|1); // Bit 0 muss 1 sein, Bits 1-3 reserved
		    tmp = pcibus.readConfig(bus, device, fn, 1);
		    tmp = (tmp & 0xffff0000) | (pcicmd | 5);
		    pcibus.writeConfig(bus, device, fn, 1, tmp); // I/O access enable und Bus Master Enable setzen
		}
	    }
	}

	// Nachschauen, ob beide IDE-Register deaktiviert sind
	timings = pcibus.readConfig(bus, device, fn, 0x10); // IDETIM - IDE Timing Register
	if ((timings & 0x80008000) == 0) {
	    Debug.out.print("initTriton: beide Register sind inaktiv, ");
	    return -1;
	}
  
	// dma_base-Registeradresse fuer jeden Controller sichern
	time = timings & 0xffff;
	if ((time & 0x8000) != 0) {	// IDE-Decode (und damit Controller) aktiv?
	    if (dma_enabled)
		controllers[0].initTritonDma(bmiba);
	}
	time = timings >> 16;
	if ((time & 0x8000) != 0) {	// IDE-Decode (und damit Controller) aktiv?
	    if (dma_enabled)
		controllers[1].initTritonDma(bmiba + 8);
	}

	return 0;
    }
}

/**
 * Access to "Fixed Disk Parameter Tables".
 */
class CmosData  {
    
    private Memory  b_data;

    public CmosData() {
	b_data = Env.memoryManager.alloc(16);
    }

    public short cyl()   { return b_data.get16(0>>1); }
    public byte  head()  { return b_data.get8(2); }
    public byte  ctl()   { return b_data.get8(8); }
    public byte  sect()  { return b_data.get8(14); }
}


class MyTimerArg {
    AtomicVariable v;
    CPUState cpuState;
    MyTimerArg(AtomicVariable v, CPUState s) {
	this.v = v;
	v.set(s);
	cpuState = s; 
    }
}

class MyTimer implements TimerHandler {
    public void timer(Object arg) {
	MyTimerArg a = (MyTimerArg)arg;
	a.v.atomicUpdateUnblock(null, a.cpuState);
    }
}
