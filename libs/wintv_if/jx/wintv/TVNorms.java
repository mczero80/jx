package jx.wintv;

public interface TVNorms {
   int fscPAL  = 4433619;
   int fscNTSC = 3579545;
   
   int fsc8PAL  = 35468952;		/* 8*Fsc(PAL)  */
   int fsc8NTSC = 28636360;		/* 8*Fsc(NTSC) */
   
   
   int FORMAT_AUTO	= 0x0;
   int FORMAT_NTSC_M	= 0x1;
   int FORMAT_NTSC_J	= 0x2;
   int FORMAT_PAL_BDGHI	= 0x3;
   int FORMAT_PAL_M	= 0x4;
   int FORMAT_PAL_N	= 0x5;
   int FORMAT_SECAM	= 0x6;
   int FORMAT_PAL_NC	= 0x7;		// PAL (N-combination)

   
   // Linux Defaults
   /* 
    * This are not the values in the tvnorms but the values written into
    * the registers at startup. */
   TVNorm pal_linux_boot = new TVNorm("PAL B/G", fscPAL, 
				      202, 32,
				      768, 576,
				      1135, 625,
				      FORMAT_PAL_BDGHI);
   
   /* The following values are calculated from the following PAL timings:
    * 
    *     total line period:           64 us	== 1135 pixel
    *     picture information:	       52 us	==  922 pixel
    *     front porch:                  1.65 us	==   29 pixel
    * 
    * Which gives 1135-922-29 = 184 pixel for hsync, back porch and color
    * burst. This values roughly comply with the BT878/879 manual page 29.
    * 
    * The vertical value for xoff is the Linux default.
    */
   TVNorm pal_calc = new TVNorm("PAL B/G",  fscPAL,
				184, 32,
				922, 576,
				1135, 625,
				FORMAT_PAL_BDGHI);
   
   /* 
    * The calculated horizontal values gives you a small
    * black stripe at the left. This mode corrects this. 
    */
   TVNorm pal_opt = new TVNorm("PAL B/G",  fscPAL,
				190, 32,
				922, 576,
				1135, 625,
			       FORMAT_PAL_BDGHI);
   
   TVNorm pal_try = new TVNorm("PAL B/G",  fscPAL,
			       190, 32,
			       796, 576,
			       1135, 625,
			       FORMAT_PAL_BDGHI);

   
   TVNorm pal = pal_opt;
   
   // Defaultwerte des Bt878/879
   TVNorm ntsc_bt = new TVNorm("NTSC", fscNTSC, 
			       120, 24,
			       640, 480,
			       910, 525,
			       FORMAT_NTSC_M);

   TVNorm ntsc_full = new TVNorm("NTSC", fscNTSC, 
				 135, 24/*?*/, 
				 754, 480/*?*/,
				 910, 525,
				 FORMAT_NTSC_M);
   
   TVNorm ntsc = ntsc_full;
}
