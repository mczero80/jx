package test.awt;
//
// Breakout game
//
// (C)2000
// Brian Postma
// b.postma@hetnet.nl
//

import java.awt.*;
import java.applet.Applet;

public class Breakout extends Applet implements Runnable
{
  Dimension	d;
  Font 		largefont = new Font("Helvetica", Font.BOLD, 24);
  Font		smallfont = new Font("Helvetica", Font.BOLD, 14);

  FontMetrics	fmsmall, fmlarge;  
  Graphics	goff;
  Image		ii;
  Thread	thethread;

  boolean	ingame=false;

  int		player1score;
  int		ballx,bally;
  int		batpos;
  int		batdpos=0;
  int		balldx=0, balldy=0;
  int		dxval;
  int		ballsleft;
  int		count;
  boolean	showtitle=true;
  boolean[]	showbrick;
  int		bricksperline;

  final int	borderwidth=5;
  final int	batwidth=20;
  final int	ballsize=5;
  final int	batheight=5;
  final int	scoreheight=20;
  final int	screendelay=300;
  final int	brickwidth=15;
  final int     brickheight=8;
  final int	brickspace=2;
  final int	backcol=0x102040;
  final int	numlines=4;
  final int     startline=32;

  public String getAppletInfo()
  {
    return("Breakout - by Brian Postma");
  }

  public void init()
  {
    Graphics g;
    d = size();
    setBackground(new Color(backcol));
    bricksperline=(d.width-2*borderwidth)/(brickwidth+brickspace);
    d.width=bricksperline*(brickwidth+brickspace)+(2*borderwidth);
    g=getGraphics();
    g.setFont(smallfont);
    fmsmall = g.getFontMetrics();
    g.setFont(largefont);
    fmlarge = g.getFontMetrics();

    showbrick=new boolean[bricksperline*numlines];
    GameInit();
  }

  public void GameInit()
  {
    batpos=(d.width-batwidth)/2;
    ballx=(d.width-ballsize)/2;
    bally=(d.height-ballsize-scoreheight-2*borderwidth);
    player1score=0;
    ballsleft=3;
    dxval=2;
    if (Math.random()<0.5)
      balldx=dxval;
    else
      balldx=-dxval;
    balldy=-dxval;
    count=screendelay;
    batdpos=0;
    InitBricks();
  }

  public void InitBricks()
  {
    int i;
    for (i=0; i<numlines*bricksperline; i++)
      showbrick[i]=true;
  }

  public boolean keyDown(Event e, int key)
  {
    if (ingame)
    {
      if (key == Event.LEFT)
          batdpos=-3;
      if (key == Event.RIGHT)
        batdpos=3;
      if (key == Event.ESCAPE)
        ingame=false;
    }
    else
    {
      if (key == 's' || key == 'S')
      {
        ingame=true;
        GameInit();
      }
    }
    return true;
  }

  public boolean keyUp(Event e, int key)
  {
    System.out.println("Key: "+key);
    if (key == Event.LEFT || key == Event.RIGHT)
       batdpos=0;
    return true;
  }

  public void paint(Graphics g)
  {
    String s;
    Graphics gg;

    if (goff==null && d.width>0 && d.height>0)
    {
      ii = createImage(d.width, d.height);
      goff = ii.getGraphics();
    }
    if (goff==null || ii==null)
      return;

    goff.setColor(new Color(backcol));
    goff.fillRect(0, 0, d.width, d.height);
    if (ingame)
      PlayGame();
    else
      ShowIntroScreen();
    g.drawImage(ii, 0, 0, this);
  }


  public void PlayGame()
  {
    MoveBall();
    CheckBat();
    CheckBricks();
    DrawPlayField();
    DrawBricks();
    ShowScore();
  }

  public void ShowIntroScreen()
  {
    String s;

    MoveBall();
    CheckBat();
    CheckBricks();
    BatDummyMove();
    DrawPlayField();
    DrawBricks();
    ShowScore();
    goff.setFont(largefont);
    goff.setColor(new Color(96,128,255));

    if (showtitle)
    {
      s="Java Breakout";
      goff.drawString(s,(d.width-fmlarge.stringWidth(s)) / 2, (d.height-scoreheight-borderwidth)/2 - 20);
      s="(c)2000 by Brian Postma";
      goff.setFont(smallfont);
      goff.setColor(new Color(255,160,64));
      goff.drawString(s,(d.width-fmsmall.stringWidth(s))/2,(d.height-scoreheight-borderwidth)/2 + 10);
      s="b.postma@hetnet.nl";
      goff.drawString(s,(d.width-fmsmall.stringWidth(s))/2,(d.height-scoreheight-borderwidth)/2 + 30);
    }
    else
    {
      goff.setFont(smallfont);
      goff.setColor(new Color(96,128,255));
      s="'S' to start game";
      goff.drawString(s,(d.width-fmsmall.stringWidth(s))/2,(d.height-scoreheight-borderwidth)/2 - 10);
      goff.setColor(new Color(255,160,64));
      s="Use cursor left and right to move";
      goff.drawString(s,(d.width-fmsmall.stringWidth(s))/2,(d.height-scoreheight-borderwidth)/2 + 20);
    }
    count--;
    if (count<=0)
    { count=screendelay; showtitle=!showtitle; }
  }


