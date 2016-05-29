package test.wintv;

/*
 * IDEAS:
 * 
 *     - oberes Bild auch als LinkedFramebuffer (damit faellt die Orientierung leichter [eventuell zu leicht?])
 * 
 */


import jx.framebuffer.*;
import jx.wintv.*;
import jx.zero.*;
import jx.zero.debug.*;
import java.util.Random;

public class Puzzle {
    static Naming naming = InitialNaming.getInitialNaming();
   // grid parameters
   final static int gridSpace	= 2;
   final static int nPiecesX	= 4;
   final static int nPiecesY	= 4;
   
   // move parameters
   final static int stepwidth	= 4;	/* 4 is smooth */
   final static int moveTime	= 500;
   
   // physical colors
   final static short COLOR_WHITE	= (short)0xFFFF;
   final static short COLOR_RED		= (short)0xF800;
   
   // logical colors
   final static short COLOR_FREE	= COLOR_WHITE;
   final static short COLOR_GRID	= COLOR_RED;
   
   
   final static boolean verbose	= false;
   
   // indizes into moveInfo array
   final static int MOVE_UP	= 0;
   final static int MOVE_DOWN	= 1;
   final static int MOVE_LEFT	= 2;
   final static int MOVE_RIGHT	= 3;
   
   
   CaptureDevice captureDevice;
   PackedFramebuffer screenbuffer;
   LinkedPackedFramebuffer linkedFramebuffer;
//   ExtendedSubFramebuffer sparePiece, freePiece;
   
   PFBTools pfbtools;
   
   int pieceWidth, pieceHeight;
   
   class MoveInfos {
      int stepX, stepY;
      int steps;
      int sleeptime;
      
      MoveInfos(int stepX, int stepY, int steps){
	 this.stepX = stepX;
	 this.stepY = stepY;
	 this.steps = steps;
	 this.sleeptime = moveTime / steps;
	 
	 Debug.out.println("sleeptime = "+sleeptime);
      }
   }
   
   MoveInfos moveInfo[];
   
   
   /*** game state ***/
   
   int nMoves	= 0;			/* # of moves so far */
   
   ExtendedSubFramebuffer mapping[][];	/* mapping of (visible) screen cooridnates to video picture coordinates */
   
   /* index of free tile in "mapping" */
   int cur_x = nPiecesX;
   int cur_y = nPiecesY-1;
   
   
   class KeyWait {
      CPUState mystate;
      PuzzleInputDevice input = null;
      
      boolean autoplayToggle = false;
      
      ThreadEntry threadEntry = new ThreadEntry(){
	 /* 
	  * This is a inner class due to the fact, that the run-method
	  * should be hidden to the outside. Due to the fact, that KeyWait
	  * itself is a non-public inner class this is purely academical.
	  */
	 public void run(){
	    ZeroDomain.cpuMgr.setThreadName("PuzzleAutoplayToggler");
	    
	    // switch between autoplay and user play modes
	    int key;
	    while(true){
	       if(input != null){
		  do {
		     key = input.getKey();
		  } while( key != PuzzleInputDevice.KEY_AUTOPLAY );
		  autoplayToggle = true;
	       }

	       ZeroDomain.cpuMgr.block();
	    }
	 }
      };
      
      public KeyWait(){
	 mystate = ZeroDomain.cpuMgr.createCPUState(threadEntry);
      }
            
      public void watchAutoplayKey(PuzzleInputDevice input){
	 this.input = input;
	 autoplayToggle = false;
	 if( input != null )
	   ZeroDomain.cpuMgr.unblock(mystate);
      }
      
      public boolean isAutomodeKeyPressed(){
	 return autoplayToggle;
      }
   }
   
   KeyWait keywait = new KeyWait();
   
   /********************************************************************/
   
