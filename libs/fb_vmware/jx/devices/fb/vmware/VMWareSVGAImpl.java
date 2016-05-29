package jx.devices.fb.vmware;

import jx.zero.*;
import jx.zero.debug.*;

import jx.devices.pci.*;
import jx.devices.*;
import jx.devices.fb.*;

public class VMWareSVGAImpl implements VMWareDefines, FramebufferDevice
{
	DeviceMemory 	m_cRegister;
	DeviceMemory	m_cFIFO;
	DeviceMemory    m_cFrameBuffer;
   	PCIDevice 	m_cPCIDevice;
	Ports		m_cPorts;
	int		m_nIndexPort;
	int		m_nValuePort;
	int		m_nVideoRam;
	int		m_nMemPhysBase;
	int		m_nMaxWidth;
	int		m_nMaxHeight;
	int		m_nMMIOPhysBase;
	int		m_nMMIOSize;
	int		m_nWidth;
	int		m_nHeight;
	int		m_nOffset;
	int		m_nBitsPerPixel;
	int 		m_nBytesPerLine;
	int		m_nDisplayWidth;
	ColorSpace	m_eColorSpace;
	boolean		m_bFIFODirty;
	boolean 	m_bIsOpen = false;
	static Naming s_cNaming = null;

   	/* PCI stuff */
   	int ReadConfig (int nAddr)
	{
      		return m_cPCIDevice.readConfig (nAddr / 4);
   	}
   	void WriteConfig (int nAddr, int nValue)
	{
      		m_cPCIDevice.writeConfig (nAddr / 4, nValue);
   	}
	int ReadReg32 (int nIndex)
	{
		int nValue;
		
		m_cPorts.outl (m_nIndexPort, nIndex);
		nValue = m_cPorts.inl (m_nValuePort);
/*		
		m_cRegister.set32 (SVGA_INDEX_PORT, nIndex);
		nValue = m_cRegister.get32 (SVGA_VALUE_PORT);
*/		
		return nValue;
	}
	void WriteReg32 (int nIndex, int nValue)
	{
/*	
		m_cRegister.set32 (SVGA_INDEX_PORT, nIndex);
		m_cRegister.set32 (SVGA_VALUE_PORT, nValue);
*/	
		m_cPorts.outl (m_nIndexPort, nIndex);
		m_cPorts.outl (m_nValuePort, nValue);	
	}
	int ReadFIFO (int nIndex)
	{
		return m_cFIFO.get32 (nIndex);
	}
	void WriteFIFO (int nIndex, int nValue)
	{
		m_cFIFO.set32 (nIndex, nValue);
	}

