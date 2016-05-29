package jx.fs;

import jx.zero.Clock;
import jx.bio.BlockIO;
import jx.fs.buffercache.BufferCache;

/**
 * Dieses Interface stellt Operationen zur Verf&uuml;gung, die das Dateisystem als Ganzes betreffen. Eine konkrete
 * Dateisystem-Implementierung stellt dem virtuellen Dateisystem damit Funktionen zur Verf&uuml;gung, um ein Dateisystem
 * zu an- und abzumelden, zu erzeugen und zu reparieren.
 */
public interface FileSystem extends jx.zero.Portal {

    /**
     * Stellt der Filesystem-Implementierung ein Blockdevice
     * zur Verfuegung.
     */
    void init(BlockIO blockDevice, BufferCache bufferCache, Clock clock);

    /**
     * Liefert den Namen des Dateisystems zur&uuml;ck.
     *
     * @return Zeichenkette mit dem Dateisystem-Namen
     */
    String name();



    /**
     * Liefert die Root-Inode dieses Dateisystems zur&uuml;ck.
     *
     * @return die Root-Inode des Dateisystems
     */
    Inode getRootInode();

    /**
     * Initialisiert das Dateisystem (wird vom <code>mount</code>-Mechanismus der Klasse <code>FS</code> verwendet).
     * Auf der angegebenen Partition mu&szlig; sich das entsprechende Dateisystem befinden.
     *
     * @param read_only falls <code>true</code>, wird das Dateisystem als nur lesbar gekennzeichnet
     */
    void init(boolean read_only);

    /**
     * Gibt vom Dateisystem belegte Strukturen wieder frei und schreibt Bl&ouml;cke, die als
     * "dirty" markiert sind, auf die entsprechende Partition.
     */
    void release();

    /**
     * Erzeugt ein neues Dateisystem auf der angegebenen Partition (die Partition
     * wird &uuml;berschrieben).
     *
     * @param name      der Name des neuen Dateisystems
     * @param blocksize die Blockgr&ouml;&szlig;e innerhalb des Dateisystems (Vielfache von 512)
     */
    void build(String name, int blocksize);

    /**
     * &Uuml;berpr&uuml;ft das Dateisystem auf der angegebenen Partition und repariert es gegebenenfalls,
     * um es in einen konsistenten Zustand zu bringen.
     *
     */
    void check();

    Inode getInode(int identifier) throws FSException;

//     int getDeviceID();
    Integer getDeviceID();
}
