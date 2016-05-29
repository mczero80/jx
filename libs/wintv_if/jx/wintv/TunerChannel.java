package jx.wintv;

public class TunerChannel {
   String id;
   int frequ;
   
   public TunerChannel(String id, int frequ){
      this.id=id;
      this.frequ = frequ;
   }
   
   public String toString(){
      return "TunerChannel("+id+", "+frequ+")";
   }
   
   public String getId(){ return id; }
   public int getFrequ(){ return frequ; }
}
