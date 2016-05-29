package jx.fs;

import jx.zero.Portal;

import jx.zero.Memory;

/**
 * Diese Klasse stellt die Schnittstelle des Dateisystems zur Anwendungsebene dar. Sie enth&auml;lt Methoden, die grundlegende
 * Dateisystemoperationen ausf&uuml;hren (&auml;hnlich den Systemcalls in einem Unix-Dateisystem). Dazu z&auml;hlen:
 * <code>cd</code>, <code>mkdir</code>, <code>rmdir</code>, <code>create</code>, <code>unlink</code>, <code>rename</code>,
 * <code>symlink</code>, <code>read</code> und <code>write</code>.
 * <p>
 * Der Unterschied zu den gleichlautenden Befehlen der Klasse <code>Inode</code> ist, dass hier Dateisystem-weite Pfade
 * verwendet werden k&ouml;nnen und nicht Eintr&auml;ge innerhalb einer Verzeichnis-Inode angesprochen werden. Hier geschieht
 * also die Umsetzung von Befehlen auf Pfaden in Methodenaufrufen von Inode-Objekten. Dabei wird ein aktuelles Verzeichnis
 * mitgef&uuml;hrt. Soll z.B. der Befehl <code>mkdir /usr/local/bin</code> ausgef&uuml;hrt werden, wird in der Inode des
 * Wurzelverzeichnisses (die der Klasse immer bekannt ist) nach dem Eintrag "usr" gesucht, dessen Inode geladen, in dieser
 * Inode nach dem Eintrag "local" gesucht und in der zugeh&ouml;rigen Inode die Methode <code>mkdir(bin)</code> aufgerufen.
 * Es werden zwei Inodes permanent gespeichert: die des Wurzelverzeichnisses und die des aktuellen Verzeichnisses zusammen mit
 * ihrem Pfad. Es wird im Folgenden nur auf die Methoden eingegangen, die nicht auch in der <code>Inode</code>-Klasse vorhanden
 * sind, da diese im Wesentlichen auf eine Zerlegung des Pfades und Aufruf der entsprechenden Methoden in der Inode des
 * Dateinamens bestehen.
 */
public interface FS extends Portal {

    /**
     * Liefert den Pfad des aktuellen Verzeichnisses ("current working directory").
     *
     * @return der absolute Pfad des aktuellen Verzeichnisses
     */
    public String getCwdPath();

    /**
     * Liefert die Inode, die dem aktuellen Verzeichnis zugeordnet ist.
     *
     * @return die Inode des aktuellen Verzeichnisses
     */
    public Inode getCwdInode();

    /**
     * Gibt belegte Ressourcen frei und beendet Threads des Dateisystem. Diese Methode ist f&uuml;r das "Herunterfahren" des
     * Systems gedacht; nach <code>cleanUp</code> sollte nicht mehr auf das Dateisystem zugegriffen werden.
     */
    public void cleanUp() throws InodeIOException,NotExistException;

    /**
     * H&auml;ngt ein Dateisystem in den Verzeichnisbaum ein, indem eine Inode durch die Wurzelinode des
     * Dateisystems &uuml;berlagert wird. Die Wurzelinode ist unter dem angegebenen Namen ansprechbar.
     *
     * @param     filesystem das Dateisystem, das angemeldet werden soll
     * @param     path       der Pfad, unter dem das einzuh&auml;ngende Dateisystem erreichbar sein soll. Die Inode, die durch
     *                       diesen Pfad repr&auml;sentiert wird, wird &uuml;berlagert.
     * @param     read_only  falls <code>true</code>, kann auf die Partition nur lesend zugegriffen werden
     * @exception InodeIOException          falls ein Fehler bei der Ein-/Ausgabe auftritt
     * @exception InodeNotFoundException    falls der Verzeichniseintrag (und damit die Inode) nicht gefunden werden kann
     * @exception NoDirectoryInodeException falls es sich bei dem zu &uuml;berlagernden Verzeichniseintrag nicht um ein
     *                                      Verzeichnis handelt
     */
    public void mount(FileSystem filesystem, String path, boolean read_only) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException;

    /**
     * Entfernt ein Dateisystem aus dem Verzeichnisbaum. Die Inode, die durch die Wurzelinode des
     * Dateisystems &uuml;berlagert war, wird dadurch wieder zug&auml;nglich.
     *
     * @param     filesystem das Dateisystem, das abgemeldet werden soll
     * @exception InodeNotFoundException    falls der Verzeichniseintrag (und damit die Inode) nicht gefunden werden kann
     * @exception NoDirectoryInodeException falls es sich bei dem &uuml;berlagerten Verzeichniseintrag nicht um ein
     *                                      Verzeichnis handelt
     */
    public void unmount(FileSystem filesystem) throws InodeNotFoundException, NoDirectoryInodeException, NotExistException;

