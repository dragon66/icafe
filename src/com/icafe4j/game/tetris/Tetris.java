/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.game.tetris;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;

/** 
 * Tetris like game.
 * <p>
 * This version changes the behavior when the down arrow is pressed,
 * make it more reasonable logically.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 2.01 01/04/2002
 */
public class Tetris extends Applet implements Runnable
{
	private static final long serialVersionUID = -6071167213144315965L;

	private Thread t;

	private Image offscreenImage;
	private Graphics offScrGraphics;

	private MediaTracker mediatracker;

	private int width, height;
   
	private int xCount;
	private int yCount;
	private char board[][];

	private Image piece0, piece1, piece2, piece3, image; 
	
	private int count1, count2, count3;

	private int blocksize;
	private int boardx;
	private int boardy;

	private Piece piece[];
	private int piecetype;
	private int pieceposition;

	private char moving;

	private char state;

	private int valid;

	private int score;

	private int speed;

	private int sleepnum;

	private boolean sync;

	
	public void init() {
		blocksize = 15;
		boardx = 150;
		boardy = 100;
		speed = 3;
		xCount = 12;
	    yCount = 20;
		state = 'p';
	    board = new char[yCount][xCount];
		piece = new Piece[4];
		for (int i = 0; i < 4; i++)	{
			piece[i] = new Piece();
		}
	
     	width = this.getSize().width;
		height = this.getSize().height;
		
		// Create offscreen Image
		offscreenImage = createImage(width, height);
		offScrGraphics = offscreenImage.getGraphics();
        // Absolute resource path
		String imagePath = "/cafe/game/images/";
		
		piece0 = getImage(getClass().getResource(imagePath + "piece0.gif"));
		piece1 = getImage(getClass().getResource(imagePath + "piece1.gif"));
		piece2 = getImage(getClass().getResource(imagePath + "piece2.gif"));
		piece3 = getImage(getClass().getResource(imagePath + "piece3.gif"));

		mediatracker = new MediaTracker(this);
		
		mediatracker.addImage(piece0, 0);
		mediatracker.addImage(piece1, 1);
		mediatracker.addImage(piece2, 2);
		mediatracker.addImage(piece3, 3);

		try	{
			mediatracker.waitForAll();	
		}
		catch (InterruptedException e) {
			System.out.println("error loading image!");
		}

	  	this.addKeyListener(new MyKeyAdapter()); 
		
		this.addMouseListener(new MouseAdapter() {
			@SuppressWarnings("deprecation")
			public void mousePressed(MouseEvent e) {
			  if (state == 'p') {
		    	   state = 'g';
				   t = new Thread(Tetris.this);
		           t.start();   
			  }
		      
			  if (state == 'o') {
				  t.stop();
				  t = null;
				  state = 'g';
				  t = new Thread(Tetris.this);
		          t.start();
			  }
	        }
		});
	}

	private void setUp() {
 	    score = 0;
		moving = 'n';
		sync = true;
		sleepnum = 100;
		count3 = 0;
		
		for (count1 = 0; count1 < yCount; count1++) {
			for (count2 = 0; count2 < xCount; count2++)	{
				board[count1][count2] = 'n';
			}
		}
	}
	
	public void run() {   
		setUp();

		while(true)	{
	      if (state == 'g') {
				if (moving == 'n') {
			      	initpiece();
		           	repaint();
					moving = 'y';
				} else   {
					count3++;
                   
					if (count3 >= speed) {
        				if(valid != 0) {
							if(sync) {
							sync = false;
							board[piece[0].y][piece[0].x] = 'n';
					    	board[piece[1].y][piece[1].x] = 'n';
						    board[piece[2].y][piece[2].x] = 'n';
					    	board[piece[3].y][piece[3].x] = 'n';
												
							valid = checkCollision(piece[0].y + 1, piece[0].x, piece[1].y + 1,
									piece[1].x, piece[2].y + 1, piece[2].x,
									piece[3].y + 1, piece[3].x);
										
							if (valid == 1)
							{
								piece[0].y++;
								piece[1].y++;
								piece[2].y++;
								piece[3].y++;
							}
							board[piece[0].y][piece[0].x] = piece[0].id;
							board[piece[1].y][piece[1].x] = piece[1].id;
							board[piece[2].y][piece[2].x] = piece[2].id;
							board[piece[3].y][piece[3].x] = piece[3].id;
							
							repaint();
	                       
							if (valid == 0)	{
								calcScore();		
							    repaint();
							   	moving = 'n';
							}
						    sync = true;
						   }
						 }
					    count3 = 0;
				     }
			     }//end of moving='y'
		  try {
				 Thread.sleep(sleepnum);
			  } catch (InterruptedException e) { ; }
		  }//end of (state=='g')
		}//end of while
	}

