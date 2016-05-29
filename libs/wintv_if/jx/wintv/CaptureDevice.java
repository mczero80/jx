package jx.wintv;

import jx.zero.Portal;
import jx.framebuffer.PackedFramebuffer;
import jx.framebuffer.ClippingRectangle;

public interface CaptureDevice extends Portal {
   
   /********************************************************************/
   /* selection of video input source                                  */
   /********************************************************************/
   
   InputSource getCurrentInputSource();
   InputSource[] getInputSources();
   InputSource	getChinch();
   SVideo	getSVideo();
   InputSource  getColorBars();
   Tuner	getTuner();
   
   
   /********************************************************************/
   /* selection of video norm & picture geometry                       */
   /********************************************************************/
   
   /**
    * Set the parameters of the video norm.
    * 
    * @see TVNorm, TVNorms
    * 
    */
   void setNorm(TVNorm tvnorm);
   
   /** 
    * Get geometry & scaling interface.
    * 
    * This very hardware dependent interface is made public to keep the
    * full flexibility of scaling and cropping the video picture. The
    * abstraction of the hardware is provided by special scaling and
    * cropping classes which can be implemented without bloating the
    * capture interface.
    * 
    * An simple scaling class is <FramebufferScaler> which will scale down
    * the full video picture into a given framebuffer.
    * 
    * @see FramebufferScaler 
    * 
    */
   BT878GeometryInterface getGeometryInterface();
   
   /**
    * Set frequency of color subcarrier.
    * 
    * This call is usually not used, because the Fsc will be set
    * correctly by the method "setNorm".
    * 
    * @see setNorm
    * 
    */
   void setFsc(int frequ);		

   
   /********************************************************************/
   /* video picture adjustment interface                               */
   /********************************************************************/
   
   VideoAdjustment getVideoAdjustment(String name);
   void setVideoAdjustment(VideoAdjustment newValue);
   
   /********************************************************************/
   /* picture output and capture control                               */
   /********************************************************************/
   
   /** 
    * Register or unregister a framebuffer for future use.
    * 
    * To register a framebuffer for a video field, the <framebuffer>
    * argument must be a valid referece to a Framebuffer. The field will be
    * still inactive after registration and must be activated explicitly
    * with set{Odd,Even}FieldActive. A currently registered and possibly
    * activated framebuffer will stay activated.
    * 
    * To unregister a framebuffer for a field, the <framebuffer> argument
    * must be <null>. The framebuffer have been disabled before otherwise
    * an exception (FIXME: which one?) is thrown.
    * 
    * @param framebuffer Reference to a PackedFramebuffer object.
    * 
    * @see setOddFieldActive, setEvenFieldActive
    * 
    */
   void setFramebufferOdd(PackedFramebuffer framebuffer);
   void setFramebufferEven(PackedFramebuffer framebuffer);
   
   /**
    * Update clipping information for video field. 
    * 
    * Changes are not activated until <set*FieldActive> is called.
    * 
    * @throws NoFramebufferRegistered	No framebuffer was registered.
    */
   void setClippingOdd(ClippingRectangle clippings[]);
   void setClippingEven(ClippingRectangle clippings[]);
   
   void setClipping(PackedFramebuffer framebuffer, ClippingRectangle clippings[]);
   
   
   /**
    * Prepare field to become active.
    * 
    * The information gathered by the methods <setFrambufferOdd>,
    * <setFrambufferEven>, <setClippingOdd>, <setClippingEven> is used to
    * set up the capture information. This is an optional step in setting
    * up a capture area, because this methods will be called by
    * <setOddFieldActive> and <setEvenFieldActive> if this method was not
    * called before or new information became available. The purpose of
    * this method is to isolate possibly time consuming operations in this
    * methods and to allow <setOddFieldActive> and <setEvenFieldActive> to
    * be very fast, so you can activate/deactivate field very quickly. This
    * may be important if you use the notification support.
    * 
    * 
    * @throws NoFramebufferRegistered	No framebuffer was registered for this field.
    * 
    * @see setOddFieldActive, setEvenFieldActive, addNotifierOdd, addNotifierEven
    * 
    */
   void prepareOddField();
   void prepareEvenField();
   
   /**
    * Activate or deactivate a field.
    * 
    * The registered framebuffer for the field is set up and activated. The
    * changes take place immediatly. If <prepareOddField> or
    * <prepareEvenField> were called before and no new information is made
    * available (e.g. a new framebuffer was registered), the precomputed
    * stuff is only actived. Otherwise the appropriate <prepare*Field>
    * method is called before the changes are activated.
    * 
    * It is possible to reactivate a field after it was deactivated. 
    * 
    * If capture is already in progress, the other fields are not affected.
    * 
    * @throws NoFramebufferRegistered	No framebuffer was registered for this field.
    * 
    */
   void setOddFieldActive(boolean active);
   void setEvenFieldActive(boolean active);
   void setFieldsActive(boolean oddActive, boolean evenActive);
   
   /**
    * Start capture process.
    * 
    * The capture process is started with all fields with registered and activated framebuffers.
    * Throws an exception if there are no activated framebuffers. (FIXME: which one?)
    */
   void captureOn();
   
   /**
    * Stop capture process.
    * 
    * The capture process is stopped for all fields.
    */
   void captureOff();
   
   
   /********************************************************************/
   /* notification support                                             */
   /********************************************************************/

   /**
    * Add a notifier for a capture "event".
    * 
    * @notifyObject		Object on which "notifyEvent" is called.
    * 
    * @throws NoFramebufferRegistered	No framebuffer was registered for this field.
    * 
    */
   void addNotifierOdd (Notifier notifyObject);
   void addNotifierEven(Notifier notifyObject);

//   void addNotifierOdd(TriggerPoint triggerTime, int numberOfTriggers, Notifier notifyObject);
//   void addNotifierEven(TriggerPoint triggerTime, int numberOfTriggers, Notifier notifyObject);

   /**
    * Remove a notifier.
    * 
    */
   void deleteNotifier(Notifier notifyObject);
   
   /********************************************************************/
   /* misc stuff                                                       */
   /********************************************************************/

   // unsorted stuff
   String getBoardName();
   void setIrqWatchBuffer(PackedFramebuffer irqFramebuffer);
   
   // for debugging only
   boolean isVideoSignalStable(int millis);
   void printIsVideoSignalStable(int millis);
   void startPollIntStat(int millis);
   void dumpControlBlock(int bytes);
   int getFieldCounter(boolean reset);
   void setTDec(boolean byField, boolean alignOddField, int rate);
   void dumpStatus();
   
   // timing test
   void doTimingTests();
   
   
   // no "stable" interface yet
   void markOddFramebufferRelocateable(boolean isRelocatable);
   void markEvenFramebufferRelocateable(boolean isRelocatable);
   void markFramebufferRelocateable(PackedFramebuffer framebuffer, boolean isRelocatable);
   
   void relocateFramebufferOdd(PackedFramebuffer framebuffer, int offset);
   void relocateFramebufferEven(PackedFramebuffer framebuffer, int offset);
   void relocateFramebuffer(PackedFramebuffer oldFramebuffer, PackedFramebuffer newFramebuffer, int offset);
   
}
