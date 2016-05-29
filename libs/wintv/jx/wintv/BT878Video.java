package jx.wintv;

import jx.zero.*;
import jx.zero.debug.*;

/** 
 * Hardware Acces to BT878 chip.
 * 
 * This class "hides" the raw hardware of the Bt878 chip by providing
 * methods for reading and writing to the various registers of the chip.
 * 
 * Terminology:
 * 
 *     - Simple Registers: This register holds a 8 or 32 bit numeric value
 *       and is located on a single memory address. Each register should
 *       have to methods: "get*" and "set*".
 * 
 *     - Splitted Register: This is a register, which numerical "logical
 *       value" is larger than eight bit. The "logical value" is divided
 *       into two parts which are read/writen to different memory adresses.
 *       Usually the MSBs are part of a "packed register". Each register
 *       should have two methods: "get*" and "set*".
 * 
 *     - Packed Register: This is a register, which shares its memory
 *       address with several other registers. Usuallay a packed register
 *       is more than one bit but less than eight bit wide. When writing to
 *       such a register, care is taken to not disturb the other registers
 *       at the same memory location.
 * 
 *     - Bit Register: This register shares the same memory location with
 *       other registers and is usually one one bit wide. Since most of the
 *       bit registers are grouped by related functionality, it wouldn't be
 *       wise to implement two methods for each of the bits. So most bit
 *       registers have only two methods to atomically manipulate all the
 *       bits in the same memory location. Appropriate bitmasks are
 *       defined, but they lack the usual "_MASK" suffix.
 * 
 */

// FIXME: all Methods should be synchronized!
class BT878Video implements BT878Defines {
   DeviceMemory localRegisters;
   
   public BT878Video(DeviceMemory localRegisters){
      this.localRegisters = localRegisters;
   }
   
   /**** inline ****/ private final int readRegister(int offset){
      return localRegisters.get32(offset/4);
   }
   
   /**** inline ****/ private final void writeRegister(int offset, int value){
      localRegisters.set32(offset/4, value);
   }
   
   /******************************/
   /* splitted registers         */
   /******************************/
   
   /*
    * Nomenklatur: Die Methoden der "Splitted Register" werden von der
    * Bezeichnung des Registers abgeleitet, wobei man die Präfixe "E_" und
    * "O_" sowie die Postfixe "_LO" und "_HI" wegläßt und anschließend eine
    * Konvertierung gemäß den Konventionen für Java Methodennamen  durchführt.
    *
    * Diese Bezeichnung der Register unterscheidet sich manchmal von der
    * Bezeichnung der Bits innerhalb der Register, obwohl diese Bits das
    * komplette Register ausfüllen. So wird z.B. das Register 0x0E0
    * VBI_PACK_SIZE genannt, sämtliche Bits innerhalb des Registers werden
    * aber mit "VBI_PKT_LO" bezeichnet. Die Methoden heißen daher
    * [sg]etVBIPackSize.
    */
   
   /**** inline ****/ private final int readSplitRegister(int highOff, int lowOff){
      return readRegister(highOff) << 8 | readRegister(lowOff);
   }
   
   /**** inline ****/ private final int readSplitRegister(int highOff, int highMask, int highShift, int lowOff){
      return ((readRegister(highOff) & highMask) << highShift) | readRegister(lowOff) ;
   }
   
   /**** inline ****/ private final void writeSplitRegister(int highOff, int lowOff, int val){
      writeRegister(highOff, val >> 8);
      writeRegister(lowOff, val & 0xff);
   }
   
   /**** inline ****/ private final void writeSplitRegister(int highOff, int highMask, int highShift, int lowOff, int value){
      int oldval = readRegister(highOff) & ~highMask;
      writeRegister(highOff, ((value >> highShift) & highMask) | oldval);
      writeRegister(lowOff, value & 0xff);
   }
   
   /* VDelay */
   int getVDelayEven(){
      /* See strange note in bt878 spec for mixing odd and even registers. */
      return readSplitRegister(O_CROP, 0xc0, 2, E_VDELAY_LO);
   }
   void setVDelayEven(int val){
      /* See strange note in bt878 spec for mixing odd and even registers. */
      writeSplitRegister(O_CROP, 0xc0, 2, E_VDELAY_LO, val);
   }
   
   int getVDelayOdd(){
      /* See strange note in bt878 spec for mixing odd and even registers. */
      return readSplitRegister(E_CROP, 0xc0, 2, O_VDELAY_LO);
   }
   void setVDelayOdd(int val){
      /* See strange note in bt878 spec for mixing odd and even registers. */
      writeSplitRegister(E_CROP, 0xc0, 2, O_VDELAY_LO, val);
   }
   