	VMWareSVGAImpl(PCIDevice pci, boolean legacy) 
	{
		int val;
		if (s_cNaming == null)
			s_cNaming = InitialNaming.getInitialNaming();
		
		if ((m_cPCIDevice = FindDevice ()) == null)
			return;
			
		m_cPorts = (Ports)s_cNaming.lookup ("Ports");
		if (m_cPorts == null)
			throw new Error ("VMWareSVGAImpl: no Ports found!");			

		int id = m_cPCIDevice.readConfig (PCI.REG_DEVVEND);
		
		// get Memory Mapped regions
      		MemoryManager memMgr = (MemoryManager)s_cNaming.lookup("MemoryManager");
      		if (memMgr == null)
			throw new Error("VMWareSVGAImpl: no MemoryManager found");
      		
		if (id == 0x071015ad)
		{
			Debug.out.print ("Found VMWare 0x071015ad\n");
			val = SVGA_LEGACY_BASE_PORT;
		}
		else
		{
			Debug.out.print ("Found VMWare 0x040515ad\n");
      			val = ReadConfig(0) & 0xfffffff0;
		}		
		
		m_bFIFODirty = false;
		m_nIndexPort = val + SVGA_INDEX_PORT * 4;
		m_nValuePort = val + SVGA_VALUE_PORT * 4;
		id = GetSVGAId ();

		if (id == SVGA_ID_0 || id == SVGA_ID_INVALID)
			throw new Error ("VMWareSVGAImpl: no supported VMWare SVGA found\n");
		
		m_nVideoRam = ReadReg32 (SVGA_REG_FB_MAX_SIZE);
		m_nMemPhysBase = ReadReg32 (SVGA_REG_FB_START);
		m_nMaxWidth = ReadReg32 (SVGA_REG_MAX_WIDTH);
		m_nMaxHeight = ReadReg32 (SVGA_REG_MAX_HEIGHT);						
		m_nBitsPerPixel = ReadReg32 (SVGA_REG_BITS_PER_PIXEL);
		m_nBytesPerLine = ReadReg32 (SVGA_REG_BYTES_PER_LINE);
		m_eColorSpace = new ColorSpace (ColorSpace.CS_RGB16);
	
		Debug.out.println ("VideoRam: 0x" + Integer.toHexString (m_nVideoRam));
		Debug.out.println ("MemPhysBase: 0x" + Integer.toHexString (m_nMemPhysBase));
		Debug.out.println ("Max Res.: " + m_nMaxWidth + "/" + m_nMaxHeight);
		Debug.out.println ("BitsPerPixel: " + m_nBitsPerPixel);
		Debug.out.println ("BytesPerLine: " + m_nBytesPerLine);
		Debug.out.println ("Pseudocolor: " + ReadReg32 (SVGA_REG_PSEUDOCOLOR));
		Debug.out.println ("Red mask:   " + Integer.toHexString (ReadReg32 (SVGA_REG_RED_MASK)));
		Debug.out.println ("Green mask: " + Integer.toHexString (ReadReg32 (SVGA_REG_GREEN_MASK)));
		Debug.out.println ("Blue mask:  " + Integer.toHexString (ReadReg32 (SVGA_REG_BLUE_MASK)));
		
		m_cFrameBuffer = memMgr.allocDeviceMemory (m_nMemPhysBase, m_nVideoRam);
		if (m_cFrameBuffer == null)
			throw new Error ("VMWareSVGAImpl: cannot allocate memory for framebuffer");
		
		InitFIFO ();
	}
	
	/*public VMWareSVGAImpl ()*/
	public void open(DeviceConfiguration conf) 
	{
		FramebufferConfiguration c = (FramebufferConfiguration)conf;
		if (m_bIsOpen) 
		{
	    		Debug.out.println("Attempt to open the VMWare SVGA a second time!");
	    		return;
		}		
		m_bIsOpen = true;
		setMode (c.xresolution, c.yresolution, c.colorSpace);
	}
	public void close() 
	{
		m_bIsOpen = false;
	} 
	public DeviceConfigurationTemplate[] getSupportedConfigurations () 
	{ 
		return new DeviceConfigurationTemplate[] {
	    		new FramebufferConfigurationTemplate(640, 480, new ColorSpace (ColorSpace.CS_RGB16)),
			new FramebufferConfigurationTemplate(800, 600, new ColorSpace (ColorSpace.CS_RGB16)),
			new FramebufferConfigurationTemplate(1024, 768, new ColorSpace (ColorSpace.CS_RGB16)),
		};
    	}
	public int getWidth()
	{
		return m_nWidth;
	}
    	public int getHeight()
	{
		return m_nHeight;
	}	
	public int setMode (int nWidth, int nHeight, ColorSpace eColorSpace)
	{
		if (nWidth > m_nMaxWidth || nHeight > m_nMaxHeight || m_eColorSpace.getValue() != eColorSpace.getValue())
			return -1;
		WriteReg32 (SVGA_REG_WIDTH, nWidth);
		WriteReg32 (SVGA_REG_HEIGHT, nHeight);
		WriteReg32 (SVGA_REG_ENABLE, 1);
		m_nOffset = ReadReg32 (SVGA_REG_FB_OFFSET);
		WriteReg32 (SVGA_REG_GUEST_ID, GUEST_OS_OTHER);
		m_nDisplayWidth = (ReadReg32(SVGA_REG_BYTES_PER_LINE) * 8) / ((m_nBitsPerPixel + 7) & ~7);
		m_nBytesPerLine = ReadReg32 (SVGA_REG_BYTES_PER_LINE);
		m_nWidth = nWidth;
		m_nHeight = nHeight;
		
		Debug.out.println ("VMWareSVGAImpl::SetMode() to " + nWidth + "/" + nHeight + "/" + eColorSpace);
		Debug.out.println ("BytesPerLine: " + m_nBytesPerLine);
		Debug.out.println ("Offset: " + m_nOffset);
		return 0;
	}
	public void startFrameBufferUpdate ()
	{
		WaitForFB ();
	}
	public void endFrameBufferUpdate ()
	{
	/*
		UpdateFullScreen ();
		WaitForFB ();
	*/		
	}
	public void startUpdate ()
	{
	}
	public void endUpdate ()
	{
		UpdateFullScreen ();
		WaitForFB ();
	}
	public DeviceMemory getFrameBuffer ()
	{
		return m_cFrameBuffer;
	}
	public int getFrameBufferOffset ()
	{
		return m_nOffset;
	}
	public int BitsPerPixel ()
	{
		return m_nBitsPerPixel;
	}
	public int getBytesPerLine ()
	{
		return m_nBytesPerLine;
	}