   public Puzzle(CaptureDevice captureDevice, TVNorm tvnorm, PackedFramebuffer screenbuffer){
      this.captureDevice = captureDevice;
      this.screenbuffer = screenbuffer;
      
      // setup screen layout
      final int sw = screenbuffer.width();
      final int sh = screenbuffer.height();
      
      final int fw = Math.min(sw/2, tvnorm.width);
      final int fh = Math.min(sh/2, tvnorm.height/2);
      
      ExtendedSubFramebuffer fullPicture = 
	new ExtendedSubFramebuffer(screenbuffer,
				   0, 0,
				   fw, fh);
      
      ExtendedSubFramebuffer gridPicture = 
	new ExtendedSubFramebuffer(screenbuffer, 
				   0, sh/2,
				   Math.min(fw+gridSpace*(nPiecesX-1), sw/2), 
				   Math.min(fh+gridSpace*(nPiecesY-1), sh/2));
      
      
      linkedFramebuffer = calcLinkedFramebuffer(gridPicture);
      pieceWidth  = linkedFramebuffer.getFramebuffer(0).width();
      pieceHeight = linkedFramebuffer.getFramebuffer(0).height();
      
      mapping = new ExtendedSubFramebuffer[nPiecesX+1][];
      for(int i=0; i<nPiecesX; ++i){
	 mapping[i] = new ExtendedSubFramebuffer[nPiecesY];
	 for(int j=0; j<nPiecesY; ++j){
	    mapping[i][j] = (ExtendedSubFramebuffer)linkedFramebuffer.getFramebuffer(i,j);
	 }
      }
      mapping[nPiecesX] = new ExtendedSubFramebuffer[nPiecesY];
      for(int i=0; i<nPiecesY; ++i)
	mapping[nPiecesX][i] = null;
      
      // extend gridPicture 
      gridPicture = new ExtendedSubFramebuffer(screenbuffer, 
					       gridPicture.xoffset(), gridPicture.yoffset(),
					       gridPicture.width()+pieceWidth+gridSpace, gridPicture.height());
      
      // reparent all subframebuffers
      for(int i=0; i<nPiecesX; ++i){
	 for(int j=0; j<nPiecesY; ++j){
	    mapping[i][j].reparent(gridPicture);
	 }
      }
      
      // last piece
      ExtendedSubFramebuffer tmp = mapping[nPiecesX-1][nPiecesY-1];
      mapping[nPiecesX][nPiecesY-1] = 
	new ExtendedSubFramebuffer(gridPicture,
				   tmp.xoffset()+pieceWidth+gridSpace, tmp.yoffset(),
				   pieceWidth, pieceHeight);
      
      moveInfo = new MoveInfos[] {
	 new MoveInfos(0, -stepwidth, pieceHeight/stepwidth),		/* MOVE_UP */
	 new MoveInfos(0,  stepwidth, pieceHeight/stepwidth),		/* MOVE_DOWN */
	 new MoveInfos(-stepwidth, 0, pieceWidth/stepwidth),		/* MOVE_LEFT */
	 new MoveInfos( stepwidth, 0, pieceWidth/stepwidth)		/* MOVE_RIGTH */
      };
      
      
      pfbtools = new PFBTools(gridPicture);
      pfbtools.drawRect16(0, 0, gridPicture.width(), gridPicture.height(), COLOR_GRID);
      
      tmp = mapping[nPiecesX][nPiecesY-1];
      pfbtools.drawRect16(tmp.xoffset(), tmp.yoffset(), tmp.width(), tmp.height(), COLOR_FREE);
      
      // Note: InputSource is already selected
      FramebufferScaler scaler = new FramebufferScaler(captureDevice);
      
      Debug.out.println("odd framebuffer : " + fullPicture);
      captureDevice.setFramebufferOdd(fullPicture);
      scaler.setScalingOdd(tvnorm, fullPicture, false);
      
      Debug.out.println("even framebuffer: " + linkedFramebuffer);
      captureDevice.setFramebufferEven(linkedFramebuffer);
      scaler.setScalingEven(tvnorm, linkedFramebuffer, false);
      
      // misc video adjustments
      captureDevice.setVideoAdjustment(new GammaCorrectionRemovalAdjustment(false));
      captureDevice.setVideoAdjustment(new ChromaCombAdjustment(ChromaCombAdjustment.ODD,  false));
      captureDevice.setVideoAdjustment(new ChromaCombAdjustment(ChromaCombAdjustment.EVEN, false));

      captureDevice.setFieldsActive(true, true);
      captureDevice.captureOn();
   }
   
   
   ExtendedSubFramebuffer calcFieldBuffer(PackedFramebuffer framebuffer, TVNorm tvnorm, 
					  int aspectWidth, int aspectHeight,
					  int xoffset, int yoffset,
					  int marginsX, int marginsY){
      
      /* calling example
      gridPicture = calcFieldBuffer(screenbuffer, tvnorm, 4, 3
				    0, sh/2,
				    sw/2-gridSpace*(nPiecesX-1), 
				    sh/2-gridSpace*(nPiecesY-1));
       */
      
      int width  = framebuffer.width();
      int height = framebuffer.height();
      
      if( width > tvnorm.width )
	width = tvnorm.width;
      if( height > tvnorm.height/2 )
	height = tvnorm.height;
      
      
      
      throw new NotImpl(); // FIXME
   }
   
