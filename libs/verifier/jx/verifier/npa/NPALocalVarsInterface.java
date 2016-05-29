package jx.verifier.npa;

import jx.verifier.VerifyException;


public interface NPALocalVarsInterface {
    public void write(int index, NPAValue type, int bcAddr) throws VerifyException;
    public NPAValue NPAread(int index) throws VerifyException;
    //find all local Vars with same id as value (must be a valid id!) and change their
    //value to newVal.
    public void setValue(NPAValue value, int newVal);
}