   	/**
    	 * Search a VMWare graphic board on the given PCI bus.
    	 */
   	public PCIDevice FindDevice()
	{
      		PCIDevice cDev = null;
      		PCIAccess cPCIBus = (PCIAccess)s_cNaming.lookup("PCIAccess");
      		int devc = cPCIBus.getNumberOfDevices();
     		
	 	for(int devindex=0; devindex<devc; ++devindex)
		{
	 		cDev = cPCIBus.getDeviceAt(devindex);
	 
	 		int id = cDev.readConfig(PCI.REG_DEVVEND);
	 		if (id != 0x040515ad && 
	    		    id != 0x071015ad)
	   			continue;
			return cDev;				
      		}
		Debug.out.print ("unable to find VMWare SVGA graphics card!\n");
      		return null;
  	}

	/*
 	 *-----------------------------------------------------------------------------
	 *
	 * VMXGetVMwareSvgaId --
	 *
	 *    Retrieve the SVGA_ID of the VMware SVGA adapter.
	 *    This function should hide any backward compatibility mess.
	 *
	 * Results:
	 *    The SVGA_ID_* of the present VMware adapter.
	 *
	 * Side effects:
	 *    ins/outs
	 *
	 *-----------------------------------------------------------------------------
	 */

	int GetSVGAId()
	{
   		int vmware_svga_id;

   		/* Any version with any SVGA_ID_* support will initialize SVGA_REG_ID
   		 * to SVGA_ID_0 to support versions of this driver with SVGA_ID_0.
   		 *
   		 * Versions of SVGA_ID_0 ignore writes to the SVGA_REG_ID register.
   		 *
   		 * Versions of SVGA_ID_1 will allow us to overwrite the content
   		 * of the SVGA_REG_ID register only with the values SVGA_ID_0 or SVGA_ID_1.
   		 *
   		 * Versions of SVGA_ID_2 will allow us to overwrite the content
   		 * of the SVGA_REG_ID register only with the values SVGA_ID_0 or SVGA_ID_1
   		 * or SVGA_ID_2.
   		 */

		WriteReg32 (SVGA_REG_ID, SVGA_ID_2);
		vmware_svga_id = ReadReg32 (SVGA_REG_ID);
		
   		if (vmware_svga_id == SVGA_ID_2) 
      			return SVGA_ID_2;

   		WriteReg32 (SVGA_REG_ID, SVGA_ID_1);
   		vmware_svga_id = ReadReg32 (SVGA_REG_ID);
 	  	if (vmware_svga_id == SVGA_ID_1) 
		      return SVGA_ID_1;

   		if (vmware_svga_id == SVGA_ID_0) 
	      		return SVGA_ID_0;

   		/* No supported VMware SVGA devices found */
   		return SVGA_ID_INVALID;
	}
	void InitFIFO ()
	{
      		MemoryManager memMgr = (MemoryManager)s_cNaming.lookup("MemoryManager");
      		if (memMgr == null)
			throw new Error("VMWare: no MemoryManager found");
			
		m_nMMIOPhysBase = ReadReg32 (SVGA_REG_MEM_START);
		m_nMMIOSize = ReadReg32 (SVGA_REG_MEM_SIZE);
		Debug.out.println ("VMWare::InitFIFO() PhysBase: " + Integer.toHexString (m_nMMIOPhysBase) + ", Size: " + m_nMMIOSize);
		m_cFIFO = memMgr.allocDeviceMemory (m_nMMIOPhysBase, m_nMMIOSize);
		WriteFIFO (SVGA_FIFO_MIN, 16);
		WriteFIFO (SVGA_FIFO_MAX, m_nMMIOSize);
		WriteFIFO (SVGA_FIFO_NEXT_CMD, 16);
		WriteFIFO (SVGA_FIFO_STOP, 16);
		WriteReg32 (SVGA_REG_CONFIG_DONE, 1);
	}
	void WriteWordToFIFO (int nValue)
	{
		m_bFIFODirty = true;
		//Debug.out.println ("VMWare::WriteWordToFIFO(" + Integer.toHexString(nValue) + ") pos: " + ReadFIFO (SVGA_FIFO_NEXT_CMD));
		/* Need to sync? */
		if ((ReadFIFO (SVGA_FIFO_NEXT_CMD) + 4 == ReadFIFO (SVGA_FIFO_STOP)) ||
		    (ReadFIFO (SVGA_FIFO_NEXT_CMD) == ReadFIFO (SVGA_FIFO_MAX) - 4 &&
		     ReadFIFO (SVGA_FIFO_STOP) == ReadFIFO (SVGA_FIFO_MIN)))
		{		     
			Debug.out.println ("VMWare::WriteWordToFIFO() syncing FIFO");
			WriteReg32 (SVGA_REG_SYNC, 1);
			while (ReadReg32 (SVGA_REG_BUSY) != 0);
		}
		WriteFIFO (ReadFIFO (SVGA_FIFO_NEXT_CMD) / 4, nValue);
		WriteFIFO (SVGA_FIFO_NEXT_CMD, ReadFIFO (SVGA_FIFO_NEXT_CMD) + 4);
		if (ReadFIFO (SVGA_FIFO_NEXT_CMD) == ReadFIFO (SVGA_FIFO_MAX))
			WriteFIFO (SVGA_FIFO_NEXT_CMD, ReadFIFO (SVGA_FIFO_MIN));
/*		
    if ((vmwareFIFO[SVGA_FIFO_NEXT_CMD] + sizeof(CARD32) == vmwareFIFO[SVGA_FIFO_STOP])
     || (vmwareFIFO[SVGA_FIFO_NEXT_CMD] == vmwareFIFO[SVGA_FIFO_MAX] - sizeof(CARD32) &&
	 vmwareFIFO[SVGA_FIFO_STOP] == vmwareFIFO[SVGA_FIFO_MIN])) {
	vmwareWriteReg(pVMWARE, SVGA_REG_SYNC, 1);
	while (vmwareReadReg(pVMWARE, SVGA_REG_BUSY)) ;
    }
    vmwareFIFO[vmwareFIFO[SVGA_FIFO_NEXT_CMD] / sizeof(CARD32)] = value;
    vmwareFIFO[SVGA_FIFO_NEXT_CMD] += sizeof(CARD32);
    if (vmwareFIFO[SVGA_FIFO_NEXT_CMD] == vmwareFIFO[SVGA_FIFO_MAX]) {
	vmwareFIFO[SVGA_FIFO_NEXT_CMD] = vmwareFIFO[SVGA_FIFO_MIN];
    }
}
*/
	}
	void UpdateFullScreen ()
	{
		WriteWordToFIFO (SVGA_CMD_UPDATE);
		WriteWordToFIFO (0);
		WriteWordToFIFO (0);
		WriteWordToFIFO (m_nWidth);
		WriteWordToFIFO (m_nHeight);
	}		
	void WaitForFB ()
	{
		if (m_bFIFODirty == false)
			return;
		WriteReg32 (SVGA_REG_SYNC, 1);
		while (ReadReg32 (SVGA_REG_BUSY) != 0);
		m_bFIFODirty = false;
	}
	