   /********************************************************************/
   
   public void play(PuzzleInputDevice inputDevice){
      boolean result;
      int key;
      while(true){
	 key = inputDevice.getKey();
	 switch(key){
	  case PuzzleInputDevice.KEY_UP:
	    result = moveUp();
	    break;
	  case PuzzleInputDevice.KEY_DOWN:
	    result = moveDown();
	    break;
	  case PuzzleInputDevice.KEY_LEFT:
	    result = moveLeft();
	    break;
	  case PuzzleInputDevice.KEY_RIGHT:
	    result = moveRight();
	    break;
	  case PuzzleInputDevice.KEY_RESET:
	    
	    // dump some interesting debugging information
	    captureDevice.dumpStatus();
	    
	    Debug.out.println("LinkedPackedFramebuffer:");
	    Debug.out.println(linkedFramebuffer.subbuffersString());
	    
	    Debug.out.println("mapping:");
	    for(int y=0; y<nPiecesY; ++y){
	       for(int x=0; x<nPiecesX; ++x){
		  Debug.out.print(Hex.toHexString(mapping[x][y].startAddress())+"   ");
	       }
	       Debug.out.println();
	    }
	    // */

	    captureDevice.setFramebufferEven(linkedFramebuffer);
	    captureDevice.setFieldsActive(true, true);
	    captureDevice.captureOn();
	    paintGrid();
	    pfbtools.drawRect16(mapping[cur_x][cur_y].xoffset(), mapping[cur_x][cur_y].yoffset(), 
				mapping[cur_x][cur_y].width(),   mapping[cur_x][cur_y].height(), 
				COLOR_FREE);
	    Debug.out.println("**** reseted ****");
	    continue;
	    
	  case PuzzleInputDevice.KEY_AUTOPLAY:
	    return;
	    
	  default:
	    throw new Hell();
	 }
	 
	 Debug.out.println("Moves: "+nMoves);
	 if( !result )
	   Debug.out.println("**** BEEP ****");
      }
   }
   
   /********************************************************************/
   
