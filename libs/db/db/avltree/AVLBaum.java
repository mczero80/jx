/*****************************  AVLBaum.java  *********************************/

//import AlgoTools.IO;

/** Ein AVLBaum ist ein SuchBaum, bei dem alle Knoten ausgeglichen
 *  sind. Das heisst, die Hoehe aller Teilbaeume unterscheidet sich
 *  maximal um eins.
 */

package db.avltree;


public class AVLBaum extends SuchBaum {

    private int balance;                 // Balance

    public AVLBaum() {                   // erzeugt leeren AVLBaum

        balance = 0;
    }

    private static class Status {        // Innere Klasse zur Uebergabe eines 
        // Status in der Rekursion
        boolean unbal;                   // unbal ist true, wenn beim Einfue-
        // gen ein Sohn groesser geworden ist
        Status () {                      // Konstruktor der inneren Klasse
            unbal = false;               // Element noch nicht eingefuegt =>
        }                                // noch keine Unausgeglichenheit
    }

    public String toString() {           // fuer Ausgabe: Inhalt(Balance)

        return new String(inhalt + "(" + balance + ")");
    }

    public boolean insert(Comparable x) throws Exception {// fuegt x in den AVLBaum ein: true,
        // wenn erfolgreich, sonst false.
        // Kapselt die Funktion insertAVL
        return insertAVL(x, new Status());
    }

    private boolean insertAVL(Comparable x, Status s) throws Exception { // Tatsaechliche Methode zum
        // Einfuegen (rekursiv)
        boolean eingefuegt;

        if (empty()) {                    // Blatt: Hier kann eingefuegt werden
            inhalt = x;                  // Inhalt setzen
            links = new AVLBaum();      // Neuer leerer AVLBaum links
            rechts = new AVLBaum();      // Neuer leerer AVLBaum rechts
            s.unbal = true;              // Dieser Teilbaum wurde groesser
            return true;                 // Einfuegen erfolgreich und
        }                                // dieser Teilbaum groesser

        else if (((Comparable) value()).compareTo(x) == 0)   // Element schon im AVLBaum
            return false;

        else if (((Comparable) value()).compareTo(x) > 0) {   // Element x ist kleiner =>
            eingefuegt = ((AVLBaum) left()).insertAVL(x, s);  // linker Teilbaum

            if (s.unbal) {                // Linker Teilbaum wurde groesser
                if (balance == 1) {      // Alte Unausgeglichenheit ausgegl.
                    balance = 0;         // => neue Balance = 0
                    s.unbal = false;     // Unausgeglichenheit ausgeglichen
                    return true; 
                } else if (balance == 0) { // Hier noch kein Rotieren noetig
                    balance = -1;        // Balance wird angeglichen
                    return true; 
                } else {                   // Rotieren notwendig

                    if (((AVLBaum) links).balance == -1)     
                        rotateLL();
                    else 
                        rotateLR();     
                    s.unbal = false;     // Unausgeglichenheit ausgeglichen
                    return true;         // => Rueckgabewert
                }                        // angleichen
            }

        } else {                         // Element ist groesser =>
            eingefuegt = ((AVLBaum) right()).insertAVL(x, s);// rechter Teilbaum

            if (s.unbal) {                // Rechter Teilbaum wurde groesser
                if (balance == -1) {     // Alte Unausgeglichenheit ausgegl.
                    balance = 0;         // => neue Balance = 0
                    s.unbal = false;     // Unausgeglichenheit ausgeglichen
                    return true;
                } else if (balance == 0) { // Hier noch kein Rotieren noetig
                    balance = 1;         // Balance wird angeglichen
                    return true;
                } else {                   // Rotieren notwendig

                    if (((AVLBaum) rechts).balance == 1)     
                        rotateRR();
                    else            
                        rotateRL();
                    s.unbal = false;     // Unausgeglichenheit ausgeglichen
                    return true;         // => Rueckgabewert
                }                        // angleichen
            }
        }
        return eingefuegt;               // Keine Rotation => Ergebnis zurueck
    }