	public int drawLine (int x1, int y1, int x2, int y2, PixelColor c)
	{
		return -1;
	}
	public int drawLine (PixelRect cDraw, PixelRect cClipped, PixelColor cColor, DrawingMode nDrawingMode)
	{
		return -1;
	}
	public int fillRect (PixelRect cRect[], int nCount, PixelColor cColor, DrawingMode nDrawingMode)
	{
		if (nDrawingMode.getValue() != DrawingMode.DM_COPY)
			return -1;
		/*Debug.out.println ("VMWare::fillRect: " + cColor + "," + Integer.toHexString ((int)cColor.toRGB16()));*/
		for (int i = 0; i < nCount; i++)
		{
			WriteWordToFIFO (SVGA_CMD_RECT_FILL);
			switch (m_eColorSpace.getValue())
			{
				case ColorSpace.CS_RGB16:
					WriteWordToFIFO ((int)cColor.toRGB16 ());
					break;
				default:
					WriteWordToFIFO (0);
					break;
			}
			WriteWordToFIFO (cRect[i].left ());
			WriteWordToFIFO (cRect[i].top ());
			WriteWordToFIFO (cRect[i].width () + 1);
			WriteWordToFIFO (cRect[i].height () + 1);
		}			
		
		return 0;
	}
	public int bitBlt (PixelRect acOldPos[], PixelRect acNewPos[], int nCount)
	{
		return -1;
	}
	public ColorSpace getColorSpace ()
	{
		return m_eColorSpace;
	}
};

