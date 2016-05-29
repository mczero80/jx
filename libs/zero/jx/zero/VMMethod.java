package jx.zero;

public interface VMMethod {
    String getName();
    String getSignature();

    Object invoke(Object obj, Object args[]);
}
