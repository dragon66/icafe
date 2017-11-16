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

package com.icafe4j.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**	
 * A Vector-like growable array, but more simple.
 * It's not synchronized.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 May 30, 2007
 */
public class GrowableArray<E>{
	
	private transient E[] objarray; 
	private int increment;
	private int elements;
	private int initsize;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(GrowableArray.class);

	/** Construct an array with initial size inisize
	 *  and increment inc
	 */
 	@SuppressWarnings("unchecked")
	public GrowableArray(int inisize,int inc)
	{
		if (inisize <= 0) inisize = 10;
		if (inc <= 0) inc = 5;
		
		initsize = inisize;
		objarray = (E[])new Object[inisize];
		increment = inc;
		elements = 0;
	}

	public int getSize(){
		return elements;
	}

	
	@SuppressWarnings("unchecked")
	public E[] toArray(E[] a) {
        if (a.length < elements)
            a = (E[])java.lang.reflect.Array.newInstance(
                                a.getClass().getComponentType(), elements);

	    System.arraycopy(objarray, 0, a, 0, elements);

        if (a.length > elements)
            a[elements] = null;

        return a;
    }

    // Trim to size of elements

	@SuppressWarnings("unchecked")
	public void shrink() {
	  int oldCapacity = objarray.length;
	  if (elements < oldCapacity) {
	    E temp[] = objarray;
	    objarray = (E[])new Object[elements];
	    System.arraycopy(temp, 0, objarray, 0, elements);
	  }
    }
 
    /**	Displays the size of objarray.
 	 *	If objarray size > 0, displays each element.
	 */
	public void printArray(){
		LOGGER.info("Size is {}.", String.valueOf(elements));
		for (int i = 0;i < elements;i++){
			LOGGER.info("Position [{}] = {}", String.valueOf(i), String.valueOf(objarray[i]));
		}
	}

    /** Get the element at index j.
	 */
	public E getElement(int j){
		if ((0 <= j)&&(j < elements))
		{
			return objarray[j];
		}
		throw new ArrayIndexOutOfBoundsException(j);
	}

    /** Set the element at index j.
	 */
	public boolean setElement(int j,E o){
		if ((0 <= j)&&(j < elements))
		{
			objarray[j]=o;
			return true;
		}
		throw new ArrayIndexOutOfBoundsException(j);
	}
	/**	Creates a new array from objarray skipping element j.
	 */
	public E removeArrayElement(int j){
		if ((elements >= 1)&&(0 <= j)&&(j < elements))
		{
		   E oldValue=objarray[j];
		   System.arraycopy(objarray,j+1,objarray,j,elements-j-1);
		   elements--;
		   objarray[elements]=null;
		   return oldValue;
		}
		throw new ArrayIndexOutOfBoundsException(j);
	}

   /** Creates a new array from objarray skipping the first element
    *  with the same value as o.
	*/
	public E removeArrayElement(E o){
		int pointer=-1;
		for (int i = 0;i < elements;i++){
			if (o.equals(objarray[i])) 
			{
				pointer = i;
				break;
			}
		}
		// Remove the pointer's element
		if(pointer != -1){
			return removeArrayElement(pointer);
		}
		return null;
	}


	/**	Creates a new array from objarray adding an element o.
	 */
	public void addArrayElement(E o){
        checkCapacity(elements+1);
		objarray[elements++] = o;
	}


	/**	Creates a new array from objarray inserting a new element 
	 *  at index j with value o.
	 */
	public void insertArrayElement(int j,E o){
		if ((0 <= j)&&(j <= elements))
		{
			checkCapacity(elements+1);
		    System.arraycopy(objarray,j,objarray,j+1,elements-j);
		    objarray[j] = o;
		    elements++;
		}
		else throw new ArrayIndexOutOfBoundsException(j);
	}

	@SuppressWarnings("unchecked")
	private void checkCapacity(int capacityRequired) {
        int oldCapacity = objarray.length;
 	    if (capacityRequired > oldCapacity) {
	       E temp[] = objarray;
	       int newCapacity;
		   if (increment>0)
	       {
		    	newCapacity=oldCapacity+increment;
	       }
		   else newCapacity = oldCapacity * 2;
	       objarray = (E[])new Object[newCapacity];
	       System.arraycopy(temp, 0, objarray, 0, elements);
	    }
    }
	
	@SuppressWarnings("unchecked")
	public void clear() {
	   for (int i = 0; i < elements; i++)
		{
	     objarray[i] = null;
		}
		objarray = (E[])new Object[initsize];
	    elements = 0;
    }
}
