package jx.classfile;


abstract public class VerifyResult {

    public static final int NPA_RESULT = 1;
    public static final int FLA_RESULT = 2;
    public static final int WCET_RESULT = 3;
    public static final int CINSTR_RESULT = 4;
    protected int type;
    public int getType(){return type;}

    public VerifyResult(int type) {
	this.type = type;
    }
    
}
