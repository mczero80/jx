package jx.emulation.devices.ide;

public class DeviceNaming {

    public static String deviceToName(int device) {
	String retval = null;
	int part;
	if (device == 99) return "hdemul";
	if ((device & 3) == 0) retval = new String("hda");
	if ((device & 3) == 1) retval = new String("hdc");
	if ((device & 3) == 2) retval = new String("hdb");
	if ((device & 3) == 3) retval = new String("hdd");
	part = (device >> 4) & 15;
	if (part > 0)
	    retval += String.valueOf(part); // ohne +1
	return retval;
    }

    public static int nameToDevice(String name) {
	int device = -1, part = 0;

	if (name.equals("hdemul")) return 99;
	if (name.startsWith("hda")) device = 0;
	if (name.startsWith("hdb")) device = 2;
	if (name.startsWith("hdc")) device = 1;
	if (name.startsWith("hdd")) device = 3;
	if (device == -1)
	    return -1;
	if (name.length() > 3)
	    part = Integer.parseInt(name.substring(3,4)); // ohne -1, besser nur substring(3) ?
	return ((part << 4) | device);
    }


}