   /* VActive */
   int getVActiveEven(){
      return readSplitRegister(E_CROP, 0x30, 4, E_VACTIVE_LO);
   }
   void setVActiveEven(int val){
      writeSplitRegister(E_CROP, 0x30, 4, E_VACTIVE_LO, val);
   }
   
   int getVActiveOdd(){
      return readSplitRegister(O_CROP, 0x30, 4, O_VACTIVE_LO);
   }
   void setVActiveOdd(int val){
      writeSplitRegister(O_CROP, 0x30, 4, O_VACTIVE_LO, val);
   }
   
   /* HDelay */
   int getHDelayEven(){
      return readSplitRegister(E_CROP, 0x0c, 6, E_HDELAY_LO);
   }
   void setHDelayEven(int val){
      writeSplitRegister(E_CROP, 0x0c, 6, E_HDELAY_LO, val);
   }
   int getHDelayOdd(){
      return readSplitRegister(O_CROP, 0x0c, 6, O_HDELAY_LO);
   }
   void setHDelayOdd(int val){
      writeSplitRegister(O_CROP, 0x0c, 6, O_HDELAY_LO, val);
   }
   
   /* HActive */
   int getHActiveEven(){
      return readSplitRegister(E_CROP, 0x03, 8, E_HACTIVE_LO);
   }
   void setHActiveEven(int val){
      writeSplitRegister(E_CROP, 0x03, 8, E_HACTIVE_LO, val);
   }
   int getHActiveOdd(){
      return readSplitRegister(O_CROP, 0x03, 8, O_HACTIVE_LO);
   }
   void setHActiveOdd(int val){
      writeSplitRegister(O_CROP, 0x03, 8, O_HACTIVE_LO, val);
   }
   
   
   /* HScale */
   int getHScaleEven(){
      return readSplitRegister(E_HSCALE_HI, E_HSCALE_LO);
   }
   void setHScaleEven(int val){   
      writeSplitRegister(E_HSCALE_HI, E_HSCALE_LO, val);
   }
   int getHScaleOdd(){
      return readSplitRegister(O_HSCALE_HI, O_HSCALE_LO);
   }
   void setHScaleOdd(int val){   
      writeSplitRegister(O_HSCALE_HI, O_HSCALE_LO, val);
   }
   
   
   void setGeometryOdd(int hscale, int hdelay, int hactive, 
		       int vscale, int vdelay, int vactive){
      Debug.out.println("setGeometryOdd ("+hscale+","+hdelay+","+hactive+","+vscale+","+vdelay+","+vactive+")");
      
      setHScaleOdd(hscale);
      setHDelayOdd(hdelay);
      setHActiveOdd(hactive);
      
      setVScaleOdd(vscale);
      setVDelayOdd(vdelay);
      setVActiveOdd(vactive);
   }
   
   void setGeometryEven(int hscale, int hdelay, int hactive, 
			int vscale, int vdelay, int vactive){
      Debug.out.println("setGeometryEven("+hscale+","+hdelay+","+hactive+","+vscale+","+vdelay+","+vactive+")");
      
      setHScaleEven(hscale);
      setHDelayEven(hdelay);
      setHActiveEven(hactive);
      
      setVScaleEven(vscale);
      setVDelayEven(vdelay);
      setVActiveEven(vactive);
   }
   
   /* 
    * The following Luma/Chroma registers have a strange oddity: The lower
    * byte is contained in one registers, but the most MSB is part of two
    * registers, which are field-sensitive!
    * 
    */
   
   /**** inline ****/ private final int readMixedSplitRegister(int high1, int high2, int highMask, int highShift, int low){
      int val;
      val  = (readRegister(high1) & highMask) << highShift;
      val |= (readRegister(high2) & highMask) << highShift;   /* :-] */
      val |= readRegister(low);
      return val;
   }
   /**** inline ****/ private final void writeMixedSplitRegister(int high1, int high2, int highMask, int highShift, int low, int val){
      writeRegister(low, val & 0xff);
      val = (val >> highShift) & highMask;
      
      int oldval;
      oldval = readRegister(high1) & ~highMask;
      writeRegister(high1, val | oldval);
      oldval = readRegister(high2) & ~highMask;
      writeRegister(high2, val | oldval);
   }
   
   
   /* Luma Gain */
   int getContrast(){
      return readMixedSplitRegister(E_CONTROL, O_CONTROL, 0x04, 6, CONTRAST_LO);
   }
   void setContrast(int val){
      writeMixedSplitRegister(E_CONTROL, O_CONTROL, 0x04, 6, CONTRAST_LO, val);
   }
   