    public void rotateLL() {

        //IO.println("LL-Rotation im Teilbaum mit Wurzel "+ inhalt);

        AVLBaum a1 = (AVLBaum) links;     // Merke linken
        AVLBaum a2 = (AVLBaum) rechts;    // und rechten Teilbaum

        // Idee: Inhalt von a1 in die Wurzel
        links = a1.links;                // Setze neuen linken Sohn
        rechts = a1;                     // Setze neuen rechten Sohn
        a1.links = a1.rechts;            // Setze dessen linken
        a1.rechts = a2;                  // und rechten Sohn

        Object tmp = a1.inhalt;          // Inhalt von rechts (==a1)

        a1.inhalt = inhalt;              // wird mit Wurzel
        inhalt = tmp;                    // getauscht

        ((AVLBaum) rechts).balance = 0;   // rechter Teilbaum balanciert
        balance = 0;                     // Wurzel balanciert
    }

    public void rotateLR() {

        //IO.println("LR-Rotation im Teilbaum mit Wurzel "+ inhalt);

        AVLBaum a1 = (AVLBaum) links;     // Merke linken
        AVLBaum a2 = (AVLBaum) a1.rechts; // und dessen rechten Teilbaum

        // Idee: Inhalt von a2 in die Wurzel
        a1.rechts = a2.links;            // Setze Soehne von a2
        a2.links = a2.rechts;
        a2.rechts = rechts;
        rechts = a2;                     // a2 wird neuer rechter Sohn

        Object tmp = inhalt;             // Inhalt von rechts (==a2)

        inhalt = rechts.inhalt;          // wird mit Wurzel
        rechts.inhalt = tmp;             // getauscht

        if (a2.balance == 1)             // Neue Bal. fuer linken Sohn
            ((AVLBaum) links).balance = -1;
        else
            ((AVLBaum) links).balance = 0;

        if (a2.balance == -1)            // Neue Bal. fuer rechten Sohn
            ((AVLBaum) rechts).balance = 1;
        else
            ((AVLBaum) rechts).balance = 0;
        balance = 0;                     // Wurzel balanciert
    }

    public void rotateRR() {

        //IO.println("RR-Rotation im Teilbaum mit Wurzel "+ inhalt);

        AVLBaum a1 = (AVLBaum) rechts;    // Merke rechten
        AVLBaum a2 = (AVLBaum) links;     // und linken Teilbaum

        // Idee: Inhalt von a1 in die Wurzel
        rechts = a1.rechts;              // Setze neuen rechten Sohn
        links = a1;                      // Setze neuen linken Sohn
        a1.rechts = a1.links;            // Setze dessen rechten
        a1.links = a2;                  // und linken Sohn

        Object tmp = a1.inhalt;          // Inhalt von links (==a1)

        a1.inhalt = inhalt;              // wird mit Wurzel
        inhalt = tmp;                    // getauscht

        ((AVLBaum) links).balance = 0;    // linker Teilbaum balanciert
        balance = 0;                     // Wurzel balanciert
    }

    public void rotateRL() {

        //IO.println("RL-Rotation im Teilbaum mit Wurzel "+ inhalt);

        AVLBaum a1 = (AVLBaum) rechts;    // Merke rechten Sohn
        AVLBaum a2 = (AVLBaum) a1.links;  // und dessen linken Teilbaum

        // Idee: Inhalt von a2 in die Wurzel
        a1.links = a2.rechts;
        a2.rechts = a2.links;            // Setze Soehne von a2
        a2.links = links;
        links = a2;                      // a2 wird neuer linker Sohn

        Object tmp = inhalt;             // Inhalt von links (==a2)

        inhalt = links.inhalt;           // wird mit Wurzel
        links.inhalt = tmp;              // getauscht

        if (a2.balance == -1)            // Neue Bal. fuer rechten Sohn
            ((AVLBaum) rechts).balance = 1;
        else
            ((AVLBaum) rechts).balance = 0;

        if (a2.balance == 1)             // Neue Bal. fuer linken Sohn
            ((AVLBaum) links).balance = -1;
        else
            ((AVLBaum) links).balance = 0;
        balance = 0;                     // Wurzel balanciert
    }

    public boolean delete(Comparable x) throws Exception {// loescht x im AVLBaum: true,
        // wenn erfolgreich, sonst false.
        // Kapselt die Funktion deleteAVL
        return deleteAVL(x, new Status());
    }

