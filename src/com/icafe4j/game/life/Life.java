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

package com.icafe4j.game.life;

import java.awt.*;
import java.awt.event.*;
import java.applet.Applet;

import com.icafe4j.util.DoublyLinkedList;

/**
 * Conway's Life game.
 * <p>
 * This version uses the java 5.0 generic type container
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.1 04/18/2007
 */
public class Life extends Applet implements Runnable, ActionListener {

	private static final long serialVersionUID = 161821458639018390L;
	
	private myCanvas canvas;
	private Button start;
	
	private Panel controlPanel;
	private Panel canvasPanel;

	private Image grid;//memory image to draw on
	private Graphics offScrGraphics;//for double buffering

	private int cell[][],cellNeighbours[][];
	private int cellSize,cellCount,speed;
	private int xmin,xmax,ymin,ymax;
	private DoublyLinkedList<CellCoordinate> live,die,nextLive,nextDie;
	private Thread t;
	private boolean firstTime;

	public void init() {

       // get initial cell parameters from HTML file
	    try {
			cellSize = Integer.parseInt(getParameter("cellSize"));
			cellCount = Integer.parseInt(getParameter("cellCount"));
			speed = Integer.parseInt(getParameter("speed"));
		} catch(NumberFormatException e) {
			cellSize = 20;
			cellCount = 30;
			speed = 50;
		}
		if (cellCount == 0)
		    cellCount = 30;	
		if (cellSize == 0)
		    cellSize = 20;
		if (speed == 0)
			speed = 50;
		// construct a square cell grid 
		cell = new int[cellCount][cellCount];
		// record the neighbors of a certain cell
		cellNeighbours = new int[cellCount][cellCount];
		
		live = new DoublyLinkedList<CellCoordinate>();
		die = new DoublyLinkedList<CellCoordinate>();
		nextLive = new DoublyLinkedList<CellCoordinate>();
		nextDie = new DoublyLinkedList<CellCoordinate>();
		initCells();
		setComponents();
		firstTime = true;
		System.gc();
	}
	
	public void initCells() {
		
		grid = createImage(cellSize*cellCount, cellSize*cellCount);
		offScrGraphics = grid.getGraphics();

		offScrGraphics.setColor(Color.black);
		offScrGraphics.fillRect(0, 0, cellSize*cellCount, cellSize*cellCount);
		
		offScrGraphics.setColor(Color.green);
		for (int i = 0; i < cellCount; i++) {
			offScrGraphics.drawLine(i*cellSize, 0, i*cellSize, cellSize*cellCount);
			offScrGraphics.drawLine(0, i*cellSize, cellSize*cellCount, i*cellSize);
		}
	}
	
	public void setComponents() {
		setLayout(new BorderLayout());
		controlPanel = new Panel();
		start = new Button("START");
		start.addActionListener(this);
		controlPanel.add(start);
		add(BorderLayout.SOUTH, controlPanel);
		canvas = new myCanvas(500, 500, grid, this);
		canvasPanel = new Panel();
		canvasPanel.add(canvas);
		add(BorderLayout.CENTER,canvasPanel);
	}
	
	public synchronized void nextGeneration() {
		int x, y;
	
		offScrGraphics.setColor(Color.red);
	    
		CellCoordinate cellCoordinate;
		
		while (!nextLive.isEmpty())	{
			cellCoordinate = nextLive.removeFromHead();
			x = cellCoordinate.x;
          	y = cellCoordinate.y;
		
			if(cell[x][y] == 0 && cellNeighbours[x][y] == 3) {
				cell[x][y] = 1;
				offScrGraphics.fillOval(x*cellSize + 1, y*cellSize + 1, cellSize - 2, cellSize - 2);
				live.addToTail(new CellCoordinate(x, y));
			}
			cellCoordinate = null;
		}

		offScrGraphics.setColor(Color.black);
				
		while(!nextDie.isEmpty()) {
			cellCoordinate = nextDie.removeFromHead();
			x = cellCoordinate.x;
			y = cellCoordinate.y;
		
			if((cell[x][y] == 1) && (cellNeighbours[x][y] != 2) && (cellNeighbours[x][y] != 3))	{
				cell[x][y] = 0;
		        offScrGraphics.fillRect(x*cellSize + 1, y*cellSize + 1, cellSize - 2, cellSize - 2);
				die.addToTail(new CellCoordinate(x, y));
			}
			cellCoordinate = null;
		}
		canvas.repaint();
		notifyAll();
	}

	public void placeCell(int x, int y) {

   		if (t != null) return;

		x = x/cellSize;
		y = y/cellSize;

		if (cell[x][y] == 0) {
			cell[x][y] = 1;
			offScrGraphics.setColor(Color.red);
			offScrGraphics.fillOval(x*cellSize + 1, y*cellSize + 1, cellSize - 2,cellSize - 2);
		} else {
			cell[x][y] = 0;
			offScrGraphics.setColor(Color.black);
			offScrGraphics.fillRect(x*cellSize + 1, y*cellSize + 1, cellSize - 2, cellSize - 2);
		}

		canvas.repaint();
	}
   
