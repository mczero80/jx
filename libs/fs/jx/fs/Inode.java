package jx.fs;

import jx.zero.Memory;
import jx.zero.ReadOnlyMemory;

import java.util.*;

/**
 * Portal interface to access the files and directories of the filesystem.
 */
public interface Inode extends jx.zero.Portal {
    
    /**
     * Liefert die &uuml;bergeordnete Inode (<code>parent</code>-Verzeichnis) zur&uuml;ck.
     *
     * @return die &uuml;bergeordnete Inode
     */
    Inode getParent();

    /**
     * Setzt die &uuml;bergeordnete Inode (<code>parent</code>-Verzeichnis).
     *
     * @param parent die &uuml;bergeordnete Inode
     */
    void setParent(Inode parent);

    /**
     * Ermittelt, ob die Inode noch geschrieben werden muss ("dirty").
     *
     * @return <code>true</code>, falls die Inode als "dirty" markiert ist
     */
    boolean isDirty();

    /**
     * Legt fest, ob die Inode noch geschrieben werden muss oder ob die Version
     * auf der Partition mit dem Speicherobjekt &uuml;bereinstimmt.
     *
     * @param der neue Zustand der Inode (<code>true</code> f&uuml;r "dirty")
     */
    void setDirty(boolean value);

    /**
     * Erhöht den usecount der Inode.
     */
    void incUseCount();

    /**
     * Gibt die Inode frei. Dem Cache wird mitgeteilt, dass die Inode nicht mehr verwendet wird.
     */
    void decUseCount();

    /**
     * Liefert die Anzahl der Verweise auf die Inode zur&uuml;ck. Bei einem Verzeichnis ist die Anzahl der Verweise gleich der
     * Anzahl an Verzeichnissen, die darin enthalten sind (also mindestens 2 f&uuml;r " . " und " .. "). Weiterhin erh&ouml;ht
     * jeder symbolische Link auf die Inode die Zahl der Verweise.
     */
    int   i_nlinks() throws NotExistException;

    /**
     * L&ouml;scht die Inode (notwendig bei den Kommandos <code>unlink</code> und <code>rmdir</code>).
     */
    void  deleteInode() throws InodeIOException, NotExistException;

    /**
     * Schreibt &Aauml;nderungen, die an der Inode vorgenommen wurden, auf die Partition.
     */
    void  writeInode() throws InodeIOException, NotExistException;

    /**
     * Gibt die Ressourcen, die von der Inode verwendet werden, frei (wird aufgerufen, wenn die Inode
     * aus dem Cache entfernt wird). Die Inode auf der Partition wird nicht gel&ouml;scht, nur ihr Objekt
     * im Speicher.
     */
    void  putInode() throws NotExistException;