    private boolean deleteAVL(Comparable x, Status s) throws Exception { // Tatsaechliche Methode
        // zum Loeschen (rekursiv); true, wenn erfolgreich
        boolean geloescht;               // true, wenn geloescht wurde

        if (empty()) {                    // Blatt: Element nicht gefunden
            return false;                // => Einfuegen erfolglos
        } else if (((Comparable) value()).compareTo(x) < 0) {     // Element x ist groesser =>
            // Suche rechts weiter
            geloescht = ((AVLBaum) rechts).deleteAVL(x, s);
            if (s.unbal == true) balance2(s);       // Gleiche ggf. aus
            return geloescht;
        } else if (((Comparable) value()).compareTo(x) > 0) {     // Element x ist kleiner =>
            // Suche links weiter
            geloescht = ((AVLBaum) links).deleteAVL(x, s);
            if (s.unbal == true) balance1(s);       // Gleiche ggf. aus
            return geloescht;
        } else {                           // Element gefunden
            if (rechts.empty()) {        // Kein rechter Sohn
                inhalt = links.inhalt;   // ersetze Knoten durch linken Sohn
                links = links.links;     // Kein linker Sohn mehr
                balance = 0;             // Knoten ist Blatt
                s.unbal = true;          // Hoehe hat sich geaendert
            } else if (links.empty()) {  // Kein linker Sohn
                inhalt = rechts.inhalt;  // ersetze Knoten durch rechten Sohn
                rechts = rechts.rechts;  // Kein rechter Sohn mehr
                balance = 0;             // Knoten ist Blatt
                s.unbal = true;          // Hoehe hat sich geaendert
            } else {                     // Beide Soehne vorhanden
                inhalt = ((AVLBaum) links).del(s);   // Rufe del() auf
                if (s.unbal) {           // Gleiche Unbalance aus
                    balance1(s);
                }
            }
            return true;                 // Loeschen erfolgreich
        }
    }

    private Object del(Status s) {       // Sucht Ersatz fuer gel. Objekt
        Object ersatz;                   // Das Ersatz-Objekt

        if (!rechts.empty()) {           // Suche groessten Sohn im Teilbaum
            ersatz = ((AVLBaum) rechts).del(s);
            if (s.unbal)                 // Gleicht ggf. Unbalance aus
                balance2(s);
        } else {                         // Tausche mit geloeschtem Knoten
            ersatz = inhalt;             // Merke Ersatz und
            inhalt = links.inhalt;       // ersetze Knoten durch linken Sohn.
            links = links.links;         // Kein linker Sohn mehr
            balance = 0;                 // Knoten ist Blatt
            s.unbal = true;              // Teilbaum wurde kuerzer
        }
        return ersatz;                   // Gib Ersatz-Objekt zurueck
    }

    private void balance1(Status s) {    // Unbalance, weil linker Ast kuerzer
        if (balance == -1) 
            balance = 0;                 // Balance geaendert, nicht ausgegl.
        else if (balance == 0) {
            balance = 1;                 // Ausgeglichen
            s.unbal = false;
        } else {                         // Ausgleichen (Rotation) notwendig
            int b = ((AVLBaum) rechts).balance; //Merke Balance des rechten Sohns

            if (b >= 0) {
                rotateRR();
                if (b == 0) {            // Gleiche neue Balancen an
                    balance = -1;
                    ((AVLBaum) links).balance = 1;
                    s.unbal = false;
                }
            } else 
                rotateRL();
        }
    }

    private void balance2(Status s) {    // Unbalance, weil recht. Ast kuerzer
        if (balance == 1) 
            balance = 0;                 // Balance geaendert, nicht ausgegl.
        else if (balance == 0) {
            balance = -1;                // Ausgeglichen
            s.unbal = false;
        } else {                         // Ausgleichen (Rotation) notwendig
            int b = ((AVLBaum) links).balance; // Merke Balance des linken Sohns

            if (b <= 0) {
                rotateLL();
                if (b == 0) {            // Gleiche neue Balancen an
                    balance = 1;
                    ((AVLBaum) rechts).balance = -1;
                    s.unbal = false;
                }
            } else
                rotateLR();
        }
    }
}