	public void clearLists() {
		live.clear();
		die.clear();
		nextLive.clear();
		nextDie.clear();
	}

	public void initLists() {
		clearLists();
		
		int x, y;

		for (int i = 0;i < cellCount*cellCount; i++) {
			x = i%cellCount;
			y = i/cellCount;
			cellNeighbours[x][y] = 0;
			
			if(cell[x][y] == 1)	{
				live.addToTail(new CellCoordinate(x, y));
			}
		}
		addNeighbours();
    
		for (int i = 0; i < cellCount*cellCount; i++) {
			x = i%cellCount;
			y = i/cellCount;
			
			if((cellNeighbours[x][y] < 2) && (cell[x][y] == 1)) {
				nextDie.addToTail(new CellCoordinate(x, y));
			}
		}
	}

	public void addNeighbours() {
		int x, y;
		CellCoordinate cellCoordinate;
		
		while(!live.isEmpty()) {
			cellCoordinate = live.removeFromHead();
			x = cellCoordinate.x;
			y = cellCoordinate.y;
			checkBoundary(x, y);
						
			for (int j = xmin; j <= xmax; j++) {
				for (int k = ymin; k <= ymax; k++) {
					if (j != x || k != y) {
						cellNeighbours[j][k]++;
						switch(cellNeighbours[j][k]) {
							case 3:
								if(cell[j][k] == 0)
									nextLive.addToTail(new CellCoordinate(j,k));
								break;
							case 4:
								if(cell[j][k] == 1)
									nextDie.addToTail(new CellCoordinate(j,k));
								break;
							default:
								break;
						}
					}
				}
			}
			cellCoordinate = null;
		}
	}

	public void subNeighbours() {
		int x, y;
		CellCoordinate cellCoordinate;
		
		while(!die.isEmpty()) {
			cellCoordinate = die.removeFromHead();
			x = cellCoordinate.x;
			y = cellCoordinate.y;
			checkBoundary(x,y);
			
			for (int j = xmin; j <= xmax; j++) {
				for (int k = ymin;k <= ymax; k++) {
					if (j != x || k != y) {
						cellNeighbours[j][k]--;
						switch(cellNeighbours[j][k]) {
							case 1:
								if(cell[j][k]==1)
									nextDie.addToTail(new CellCoordinate(j,k));
								break;
							case 3:
								if(cell[j][k]==0)
								nextLive.addToTail(new CellCoordinate(j,k));
								break;
								default:
									break;
						}
					}
				}
			}
			cellCoordinate = null;
		}
	}

	public void checkBoundary(int x, int y) {
		if(x == 0)
			xmin = 0;
		else
			xmin = x - 1;
		if(x == cellCount - 1)
			xmax = cellCount - 1;
		else
			xmax = x + 1;
		if(y == 0)
			ymin = 0;
		else
			ymin = y - 1;
		if(y == cellCount - 1)
			ymax = cellCount - 1;
		else
			ymax = y + 1;
	}

	public void run() {
		initLists();
		while (true) {
			nextGeneration();
			addNeighbours();
			subNeighbours();
			try {
				Thread.sleep(speed);
			} catch(InterruptedException e) { }
		}
	}

	public void start() {
		if (firstTime) return;		
		if (t == null) {
			t = new Thread(this);
			t.start();
		}		
	}
	
	@SuppressWarnings("deprecation")
	public synchronized void stop() {
		if (t != null) {
	        try{
				wait();
			}
	        catch (InterruptedException ex) { }
			t.stop();
			t = null;
		}
	//	nextGeneration();
	}
	
	public void actionPerformed(ActionEvent e) {
		firstTime = false;
		
		if (e.getSource().equals(start)) {
			if (t != null) {
				stop();
				start.setLabel("START");
			} else{
				start();
				start.setLabel("STOP");
			}
		}
	}
	
	private class CellCoordinate {
		private int x, y;
	
		private CellCoordinate(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}
}

class myCanvas extends Canvas {
	private static final long serialVersionUID = -7398699000638444634L;
	
	Life life;
	Image img;
	
	public myCanvas(int w, int h, Image img, Life life) {
		setSize(w, h);
		this.life = life;
		this.img = img;
		addMouseListener(new myMouseAdapter());
	}
	
	private class myMouseAdapter extends MouseAdapter {
		
		public void mouseClicked(MouseEvent e) {
			life.placeCell(e.getX(), e.getY());
		}		
	}
	
	public void paint(Graphics g) {
		g.drawImage(img, 0, 0, this);
	}
	
	public void update(Graphics g) {
		paint(g);
	}
}