   /* Chroma (U) Gain */
   int getSatU(){
      return readMixedSplitRegister(E_CONTROL, O_CONTROL, 0x02, 7, SAT_U_LO);
   }
   void setSatU(int val){
      writeMixedSplitRegister(E_CONTROL, O_CONTROL, 0x02, 7, SAT_U_LO, val);
   }
   
   /* Chroma (V) Gain */
   int getSatV(){
      return readMixedSplitRegister(E_CONTROL, O_CONTROL, 0x01, 8, SAT_V_LO);
   }
   void setSatV(int val){
      writeMixedSplitRegister(E_CONTROL, O_CONTROL, 0x01, 8, SAT_V_LO, val);
   }
   
   
   /* VScale */
   int getVScaleEven(){
      return readSplitRegister(E_VSCALE_HI, VSCALE_MASK, 8, E_VSCALE_LO);
   }
   void setVScaleEven(int val){
      writeSplitRegister(E_VSCALE_HI, VSCALE_MASK, 8, E_VSCALE_LO, val);
   }
   int getVScaleOdd(){
      return readSplitRegister(O_VSCALE_HI, VSCALE_MASK, 8, O_VSCALE_LO);
   }
   void setVScaleOdd(int val){
      writeSplitRegister(O_VSCALE_HI, VSCALE_MASK, 8, O_VSCALE_LO, val);
   }
   
   
   /* Total Line Count */
   int getVTotal(){
      return readSplitRegister(VTOTAL_HI, VTOTAL_LO);
   }
   void setVTotal(int val){
      writeSplitRegister(VTOTAL_HI, VTOTAL_LO, val);
   }
   
   /* VBI Packet Size */
   int getVBIPackSize(){
      return readSplitRegister(VBI_PACK_DEL, 0x01, 8, VBI_PACK_SIZE);
   }
   void setVBIPackSize(int val){
      writeSplitRegister(VBI_PACK_DEL, 0x01, 8, VBI_PACK_SIZE, val);
   }
   

   /******************************/
   /* Simple Registers           */
   /******************************/
   
   int getBright(){
      return readRegister(BRIGHT);
   }
   void setBright(int val){
      writeRegister(BRIGHT, val);
   }
   
   int getHue(){
      return readRegister(HUE);
   }
   void setHue(int val){
      writeRegister(HUE, val);
   }
   
   int getADelay(){
      return readRegister(ADELAY);
   }
   void setADelay(int val){
      writeRegister(ADELAY, val);
   }
   
   int getBDelay(){
      return readRegister(BDELAY);
   }
   void setBDelay(int val){
      writeRegister(BDELAY, val);
   }
   
   void sreset(){
      writeRegister(SRESET, 0);
   }
   
   int getTGLB(){
      return readRegister(TGLB);
   }
   void setTGLB(int val){
      writeRegister(TGLB, val);
   }
   
   int getFCap(){
      return readRegister(FCAP);
   }
   void clearFCap(){
      writeRegister(FCAP, 0);		/* writing anything resets register */
   }
   
   int getI2C(){
      return readRegister(I2C);
   }
   int getDB2(){
      return readPackedRegister(I2C, I2CDB2, I2CDB2_OFFSET_BIT);
   }
   void setI2C(int value){
      writeRegister(I2C, value);
   }
   
   int getRiscStartAdd(){
      return readRegister(RISC_STRT_ADD);
   }
   void setRiscStartAdd(int val){
      writeRegister(RISC_STRT_ADD, val);
   }
   
   int getGPIOOutEn(){
      return readRegister(GPIO_OUT_EN);
   }
   void setGPIOOutEn(int val){
      writeRegister(GPIO_OUT_EN, val);
   }
   
   int getRiscCount(){
      return readRegister(RISC_COUNT);
   }
   
   int getGPIOData(int off){
      return readRegister(GPIO_DATA+off);
   }
   void setGPIOData(int off, int val){
      writeRegister(GPIO_DATA+off, val);
   }
   
   /******************************/
   /* Packed Register            */
   /******************************/
   /**** Note: Only a few used Packed Registers are implemented. ****/
   