	public void paint(Graphics g) {
		char id = 'n';
		offScrGraphics.setColor(Color.gray);
		offScrGraphics.fillRect(0, 0, width, height);

		if ((state == 'g')||(state == 'o'))	{
			offScrGraphics.setColor(Color.black);
    		offScrGraphics.fillRect(boardx, boardy, xCount*blocksize, yCount*blocksize);
		    
			for (count1 = 0; count1 < yCount; count1++)
				for (count2 = 0; count2 < xCount; count2++)	{
					id = board[count1][count2];
					if(id != 'n') {
						switch (board[count1][count2]) {
							case '0': 
								image = piece0;
								break;
							 case '1':
								image = piece1;
						  	    break;
							 case '2':
								image = piece2;
								break;
							 case '3': 
								image = piece3;
								break;
	   	       			}
						offScrGraphics.drawImage(image,boardx+count2*blocksize,boardy+count1*blocksize,this);
					}
		        }

			offScrGraphics.setColor(Color.yellow);
			offScrGraphics.drawString("Score: " + score, 130, 60);

			if (state == 'o')
				offScrGraphics.drawString("Game Over! Click to play again!", 130, 80);
		}
		if (state == 'p') {
			offScrGraphics.setColor(Color.blue);
			offScrGraphics.drawString("Click to start", width/3, height/3);
		}

		g.drawImage(offscreenImage, 0, 0, this);
	}
	
	public void update(Graphics g) {
		paint(g);
	}

	private void initpiece() {
		piecetype = (int)(Math.random()*5);
		pieceposition = (int)(Math.random()*xCount);
		
		if(pieceposition > xCount - 5)
			pieceposition = xCount - 5;
		else if(pieceposition < 3) pieceposition = 3;

		switch (piecetype) {
		  case 0:
		 	piece[0].init(0, pieceposition, '1');
			piece[1].init(1, pieceposition, '0');
			piece[2].init(2, pieceposition, '3');
			piece[3].init(3, pieceposition, '2');

			valid = checkCollision(piece[0].y, piece[0].x, piece[1].y,
					   piece[1].x, piece[2].y, piece[2].x,
					   piece[3].y, piece[3].x);
			break;
		  case 1:
			piece[0].init(0,pieceposition, '0');
			piece[1].init(0,pieceposition + 1, '1');
			piece[2].init(0,pieceposition + 2, '2');
			piece[3].init(1,pieceposition + 1, '3');
			
			valid = checkCollision(piece[0].y, piece[0].x, piece[1].y,
					   piece[1].x, piece[2].y, piece[2].x,
					   piece[3].y, piece[3].x);
			break;
		  case 2:
			piece[0].init(0, pieceposition, '1');
			piece[1].init(0, pieceposition + 1, '0');
			piece[2].init(1, pieceposition, '2');
			piece[3].init(1, pieceposition + 1, '3');
			
			valid = checkCollision(piece[0].y, piece[0].x, piece[1].y,
					   piece[1].x, piece[2].y, piece[2].x,
					   piece[3].y, piece[3].x);
			break;
		  case 3:
	      	piece[0].init(0, pieceposition, '3');
			piece[1].init(1, pieceposition, '0');
			piece[2].init(2, pieceposition, '1');
			piece[3].init(2, pieceposition + 1, '2');

			valid = checkCollision(piece[0].y, piece[0].x, piece[1].y,
					   piece[1].x, piece[2].y, piece[2].x,
					   piece[3].y, piece[3].x);
			break;
		  case 4:
			piece[0].init(0, pieceposition, '2');
			piece[1].init(1, pieceposition, '1');
			piece[2].init(2, pieceposition, '0');
			piece[3].init(2, pieceposition - 1, '3');
			
			valid = checkCollision(piece[0].y, piece[0].x, piece[1].y,
					   piece[1].x, piece[2].y, piece[2].x,
					   piece[3].y, piece[3].x);
			break;
		}
		board[piece[0].y][piece[0].x] = piece[0].id;
		board[piece[1].y][piece[1].x] = piece[1].id;
		board[piece[2].y][piece[2].x] = piece[2].id;
		board[piece[3].y][piece[3].x] = piece[3].id;
		
		if (valid == 0)
		   state = 'o';
	}