    /**
     * &Uuml;berlagert die Inode des angegebenen Verzeichniseintrags mit der Inode eines anderen Dateisystems
     * ("mount"-Mechanismus). Die Verdeckung der urspr&uuml;nglichen Inode findet nicht innerhalb des Dateisystems statt
     * (an der physikalischen Struktur auf der Festplatte &auml;ndert sich nichts), sie wird durch die Methoden dieser Klasse
     * und der <code>FS</code>-Klasse realisiert und bietet dadurch die M&ouml;glichkeit, mehrere Dateisysteme in einen
     * Verzeichnisbaum "einzuh&auml;ngen". Jedesmal, wenn &uuml;ber die Methode <code>lookup</code> (weiter unten) auf die Inode
     * mit dem Namen <code>name</code> zugegriffen wird, wird die Inode <code>newChild</code> zur&uuml;ckgeliefert.
     *
     * @param     newChild die Inode, mit der der urspr&uuml;ngliche Eintrag &uuml;berlagert werden soll
     * @param     name     der Name des Eintrags, der &uuml;berlagert werden soll
     * @exception InodeIOException          falls ein Fehler bei der Ein-/Ausgabe auftritt
     * @exception InodeNotFoundException    falls die zu &uuml;berlagernde Inode nicht gefunden werden kann
     * @exception NoDirectoryInodeException falls es sich nicht um ein Verzeichnis handelt
     * @exception PermissionException       falls die Zugriffsrechte des Verzeichnisses die Operation nicht erlauben
     * @see       #lookup(String name)
     */
    void overlay(Inode newChild, String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException;

    /**
     * Entfernt die &Uuml;berlagerung durch die angegebene Inode.
     *
     * @param     child die &uuml;berlagernde Inode, die entfernt werden soll
     * @exception InodeNotFoundException    falls die &uuml;berlagernde Inode nicht gefunden werden kann
     * @exception NoDirectoryInodeException falls es sich nicht um ein Verzeichnis handelt
     */
    void removeOverlay(Inode child) throws InodeNotFoundException, NoDirectoryInodeException, NotExistException;

    /**
     * Entfernt alle vorhandenen &Uuml;berlagerungen.
     *
     * @exception NoDirectoryInodeException falls es sich nicht um ein Verzeichnis handelt
     */
    void removeAllOverlays() throws NoDirectoryInodeException, NotExistException;

    /**
     * &Uuml;berpr&uuml;ft, ob der Eintrag mit dem angegebenen Namen von einem anderen Dateisystem &uuml;berlagert wird.
     *
     * @param     name  der Name des Eintrags, dessen Zustand ermittelt werden soll
     * @exception NoDirectoryInodeException falls es sich nicht um ein Verzeichnis handelt
     */
    boolean isOverlayed(String name) throws NoDirectoryInodeException, NotExistException;

    /**
     * Liefert die Inode, die dem Verzeichniseintrag mit dem angegebenen Namen zugeordnet ist, sofern vorhanden. Zuerst wird
     * der Name mit " . " und " .. " verglichen und bei &Uuml;bereinstimmung die Inode selbst (<code>this</code>) bzw. die
     * &uuml;bergeordnete Inode (<code>parent</code>) zur&uuml;ckgeliefert. Als N&auml;chstes wird &uuml;berpr&uuml;ft, ob die
     * Inode verdeckt wurde und eventuell die &uuml;berdeckende Inode zur&uuml;ckgegeben. Schlie&szlig;lich muss der Eintrag
     * im Dateisystem gesucht werden, was &uuml;ber die abstrakte Funktion <code>getInode</code> geschieht.
     *
     * @param     name der Name des Verzeichniseintrags, dessen Inode ermittelt werden soll
     * @return    die dem Verzeichniseintrag zugeordnete Inode
     * @exception InodeIOException          falls ein Fehler bei der Ein-/Ausgabe auftritt
     * @exception InodeNotFoundException    falls die gew&uuml;nschte Inode nicht gefunden werden kann
     * @exception NoDirectoryInodeException falls es sich nicht um ein Verzeichnis handelt
     * @exception PermissionException       falls die Zugriffsrechte einer Pfadkomponente die Operation nicht erlauben
     */
    Inode lookup(String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException;

    /**
     * Testet, ob die Inode einen symbolischen Link repr&auml;sentiert.
     *
     * @return <code>true</code>, falls es sich um einen symbolischen Link handelt
     */
    boolean isSymlink() throws NotExistException;

    /**
     * Testet, ob die Inode eine regul&auml;re Datei repr&auml;sentiert.
     *
     * @return <code>true</code>, falls es sich um eine Datei handelt
     */
    boolean isFile() throws NotExistException;

    /**
     * Testet, ob die Inode ein Verzeichnis repr&auml;sentiert.
     *
     * @return <code>true</code>, falls es sich um ein Verzeichnis handelt
     */
    boolean isDirectory() throws NotExistException;

    /**
     * Testet, ob die Inode geschrieben werden kann.
     *
     * @return <code>true</code>, falls es sich um eine Datei handelt und in diese Datei geschrieben werden darf
     */
    boolean isWritable() throws NotExistException;

    /**
     * Testet, ob der Inhalt der Inode gelesen werden kann.
     *
     * @return <code>true</code>, falls es sich um eine Datei handelt und der Inhalt dieser Datei gelesen werden darf
     */
    boolean isReadable() throws NotExistException;

    boolean isExecutable() throws NotExistException;

    /**
     * Liefert den Zeitpunkt der letzten &Auml;nderung zur&uuml;ck.
     *
     * @return den Zeitstempel der letzten &Auml;nderung
     */
    int    lastModified() throws NotExistException;

    int    lastAccessed() throws NotExistException;

    int    lastChanged() throws NotExistException;

    void setLastModified(int time) throws NotExistException;
    void setLastAccessed(int time) throws NotExistException;

    /**
     * Liest den Inhalt des durch diese Inode repr&auml;sentierten Verzeichnisses aus (auch die Eintr&auml;ge " . " und " .. ").
     *
     * @return ein <code>Vector</code>-Objekt, das die Namen der Dateien und Verzeichnisse enth&auml;lt
     * @exception NoDirectoryInodeException falls es sich nicht um ein Verzeichnis handelt
     */
    String[]  readdirNames() throws NoDirectoryInodeException, NotExistException;

    /**
     * Liefert die Inode, die dem Verzeichniseintrag mit dem angegebenen Namen zugeordnet ist (sofern vorhanden).
     *
     * @param     name der Name des Verzeichniseintrags, dessen Inode ermittelt werden soll
     * @return    die dem Verzeichniseintrag zugeordnete Inode
     * @exception InodeIOException          falls ein Fehler bei der Ein-/Ausgabe auftritt
     * @exception InodeNotFoundException    falls die gew&uuml;nschte Inode nicht gefunden werden kann
     * @exception NoDirectoryInodeException falls es sich nicht um ein Verzeichnis handelt
     * @exception PermissionException       falls die Zugriffsrechte des Verzeichnisses die Operation nicht erlauben
     */
    Inode   getInode(String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException;

    /**
     * Erzeugt ein neues Verzeichnis mit dem angegebenen Namen innerhalb des durch diese Inode dargestellten Verzeichnisses,
     * sofern noch kein Eintrag mit diesem Namen vorhanden ist.
     *
     * @param name der Name des neuen Verzeichnisses
     * @param mode die Zugriffsrechte des neuen Verzeichnisses
     * @return die Inode des neu angelegten Verzeichnisses
     * @exception FileExistsException       falls eine Datei oder ein Verzeichnis mit diesem Namen bereits existiert
     * @exception InodeIOException          falls ein Fehler bei der Ein-/Ausgabe auftritt
     * @exception NoDirectoryInodeException falls es sich nicht um ein Verzeichnis handelt
     * @exception PermissionException       falls die Zugriffsrechte des Verzeichnisses die Operation nicht erlauben oder das
     *                                      Dateisystem als nur lesbar angemeldet wurde
     */
    Inode   mkdir(String name, int mode) throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, PermissionException;

    /**
     * Entfernt das Verzeichnis mit dem angegebenen Namen aus dem durch diese Inode dargestellten Verzeichnis. Die Inode,
     * die mit diesem Eintrag verkn&uuml;pft war, wird freigegeben.
     *
     * @param name der Name des zu l&ouml;schenden Verzeichnisses
     * @exception DirNotEmptyException      falls das zu l&ouml;schende Verzeichnis noch Dateien/Verzeichnisse enth&auml;lt
     * @exception InodeIOException          falls ein Fehler bei der Ein-/Ausgabe auftritt
     * @exception InodeNotFoundException    falls die zu l&ouml;schende Inode nicht gefunden werden kann
     * @exception NoDirectoryInodeException falls es sich nicht um ein Verzeichnis handelt
     * @exception PermissionException       falls die Zugriffsrechte des Verzeichnisses die Operation nicht erlauben oder das
     *                                      Dateisystem als nur lesbar angemeldet wurde
     */
    void    rmdir(String name) throws DirNotEmptyException, InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException,PermissionException;

    /**
     * Erzeugt einen neuen Verzeichniseintrag (eine neue Datei) mit dem angegebenen Namen innerhalb des durch diese Inode
     * dargestellten Verzeichnisses.
     *
     * @param name der Name der neuen Datei
     * @param mode die Zugriffsrechte der neuen Datei
     * @return die Inode der neu angelegten Datei
     * @exception FileExistsException       falls eine Datei oder ein Verzeichnis mit diesem Namen bereits existiert
     * @exception InodeIOException          falls ein Fehler bei der Ein-/Ausgabe auftritt
     * @exception NoDirectoryInodeException falls es sich nicht um ein Verzeichnis handelt
     * @exception PermissionException       falls die Zugriffsrechte des Verzeichnisses die Operation nicht erlauben oder das
     *                                      Dateisystem als nur lesbar angemeldet wurde
     */
    Inode   create(String name, int mode) throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, PermissionException;

    /**
     * Entfernt die Datei mit dem angegebenen Namen aus dem durch diese Inode dargestellten Verzeichnis. Die zu dem Eintrag
     * geh&ouml;rende Inode wird freigegeben.
     *
     * @param name der Name der zu l&ouml;schenden Datei
     * @exception InodeIOException          falls ein Fehler bei der Ein-/Ausgabe auftritt
     * @exception InodeNotFoundException    falls die zu l&ouml;schende Inode nicht gefunden werden kann
     * @exception NoDirectoryInodeException falls es sich nicht um ein Verzeichnis handelt
     * @exception NoFileInodeException      falls der zu l&ouml;schende Eintrag existiert, aber keine Datei ist
     * @exception PermissionException       falls die Zugriffsrechte des Verzeichnisses die Operation nicht erlauben oder das
     *                                      Dateisystem als nur lesbar angemeldet wurde
     */
    void    unlink(String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NoFileInodeException, NotExistException,PermissionException;

    /**
     * Erzeugt einen "symbolischen Link", einen Verweis auf einen Verzeichniseintrag. Von au&szlig;en ist zwischen Verweis
     * und urspr&uuml;nglichen Eintrag kein Unterschied festzustellen.
     *
     * @param symname der Pfad, auf den ein Verweis angelegt werden soll (der Eintrag muss nicht existieren)
     * @param newname der Name des Verweises
     * @exception FileExistsException       falls bereits ein Eintrag mit angegebenen Namen (<code>newname</code>)
     *                                      existiert
     * @exception InodeIOException          falls bei der Ein-/Ausgabe ein Fehler auftritt
     * @exception NoDirectoryInodeException falls es sich nicht um ein Verzeichnis handelt
     * @exception NotSupportedException     falls das Dateisystem diese Operation nicht unterst&uuml;tzt
     * @exception PermissionException       falls die Zugriffsrechte des Verzeichnisses die Operation nicht erlauben oder das
     *                                      Dateisystem als nur lesbar angemeldet wurde
     */
    Inode   symlink(String symname, String newname) throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, NotSupportedException,PermissionException;

    /**
     * Liefert den Pfad der Inode, auf die der symbolische Link verweist.
     *
     * @exception InodeIOException        falls bei der Ein-/Ausgabe ein Fehler auftritt
     * @exception NoSymlinkInodeException falls das Objekt keinen symbolischen Link darstellt
     * @exception NotSupportedException   falls das Dateisystem diese Operation nicht unterst&uuml;tzt
     * @exception PermissionException     falls die Zugriffsrechte des symbolischen Links die Operation nicht erlauben
     */
    String  getSymlink() throws InodeIOException, NoSymlinkInodeException, NotExistException, NotSupportedException, PermissionException;

    /**
     * Verschiebt die Inode des angegebenen Verzeichniseintrags bzw. &auml;ndert deren Namen.
     *
     * @param oldname der Name des Verzeichniseintrags, der verschoben bzw. umbenannt werden soll
     * @param new_dir die Inode des Verzeichnisses, das den zu verschiebenden Verzeichniseintrag aufnehmen soll
     * @param newname der neue Name des Verzeichniseintrags
     * @exception InodeIOException          falls bei der Ein-/Ausgabe ein Fehler auftritt
     * @exception InodeNotFoundException    falls die zu verschiebende Inode nicht gefunden werden kann
     * @exception NoDirectoryInodeException falls es sich nicht um ein Verzeichnis handelt
     * @exception PermissionException       falls es sich bei <code>new_dir</code> um ein anderes Dateisystem handelt,
     *                                      die Zugriffsrechte des Verzeichnisses die Operation nicht erlauben oder das
     *                                      Dateisystem als nur lesbar angemeldet wurde
     */
    void    rename(String oldname, Inode new_dir, String newname) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException;

    /**
     * Liest den Inhalt der Datei.
     *
     * @param     mem                  der Puffer, der die zu lesenden Daten aufnehmen soll
     * @param     off                  die Position innerhalb der Datei, ab der gelesen werden soll
     * @param     len                  die Anzahl zu lesender Byte
     * @exception InodeIOException     falls bei der Ein-/Ausgabe ein Fehler auftritt
     * @exception NoFileInodeException falls es sich nicht um eine Datei handelt (Lesen eines Verzeichnisses ist nicht erlaubt)
     * @exception PermissionException  falls die Zugriffsrechte der Datei die Operation nicht erlauben
     */
    int     read(Memory mem, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException;

    /**
     * Liest den Inhalt der Datei.
     *
     * @param     pos                  die Position innerhalb der Datei, ab der gelesen werden soll
     * @param     b                    der Puffer, der die zu lesenden Daten aufnehmen soll
     * @param     bufoff               
     * @param     len                  die Anzahl zu lesender Byte
     * @exception InodeIOException     falls bei der Ein-/Ausgabe ein Fehler auftritt
     * @exception NoFileInodeException falls es sich nicht um eine Datei handelt (Lesen eines Verzeichnisses ist nicht erlaubt)
     * @exception PermissionException  falls die Zugriffsrechte der Datei die Operation nicht erlauben
     */
    int     read(int pos, Memory mem, int bufoff, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException;

    /** experimental method to get rid of memory copies */
    ReadOnlyMemory readWeak(int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException;

    /**
     * Schreibt Daten in die Datei. Die Datei wird u.U. vergr&ouml;&szlig;ert.
     *
     * @param     b                    der Puffer, der die zu schreibenden Daten enth&auml;lt
     * @param     off                  die Position innerhalb der Datei, ab der geschrieben werden soll
     * @param     len                  die Anzahl zu schreibender Byte
     * @exception InodeIOException     falls bei der Ein-/Ausgabe ein Fehler auftritt
     * @exception NoFileInodeException falls es sich nicht um eine Datei handelt (Lesen eines Verzeichnisses ist nicht erlaubt)
     * @exception PermissionException  falls die Zugriffsrechte der Datei die Operation nicht erlauben oder das Dateisystem
     *                                 als nur lesbar angemeldet wurde
     */
    int     write(Memory mem, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException;

    /**
     * Schreibt Daten in die Datei. Die Datei wird u.U. vergr&ouml;&szlig;ert.
     *
     * @param     pos                  die Position innerhalb der Datei, ab der geschrieben werden soll    
     * @param     b                    der Puffer, der die zu schreibenden Daten enth&auml;lt
     * @param     bufoff               die Position innerhalb der Datei, ab der geschrieben werden soll
     * @param     len                  die Anzahl zu schreibender Byte
     * @exception InodeIOException     falls bei der Ein-/Ausgabe ein Fehler auftritt
     * @exception NoFileInodeException falls es sich nicht um eine Datei handelt (Lesen eines Verzeichnisses ist nicht erlaubt)
     * @exception PermissionException  falls die Zugriffsrechte der Datei die Operation nicht erlauben oder das Dateisystem
     *                                 als nur lesbar angemeldet wurde
     */
    int     write(int pos, Memory mem, int bufoff, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException;

    /**
     * Liefert den freien Platz (in Byte) auf der Partition, die die Inode enth&auml;lt, zur&uuml;ck.
     */
    int available() throws NotExistException;

    /**
     * Liefert die Gr&ouml;&szlig;e des von der Inode belegten Bereichs auf der Partition zur&uuml;ck.
     */
    int getLength() throws NotExistException;

    /**
     * Returns an identifier that allows to locate the inode without using the inode object
     */
    int getIdentifier() throws NotExistException;

    int getVersion() throws NotExistException;

    /**
     * Returns the file system this inode belongs to
     */
    FileSystem getFileSystem() throws NotExistException;

    StatFS getStatFS();

}
