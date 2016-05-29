package jx.zero;

public interface CAS extends Portal {
    boolean casObject(Object obj, Object compareValue, Object setValue);
    boolean casInt(Object obj, int compareValue, int setValue);
    boolean casBoolean(Object obj, boolean compareValue, boolean setValue);
    boolean casShort(Object obj, short compareValue, short setValue);
    boolean casByte(Object obj, byte compareValue, byte setValue);
}
