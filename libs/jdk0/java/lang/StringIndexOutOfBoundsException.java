package java.lang;

public class StringIndexOutOfBoundsException extends RuntimeException {
    public StringIndexOutOfBoundsException() { super(); }    
    public StringIndexOutOfBoundsException(String s) { super(s); }
    public StringIndexOutOfBoundsException(int index) { super("Index="+index); }
}