   /**** inline ****/ private int readPackedRegister(int off, int mask, int shift){
      return (readRegister(off) & mask) >> shift;
   }
   /**** inline ****/ private void writePackedRegister(int off, int mask, int shift, int value){
      int oldval = readRegister(off) & ~mask ;
      writeRegister(off, ((value << shift) & mask) | oldval);
   }
   
   
   int getMuxSel(){
      return readPackedRegister(IFORM, 0x60, 5);
   }
   void setMuxSel(int val){
      writePackedRegister(IFORM, 0x60, 5, val);
   }
   int getFormat(){
      return readPackedRegister(IFORM, 0x07, 0);
   }
   void setFormat(int val){
      writePackedRegister(IFORM, 0x07, 0, val);
   }
   void setIForm(int muxSel, int format){
      writeRegister(IFORM, (muxSel << 5) & 0x60 | (format & 0x07));
   }
   
   void setTDec(boolean byField, boolean alignOddField, int rate){
      int i = 0;
      if( byField )
	i |= DEC_FIELD;
      if( alignOddField )
	i |= FLDALIGN;
      i |= rate & DEC_RAT;
      writeRegister(TDEC, i);
   }
   
   int getColorOdd(){
      return readPackedRegister(COLOR_FMT, 0xf0, 4);
   }
   void setColorOdd(int val){
      writePackedRegister(COLOR_FMT, 0xf0, 4, val);
   }
   int getColorEven(){
      return readPackedRegister(COLOR_FMT, 0x0f, 0);
   }
   void setColorEven(int val){
      writePackedRegister(COLOR_FMT, 0x0f, 0, val);
   }
   void setColor(int odd, int even){
      writeRegister(COLOR_FMT, ((odd << 4) & 0xf0) | (even & 0x0f) );
   }
   
   void setTGCKI(int val){
      writePackedRegister(TGCTRL, TGCKI, TGCKI_OFFSET, val);
   }
   
   /******************************/
   /* Bit Register               */
   /******************************/
   /**** Note: Only a few used Bit Registers are implemented. ****/
   
   /**** inline ****/ private int readBitRegister(int off, int mask){
      return readRegister(off) & mask;
   }
   /**** inline ****/  private void writeBitRegister(int off, int mask, int value){
      int old = readRegister(off) & ~mask ;
      writeRegister(off, old | (value & mask));
   }
   
   int getDStatus(){
      return readRegister(DSTATUS);
   }
   int getDStatus(int mask){
      return readBitRegister(DSTATUS, mask);
   }
   void setDStatus(int mask, int val){
      writeBitRegister(DSTATUS, mask, val);
   }
   void clearPLock(){
      writeBitRegister(DSTATUS, PLOCK, 0);
   }
   
   void setChromaCombOdd(boolean onOff){
      writeBitRegister(O_VSCALE_HI, VSCALE_COMB, onOff ? VSCALE_COMB : 0);
   }
   boolean getChromaCombOdd(){
      return readBitRegister(O_VSCALE_HI, VSCALE_COMB) != 0 ? true : false;
   }
   void setChromaCombEven(boolean onOff){
      writeBitRegister(E_VSCALE_HI, VSCALE_COMB, onOff ? VSCALE_COMB : 0);
   }
   boolean getChromaCombEven(){
      return readBitRegister(E_VSCALE_HI, VSCALE_COMB) != 0 ? true : false;
   }
   void setInterlacedOdd(boolean vsfldalign, boolean interlaced){
      writeBitRegister(O_VSCALE_HI, VSCALE_VSFLDALIGN|VSCALE_INT, 
		       (vsfldalign ? VSCALE_VSFLDALIGN : 0) | (interlaced ? VSCALE_INT : 0) );
   }
   void setInterlacedEven(boolean vsfldalign, boolean interlaced){
      writeBitRegister(O_VSCALE_HI, VSCALE_VSFLDALIGN|VSCALE_INT, 
		       (vsfldalign ? VSCALE_VSFLDALIGN : 0) | (interlaced ? VSCALE_INT : 0) );
   }
   
   int getControlOdd(int mask){
      return readBitRegister(O_CONTROL, mask);
   }
   void setControlOdd(int mask, int val){
      writeBitRegister(O_CONTROL, mask, val);
   }
   int getControlEven(int mask){
      return readBitRegister(E_CONTROL, mask);
   }
   void setControlEven(int mask, int val){
      writeBitRegister(E_CONTROL, mask, val);
   }
   
   boolean getCSleep(){
      return readBitRegister(ADC, C_SLEEP) != 0;
   }
   void setCSleep(boolean sleep){
      writeBitRegister(ADC, C_SLEEP, sleep ? C_SLEEP : 0);
   }
   
