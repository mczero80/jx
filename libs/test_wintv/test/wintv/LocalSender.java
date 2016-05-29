package test.wintv;

import jx.zero.*;
import jx.zero.debug.*;
import jx.devices.pci.*;
import jx.framebuffer.*;
import jx.wintv.*;

/*
 * Plan zum Senden: 
 * 
 * Es gibt zwei Framebuffer: In den ersten schreibt die Karte selbstaendig
 * rein, aus dem zweiten liest das Portal die Daten aus und sendet sie an
 * den Empfaenger. Wurde das letzte Paket an den Empfaenger gesendet,
 * werden die Buffer umgeschaltet. Beim Umschalten wird FCAP ausgelesen und
 * zurueckgesetzt. Damit hat man ein Mass, wieviele Fields dem Emfaenger
 * durch die Lappen gegangen sind.
 * 
 * 
 * Start: 
 * 
 * Grabbe in den ersten Framebuffer. Bei "startTransmission" wird das erste
 * Mal umgeschaltet, damit das erste Bild "frisch" ist.
 * 
 * 
 * TODO:
 * 
 * Es muesste eigentlich drei Puffer geben: in den ersten wird geschrieben,
 * aus dem zweiten wird gelesen und im dritten ist das letzte fertig
 * geschriebene Bild. Ansonsten muesste ich eigentlich nach der
 * Uebertragung eines Bildes erst abwarten, bis das naechste Bild fertig
 * geschrieben wurde. Aber da alles so schnell geht, ist das fast egal.
 * 
 */


public class LocalSender implements VideoDataSource, Service {
    static Naming naming = InitialNaming.getInitialNaming();
   PackedFramebuffer	framebuffers[];
   PictureIterator	pixelCursors[];
   int			framenumbers[];
   
   CaptureDevice	captureBoard;
   
   int currentCaptureBuffer;
   int currentSendBuffer;
   
   int sendLength;
   Memory sendPacket = null;
   int currentFrame = 0;
   
   LocalSenderNotifier notifier;
   
   public static void init(PackedFramebuffer framebuffer){
      new LocalSender(framebuffer);
   }
   
   LocalSender(PackedFramebuffer framebuffer){
      final int fbw = framebuffer.width();
      final int fbh = framebuffer.height();
      
      // setup some framebuffers 
      framebuffers = new PackedFramebuffer[] {
	 // new MemoryFramebuffer(800, 600, 16),
	 new SubFramebuffer(framebuffer, 0, 0,     fbw/2, fbh/2),
	 new SubFramebuffer(framebuffer, 0, fbh/2, fbw/2, fbh/2),
      };
      
      sendLength   = framebuffers[0].pixelOffset() * framebuffers[0].width();
      pixelCursors = new PictureIterator[framebuffers.length];
      for(int i=0; i<framebuffers.length; ++i){
	 pixelCursors[i] = new PictureIterator(framebuffers[i], sendLength);
      }
      
      framenumbers = new int[framebuffers.length];
      for(int i=0; i<framenumbers.length; ++i)
	framenumbers[i] = 0;
      
      // setup send variables
      currentCaptureBuffer = 0;
      currentSendBuffer	   = 1;
      sendPacket = ((MemoryManager)naming.lookup("MemoryManager")).alloc(20 + sendLength);
      
      // setup framegrabber
      captureBoard = (CaptureDevice)naming.lookup("Framegrabber");
      Debug.assert(captureBoard != null, "found no framegrabber board");
      captureBoard.getChinch().activate();
      captureBoard.setNorm(TVNorms.pal);
      
      captureBoard.setFramebufferOdd (framebuffers[currentCaptureBuffer]);
      captureBoard.markOddFramebufferRelocateable(true);
      FramebufferScaler scaler = new FramebufferScaler(captureBoard.getGeometryInterface());
      scaler.setScaling(true,  TVNorms.pal, framebuffers[currentCaptureBuffer], false);
      
      captureBoard.setVideoAdjustment(new GammaCorrectionRemovalAdjustment(false));
      
      // setup notifier
      notifier = new LocalSenderNotifier(this);
      captureBoard.addNotifierOdd(notifier.getDepHandle());
      
      // start capturing
      captureBoard.setOddFieldActive(true);
      captureBoard.captureOn();
      
      // setup VideoDataSource portal
      naming.registerPortal(this, "LocalVideoDataSource");
   }
   
