package jx.wintv;

import jx.zero.*;

import timerpc.SleepManagerImpl;
  
public class MySleep {
   /* loops per millisecond */
    //   public final static int lpms = 77000;
   
    static SleepManagerImpl sleepManager = new SleepManagerImpl();

   
    public static void msleep(int millis){
       sleepManager.mdelay(millis);
    }

   public static void usleep(int micros){
       sleepManager.udelay(micros);
   }

   public static int timestamp(){
      return ZeroDomain.clock.getTimeInMillis();
   }
}