	private int checkCollision(int p0y,int p0x, int p1y, int p1x, int p2y, int p2x, int p3y, int p3x) {
		if ((p0y > yCount - 1) || (p1y > yCount - 1) || (p2y > yCount - 1) || (p3y > yCount - 1))
			return 0;

		if ((p0y < 0) || (p1y < 0) || (p2y < 0) || (p3y < 0))
			return 2;

		if ((p0x < 0) || (p1x < 0) || (p2x < 0) || (p3x < 0))
			return 2;

		if ((p0x > xCount - 1) || (p1x > xCount - 1) || (p2x > xCount - 1) || (p3x > xCount - 1))
			return 2;

		if ((board[p0y][p0x] == 'n') &&
		    (board[p1y][p1x] == 'n') &&
		    (board[p2y][p2x] == 'n') &&
		    (board[p3y][p3x] == 'n'))
			
			return 1;
		
		return 0;
	}

	private void calcScore() {
		char fullrow = 'n';
		int count = 0;
		
		for (int j = yCount-1; j >= 0; j--)	{
			if (fullrow == 'n')
				fullrow = 'y';
			else
				j++;

			for (int i = 0; i < xCount; i++)
				if (board[j][i] == 'n') {
					fullrow = 'n';
					if(count > 1)
					score += count*100;
					count = 0;
					
					break;
				 }

			if (fullrow == 'y')	{
				for (int k = j; k > 0; k--)
					for (int l = 0; l < xCount; l++)
						board[k][l] = board[k-1][l];

				for (int m = 0; m < xCount; m++)
					board[0][m] = 'n';
				
				score += 100;
				count++;
				sleepnum -= 2;
				
				if (sleepnum < 10)
					sleepnum = 10;
			}
		}
	}

	private class MyKeyAdapter extends KeyAdapter {
		private int key;

		public void keyPressed(KeyEvent e) {
			if((moving == 'y')&&(state!='o')) {
				key = e.getKeyCode();
		
				if (key == 40) {//DOWN ARROW
					if(sync) {
						sync = false;
			
						board[piece[0].y][piece[0].x] = 'n';
						board[piece[1].y][piece[1].x] = 'n';
						board[piece[2].y][piece[2].x] = 'n';
						board[piece[3].y][piece[3].x] = 'n';
												
						while ((valid = checkCollision(piece[0].y + 1, piece[0].x, piece[1].y + 1,
							piece[1].x, piece[2].y + 1, piece[2].x,
							piece[3].y + 1, piece[3].x)) == 1)
						{
							piece[0].y++;
							piece[1].y++;
							piece[2].y++;
							piece[3].y++;
						}
	
						board[piece[0].y][piece[0].x] = piece[0].id;
						board[piece[1].y][piece[1].x] = piece[1].id;
						board[piece[2].y][piece[2].x] = piece[2].id;
						board[piece[3].y][piece[3].x] = piece[3].id;
						
						calcScore();		
						repaint();
						moving = 'n';
						sync = true;
					}
				}
			
				if (key == 37) {//LEFT ARROW
					if((piece[0].x-1 >= 0)&&
							(piece[1].x-1 >= 0) &&
							(piece[2].x-1 >= 0) &&
							(piece[3].x-1 >= 0))
					{
						if(sync) {
							sync = false;
							board[piece[0].y][piece[0].x] = 'n';
							board[piece[1].y][piece[1].x] = 'n';
							board[piece[2].y][piece[2].x] = 'n';
							board[piece[3].y][piece[3].x] = 'n';

							valid = checkCollision(piece[0].y, piece[0].x - 1, piece[1].y,
									piece[1].x - 1, piece[2].y, piece[2].x - 1,
									piece[3].y, piece[3].x - 1);

							if (valid == 1)	{
								piece[0].x--;
								piece[1].x--;
								piece[2].x--;
								piece[3].x--;
								count3 = 0;
							}

							board[piece[0].y][piece[0].x] = piece[0].id;
							board[piece[1].y][piece[1].x] = piece[1].id;
							board[piece[2].y][piece[2].x] = piece[2].id;
							board[piece[3].y][piece[3].x] = piece[3].id;

							valid = 2;
							repaint();
							sync = true;
						}
					}
				}

				if (key == 39) {//RIGHT ARROW
					if((piece[0].x+1 < xCount)&&
							(piece[1].x+1 < xCount)&&
							(piece[2].x+1 < xCount)&&
							(piece[3].x+1 < xCount))
					{
						if(sync) {
							sync = false;
							board[piece[0].y][piece[0].x] = 'n';
							board[piece[1].y][piece[1].x] = 'n';
							board[piece[2].y][piece[2].x] = 'n';
							board[piece[3].y][piece[3].x] = 'n';


							valid = checkCollision(piece[0].y, piece[0].x + 1, piece[1].y,
									piece[1].x + 1, piece[2].y, piece[2].x + 1,
									piece[3].y, piece[3].x + 1);

							if (valid == 1)	{
								piece[0].x++;
								piece[1].x++;
								piece[2].x++;
								piece[3].x++;
								count3 = 0;
							}

							board[piece[0].y][piece[0].x] = piece[0].id;
							board[piece[1].y][piece[1].x] = piece[1].id;
							board[piece[2].y][piece[2].x] = piece[2].id;
							board[piece[3].y][piece[3].x] = piece[3].id;

							valid = 2;
							repaint();
							sync = true;
						}
					}
				} 
			}
		}