    /**
     * Initialisiert das angegebene Dateisystem als Root-Dateisystem. Setzt das Wurzelverzeichnis und das aktuelle Verzeichnis
     * auf die Root-Inode des Dateisystems, den Pfad des aktuellen Verzeichnisses auf " / ".
     *
     * @param filesystem ein FileSystem-Objekt, das das Root-Dateisystem repr&auml;sentiert
     * @param read_only  falls <code>true</code>, kann auf die Partition nur lesend zugegriffen werden
     */
    public void mountRoot(FileSystem filesystem, boolean read_only);

    /**
     * Liefert den freien Platz innerhalb des Dateisystems.
     *
     * @return der freie Platz in Byte
     */
    public int available() throws NotExistException;

    /**
     * Setzt das aktuelle Verzeichnis auf den angegebenen Pfad (sofern g&uuml;ltig).
     *
     * @param path der Pfad, der als aktuelles Verzeichnis verwendet werden soll
     */
    public void cd(String path);

    /**
     * Verschiebt die Datei bzw. das Verzeichnis an einen anderen Ort im Verzeichnisbaum bzw. &auml;ndert den Namen.
     *
     * @param path    der Pfad des Verzeichniseintrags, der verschoben bzw. umbenannt werden soll
     * @param pathneu der Zielpfad
     * @exception InodeIOException          falls ein Fehler bei der Ein-/Ausgabe auftritt
     * @exception InodeNotFoundException    falls der Verzeichniseintrag (und damit die Inode) nicht gefunden werden kann
     * @exception NoDirectoryInodeException falls es sich beim Pfad zur Datei/zum Verzeichnis nicht um ein Verzeichnis handelt
     * @exception PermissionException       falls es sich um ein Rootverzeichnis oder einen Mountpunkt handelt
     */
    public void rename(String path, String pathneu) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException;

    /**
     * Legt einen symbolischen Link an, der auf den angegebenen Pfad verweist.
     *
     * @param path    der Verzeichniseintrag, auf den verwiesen werden soll
     * @param pathneu der Pfad des anzulegenden symbolischen Links
     * @exception FileExistsException       falls der Pfad des symbolischen Links bereits existiert
     * @exception InodeIOException          falls ein Fehler bei der Ein-/Ausgabe auftritt
     * @exception InodeNotFoundException    falls der Verzeichniseintrag (und damit die Inode) nicht gefunden werden kann
     * @exception NoDirectoryInodeException falls es sich bei dem Pfad zur Datei/zum Verzeichnis nicht um ein Verzeichnis handelt
     */
    public void symlink(String path, String pathneu) throws FileExistsException, InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, NotSupportedException, PermissionException;

    /**
     * Erzeugt ein neues Verzeichnis mit dem angegebenen Pfadnamen.
     *
     * @param path der Pfad des neuen Verzeichnisses
     * @param mode die Zugriffsrechte des neuen Verzeichnisses
     * @exception FileExistsException       falls eine Datei/ein Verzeichnis mit diesem Pfad bereits existiert.
     * @exception InodeIOException          falls ein Fehler bei der Ein-/Ausgabe auftritt
     * @exception InodeNotFoundException    falls das Verzeichnis, das das neue Verzeichnis aufnehmen soll, nicht gefunden
     *                                      werden kann
     * @exception NoDirectoryInodeException falls es sich bei dem Pfad zum Verzeichnis nicht um ein Verzeichnis handelt
     */
    public void mkdir(String path, int mode) throws FileExistsException, InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException,PermissionException;

    /**
     * Entfernt das Verzeichnis mit dem angegebenen Pfadnamen.
     *
     * @param path der Pfad des zu l&ouml;schenden Verzeichnisses
     * @exception DirNotEmptyException      falls das zu l&ouml;schende Verzeichnis nicht leer ist
     * @exception InodeIOException          falls ein Fehler bei der Ein-/Ausgabe auftritt
     * @exception InodeNotFoundException    falls das Verzeichnis, das das zu l&ouml;schende Verzeichnis enthalten soll, nicht
     *                                      gefunden werden kann
     * @exception NoDirectoryInodeException falls es sich bei dem Pfad zum Verzeichnis nicht um ein Verzeichnis handelt
     */
    public void rmdir(String path) throws DirNotEmptyException, InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException,PermissionException;
    
