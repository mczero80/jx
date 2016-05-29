package java.text;

public class DecimalFormat extends NumberFormat {
    public void applyPattern(String pattern) {
	throw new Error("NOT IMPLEMENTED");	
    }
    public Number parse(String text, ParsePosition parsePosition) {
	throw new Error("NOT IMPLEMENTED");	
    }
    public StringBuffer format(double number,StringBuffer result, FieldPosition fieldPosition) {
	throw new Error("NOT IMPLEMENTED");	
    }
    public StringBuffer format(long number,
			       StringBuffer result,
			       FieldPosition fieldPosition) {
	throw new Error("NOT IMPLEMENTED");	
    }

}