   boolean getColorBars(){
      return readBitRegister(COLOR_CTL,COLOR_BARS) != 0;
   }
   void setColorBars(boolean val){
      int i = val ? COLOR_BARS : 0;
      writeBitRegister(COLOR_CTL, COLOR_BARS, i);
   }
   boolean getGamma(){
      return readBitRegister(COLOR_CTL, GAMMA) != 0;
   }
   void setGamma(boolean disable){
      writeBitRegister(COLOR_CTL, GAMMA, disable ? GAMMA : 0);
   }
   
   void setCapCtl(int maskval){
      writeBitRegister(CAP_CTL, maskval, maskval);
   }
   void setCapCtl(int mask, int val){
      writeBitRegister(CAP_CTL, mask, val);
   }
   int getCapCtl(int mask){
      return readBitRegister(CAP_CTL, mask);
   }
   
   void setDMACtl(int maskval){
      writeBitRegister(GPIO_DMA_CTL, maskval, maskval);
   }
   void setDMACtl(int mask, int val){
      writeBitRegister(GPIO_DMA_CTL, mask, val);
   }
   int getPackedFifoTriggerPoint(){
      return readPackedRegister(GPIO_DMA_CTL, PKTP, 2);
   }
   void setPackedFifoTriggerPoint(int val){
      writePackedRegister(GPIO_DMA_CTL, PKTP, 2, val);
   }
   void setFifoTriggerPoints(int packed, int planar1, int planar23){
      int val = 0;
      val |= (packed << 0x2) & PKTP;
      val |= (planar1 << 0x4) & PLTP1;
      val |= (planar23 << 0x6) & PLTP23;
      writeBitRegister(GPIO_DMA_CTL, PKTP | PLTP1 | PLTP23, val);
   }
   
   int getIntStat(int mask){
      return readBitRegister(INT_STAT, mask);
   }
   void clearIntStat(int mask){
      writeRegister(INT_STAT, mask);
   }
   
   int getIntMask(){
      return readRegister(INT_MASK);
   }
   void setIntMask(int maskval){
      writeBitRegister(INT_MASK, maskval, maskval);
   }
   void setIntMask(int mask, int val){
      writeBitRegister(INT_MASK, mask, val);
   }

   
   /******************************/
   /* Special Registers          */
   /******************************/
   
   /* Phase Lock Loop (PLL) Reference Registers */
   void setPLL(int pllX, int pllI, int pllF, int pllC){
      writeSplitRegister(PLL_F_HI, PLL_F_LO, pllF);
      writeRegister(PLL_XCI, 
		    ((pllX << PLL_X_OFFSET_BIT) & PLL_X) |
		    ((pllC << PLL_C_OFFSET_BIT) & PLL_C) |
		    ((pllI << PLL_I_OFFSET_BIT) & PLL_I) );
   }
   
   void setPLL(boolean pllX, int pllI, int pllF, boolean pllC){
      setPLL(pllX? 1: 0, pllI, pllF, pllC? 1: 0);
   }
      
   void setPLL(BT878PLL pll){
      setPLL(pll.pllX, pll.pllI, pll.pllF, pll.pllC);
   }
   
   BT878PLL getPLL(){
      int xci = readRegister(PLL_XCI);
      return new BT878PLL((xci & PLL_X) != 0,
			  (xci & PLL_I) >> PLL_I_OFFSET_BIT,
			  readSplitRegister(PLL_F_HI, PLL_F_LO),
			  (xci & PLL_C) != 0);
   }
   
   /******************************/
   /* Register Test              */
   /******************************/
   
   public void registerTest(DebugPrintStream out){
      out.println("writing something to E_VDELAY_LO...");
      localRegisters.set8(E_VDELAY_LO, (byte)0);   /* E_VDELAY_LO */
      
      out.println("looking for some typical info in local registers...");
      
      int i;
      i = readRegister(E_HSCALE_LO);	/* E_HSCALE_LO */
      out.println("e_hscale_lo = 0x" + Integer.toHexString(i) + " (0xac)");
      i = readRegister(O_HSCALE_LO);	/* O_HSCALE_LO */
      out.println("o_hscale_lo = 0x" + Integer.toHexString(i) + " (0xac)");
      i = readRegister(CONTRAST_LO);	/* CONTRAST_LO */
      out.println("contrast_lo = 0x" + Integer.toHexString(i) + " (0xd8)");
      out.println("that's enough looking!");
   }
   
