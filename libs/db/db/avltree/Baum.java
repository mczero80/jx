/***************************  Baum.java  **************************************/
//import AlgoTools.IO;

/** Klasse Baum  mit drei Konstruktoren und vier Methoden.
 *  Ein Baum besteht aus den Datenfeldern inhalt, links, rechts.
 */

package db.avltree;


public class Baum {

    Object inhalt;                           // Inhalt
    Baum links, rechts;                      // linker, rechter Teilbaum

    public final static Baum LEER = new Baum(); // leerer Baum als Klassenkonst.

    public Baum () {                         // konstruiert einen leeren Baum
        inhalt = null;                         // kein Inhalt
        links = null;                         // keine
        rechts = null;                         // Kinder
    }

    public Baum (Object x) {                 // konstruiert ein Blatt
        this(LEER, x, LEER);
    }                 // mit Objekt x

    public Baum (Baum l, Object x, Baum r) { // konstruiert einen Baum
        inhalt = x;                            // aus einem Objekt x und
        links = l;                            // einem linken Teilbaum
        rechts = r;
    }                          // und einem rechten Teilbaum

    public boolean empty() {                // liefert true,
        return (inhalt == null);               // falls Baum leer ist
    }

    public Baum left() throws Exception {                    // liefert linken Teilbaum
        if (empty()) throw new Exception("in left: leerer Baum");
        return links;
    }

    public Baum right() throws Exception {                   // liefert rechten Teilbaum
        if (empty()) throw new Exception("in right: leerer Baum");
        return rechts;
    }

    public Object value() throws Exception {                 // liefert Objekt in der Wurzel
        if (empty()) throw new Exception("in value: leerer Baum");
        return inhalt;
    }
}
