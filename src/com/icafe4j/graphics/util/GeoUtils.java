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

package com.icafe4j.graphics.util;

/** 
 * This utility class contains static methods 
 * to work with geometry. 
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/15/2013
 */
public class GeoUtils {

	private GeoUtils() {} // Prevents instantiation
	
	// Algorithm to find the area of a non self-crossing polygons
	// From http://www.mathopenref.com/coordpolygonarea2.html
	public static float area(float[] xp, float[] yp) {
		float area = 0;
		int numPoints = xp.length;
		int j = numPoints - 1;

		for (int i = 0; i < numPoints; i++) {
			area = area +  (xp[j] + xp[i]) * (yp[j] - yp[i]); 
			j = i;
	    }
			
		return Math.abs(area)/2;
	}

	public static boolean isInsidePoly(int x, int y, int[] xp, int[] yp)
	{		
	     int i, j;
	     boolean c = false;
	     int npol = xp.length;
	     
	     for (i = 0, j = npol-1; i < npol; j = i++) {
	    	 if(((yp[i] > y) != (yp[j] > y)) &&	          
	                (x < (xp[j] - xp[i]) * (y - yp[i]) / (yp[j] - yp[i]) + xp[i]))
	              c = !c;
	     }
	     
	     return c;
	}
	
	/**
	 * W. Randolph Franklin - Point Inclusion in Polygon Test
	 * 
	 * @param x x coordinate of the test point. 
	 * @param y y-coordinate of the test point. 
	 * @param xp array containing the x coordinates of the polygon's vertices. 
	 * @param yp array containing the y-coordinates of the polygon's vertices.
	 * 
	 * @return true if (x, y) inside the polygon, otherwise false.
	 * 
	 * @see <a href = "http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html">pnpoly</a>
	 */
	public static boolean isInsidePoly(float x, float y, float[] xp, float[] yp)
	{
		int i, j;
		boolean c = false;
		int npol = xp.length;
		
		 for (i = 0, j = npol-1; i < npol; j = i++) {
	    	 if(((yp[i] > y) != (yp[j] > y)) &&	          
	                (x < (xp[j] - xp[i]) * (y - yp[i]) / (yp[j] - yp[i]) + xp[i]))
	              c = !c;
	     }
     
		return c;
	}

	public static boolean isInsidePoly(double x, double y, double[] xp, double[] yp)
	{
		int i, j;
		boolean c = false;
		int npol = xp.length;
		
		 for (i = 0, j = npol-1; i < npol; j = i++) {
	    	 if(((yp[i] > y) != (yp[j] > y)) &&	          
	                (x < (xp[j] - xp[i]) * (y - yp[i]) / (yp[j] - yp[i]) + xp[i]))
	              c = !c;
	     }
     
		return c;
	}   
}