  public void DrawBricks()
  {
    int i,j;
    boolean nobricks=true;
    int colordelta=255/(numlines-1);

    for (j=0; j<numlines; j++)
    {
      for (i=0; i<bricksperline; i++)
      {
        if (showbrick[j*bricksperline+i])
        {
          nobricks=false;
          goff.setColor(new Color(255,j*colordelta,255-j*colordelta));
          goff.fillRect(borderwidth+i*(brickwidth+brickspace), startline+j*(brickheight+brickspace),
               brickwidth, brickheight);
        }
      }
    }
    if (nobricks)
    {
      InitBricks();
      if (ingame)
        player1score+=100;
    }
  }
   
  public void DrawPlayField()
  {
    goff.setColor(Color.white);
    goff.fillRect(0,0,d.width,borderwidth);
    goff.fillRect(0,0,borderwidth,d.height);
    goff.fillRect(d.width-borderwidth,0,borderwidth,d.height);
    goff.fillRect(batpos,d.height-2*borderwidth-scoreheight, batwidth,batheight); // bat
    goff.fillRect(ballx,bally,ballsize,ballsize); // ball
  }


  public void ShowScore()
  {
    String s;
    goff.setFont(smallfont);
    goff.setColor(Color.white);

    s="Score: "+player1score;
    goff.drawString(s,40,d.height-5);
    s="Balls left: "+ballsleft;
    goff.drawString(s,d.width-40-fmsmall.stringWidth(s),d.height-5);
  }


  public void MoveBall()
  {
    ballx+=balldx;
    bally+=balldy;
    if (bally<=borderwidth)
    {
      balldy=-balldy;
      bally=borderwidth;
    }
    if (bally>=(d.height-ballsize-scoreheight))
    {
      if (ingame)
      {
        ballsleft--;
        if (ballsleft<=0)
          ingame=false;
      }
      ballx=batpos+(batwidth-ballsize)/2;
      bally=startline+numlines*(brickheight+brickspace);
      balldy=dxval;
      balldx=0;
    }
    if (ballx>=(d.width-borderwidth-ballsize))
    {
      balldx=-balldx;
      ballx=d.width-borderwidth-ballsize;
    }
    if (ballx<=borderwidth)
    {
      balldx=-balldx;
      ballx=borderwidth;
    }
  }

  public void BatDummyMove()
  {
    if (ballx<(batpos+2))
      batpos-=3;
    else if (ballx>(batpos+batwidth-3))
      batpos+=3;
  }

  public void CheckBat()
  {
    batpos+=batdpos;

    if (batpos<borderwidth)
      batpos=borderwidth;
    else if (batpos>(d.width-borderwidth-batwidth))
      batpos=(d.width-borderwidth-batwidth);
 
    if (bally>=(d.height-scoreheight-2*borderwidth-ballsize) && 
        bally<(d.height-scoreheight-2*borderwidth) &&
        (ballx+ballsize)>=batpos && ballx<=(batpos+batwidth))
    {
      bally=d.height-scoreheight-ballsize-borderwidth*2;
      balldy=-dxval;
      balldx=CheckBatBounce(balldx,ballx-batpos);
    }
  }

  public int CheckBatBounce(int dy, int delta)
  {
    int sign;
    int stepsize, i=-ballsize, j=0;
    stepsize=(ballsize+batwidth)/8;

    if (dy>0)
      sign=1;
    else
      sign=-1;

    while(i<batwidth && delta>i)
    {
      i+=stepsize;
      j++;
    }
    switch(j)
    {
      case 0:
      case 1:
        return -4;
      case 2:
	return -3;
      case 7:
        return 3;
      case 3:
      case 6:
        return sign*2;
      case 4:
      case 5:
        return sign*1;
      default:
        return 4;
    }
  }

  public void CheckBricks()
  {
    int i,j,x,y;
    int xspeed=balldx;
    if (xspeed<0) xspeed=-xspeed;
    int ydir=balldy;

    if (bally<(startline-ballsize) || bally>(startline+numlines*(brickspace+brickheight)))
      return;
    for (j=0; j<numlines; j++)
    {
      for (i=0; i<bricksperline; i++)
      {
        if (showbrick[j*bricksperline+i])
        {
          y=startline+j*(brickspace+brickheight);
          x=borderwidth+i*(brickspace+brickwidth);
          if (bally>=(y-ballsize) && bally<(y+brickheight) &&
              ballx>=(x-ballsize) && ballx<(x+brickwidth))
          {
            showbrick[j*bricksperline+i]=false;
            if (ingame)
              player1score+=(numlines-j);
            // Where did we hit the brick
            if (ballx>=(x-ballsize) && ballx<=(x-ballsize+3))
            { // leftside
              balldx=-xspeed;
            }
            else if (ballx<=(x+brickwidth-1) && ballx>=(x+brickwidth-4))
            { // rightside
              balldx=xspeed;
            }              
            balldy=-ydir;
          }
        }
      }
    }
  }

  public void run()
  {
    long  starttime;
    Graphics g;

    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
    g=getGraphics();

    while(true)
    {
      starttime=System.currentTimeMillis();
      try
      {
        paint(g);
        starttime += 20;
        Thread.sleep(Math.max(0, starttime-System.currentTimeMillis()));
      }
      catch (InterruptedException e)
      {
        break;
      }
    }
  }

  public void start()
  {
    if (thethread == null) {
      thethread = new Thread(this);
      thethread.start();
    }
  }

  public void stop()
  {
    if (thethread != null) {
      thethread.stop();
      thethread = null;
    }
  }
}