		public void keyReleased(KeyEvent e)	{
			if((moving == 'y') && (state != 'o')) {
				key = e.getKeyCode();
	   
				if (state == 'g') {
					if (key == 38) {//UP ARROW
						if(sync) {
							sync = false;
			   
							board[piece[0].y][piece[0].x] = 'n';
							board[piece[1].y][piece[1].x] = 'n';
							board[piece[2].y][piece[2].x] = 'n';
							board[piece[3].y][piece[3].x] = 'n';
		
							switch(piecetype) {	 
								case 0: // line
						   		{
						   			if (piece[0].y != piece[1].y) {// vertical line
						   				if(piece[0].y<piece[1].y) {// upright
						   					valid = checkCollision(piece[1].y,piece[1].x - 2, piece[1].y,
						   							piece[1].x - 1, piece[1].y, piece[1].x,
						   							piece[1].y, piece[1].x+1);
						   					if (valid == 1)	{
						   						piece[0].y = piece[1].y;
						   						piece[2].y = piece[1].y;
						   						piece[3].y = piece[1].y;
	
						   						piece[0].x = piece[1].x + 1;
						   						piece[2].x = piece[1].x - 1;
						   						piece[3].x = piece[1].x - 2;
						   					}
						   				} else {//upside down 
						   			 		valid = checkCollision(piece[1].y, piece[1].x - 1, piece[1].y,
						   							piece[1].x, piece[1].y, piece[1].x + 1,
						   							piece[1].y, piece[1].x + 2);
						   					if (valid == 1)	{
						   						piece[0].y = piece[1].y;
						   						piece[2].y = piece[1].y;
						   						piece[3].y = piece[1].y;
	
						   						piece[0].x = piece[1].x - 1;
						   						piece[2].x = piece[1].x + 1;
						   						piece[3].x = piece[1].x + 2;
						   					}
						   				}
						   			} else { //Horizontal line
						   				if(piece[0].x > piece[1].x) {// from right to left
						   		 			valid = checkCollision(piece[1].y - 2, piece[1].x, piece[1].y - 1,
						   							piece[1].x, piece[1].y, piece[1].x,
						   							piece[1].y + 1, piece[1].x);
						   					if (valid == 1)	{
						   						piece[0].y = piece[1].y + 1;
						   						piece[2].y = piece[1].y - 1;
						   						piece[3].y = piece[1].y - 2;
	
						   						piece[0].x = piece[1].x;
						   						piece[2].x = piece[1].x;
						   						piece[3].x = piece[1].x;
						   					}
						   				}
						   				else {// from left to right 
					 	   					valid = checkCollision(piece[1].y - 1, piece[1].x, piece[1].y,
						   							piece[1].x, piece[1].y + 1, piece[1].x,
						   							piece[1].y + 2, piece[1].x);
						   					if (valid == 1)	{
						   						piece[0].y = piece[1].y - 1;
						   						piece[2].y = piece[1].y + 1;
						   						piece[3].y = piece[1].y + 2;
	
						   						piece[0].x = piece[1].x;
						   						piece[2].x = piece[1].x;
						   						piece[3].x = piece[1].x;
						   					}
						   				}
						   			}
						   			break;
						   		}
	
						   		case 1: // 3 across and one in middle
						   		{
						   			if (piece[3].y > piece[1].y) {
						   				valid = checkCollision(piece[3].y, piece[3].x, piece[1].y,
						   						piece[1].x, piece[1].y - 1, piece[1].x,
						   						piece[2].y, piece[2].x);
	
						   				if (valid == 1)	{
						   					piece[0].y = piece[3].y;
						   					piece[0].x = piece[3].x;
						   					piece[3].y = piece[2].y;
						   					piece[3].x = piece[2].x;
						   					piece[2].y = piece[1].y - 1;
						   					piece[2].x = piece[1].x;
						   				}
						   			} else if (piece[3].y < piece[1].y) {
						   				valid = checkCollision(piece[3].y, piece[3].x, piece[1].y,
						   						piece[1].x, piece[1].y + 1, piece[1].x,
						   						piece[2].y, piece[2].x);
	
						   				if (valid == 1)	{
						   					piece[0].y = piece[3].y;
						   					piece[0].x = piece[3].x;
						   					piece[3].y = piece[2].y;
						   					piece[3].x = piece[2].x;
						   					piece[2].y = piece[1].y + 1;
						   					piece[2].x = piece[1].x;
						   				}
						   			} else if (piece[2].y > piece[1].y)	{
						   				valid = checkCollision(piece[3].y, piece[3].x, piece[1].y,
						   						piece[1].x, piece[1].y, piece[1].x + 1,
						   						piece[2].y, piece[2].x);
	
						   				if (valid == 1) {
						   					piece[0].y = piece[3].y;
						   					piece[0].x = piece[3].x;
						   					piece[3].y = piece[2].y;
						   					piece[3].x = piece[2].x;
						   					piece[2].y = piece[1].y;
						   					piece[2].x = piece[1].x + 1;
						   				}
						   			} else if (piece[2].y < piece[1].y)	{
						   				valid = checkCollision(piece[3].y, piece[3].x, piece[1].y,
						   						piece[1].x, piece[1].y, piece[1].x - 1,
						   						piece[2].y, piece[2].x);
	
						   				if (valid == 1)	{
						   					piece[0].y = piece[3].y;
						   					piece[0].x = piece[3].x;
						   					piece[3].y = piece[2].y;
						   					piece[3].x = piece[2].x;
						   					piece[2].y = piece[1].y;
						   					piece[2].x = piece[1].x - 1;
						   				}
						   			}
						   			break;
						   		}
	
						   		case 3: // 3 down 1 to right
						   		{
						   			if (piece[3].x > piece[2].x) {
						   				valid = checkCollision(piece[1].y, piece[1].x - 1, piece[1].y,
						   						piece[1].x, piece[1].y, piece[1].x + 1,
						   						piece[1].y - 1, piece[1].x + 1);
	
						   				if (valid == 1)	{
						   					piece[0].y = piece[1].y;
						   					piece[0].x = piece[1].x - 1;
						   					piece[2].y = piece[1].y;
						   					piece[2].x = piece[1].x + 1;
						   					piece[3].y = piece[2].y - 1;
						   					piece[3].x = piece[2].x;
						   				}
						   			} else if (piece[3].x < piece[2].x)	{
						   				valid = checkCollision(piece[1].y, piece[1].x + 1, piece[1].y,
						   						piece[1].x, piece[1].y, piece[1].x - 1,
						   						piece[1].y + 1, piece[1].x - 1);
	
						   				if (valid == 1)	{
						   					piece[0].y = piece[1].y;
						   					piece[0].x = piece[1].x + 1;
						   					piece[2].y = piece[1].y;
						   					piece[2].x = piece[1].x - 1;
						   					piece[3].y = piece[2].y + 1;
						   					piece[3].x = piece[2].x;
						   				}
						   			} else if (piece[3].y < piece[2].y)	{
					   					valid = checkCollision(piece[1].y+1, piece[1].x, piece[1].y,
					   						piece[1].x, piece[1].y - 1, piece[1].x,
					   						piece[1].y - 1, piece[1].x - 1);
	
						   				if (valid == 1)	{
						   					piece[0].y = piece[1].y + 1;
						   					piece[0].x = piece[1].x;
						   					piece[2].y = piece[1].y - 1;
						   					piece[2].x = piece[1].x;
						   					piece[3].y = piece[2].y;
						   					piece[3].x = piece[2].x - 1;
					   					}
					   				} else if (piece[3].y > piece[2].y)	{
					   					valid = checkCollision(piece[1].y - 1, piece[1].x, piece[1].y,
					   						piece[1].x,piece[1].y + 1, piece[1].x,
					   						piece[1].y + 1, piece[1].x + 1);
					   				
					   					if (valid == 1)	{
					   						piece[0].y = piece[1].y - 1;
					   						piece[0].x = piece[1].x;
					   						piece[2].y = piece[1].y + 1;
					   						piece[2].x = piece[1].x;
					   						piece[3].y = piece[2].y;
					   						piece[3].x = piece[2].x + 1;
					   					}
					   				}
					   				break;
					   			}//end of case
			
					   			case 4:// 3 down 1 to left
					   			{
					   				if (piece[3].x > piece[2].x) {
					   					valid = checkCollision(piece[1].y, piece[1].x + 1, piece[1].y,
					   						piece[1].x, piece[1].y, piece[1].x - 1,
					   						piece[1].y-1, piece[1].x - 1);
	
					   					if (valid == 1)	{
					   						piece[0].y = piece[1].y;
					   						piece[0].x = piece[1].x + 1;
					   						piece[2].y = piece[1].y;
					   						piece[2].x = piece[1].x - 1;
					   						piece[3].y = piece[2].y - 1;
					   						piece[3].x = piece[2].x;
					   					}
					   				} else if (piece[3].x < piece[2].x) {
					   					valid = checkCollision(piece[1].y, piece[1].x - 1, piece[1].y,
					   						piece[1].x, piece[1].y, piece[1].x + 1,
					   						piece[1].y+1,piece[1].x + 1);
	
					   					if (valid == 1)	{
					   						piece[0].y = piece[1].y;
					   						piece[0].x = piece[1].x - 1;
					   						piece[2].y = piece[1].y;
					   						piece[2].x = piece[1].x + 1;
					   						piece[3].y = piece[2].y + 1;
					   						piece[3].x = piece[2].x;
					   					}
					   				} else if (piece[3].y < piece[2].y)	{
					   					valid = checkCollision(piece[1].y - 1, piece[1].x, piece[1].y,
					   						piece[1].x, piece[1].y + 1, piece[1].x,
					   						piece[1].y + 1, piece[1].x - 1);
	
					   					if (valid == 1)	{
					   						piece[0].y = piece[1].y - 1;
					   						piece[0].x = piece[1].x;
					   						piece[2].y = piece[1].y + 1;
					   						piece[2].x = piece[1].x;
					   						piece[3].y = piece[2].y;
					   						piece[3].x = piece[2].x - 1;
					   					}
					   				} else if (piece[3].y > piece[2].y)	{
					   					valid = checkCollision(piece[1].y + 1, piece[1].x, piece[1].y,
					   						piece[1].x, piece[1].y - 1, piece[1].x,
					   						piece[1].y - 1, piece[1].x + 1);
	
					   					if (valid == 1)	{
					   						piece[0].y = piece[1].y + 1;
					   						piece[0].x = piece[1].x;
					   						piece[2].y = piece[1].y - 1;
					   						piece[2].x = piece[1].x;
					   						piece[3].y = piece[2].y;
					   						piece[3].x = piece[2].x + 1;
					   					}
					   				}//end of else if
					   				break;
					   			}//end of case
							}//end of switch
							valid = 2;
		  		   
							board[piece[0].y][piece[0].x] = piece[0].id;
							board[piece[1].y][piece[1].x] = piece[1].id;
							board[piece[2].y][piece[2].x] = piece[2].id;
							board[piece[3].y][piece[3].x] = piece[3].id;
		  		  
							repaint();
							sync = true;
						}//end of sync
			    	}//end of key==38
		    	}//end of state=='g'
	    	}
		}
	}

	private class Piece {
		private int x;
		private int y;
		private char id;

		private Piece() { }
	
		private void init(int y,int x,char id) {
			this.x = x;
			this.y = y;
			this.id = id;
		}
	}
}