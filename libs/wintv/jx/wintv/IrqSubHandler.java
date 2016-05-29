package jx.wintv;

interface IrqSubHandler {
   int getInterruptBits();
   void callHandler(int istatus, int imask, int dstatus);
}
