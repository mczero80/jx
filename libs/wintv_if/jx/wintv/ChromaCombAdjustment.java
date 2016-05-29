package jx.wintv;

public class ChromaCombAdjustment extends BooleanVideoAdjustment {
   final static class Field extends Integer {
      public Field(int fieldno){
	 super(fieldno);
      }
   }

   final static public Field ODD =  new Field(0);
   final static public Field EVEN = new Field(1);
   
   Field field;
   public ChromaCombAdjustment(Field field, boolean enable){
      this.field = field;
      this.hardwareValue = enable;
   }
   
   public Field getField(){
      return field;
   }
}

