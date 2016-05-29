package java.lang;

public class ArrayIndexOutOfBoundsException extends IndexOutOfBoundsException {
    public ArrayIndexOutOfBoundsException() { super(); }
    public ArrayIndexOutOfBoundsException(String s) { super(s); }
    public ArrayIndexOutOfBoundsException(int index) { this("Array index out of bounds: " + index); }
}