   /******************************/
   /* Device Status              */
   /******************************/
   
   final static StatusFlag dstatusFlags[] = {
      new StatusFlag(0x80, "no VPRES", "VPRES"),
      new StatusFlag(0x40, "no HLOCK", "HLOCK"),
      new StatusFlag(0x20, "odd field", "even field"),
      new StatusFlag(0x10, "NTSC/PAL-M", "PAL/SECAM"),
      new StatusFlag(0x04, "PLL OK", "POLOCK"),
      new StatusFlag(0x02, "no LOF", "LOF"),
      new StatusFlag(0x01, "no COF", "COF")
   };

   final static StatusFlag istatusFlags[] = {
      new StatusFlag(0x08000000, "RISC_DIS", "RISC_ENA"),
      new StatusFlag(0x02000000, "no RACK", "RACK"),
      new StatusFlag(0x01000000, "odd field", "even field"),
      new StatusFlag(0x00080000, "no SCERR", "SCERR"),
      new StatusFlag(0x00040000, "no OCERR", "OCERR"),
      new StatusFlag(0x00020000, "no PABORT", "PABORT"),
      new StatusFlag(0x00010000, "no RIPERR", "RIPERR"),
      new StatusFlag(0x00008000, "no PPERR", "PPERR"),
      new StatusFlag(0x00004000, "no FDSR", "FDSR"),
      new StatusFlag(0x00002000, "no FTRGT", "FTRGT"),
      new StatusFlag(0x00001000, "no FBUS", "FBUS"),
      new StatusFlag(0x00000800, "no RISCI", "RISCI"),
      new StatusFlag(0x00000200, "no GPINT", "GPINT"),
      new StatusFlag(0x00000100, "no I2CDONE", "I2CDONE"),
      new StatusFlag(0x00000020, "no VPRES", "VPRES"),
      new StatusFlag(0x00000010, "no HLOCK", "HLOCK"),
      new StatusFlag(0x00000008, "no OFLOW", "OFLOW"),
      new StatusFlag(0x00000004, "no HSYNC", "HSYNC"),
      new StatusFlag(0x00000002, "no VSYNC", "VSYNC"),
      new StatusFlag(0x00000001, "no FMTCHG", "FMTCHG"),
   };

   
   public String status(int options){
      return status(options, readRegister(DSTATUS));
   }
   static public String status(int options, int value){
      String str = StatusFlag.decode(value & 0xff, dstatusFlags, options);
      return "(0x"+Hex.toHexString((byte)value)+") "+str;
   }
   
   public String interruptStatus(int options){
      return interruptStatus(options, readRegister(INT_STAT));
   }
   static public String interruptStatus(int options, int value){
      String str = StatusFlag.decode(value, istatusFlags, options);
      return "(0x"+Hex.toHexString(value)+") "+str;
   }
   
   void dumpControlBlock(DebugPrintStream out, int bytes){
      int n = Math.min(localRegisters.size(), bytes);
      Hex.dumpHex32(out, localRegisters, n);
   }
}

/********************************************************************/

