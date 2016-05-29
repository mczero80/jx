package jx.wintv;

/**
 * Key definitions for Hauppauge IR remote control.
 */
public interface RCKeys {
   RCKey key_Tv		= new RCKey("TV",		0x3c);
   RCKey key_ChUp	= new RCKey("Ch+",		0x80);
   RCKey key_Radio	= new RCKey("Radio",		0x30);
   RCKey key_VolDown 	= new RCKey("Vol-",		0x44);
   RCKey key_FullScreen	= new RCKey("Full Screen",	0xb8);
   RCKey key_VolUp 	= new RCKey("Vol+",		0x40);
   RCKey key_Mute 	= new RCKey("Mute",		0x34);
   RCKey key_ChDown 	= new RCKey("Ch-",		0x84);
   RCKey key_Source 	= new RCKey("Source",		0x88);
   RCKey key_1 		= new RCKey("1",		0x04);
   RCKey key_2 		= new RCKey("2",		0x08);
   RCKey key_3 		= new RCKey("3",		0x0c);
   RCKey key_4 		= new RCKey("4",		0x10);
   RCKey key_5 		= new RCKey("5",		0x14);
   RCKey key_6 		= new RCKey("6",		0x18);
   RCKey key_7 		= new RCKey("7",		0x1c);
   RCKey key_8 		= new RCKey("8",		0x20);
   RCKey key_9 		= new RCKey("9",		0x24);
   RCKey key_Reserved 	= new RCKey("Reserved",		0x78);
   RCKey key_0 		= new RCKey("0",		0x00);
   RCKey key_Minimize 	= new RCKey("Minimize",		0x98);
   
   RCKey keytable[] = {
      key_Tv,
	key_ChUp,
	key_Radio,
	key_VolDown,
	key_FullScreen,
	key_VolUp,
	key_Mute,
	key_ChDown,
	key_Source,
	key_1,
	key_2,
	key_3,
	key_4,
	key_5,
	key_6,
	key_7,
	key_8,
	key_9,
	key_Reserved,
	key_0,
	key_Minimize
   };
}
