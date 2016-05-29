package jx.devices.ide;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.Vector;
import jx.zero.Portal;
import jx.zero.Memory;

/**
 * Diese Klasse bietet Funktionen f&uuml;r die Daten&uuml;bertragung zwischen Hauptspeicher und Festplattencontroller und gibt
 * Auskunft &uuml;ber vorhandene Partitionen und Festplatten und deren Kapazit&auml;t. Sie stellt einen Monitor f&uuml;r den Zugriff
 * auf die IDE-Controller dar.
 * Diese Version setzt Zugriffe auf eine Festplatte in Lese- und Schreiboperationen auf eine Datei um; der Name der Datei entspricht dem
 * Partitionsnamen.
 */
public interface IDEDevice extends Portal {
    /**
     * Ermittelt das Vorhandensein der angegebenen Partition.
     *
     * @param  device die Kennung der Partition
     * @return <code>true</code>, falls die Partition vorhanden ist, ansonsten <code>false</code>
     */
    public boolean isPresent(int device);

    /**
     * Liefert die Kapazit&auml;t (die Anzahl der Sektoren) der angegebenen Partition zur&uuml;ck (Partition 0 umfasst die
     * gesamte Festplatte).
     *
     * @param  device die Kennung der Partition
     * @return die Anzahl der Sektoren der Partition (-1 falls die angegebene Partition nicht existiert)
     */
    public int getCapacity(int device);

    /**
     * Bewirkt das Lesen bzw. Schreiben von Sektoren einer Partition. Diese Methode setzt die Sektornummern innerhalb einer
     * Partition auf Sektornummern innerhalb der Festplatte um und ruft <code>Drive.bufferheadIO</code> f&uuml;r den Datentransfer
     * auf. Es wird au&szlig;erdem &uuml;berpr&uuml;ft, ob die Sektornummer im g&uuml;ltigen Bereich liegt.
     *
     * @param bh          der <code>BufferHead</code>, der die Blocknummer, die Partitionskennung, den Puffer und die Anzahl zu
     *                    &uuml;bertragender Byte f&uuml;r die I/O-Operation enth&auml;lt
     * @param sector      der Sektor, ab dem gelesen werden soll (relativ zum Partitionsanfang)
     * @param read        Falls <code>true</code>, wird von der Partition gelesen, ansonsten wird geschrieben.
     * @param synchronous Falls <code>true</code>, wird auf das Ende der I/O-Operation gewartet (synchrones Lesen bzw. Schreiben).
     * @return -1 if error, 0 if ok
     */
    public int memoryIO(Memory bh, int device, int sector, boolean read, boolean synchronous);

}