   public void autoplay(final PuzzleInputDevice input){
       DomainManager domainManager = (DomainManager) naming.lookup("DomainManager");
       Domain myDomain = domainManager.getCurrentDomain();
      
      keywait.watchAutoplayKey(input);
      
      MyRandom rnd = new MyRandom();
      //Random rnd = new Random(42);
      // Random rnd = new Random(MySleep.timestamp());
      int move = 3;
      int undomove;
      boolean moved;
      while(!keywait.isAutomodeKeyPressed()){
	 switch(move){
	  case MOVE_UP:
	    undomove = MOVE_DOWN;
	    moved = moveUp();
	    break;
	  case MOVE_DOWN:
	    undomove = MOVE_UP;
	    moved = moveDown();
	    break;
	  case MOVE_LEFT:
	    undomove = MOVE_RIGHT;
	    moved = moveLeft();
	    break;
	  case MOVE_RIGHT:
	    undomove = MOVE_LEFT;
	    moved = moveRight();
	    break;
	  default:
	    throw new Hell();
	 }
	 if( moved ){
	    Debug.out.println("Moves: "+nMoves);

	    // an opportunity for a garbage collection
	    //	    domainManager.gc(myDomain);

	    MySleep.msleep(250);
	 }
	 else
	   Debug.out.println("**** BEEP ****");
	 
	 do {
	    move = Math.abs(rnd.nextInt()%4);
	 } while( moved && undomove == move );
      }
      
      keywait.watchAutoplayKey(null);
   }
   
   /********************************************************************/
   
   public boolean moveUp(){
      int src_y = cur_y + 1;
      
      if( src_y < 0 || src_y >= nPiecesY || cur_x < 0 || cur_x >= nPiecesX)
	return false;
      
      movePart(cur_x, src_y, MOVE_UP);
      ++nMoves;
      
      return true;
   }
   
   public boolean moveDown(){
      int src_y = cur_y - 1;
      
      if( src_y < 0 || src_y >= nPiecesY || cur_x < 0 || cur_x >= nPiecesX)
	return false;
      
      movePart(cur_x, src_y, MOVE_DOWN);
      ++nMoves;
      
      return true;
   }
   
   public boolean moveLeft(){
      int src_x = cur_x + 1;
      
      if( cur_x == nPiecesX-1 && cur_y == nPiecesY-1 ){
	 Debug.out.println("Special move Left");
      }
      else if( src_x < 0 || src_x >= nPiecesX )
	return false;
      
      movePart(src_x, cur_y, MOVE_LEFT);
      ++nMoves;
      
      return true;
   }
   
   public boolean moveRight(){
      int src_x = cur_x - 1;
      
      if( src_x < 0 || src_x >= nPiecesX )
	return false;
      
      movePart(src_x, cur_y, MOVE_RIGHT);
      ++nMoves;
      
      return true;
   }
   
   /********************************************************************/
   
