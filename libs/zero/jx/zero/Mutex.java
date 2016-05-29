package jx.zero;

public interface Mutex extends Portal {
    int lock   ();
    int trylock();
    int unlock ();
    int destroy();
}
