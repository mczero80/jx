package jx.vis.mif;

import java.io.*;
import jx.vis.*;

public class MIFVisualizer implements Visualizer {
    PrintStream out;
    int unique = 10000;
    int useGroup = -1;

    public MIFVisualizer(OutputStream out) {
	this.out = new PrintStream(new BufferedOutputStream(out));
    }

    public void init() {
	// document
	out.println("<MIFFile 5.50>");
	out.println("<Units Ucm>");
	out.println("<CharUnits CUpt>");
	out.println("<Document ");
	out.println(" <DViewRect 30 30 1176 569 >");
	out.println(" <DWindowRect 1 13 1220 672 >");
	out.println(" <DViewScale  100.0%%>");
	out.println(" <DNextUnique "+unique+">");
	out.println(" <DPageSize  30 cm 20 cm>");
	out.println("> # end of Document");

	// page
	out.println("<Page ");
	out.println(" <Unique "+(unique++)+">");
	out.println(" <PageType BodyPage>");
	out.println(" <PageNum `1'>");
	out.println(" <PageTag `'>");
	out.println(" <PageSize  30 cm 20 cm>");
	out.println(" <PageOrientation Portrait>");
	out.println(" <PageAngle  0.0>");
	out.println(" <PageBackground `Default'>");
    }

    public void finish() {
	out.println("> # end of Page");
	out.println("# End of MIFFile");
	out.close();
    }


    public void drawLine(int x0, int y0, int x1, int y1) {
	out.println(" <PolyLine ");
	common();
	out.println("  <PenWidth  0.5 pt>");
	out.println("  <ObColor `Black'>");
	out.println("  <DashedPattern ");
	out.println("   <DashedStyle Solid>");
	out.println("  > # end of DashedPattern");
	out.println("  <NumPoints 2>");
	out.println("  <Point  "+x0+" pt "+y0+" pt>");
	out.println("  <Point  "+x1+" pt "+y1+" pt>");
	out.println(" > # end of PolyLine");
    }

    public void drawThinLine(int x0, int y0, int x1, int y1) {
	out.println(" <PolyLine ");
	common();
	out.println("  <PenWidth  0.1 pt>");
	out.println("  <ObColor `Black'>");
	out.println("  <DashedPattern ");
	out.println("   <DashedStyle Dashed>");
	out.println("   <NumSegments 2>");
	out.println("   <DashSegment  2.0 pt>");
	out.println("   <DashSegment  4.0 pt>");
	out.println("  > # end of DashedPattern");
	out.println("  <NumPoints 2>");
	out.println("  <Point  "+x0+" pt "+y0+" pt>");
	out.println("  <Point  "+x1+" pt "+y1+" pt>");
	out.println(" > # end of PolyLine");
    }

    public void drawEllipse(int x, int y, int w, int h) {
	out.println(" <Ellipse ");
	common();
	out.println("  <Pen 0>");
	out.println("  <Fill 0>");
	out.println("  <ObColor `Black'>");
	out.println("  <PenWidth  0.5 pt>");
	out.println("  <ShapeRect  "+x+" pt "+y+" pt "+w+" pt "+h+" pt>");
	out.println("  <BRect  "+x+" pt "+y+" pt "+w+" pt "+h+" pt>");
	out.println(" > # end of Ellipse\n");
    }

    public void drawText(String text, int x, int y, int fontSize, int align) {
	drawText(text, x, y, fontSize, align, ROTATE_0, STYLE_DEFAULT);
    }
    public void drawText(String text, int x, int y, int fontSize, int align, int rotate, int style) {
	out.println(" <TextLine ");
	common();
	out.println("  <Angle  "+(360-rotate)+">");
	out.println("  <TLOrigin  "+x+" pt "+y+" pt>");
	switch(align) {
	case ALIGN_LEFT: out.println("  <TLAlignment Left>"); break;
	case ALIGN_RIGHT: out.println("  <TLAlignment Right>"); break;
	case ALIGN_CENTER: out.println("  <TLAlignment Center>"); break;
	default: throw new Error("Unknown alignmenmt");
	}
	out.println("  <ObColor `Black'>");
	out.println("  <Font ");
	out.println("   <FSize "+fontSize+" pt>");
	if ((style & STYLE_BOLD) != 0) {
	    out.println("   <FWeight `Bold'>");
	} else {
	    out.println("   <FWeight `Regular'>");
	}
	out.println("   <FLocked No>");
	out.println("  > # end of Font");
	out.println("  "+mifstring(text));
	out.println(" > # end of TextLine");
    }

    public void drawRect(int x, int y, int w, int h, String fill) {
	out.println(" <Rectangle ");
	common();
	out.println("  <Fill "+fill+">");
	out.println("  <ObColor `Black'>");
	out.println("  <DashedPattern ");
	out.println("   <DashedStyle Solid>");
	out.println("  > # end of DashedPattern");
	out.println("  <ShapeRect  "+x+" pt "+y+" pt "+w+" pt "+h+" pt>");
	out.println("  <BRect  "+x+" pt "+y+" pt "+w+" pt "+h+" pt>");
	out.println(" > # end of Rectangle");
    }

    private void common() {
	out.println("  <Unique "+unique+++">");
	if (useGroup != -1) out.println("  <GroupID "+useGroup+">");
    }

    private String mifstring(String str) {
	StringBuffer ret = new StringBuffer();
	ret.append("   <String `");
	char [] s = str.toCharArray();
	for (int i=0; i<s.length; i++) {
	    /* if ((str[i]=='\\')||(str[i]=='>')) mstr[j++]='\\'; */
	    if (s[i]=='>') ret.append('\\');
	    ret.append(s[i]);
	}
	ret.append("'>\n");
	return ret.toString();
    }

}