   void movePart(int src_x, int src_y, int direction){
      ExtendedSubFramebuffer source = mapping[src_x][src_y];
      ExtendedSubFramebuffer dest   = mapping[cur_x][cur_y];
      MoveInfos move = moveInfo[direction];
      
      int oldx = source.xoffset();
      int oldy = source.yoffset();
      
      int newx = dest.xoffset();
      int newy = dest.yoffset();
      
      int dx = dest.xoffset() - source.xoffset();
      int dy = dest.yoffset() - source.yoffset();
      
      if( verbose )
	Debug.out.println("stepX = "+move.stepX+ ", stepY = "+move.stepY);
      
      int curx = oldx;			/* Note: do not confuse with cur_x, cur_y !! */
      int cury = oldy;
      
      int steps = move.steps;
      while( steps > 0 ){
	 curx += move.stepX;
	 cury += move.stepY;
	 
	 source.moveInFramebuffer(curx, cury);
	 captureDevice.setFramebufferEven(linkedFramebuffer);
	 captureDevice.setEvenFieldActive(true);
	 
	 // /*
	 switch(direction){
	  case MOVE_UP:
	    pfbtools.drawRect16(curx, cury+pieceHeight, pieceWidth, oldy-cury, COLOR_FREE);
	    break;
	  case MOVE_DOWN:
	    pfbtools.drawRect16(oldx, oldy, pieceWidth, cury-oldy, COLOR_FREE);
	    break;
	  case MOVE_LEFT:
	    pfbtools.drawRect16(curx+pieceWidth, cury, oldx-curx, pieceHeight, COLOR_FREE);
	    break;
	  case MOVE_RIGHT:
	    pfbtools.drawRect16(oldx, oldy, curx-oldx, pieceHeight, COLOR_FREE);
	    break;
	  default:
	    throw new Hell();
	 }
	 // */
	 
	 --steps;
	 
	 if( steps > 0 )
	   MySleep.msleep(move.sleeptime);
      }
      
      // rest from stepwidth
      if( curx != newx || cury != newy ){
	 source.moveInFramebuffer(newx, newy);
	 captureDevice.setFramebufferEven(linkedFramebuffer);
	 captureDevice.setEvenFieldActive(true);
      }
      
      dest.moveInFramebuffer(oldx, oldy);
      
      // redraw everything we touched
      pfbtools.drawRect16(oldx, oldy, pieceWidth, pieceHeight, COLOR_FREE);
      /*
      switch(direction){
       case MOVE_UP:
	 pfbtools.drawRect16(oldx, oldy-gridSpace, pieceWidth, gridSpace, COLOR_GRID);
	 break;
       case MOVE_DOWN:
	 pfbtools.drawRect16(newx, newy-gridSpace, pieceWidth, gridSpace, COLOR_GRID);
	 break;
       case MOVE_LEFT:
	 pfbtools.drawRect16(oldx-gridSpace, oldy, gridSpace, pieceHeight, COLOR_GRID);
	 break;
       case MOVE_RIGHT:
	 pfbtools.drawRect16(newx-gridSpace, newy, gridSpace, pieceHeight, COLOR_GRID);
	 break;
       default:
	 throw new Hell();
      }
      // */

      paintGrid();
      
      mapping[cur_x][cur_y] = source;
      mapping[src_x][src_y] = dest;
      
      cur_x = src_x;
      cur_y = src_y;
   }
   
   /********************************************************************/
   
   public void permutate(){
      permutate(2*nPiecesX*nPiecesY);
   }
   
   public void permutate(int permutations){
       //Random rnd = new Random(MySleep.timestamp());
      MyRandom rnd = new MyRandom();
      
      int src_x, src_y;
      int dst_x, dst_y;
      
      if( verbose ){
	 Debug.out.println("****");
	 Debug.out.println(linkedFramebuffer.subbuffersString());
      }
      
      ExtendedSubFramebuffer tmp;
      for(int i=0; i < permutations; ++i){
	 src_x = Math.abs(rnd.nextInt() % nPiecesX);
	 src_y = Math.abs(rnd.nextInt() % nPiecesY);
	 
	 dst_x = Math.abs(rnd.nextInt() % nPiecesX);
	 dst_y = Math.abs(rnd.nextInt() % nPiecesY);
	 
	 if( src_x == dst_x && src_y == dst_y ){
	    if(verbose)
	      Debug.out.println("**** skipping 1 ****");
	    continue;
	 }
	 
	 if(  (src_x == nPiecesX-1 && src_y == nPiecesY-1)    /* don't touch last piece */
	    ||(dst_x == nPiecesX-1 && dst_y == nPiecesY-1) ){
	    if( verbose )
	      Debug.out.println("**** skipping 2 ****");
	    continue;
	 }
	 
	 if(  (src_x == cur_x && src_y == cur_y)   /* keep free piece where it is */
	    ||(dst_x == cur_x && dst_y == cur_y) ){
	       if( verbose )
		 Debug.out.println("**** skipping 3 ****");
	       continue;
	 }
	 
	 if(verbose)
	   Debug.out.println(src_x+","+src_y+" <-> "+dst_x+","+dst_y);

	 tmp = (ExtendedSubFramebuffer)linkedFramebuffer.getFramebuffer(src_x, src_y);
	 linkedFramebuffer.setFramebuffer(src_x, src_y, 
					  linkedFramebuffer.getFramebuffer(dst_x, dst_y));
	 linkedFramebuffer.setFramebuffer(dst_x, dst_y, tmp);
      }
      
      if( verbose )
	Debug.out.println(linkedFramebuffer.subbuffersString());
      
      captureDevice.setFramebufferEven(linkedFramebuffer);
      captureDevice.setEvenFieldActive(true);
      captureDevice.captureOn();
      
      tmp = mapping[cur_x][cur_y];
      pfbtools.drawRect16(tmp.xoffset(), tmp.yoffset(), tmp.width(), tmp.height(), COLOR_FREE);
      paintGrid();
   }
   
