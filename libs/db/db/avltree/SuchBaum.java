/*****************************  SuchBaum.java  ********************************/

//import AlgoTools.IO;

/** Implementation eines binaeren Suchbaums ueber Comparable-Objekten.
 */

package db.avltree;


public class SuchBaum extends Baum {

    // sucht x im SuchBaum: liefert den SuchBaum mit x in der Wurzel, ggf. leer
    private SuchBaum find(db.avltree.Comparable x) throws Exception {
        if (empty())                                 return this;
        if (((db.avltree.Comparable) value()).compareTo(x) == 0)  return this;
        if (((db.avltree.Comparable) value()).compareTo(x) > 0)  return
                ((SuchBaum) left()).find(x);
        else                                         return
                ((SuchBaum) right()).find(x);
    }

    /** Sucht x im SuchBaum: liefert null, wenn x nicht gefunden wurde,
     * sonst Comparable-Objekt x */
    public db.avltree.Comparable lookup(db.avltree.Comparable x) throws Exception {
        return (db.avltree.Comparable) find(x).inhalt;
    }

    /** fuegt x in SuchBaum ein: liefert true, wenn erfolgreich, sonst false.*/
    public boolean insert(db.avltree.Comparable x) throws Exception {
        SuchBaum s = find(x);         // SuchBaum mit x in der Wurzel oder leer

        if (s.empty()) { // wenn leer, d.h. x noch nicht im SuchBaum enthalten:
            s.inhalt = x;                  // setzte Inhalt auf x
            s.links = new SuchBaum();     // neuer leerer SuchBaum links
            s.rechts = new SuchBaum();     // neuer leerer SuchBaum rechts
            return true;
        } else return false;
    }

    /** loescht x aus SuchBaum: liefert true, wenn erfolgreich geloescht,
     *  sonst false */
    public boolean delete(db.avltree.Comparable x) throws Exception {
        SuchBaum s = find(x);         // SuchBaum mit x in der Wurzel oder leer
        SuchBaum ersatz;              // Ersatzknoten

        if (s.empty()) return false;  // wenn x nicht gefunden: false
        else {                        // wenn x gefunden
            if (s.left().empty())  ersatz = (SuchBaum) s.right();
            else if (s.right().empty()) ersatz = (SuchBaum) s.left();
            else {                    // Knoten mit x hat zwei Soehne
                ersatz = ((SuchBaum) s.left()).findMax(); // Maximum im linken
                s.inhalt = ersatz.inhalt;                // ersetze Inhalt
                s = ersatz;                              // zu ersetzen
                ersatz = (SuchBaum) ersatz.left();        // Ersatz: linker
            }
            s.inhalt = ersatz.inhalt; // ersetze die Komponenten
            s.links = ersatz.links;
            s.rechts = ersatz.rechts;
            return true;
        }
    }

    // findet im nichtleeren SuchBaum das Maximum:
    // liefert den SuchBaum mit dem Maximum in der Wurzel
    private SuchBaum findMax() throws Exception {
        SuchBaum hilf = this;

        while (!hilf.right().empty()) hilf = (SuchBaum) hilf.right();
        return hilf;     // der rechteste Nachfahr von this
    }
}
