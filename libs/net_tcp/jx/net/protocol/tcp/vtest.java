import java.lang.*;
import java.util.*;

public class vtest {

  Vector test;

  public vtest() {
    System.out.println("Konstruktor");
    test = new Vector(50, 20);
  }

  public void add(int value) {
    test.addElement(new Integer(value));
  }

  public int erster() {
    int ret = ((Integer)test.firstElement()).intValue();
    test.removeElement(test.firstElement());
    return ret;
  }
  
  public boolean isEmpty() {
    return test.isEmpty();
  }

  public int size() {
    return test.size();
  }

  public void waitschleife(int v) {
    for (int i = 0; i < v; i++)
      ;
  }

  public static void main(String args[]) {
    vtest test = new vtest();

    for (int i=0; i<1000; i++)
      test.add(i);

    test.waitschleife(1000000000);
    System.out.println("test ist " + test.size() + "groß!");

    System.out.println("Wert ist: " + test.erster());
    System.out.println("Wert ist: " + test.erster());
    System.out.println("Wert ist: " + test.erster());
    test.add(1000);
    test.add(1001);
    test.add(1002);

    while (!test.isEmpty()) {
       test.waitschleife(10000000);
      System.out.println("Wert ist: " + test.erster());
    }
    System.out.println("FERTIG");
  }
}
