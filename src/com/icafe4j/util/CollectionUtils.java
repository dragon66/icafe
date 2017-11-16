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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;
/**
 * A collection utility class
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/12/2012
 */
public class CollectionUtils {
	
	public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
	     for (Map.Entry<T, E> entry : map.entrySet()) {
	         if (value.equals(entry.getValue())) {
	            return entry.getKey();
	         }
	     }
	     return null;
    }
	
	public static <T, E> Set<T> getKeysByValue(Map<T, E> map, E value) {
	     Set<T> keys = new HashSet<T>();
	     for (Map.Entry<T, E> entry : map.entrySet()) {
	         if (value.equals(entry.getValue())) {
	             keys.add(entry.getKey());
	         }
	     }
	     return keys;
	}

	public static int[] integerListToIntArray(List<Integer> integers)
	{
	    int[] ret = new int[integers.size()];
	    Iterator<Integer> iterator = integers.iterator();
	    
	    for (int i = 0; i < ret.length; i++)
	    {
	        ret[i] = iterator.next().intValue();
	    }
	    
	    return ret;
	}
	
	public static <T> LinkedList<T> reverseLinkedList(LinkedList<T> list){

        if(list == null)
            return null;

        int size = list.size();
        
        for(int i = 0; i < size; i++){
            list.add(i, list.removeLast());
        }

        return list;
    }
	
	private CollectionUtils(){} // Prevents instantiation
}