   /********************************************************************/
   
   LinkedPackedFramebuffer calcLinkedFramebuffer(PackedFramebuffer gridbuffer){
      LinkedPackedFramebuffer linked = new LinkedPackedFramebuffer(nPiecesX, nPiecesY);
      
      final int dx = (gridbuffer.width()  + gridSpace) / nPiecesX;
      final int dy = (gridbuffer.height() + gridSpace) / nPiecesY;
      
      int pieceWidth  = dx - gridSpace;
      int pieceHeight = dy - gridSpace;
      
      // generate subframebuffers
      for(int x=0; x<nPiecesX; ++x){
	 for(int y=0; y<nPiecesY; ++y){
	    PackedFramebuffer subfb= new ExtendedSubFramebuffer(gridbuffer,
								x * dx, y * dy,
								pieceWidth, pieceHeight);
	    /*
	    Debug.out.println( "("+x+", "+y+
			      "): new " + subfb +
			      " @ "+PFBTools.calcX(gridbuffer, subfb)+"+"+PFBTools.calcY(gridbuffer, subfb)
			      );
	     // */
	    linked.setFramebuffer(x, y, subfb);
	 }
      }
      return linked;
   }
   
   /********************************************************************/
   
   void paintGrid(){
      for(int i=1; i<=nPiecesX; ++i){
	 for(int j=1; j<=nPiecesY; ++j){
	    pfbtools.drawRect16(0, j*(pieceHeight+gridSpace)-gridSpace, 
				      nPiecesX*(pieceWidth+gridSpace), gridSpace, 
				      COLOR_GRID);
	    
	    pfbtools.drawRect16(i*(pieceWidth+gridSpace)-gridSpace, 0, 
				gridSpace, nPiecesY*(pieceHeight+gridSpace),
				COLOR_GRID);
	 }
      }
   }
}

/********************************************************************/

/* 
 * This framebuffer allows moving itself around within the bounds of its
 * parent framebuffer. The advantage over simply creating a new
 * SubFramebuffer is, that no new object is created to prevent memory
 * exhaustion (because the garbage collector is not yet functional).
 */
class ExtendedSubFramebuffer extends SubFramebuffer {
   PackedFramebuffer framebuffer;
   
   public ExtendedSubFramebuffer(PackedFramebuffer framebuffer, int x, int y, int w, int h){
      super(framebuffer, x, y, w, h);
      this.framebuffer = framebuffer;
   }
   public void moveInFramebuffer(int newx, int newy){
      Debug.assert(newx >= 0);
      Debug.assert(newy >= 0);
      Debug.assert(newx + width() <= framebuffer.width());
      Debug.assert(newy + height() <= framebuffer.height());
      
      xoff = newx;
      yoff = newy;
      setNewStartAddress(framebuffer.startAddress() + yoff*framebuffer.scanlineOffset() + xoff*framebuffer.pixelOffset());
   }
   
   public void reparent(PackedFramebuffer newparent){
      int x = PFBTools.calcX(newparent, this);
      int y = PFBTools.calcY(newparent, this);
      
      Debug.assert( x + width()  <= newparent.width(),  "new parent not wide enough for this framebuffer");
      Debug.assert( y + height() <= newparent.height(), "new parent not high enough for this framebuffer");
      
      xoff = x;
      yoff = y;
      framebuffer = newparent;
   }
   
   public int xoffset(){ return xoff; }
   public int yoffset(){ return yoff; }
   
   public String framebufferType(){
      return "ExtendedSubFramebuffer";
   }
}