interface BT878Defines {
   /******************************/
   /* Register Names             */
   /******************************/
   int DSTATUS		= 0x000;
   int IFORM		= 0x004;
   int TDEC		= 0x008;
   int E_CROP		= 0x00c;
   int O_CROP		= 0x08c;
   int E_VDELAY_LO	= 0x090;	// CAVEAT: E_VDELAY_LO and O_VDELAY_LO
   int O_VDELAY_LO	= 0x010;	// are really "swapped".
   int E_VACTIVE_LO	= 0x014;
   int O_VACTIVE_LO	= 0x094;
   int E_HDELAY_LO	= 0x018;
   int O_HDELAY_LO	= 0x098;
   int E_HACTIVE_LO	= 0x01c;
   int O_HACTIVE_LO	= 0x09c;
   int E_HSCALE_HI	= 0x020;
   int O_HSCALE_HI	= 0x0a0;
   int E_HSCALE_LO	= 0x024;
   int O_HSCALE_LO	= 0x0a4;
   int BRIGHT		= 0x028;
   int E_CONTROL	= 0x02c;
   int O_CONTROL	= 0x0ac;
   int CONTRAST_LO	= 0x030;
   int SAT_U_LO		= 0x034;
   int SAT_V_LO		= 0x038;
   int HUE		= 0x03c;
   int E_SCLOOP		= 0x040;
   int O_SCLOOP		= 0x0c0;
   int WC_UP		= 0x044;
   int OFORM		= 0x048;
   int E_VSCALE_HI	= 0x04c;
   int O_VSCALE_HI	= 0x0cc;
   int E_VSCALE_LO	= 0x050;
   int O_VSCALE_LO	= 0x0d0;
   int TEST		= 0x054;
   int ADELAY		= 0x060;
   int BDELAY		= 0x064;
   int ADC		= 0x068;
   int E_VTC		= 0x06c;
   int O_VTC		= 0x0ec;
   int SRESET		= 0x07c;
   /* FIXME: White Crush Down (0x078) hat keinen Namen. */
   int TGLB		= 0x080;
   int TGCTRL		= 0x084;
   int VTOTAL_LO	= 0x0b0;
   int VTOTAL_HI	= 0x0b4;
   int COLOR_FMT	= 0x0d4;
   int COLOR_CTL	= 0x0d8;
   int CAP_CTL		= 0x0dc;
   int VBI_PACK_SIZE	= 0x0e0;
   int VBI_PACK_DEL	= 0x0e4;
   int FCAP		= 0x0e8;
   int PLL_F_LO		= 0x0f0;
   int PLL_F_HI		= 0x0f4;
   int PLL_XCI		= 0x0f8;
   int DVSIF		= 0x0fc;
   int INT_STAT		= 0x100;
   int INT_MASK		= 0x104;
   int GPIO_DMA_CTL	= 0x10c;
   int I2C		= 0x110;	// CAVEAT: no official name in spec
   int RISC_STRT_ADD	= 0x114;
   int GPIO_OUT_EN	= 0x118;
   int RISC_COUNT	= 0x120;
   int GPIO_DATA	= 0x200;	// Note: This "register" ends at 0x2ff !!

   /********************************/
   /* Register Bits (some of them) */
   /* Theese are mostly bitmasks.  */
   /********************************/
   
   /* DSTATUS */
   int PRES	= 0x80;
   int HLOC 	= 0x40;
   int STAT_FIELD= 0x20;
   int NUML 	= 0x10;
   int PLOCK 	= 0x04;
   int LOF 	= 0x02;
   int COF	= 0x01;
   int DSTATUS_MASK = PRES|HLOC|STAT_FIELD|NUML|PLOCK|LOF|COF;
   
   /* TDEC */
   int DEC_FIELD	= 0x80;
   int FLDALIGN		= 0x40;
   int DEC_RAT		= 0x3f;
   
   /* E_CONTROL / O_CONTROL */
   int CONTROL_LNOTCH	= 0x80;
   int CONTROL_COMP	= 0x40;
   
   /* E_VSCALE_HI / O_VSCALE_HI */
   int VSCALE_VSFLDALIGN = 0x80;
   int VSCALE_COMB	= 0x40;
   int VSCALE_INT	= 0x20;
   int VSCALE_MASK	= 0x1f;
   
   /* ADC */
   int ADC_RESERVED	= 0x80;		/* must be set */
   int AGC_EN		= 0x10;
   int CLK_SLEEP	= 0x08;
   int Y_SLEEP		= 0x04;
   int C_SLEEP		= 0x02;
   int CRUSH		= 0x01;
     
   /* TGCTRL */
   int TGCKO	= 0x60;
   int TGCKI	= 0x18;
   int TGC_AI	= 0x04;
   int GPC_AR	= 0x02;
   int TGC_VM	= 0x01;
   int TGCKO_OFFSET = 5;
   int TGCKI_OFFSET = 3;
   
   /* COLOR_CTL */
   int EXT_FRMRATE	= 0x80;
   int COLOR_BARS	= 0x40;
   int RGB_DED		= 0x20;
   int GAMMA		= 0x10;
   int WSWAP_ODD	= 0x08;
   int WSWAP_EVEN	= 0x04;
   int BSWAP_ODD	= 0x02;
   int BSWAP_EVEN	= 0x01;
   
   /* CAP_CTL */
   int DITH_FRAME	= 0x10;
   int CAPTURE_VBI_ODD	= 0x08;
   int CAPTURE_VBI_EVEN	= 0x04;
   int CAPTURE_ODD	= 0x02;
   int CAPTURE_EVEN	= 0x01;
   
   /* PLL_XCI */
   int PLL_X	= 0x80;
   int PLL_C	= 0x40;
   int PLL_I	= 0x3f;
   int PLL_X_OFFSET_BIT	= 7;
   int PLL_C_OFFSET_BIT = 6;
   int PLL_I_OFFSET_BIT = 0;
   