interface VMWareDefines
{
	int SVGA_LEGACY_BASE_PORT = 0x4560;
	int SVGA_INDEX_PORT = 0x0;
	int SVGA_VALUE_PORT = 0x1;
	int SVGA_BIOS_PORT = 0x2;
	int SVGA_NUM_PORTS = 0x3;
		
   	int SVGA_REG_ID = 0;
   	int SVGA_REG_ENABLE = 1;
   	int SVGA_REG_WIDTH = 2;
   	int SVGA_REG_HEIGHT = 3;
   	int SVGA_REG_MAX_WIDTH = 4;
   	int SVGA_REG_MAX_HEIGHT = 5;
   	int SVGA_REG_DEPTH = 6;
   	int SVGA_REG_BITS_PER_PIXEL = 7;
   	int SVGA_REG_PSEUDOCOLOR = 8;
   	int SVGA_REG_RED_MASK = 9;
   	int SVGA_REG_GREEN_MASK = 10;
   	int SVGA_REG_BLUE_MASK = 11;
   	int SVGA_REG_BYTES_PER_LINE = 12;
   	int SVGA_REG_FB_START = 13;
   	int SVGA_REG_FB_OFFSET = 14;
   	int SVGA_REG_FB_MAX_SIZE = 15;
   	int SVGA_REG_FB_SIZE = 16;

   	int SVGA_REG_CAPABILITIES = 17;
   	int SVGA_REG_MEM_START = 18;	   /* Memory for command FIFO and bitmaps */
   	int SVGA_REG_MEM_SIZE = 19;
   	int SVGA_REG_CONFIG_DONE = 20;      /* Set when memory area configured */
   	int SVGA_REG_SYNC = 21;             /* Write to force synchronization */
   	int SVGA_REG_BUSY = 22;             /* Read to check if sync is done */
   	int SVGA_REG_GUEST_ID = 23;	   /* Set guest OS identifier */
   	int SVGA_REG_CURSOR_ID = 24;	   /* ID of cursor */
   	int SVGA_REG_CURSOR_X = 25;	   /* Set cursor X position */
   	int SVGA_REG_CURSOR_Y = 26;	   /* Set cursor Y position */
   	int SVGA_REG_CURSOR_ON = 27;	   /* Turn cursor on/off */

   	int SVGA_REG_TOP = 28;		   /* Must be 1 greater than the last register */

   	int SVGA_PALETTE_BASE = 1024;	   /* Base of SVGA color map */
	
	int SVGA_MAGIC = 0x900000;
	int SVGA_ID_INVALID = 0xffffffff;
	int SVGA_ID_0 = SVGA_MAGIC << 8;
	int SVGA_ID_1 = (SVGA_MAGIC << 8) | 1;
	int SVGA_ID_2 = (SVGA_MAGIC << 8) | 2;

