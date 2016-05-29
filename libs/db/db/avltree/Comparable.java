/***************************  Comparable.java  *******************************/


package db.avltree;


/**   Das Interface deklariert eine Methode, anhand der sich das
 *    aufgerufene Objekt mit dem uebergebenen vergleicht.
 *    Fuer jeden von Object abgeleiteten Datentyp muss eine solche
 *    Vergleichsklasse implementiert werden.
 *    Die Methode erzeugt eine Fehlermeldung, wenn a ein Objekt einer anderen
 *    Klasse als dieses Objekt ist.
 *
 *    int compareTo(Comparable a)
 *         liefert  0, wenn this == a
 *         liefert <0, wenn this <  a
 *         liefert >0, wenn this >  a
 */
public interface Comparable {

    public int compareTo(Comparable a);
}