   /* INT_STAT */
   int RISCS	= 0xf0000000;
   int RISC_EN	= 0x08000000;
   int RACK	= 0x02000000;
   int INT_FIELD= 0x01000000;
   int SCERR	= 0x00080000;
   int OCERR	= 0x00040000;
   int PABORT	= 0x00020000;
   int RIPERR	= 0x00010000;
   int PPERR	= 0x00008000;
   int FDSR	= 0x00004000;
   int FTRGT	= 0x00002000;
   int FBUS	= 0x00001000;
   int RISCI	= 0x00000800;
   int GPINT	= 0x00000200;
   int I2CDONE	= 0x00000100;
   int INT_VPRES= 0x00000020;
   int INT_HLOCK= 0x00000010;
   int OFLOW	= 0x00000008;
   int HSYNC	= 0x00000004;
   int VSYNC	= 0x00000002;
   int FMTCHG	= 0x00000001;
   
   int INT_STAT_PCI	= PABORT | RIPERR | PPERR | FTRGT | FBUS;   /* PCI related bits */
   int INT_STAT_MASK	= 0xfb0ffb3f;	/* all valid bits of INT_STAT (including RO bits)*/
   
   /* GPIO_DMA_CTL */
   int PLTP23		= 0x00c0;
   int PLTP1		= 0x0030;
   int PKTP		= 0x000c;
   int RISC_ENABLE	= 0x0002;
   int FIFO_ENABLE	= 0x0001;
   
   /* I2C */
   int I2CDB0		= 0xff000000;
   int I2CDB1		= 0x00ff0000;
   int I2CDB2		= 0x0000ff00;
   int I2CMODE		= 0x00000080;
   int I2CRATE		= 0x00000040;
   int I2CNOSTOP	= 0x00000020;
   int I2CNOS1B		= 0x00000010;
   int I2CSYNC		= 0x00000008;
   int I2CW3BRA		= 0x00000004;
   int I2CSCL		= 0x00000002;
   int I2CSDA		= 0x00000001;
   
   int I2CDB0_OFFSET_BIT = 24;
   int I2CDB1_OFFSET_BIT = 16;
   int I2CDB2_OFFSET_BIT =  8;
   
   
   /******************************/
   /* Packed Defines ;-)	 */
   /******************************/
   
   /* IFORM */
   int MUX3		= 0x0;
   int MUX2		= 0x1;
   int MUX0		= 0x2;
   int MUX1		= 0x3;
   
   int FORMAT_AUTO	= TVNorms.FORMAT_AUTO;
   int FORMAT_NTSC_M	= TVNorms.FORMAT_NTSC_M;
   int FORMAT_NTSC_J	= TVNorms.FORMAT_NTSC_J;
   int FORMAT_PAL_BDGHI	= TVNorms.FORMAT_PAL_BDGHI;
   int FORMAT_PAL_M	= TVNorms.FORMAT_PAL_M;
   int FORMAT_PAL_N	= TVNorms.FORMAT_PAL_N;
   int FORMAT_SECAM	= TVNorms.FORMAT_SECAM;
   int FORMAT_PAL_NC	= TVNorms.FORMAT_PAL_NC;		// PAL (N-combination)
   
   int MUXSEL		= 0x60;		// bitmask
   int XTBOTH		= 0x18;		// CAVEAT: undocumented in bt878/879 manual
   int FORMAT_MASK	= 0x07;
     
   /* TGCTRL */
   int TGCKO_CLK1	= 0x0;
   int TGCKO_XTAL0	= 0x1;
   int TGCKO_PLL	= 0x2;
   int TGCKO_IPLL	= 0x3;
   int TGCKI_XTAL	= 0x0;
   int TGCKI_PLL	= 0x1;
   int TGCKI_GPCLK	= 0x2;
   int TGCKI_IGPCLK	= 0x3;
   
   /* COLOR_FMT */
   int COLOR_FMT_RGB32	= 0x0;
   int COLOR_FMT_RGB24	= 0x1;
   int COLOR_FMT_RGB16	= 0x2;
   int COLOR_FMT_RGB15	= 0x3;
   int COLOR_FMT_YUY2	= 0x4;
   int COLOR_FMT_BtYUV	= 0x5;
   int COLOR_FMT_Y8	= 0x6;
   int COLOR_FMT_RGB8	= 0x7;
   int COLOR_FMT_YCrCb422= 0x8;
   int COLOR_FMT_YCrCb411= 0x9;
   int COLOR_FMT_RAW	= 0xe;
}