	int SVGA_FIFO_MIN = 0;
	int SVGA_FIFO_MAX = 1;
	int SVGA_FIFO_NEXT_CMD = 2;
	int SVGA_FIFO_STOP = 3;

	int SVGA_CMD_UPDATE = 1;
	 /* FIFO layout:
	    X, Y, Width, Height */
	
	int SVGA_CMD_RECT_FILL = 2;
	 /* FIFO layout:
	    Color, X, Y, Width, Height */
	
	int SVGA_CMD_RECT_COPY = 3;
	 /* FIFO layout:
	    Source X, Source Y, Dest X, Dest Y, Width, Height */
	
	int SVGA_CMD_DEFINE_BITMAP = 4;
	 /* FIFO layout:
	    Pixmap ID, Width, Height, <scanlines> */
	
	int SVGA_CMD_DEFINE_BITMAP_SCANLINE = 5;
	 /* FIFO layout:
	    Pixmap ID, Width, Height, Line #, scanline */

	int SVGA_CMD_DEFINE_PIXMAP = 6;
	 /* FIFO layout:
	    Pixmap ID, Width, Height, Depth, <scanlines> */

	int SVGA_CMD_DEFINE_PIXMAP_SCANLINE = 7;
	 /* FIFO layout:
	    Pixmap ID, Width, Height, Depth, Line #, scanline */
	
	int SVGA_CMD_RECT_BITMAP_FILL = 8;
	 /* FIFO layout:
	    Bitmap ID, X, Y, Width, Height, Foreground, Background */
	
	int SVGA_CMD_RECT_PIXMAP_FILL = 9;
	 /* FIFO layout:
	    Pixmap ID, X, Y, Width, Height */

	int SVGA_CMD_RECT_BITMAP_COPY = 10;
	 /* FIFO layout:
	    Bitmap ID, Source X, Source Y, Dest X, Dest Y,
	    Width, Height, Foreground, Background */

	int SVGA_CMD_RECT_PIXMAP_COPY = 11;
	 /* FIFO layout:
	    Pixmap ID, Source X, Source Y, Dest X, Dest Y, Width, Height */
	
	int SVGA_CMD_FREE_OBJECT = 12;
	 /* FIFO layout:
	    Object (pixmap, bitmap, ...) ID */

	int SVGA_CMD_RECT_ROP_FILL = 13;
         /* FIFO layout:
            Color, X, Y, Width, Height, ROP */
	
	int SVGA_CMD_RECT_ROP_COPY = 14;
         /* FIFO layout:
            Source X, Source Y, Dest X, Dest Y, Width, Height, ROP */
	
	int SVGA_CMD_RECT_ROP_BITMAP_FILL = 15;
         /* FIFO layout:
            ID, X, Y, Width, Height, Foreground, Background, ROP */

	int SVGA_CMD_RECT_ROP_PIXMAP_FILL = 16;
         /* FIFO layout:
            ID, X, Y, Width, Height, ROP */

	int SVGA_CMD_RECT_ROP_BITMAP_COPY = 17;
         /* FIFO layout:
            ID, Source X, Source Y,
            Dest X, Dest Y, Width, Height, Foreground, Background, ROP */
	
	int SVGA_CMD_RECT_ROP_PIXMAP_COPY = 18;
         /* FIFO layout:
            ID, Source X, Source Y, Dest X, Dest Y, Width, Height, ROP */
	
	int SVGA_CMD_DEFINE_CURSOR = 19;
	/* FIFO layout:
	   ID, Hotspot X, Hotspot Y, Width, Height,
	   Depth for AND mask, Depth for XOR mask,
	   <scanlines for AND mask>, <scanlines for XOR mask> */

	int SVGA_CMD_DISPLAY_CURSOR = 20;
	/* FIFO layout:
	   ID, On/Off (1 or 0) */

	int SVGA_CMD_MOVE_CURSOR = 21;
	/* FIFO layout:
	   X, Y */

	int SVGA_CMD_MAX = 22;
	
	int GUEST_OS_OTHER = 0x5000 + 10;
};
