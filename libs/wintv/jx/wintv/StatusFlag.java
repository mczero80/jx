package jx.wintv;

public class StatusFlag {
   public static final int STATUS_CLR = 0x1;
   public static final int STATUS_SET = 0x2;
   public static final int STATUS_ALL = STATUS_CLR | STATUS_SET;
   
   int mask;
   String zero;
   String nonzero;
   
   public StatusFlag(int mask, String zero, String nonzero){
      this.mask = mask;
      this.zero = zero;
      this.nonzero = nonzero;
   }
   
   public static String decode(int val, StatusFlag flags[], int options){
      String retval = "";
      boolean needSeparator = false;
      String addval;
      for(int i=0; i<flags.length; ++i){
	 addval = null;
	 if( (val & flags[i].mask) == 0 ){
	    if( (options & STATUS_CLR) != 0 )
	       addval = flags[i].zero;
	 }
	 else {
	    if( (options & STATUS_SET) != 0 )
	      addval =  flags[i].nonzero;
	 }
	 if( (addval != null) && (addval.length() > 0) ){
	    if( needSeparator ) 
	      retval += ", ";
	    retval += addval;
	    needSeparator = true;
	 }
      }
      return retval;
   }
}

