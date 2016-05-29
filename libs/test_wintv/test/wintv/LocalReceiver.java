package test.wintv;

import jx.zero.*;
import jx.zero.debug.*;
import jx.devices.pci.*;
import jx.framebuffer.PackedFramebuffer;
import jx.wintv.*;

/*
 * Empfaengt Datenpakete in folgendem Format:
 * 
 *     (word)	x Position
 *     (word)	y Position
 *     (long)	Laenge in Bytes
 *     (word)   Framenummer
 * 
 * Die Datenpakete werden im Sichtfenster einfach an die genannte Position kopiert.
 * 
 * 
 */
public class LocalReceiver {
    static Naming naming = InitialNaming.getInitialNaming();
   public static void main(PackedFramebuffer viewbuffer){
      viewbuffer.clear();
      PFBTools pfbt = new PFBTools(viewbuffer);
      
      // get transmission portal 
      VideoDataSource videodata = null;
      do {
	 videodata = (VideoDataSource)naming.lookup("LocalVideoDataSource");
      } while( videodata == null );
      
      // get data packets and copy them.
      Memory packet = null;
      int x, y, fn, l, offset;
      int lastFrame = 0;
	
      Debug.out.println("Receiver: starting transmission...");
      videodata.startTransmission();
      
      Debug.out.println("Receiver: enter transmission loop...");
      while(true){
	 packet = videodata.getNextPacket();
	 
	 x = packet.get16(0);
	 y = packet.get16(1);
	 l = packet.get32(1);
	 fn= packet.get16(4);
	 
	 // copy data into viewbuffer
	 offset = pfbt.getPixelOffset8(x, y);
	 viewbuffer.memObj().copyFromMemory(packet, 20, offset, l);
	 videodata.recyclePacket(packet);

	 if( lastFrame != fn ){
	    Debug.out.println("Receiver: new frame number: " + fn);
	    lastFrame = fn;
	 }
      }
   }
}