   public void startTransmission(){
      switchBuffers();
   }
   
   public Memory getNextPacket(){
      if( sendPacket == null ){
	 Debug.out.println("LocalSender: no memory object to send!");
	 return null;
      }
      
      PictureIterator pixelCursor   = pixelCursors[currentSendBuffer];
      PackedFramebuffer framebuffer = framebuffers[currentSendBuffer];
      
      if( pixelCursor.endOfPicture() ){
	 pixelCursor.reset();
	 currentFrame = captureBoard.getFieldCounter(true);
	 Debug.out.println(currentFrame + "/" + notifier.notifications);
	 notifier.notifications = 0;
	 
	 switchBuffers();
	 
	 pixelCursor = pixelCursors[currentSendBuffer];
	 framebuffer = framebuffers[currentSendBuffer];
      }
      
      int l = pixelCursor.nextLength();
      sendPacket.copyFromMemory(framebuffer.memObj(), 
				pixelCursor.nextOffset(), 
				20,
				l);
      sendPacket.set16(0, (short)pixelCursor.getX());
      sendPacket.set16(1, (short)pixelCursor.getY());
      sendPacket.set32(1, l);
      sendPacket.set16(4, (short)framenumbers[currentSendBuffer]);
      
      pixelCursor.advance();
      
      Memory retval = sendPacket;
      sendPacket = null;
      return retval;
   }
   
   public void recyclePacket(Memory oldpacket){
      sendPacket = oldpacket;
   }
   
   void switchBuffers(){
      currentSendBuffer = (currentSendBuffer + 1) % framebuffers.length;
      currentCaptureBuffer = (currentCaptureBuffer + 1) % framebuffers.length;
      
      framenumbers[currentSendBuffer] = notifier.allNotifications;
      
      captureBoard.relocateFramebufferOdd(framebuffers[currentCaptureBuffer], 0);
      captureBoard.setOddFieldActive(true);
   }
}

class LocalSenderNotifier implements Notifier, Service {
    static Naming naming = InitialNaming.getInitialNaming();
   int notifications = 0;
   int allNotifications = 0;
   LocalSender sender;
   
   public LocalSenderNotifier(LocalSender sender){
      this.sender = sender;
      naming.registerPortal(this, "test/wintv/TestNotifier");
   }
   
   public void notifyEvent(){
      ++notifications;
      ++allNotifications;
   }
   
   public Notifier getDepHandle(){
      return this;
   }
}

class PictureIterator {
   PackedFramebuffer framebuffer;
   PFBTools pfbtool;
   
   int x;
   int y;
   int w;
   int h;
   
   int pixelLength;
   int byteLength;
   
   boolean flagEndOfPixels = false;
   
   PictureIterator(PackedFramebuffer framebuffer, int packetLength){
      this.framebuffer = framebuffer;
      this.pfbtool = new PFBTools(framebuffer);
      this.x = this.y = 0;
      this.w = framebuffer.width();
      this.h = framebuffer.height();
      this.pixelLength = packetLength / framebuffer.pixelOffset();
      this.byteLength  = pixelLength * framebuffer.pixelOffset();
   }
   
   boolean endOfPicture(){
      return flagEndOfPixels;
   }
   
   int nextOffset(){
      return pfbtool.getPixelOffset8(x, y);
   }
   
   int nextLength(){
      if( x + pixelLength >= w )
	return (w-x)* framebuffer.pixelOffset();
      return byteLength;
   }
   
   void advance(){
      x += pixelLength;
      if( x >= w ){
	 x = 0;
	 ++y;
      }
      if( y >= h ){
	 y = 0;
	 flagEndOfPixels = true;
      }
   }
   
   void reset(){
      x = y = 0;
      flagEndOfPixels = false;
   }
   
   int getX(){ return x; }
   int getY(){ return y; }
}


   