    /**
     * Erzeugt eine neue Datei mit dem angegebenen Pfadnamen.
     *
     * @param path der Pfad der neuen Datei
     * @param mode die Zugriffsrechte der neuen Datei
     * @exception FileExistsException       falls eine Datei/ein Verzeichnis mit diesem Pfad bereits existiert
     * @exception InodeIOException          falls ein Fehler bei der Ein-/Ausgabe auftritt
     * @exception InodeNotFoundException    falls das Verzeichnis, das die neue Datei aufnehmen soll, nicht gefunden werden
     *                                      kann
     * @exception NoDirectoryInodeException falls es sich bei dem Pfad zur Datei nicht um ein Verzeichnis handelt
     */
    public void create(String path, int mode) throws FileExistsException, InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException,PermissionException;

    /**
     * Entfernt die Datei mit dem angegebenen Pfadnamen.
     *
     * @param path der Pfad der zu l&ouml;schenden Datei
     * @exception InodeIOException          falls ein Fehler bei der Ein-/Ausgabe auftritt
     * @exception InodeNotFoundException    falls das Verzeichnis, das die zu l&ouml;schende Datei enthalten soll, nicht
     *                                      gefunden werden kann
     * @exception NoDirectoryInodeException falls es sich bei dem Pfad zur Datei nicht um ein Verzeichnis handelt
     */
    public void unlink(String path) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NoFileInodeException, NotExistException,PermissionException;

    /**
     * Liefert das &uuml;bergeordnete Verzeichnis zum angegebenen Pfadnamen. Falls der Pfadname keine Pfadkomponenten enth&auml;lt,
     * wird der Pfad des aktuellen Verzeichnisses zur&uuml;ckgegeben.
     *
     * @param path der Pfadname, dessen &uuml;bergeordnetes Verzeichnis zur&uuml;ckgegeben werden soll
     * @return der Pfad des &uuml;bergeordneten Verzeichnisses
     */
    public String getPathName(String path);

    /**
     * Liefert den Datei- bzw. Verzeichnisnamen auf den der angegebene Pfad zeigt. Falls der Pfadname keine Pfadkomponenten
     * enth&auml;lt, wird er unver&auml;ndert zur&uuml;ckgegeben.
     *
     * @param path der Pfadname, der zerlegt werden soll
     * @return der Datei- oder Verzeichnisname, auf den der Pfad zeigt
     */
    public String getFileName(String path);

    /**
     * Liefert die Inode zum angegebenen Pfad zur&uuml;ck. Repr&auml;sentiert die Inode einen symbolischen Link, wird dieser
     * verfolgt und die Inode, auf die sich der Verweis bezieht, zur&uuml;ckgegeben.
     *
     * @param     path der Pfad, dessen zugeordnete Inode zur&uuml;ckgegeben werden soll
     * @return    die Inode des durch den angegebenen Pfad bezeichneten Verzeichniseintrags
     * @exception InodeIOException          falls ein Fehler bei der Ein-/Ausgabe auftritt
     * @exception InodeNotFoundException    falls der Verzeichniseintrag (und damit die Inode) nicht gefunden werden kann
     * @exception NoDirectoryInodeException falls es sich bei einem Teil des Pfades, auf den ein " / " folgt, nicht um ein
     *                                      Verzeichnis handelt
     */
    public Inode lookup(String path) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException;

    /**
     * Liest den Inhalt der Datei mit dem angegebenen Pfadnamen.
     *
     * @param path der Pfadname der zu lesenden Datei
     * @param b    das Byte-Array, das den Dateiinhalt aufnehmen soll
     * @param off  der Offset innerhalb der Datei
     * @param len  die Anzahl zu lesender Byte
     * @return die Anzahl tats&auml;chlich gelesener Byte
     * @exception InodeIOException falls ein Fehler bei der Ein-/Ausgabe auftritt
     * @exception NoFileInodeException falls es sich bei dem angegebenen Pfad nicht um eine Datei handelt
     */
    public int read(String path, Memory m, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException;

    /**
     * Schreibt den Inhalt des Byte-Arrays in die Datei mit dem angegebenen Pfadnamen.
     *
     * @param path der Pfadname der Datei, deren Inhalt ge&auml;ndert werden soll
     * @param b    das Byte-Array, das die zu schreibenden Daten enth&auml;lt
     * @param off  der Offset innerhalb der Datei
     * @param len  die Anzahl zu schreibender Byte
     * @return die Anzahl tats&auml;chlich geschriebener Byte
     * @exception InodeIOException     falls ein Fehler bei der Ein-/Ausgabe auftritt
     * @exception NoFileInodeException falls es sich bei dem angegebenen Pfad nicht um eine Datei handelt
     * @exception PermissionException  falls das Dateisystem als nur lesbar angemeldet wurde
     */
    public int write(String path, Memory m, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException;

//     public Inode getInode(int deviceIdentifier, int fileIdentifier) throws FSException, NotExistException, PermissionException;

 public Inode getInode(Integer deviceIdentifier, int fileIdentifier) throws FSException, NotExistException, PermissionException;

}
