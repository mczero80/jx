package bioide;

import jx.zero.*;
import jx.timer.SleepManager;

/**
 * Partitions of a drive.
 * @author Michael Golm
 * @author Andreas Weissel
 */
public class Partitions {
    IDEDeviceImpl dev;

    Partitions(IDEDeviceImpl dev) {
	this.dev = dev;
    }


    /**
     * Erzeugt eine Partitionskennung aus Controller-, Laufwerks- und Partitionsnummer.
     */
    private int getDevice(int controller, int unit, int partition) {
	return ((controller & 1) | ((unit & 1) << 1) | ((partition & 15) << 4));
    }

    /**
     * Extrahiert die Nummer des Controllers aus der Partitionskennung.
     */
    private int getController(int device) {
	return (device & 1);
    }

    /**
     * Extrahiert die Nummer des Laufwerks aus der Partitionskennung.
     */
    private int getUnit(int device) {
	return (device & 2);
    }

    /**
     * Extrahiert die Nummer der Partition aus der Partitionskennung.
     */
    private int getPartition(int device) {
	return ((device >> 4) & 15);
    }



    /**
     * Ermittelt das Vorhandensein der angegebenen Partition.
     *
     * @param  device die Kennung der Partition
     * @return <code>true</code>, falls die Partition vorhanden ist, ansonsten <code>false</code>
     */
    /*
    public boolean isPresent(int device) {
	int contr = getController(device);
	int unit  = getUnit(device);
	int part  = getPartition(device);
	if ((contr < 0) || (contr >= MAX_CONTROLLERS) || (unit < 0) || (unit >= MAX_DRIVES))
	    return false;
	return controllers[contr].drives[unit].getPartitionTable().isPresent(part);
	
    }
    */

    /**
     * Liefert die Kapazit&auml;t (die Anzahl der Sektoren) der angegebenen Partition zur&uuml;ck (Partition 0 umfasst die
     * gesamte Festplatte).
     *
     * @param  device die Kennung der Partition
     * @return die Anzahl der Sektoren der Partition (-1 falls die angegebene Partition nicht existiert)
     */
    /*
    public int getCapacity(int device) {
	int contr = getController(device);
	int unit  = getUnit(device);
	int part  = getPartition(device);
	if ((contr < 0) || (contr >= MAX_CONTROLLERS) || (unit < 0) || (unit >= MAX_DRIVES))
	    return -1;
	return controllers[contr].drives[unit].getPartitionTable().getCapacity(part);
    }
    */
}
