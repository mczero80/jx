package test.wintv.file;

import jx.zero.*;
import jx.framebuffer.*;
import jx.wintv.*;
import test.wintv.*;
import jx.bio.*;

/* Format written to disk:
 * 
 * First block of data (512 bytes):
 * 
 * off	length	description
 * 0	4	magic number (0xfecfda42)
 * 4	4	width of one picture in pixel
 * 8	4	height of one picture in pixel
 * 12	4	pixelOffset in bytes
 * 
 * Each picture is width*height*pixelOffset bytes long. It ist stored in
 * 512 byte blocks. Therefore the last block my contain invalid trainling
 * data.
 */

class FileStore {
    static Naming naming = InitialNaming.getInitialNaming();
   CaptureDevice captureBoard;
    BlockIO bio;
    Memory sector;
    MemoryManager memoryManager;

   FileStore(CaptureDevice captureBoard, PackedFramebuffer framebuffer, BlockIO bio, TVNorm tvnorm){
      this.captureBoard = captureBoard;
      this.bio = bio;
      this.memoryManager = (MemoryManager)naming.lookup("MemoryManager");
      this.sector = memoryManager.alloc(bio.getSectorSize());

      int fw = 384;			/* CIF - PAL SQ Pixel  */
      int fh = 288;
      
      PackedFramebuffer framebufferOdd   = new SubFramebuffer(framebuffer, 8, 8, fw, fh);
      MemoryFramebuffer framebufferEven1 = new MemoryFramebuffer(fw, fh, 16);
      MemoryFramebuffer framebufferEven2 = new MemoryFramebuffer(fw, fh, 16);
      
      PackedFramebuffer notifyBuffer = 	new SubFramebuffer(framebuffer, 16+fw, 8, 
							   framebuffer.width() - 24 - fw,
							   framebuffer.height() - 16);
      
      // setup framegrabber
      captureBoard.setFramebufferOdd (framebufferOdd);
      captureBoard.setFramebufferEven(framebufferEven1);
      captureBoard.markEvenFramebufferRelocateable(true);
      FramebufferScaler scaler = new FramebufferScaler(captureBoard.getGeometryInterface());
      scaler.setScaling(true,  tvnorm, framebufferOdd, false);
      scaler.setScaling(false,  tvnorm, framebufferEven1, false);
      
      captureBoard.setVideoAdjustment(new GammaCorrectionRemovalAdjustment(false));
      
      // setup notifier
      /*      notifier = new FileStoreNotifier(notifyBuffer, this);
      captureBoard.addNotifierOdd(notifier);
      */

      // start capturing
      captureBoard.setOddFieldActive(true);
      captureBoard.setEvenFieldActive(true);
      captureBoard.captureOn();
      
   }
   
   void savePicture(MemoryFramebuffer framebuffer){
      final int blocksize = 512;
      final int bytes = framebuffer.scanlineOffset() * framebuffer.height();
      final int blocks = (bytes -1 + blocksize ) / blocksize;
      
      write(framebuffer.memObj(), framebuffer.startAddress(), bytes);
   }
   
   void write(Memory mem, int offset, int bytes){
       Debug.out.println("write("+offset+", "+bytes+")");
       bio.writeSectors(0, 1, sector, true);
       //      MySleep.msleep(bytes/512);		/* assume 1ms per block */
   }
}



