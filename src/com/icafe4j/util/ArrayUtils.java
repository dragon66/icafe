/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * ArrayUtils.java
 *
 * Who   Date       Description
 * ====  =========  ======================================================================
 * WY    30Sep2015  Added mergesort()
 * WY    08Sep2015  Changed Shell sort to use Knuth's sequence
 * WY    14Jun2015  Bug fix for toNBits() to use long data type internally 
 * WY    04Jun2015  Rewrote all concatenation related methods
 * WY    02Jun2015  Bug fix for generic concatenation methods
 * WY    06Apr2015  Added reverse(byte[]) to reverse byte array elements
 * WY    06Jan2015  Added reverse() to reverse array elements
 * WY    10Dec2014  Moved reverseBits() from IMGUtils to here along with BIT_REVERSE_TABLE
 * WY    08Dec2014  Fixed bug for flipEndian() with more than 32 bit sample data 
 * WY    07Dec2014  Changed method names for byte array to other array types conversion
 * WY    07Dec2014  Added new methods to work with floating point TIFF images
 * WY    03Dec2014  Added byteArrayToFloatArray() and byteArrayToDoubleArray()
 * WY    25Nov2014  Added removeDuplicates() to sort and remove duplicates from int arrays
 * WY    12Nov2014  Changed the argument sequence for flipEndian()
 * WY    11Nov2014  Changed flipEndian() to include scan line stride to skip bits
 * WY    11Nov2014  Added toNBits() to convert byte array to nBits data unit
 * WY    28Oct2014  Added flipEndian() to work with TIFTweaker mergeTiffImagesEx()
 */

package com.icafe4j.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.lang.reflect.Array;
import java.nio.ByteOrder;
import java.util.AbstractList;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * Array utility class 
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 09/18/2012
 */
public class ArrayUtils 
{
	// Bit mask 0 - 32 bits (inclusive)
	private static final int[] MASK = { 0x000,
					    0x1, 0x3, 0x7, 0xf,
					    0x1f, 0x3f, 0x7f, 0xff,
					    0x1ff, 0x3ff, 0x7ff, 0xfff,
				        0x1fff, 0x3fff, 0x7fff, 0xffff,
				        0x1ffff, 0x3ffff, 0x7ffff, 0xfffff,
				        0x1fffff, 0x3fffff, 0x7fffff, 0xffffff,
				        0x1ffffff, 0x3ffffff, 0x7ffffff, 0xfffffff,
				        0x1fffffff, 0x3fffffff, 0x7fffffff, 0xffffffff // 32 bits				    
					};
	
	// Bit reverse table to work with TIFF FillOrder field.
	private static final byte[] BIT_REVERSE_TABLE =	{
	   (byte)0x00, (byte)0x80, (byte)0x40, (byte)0xc0, (byte)0x20, (byte)0xa0, (byte)0x60, (byte)0xe0,
	   (byte)0x10, (byte)0x90, (byte)0x50, (byte)0xd0, (byte)0x30, (byte)0xb0, (byte)0x70, (byte)0xf0,
	   (byte)0x08, (byte)0x88, (byte)0x48, (byte)0xc8, (byte)0x28, (byte)0xa8, (byte)0x68, (byte)0xe8,
	   (byte)0x18, (byte)0x98, (byte)0x58, (byte)0xd8, (byte)0x38, (byte)0xb8, (byte)0x78, (byte)0xf8,
	   (byte)0x04, (byte)0x84, (byte)0x44, (byte)0xc4, (byte)0x24, (byte)0xa4, (byte)0x64, (byte)0xe4,
	   (byte)0x14, (byte)0x94, (byte)0x54, (byte)0xd4, (byte)0x34, (byte)0xb4, (byte)0x74, (byte)0xf4,
	   (byte)0x0c, (byte)0x8c, (byte)0x4c, (byte)0xcc, (byte)0x2c, (byte)0xac, (byte)0x6c, (byte)0xec,
	   (byte)0x1c, (byte)0x9c, (byte)0x5c, (byte)0xdc, (byte)0x3c, (byte)0xbc, (byte)0x7c, (byte)0xfc,
	   (byte)0x02, (byte)0x82, (byte)0x42, (byte)0xc2, (byte)0x22, (byte)0xa2, (byte)0x62, (byte)0xe2,
	   (byte)0x12, (byte)0x92, (byte)0x52, (byte)0xd2, (byte)0x32, (byte)0xb2, (byte)0x72, (byte)0xf2,
	   (byte)0x0a, (byte)0x8a, (byte)0x4a, (byte)0xca, (byte)0x2a, (byte)0xaa, (byte)0x6a, (byte)0xea,
	   (byte)0x1a, (byte)0x9a, (byte)0x5a, (byte)0xda, (byte)0x3a, (byte)0xba, (byte)0x7a, (byte)0xfa,
	   (byte)0x06, (byte)0x86, (byte)0x46, (byte)0xc6, (byte)0x26, (byte)0xa6, (byte)0x66, (byte)0xe6,
	   (byte)0x16, (byte)0x96, (byte)0x56, (byte)0xd6, (byte)0x36, (byte)0xb6, (byte)0x76, (byte)0xf6,
	   (byte)0x0e, (byte)0x8e, (byte)0x4e, (byte)0xce, (byte)0x2e, (byte)0xae, (byte)0x6e, (byte)0xee,
	   (byte)0x1e, (byte)0x9e, (byte)0x5e, (byte)0xde, (byte)0x3e, (byte)0xbe, (byte)0x7e, (byte)0xfe,
	   (byte)0x01, (byte)0x81, (byte)0x41, (byte)0xc1, (byte)0x21, (byte)0xa1, (byte)0x61, (byte)0xe1,
	   (byte)0x11, (byte)0x91, (byte)0x51, (byte)0xd1, (byte)0x31, (byte)0xb1, (byte)0x71, (byte)0xf1,
	   (byte)0x09, (byte)0x89, (byte)0x49, (byte)0xc9, (byte)0x29, (byte)0xa9, (byte)0x69, (byte)0xe9,
	   (byte)0x19, (byte)0x99, (byte)0x59, (byte)0xd9, (byte)0x39, (byte)0xb9, (byte)0x79, (byte)0xf9,
	   (byte)0x05, (byte)0x85, (byte)0x45, (byte)0xc5, (byte)0x25, (byte)0xa5, (byte)0x65, (byte)0xe5,
	   (byte)0x15, (byte)0x95, (byte)0x55, (byte)0xd5, (byte)0x35, (byte)0xb5, (byte)0x75, (byte)0xf5,
	   (byte)0x0d, (byte)0x8d, (byte)0x4d, (byte)0xcd, (byte)0x2d, (byte)0xad, (byte)0x6d, (byte)0xed,
	   (byte)0x1d, (byte)0x9d, (byte)0x5d, (byte)0xdd, (byte)0x3d, (byte)0xbd, (byte)0x7d, (byte)0xfd,
	   (byte)0x03, (byte)0x83, (byte)0x43, (byte)0xc3, (byte)0x23, (byte)0xa3, (byte)0x63, (byte)0xe3,
	   (byte)0x13, (byte)0x93, (byte)0x53, (byte)0xd3, (byte)0x33, (byte)0xb3, (byte)0x73, (byte)0xf3,
	   (byte)0x0b, (byte)0x8b, (byte)0x4b, (byte)0xcb, (byte)0x2b, (byte)0xab, (byte)0x6b, (byte)0xeb,
	   (byte)0x1b, (byte)0x9b, (byte)0x5b, (byte)0xdb, (byte)0x3b, (byte)0xbb, (byte)0x7b, (byte)0xfb,
	   (byte)0x07, (byte)0x87, (byte)0x47, (byte)0xc7, (byte)0x27, (byte)0xa7, (byte)0x67, (byte)0xe7,
	   (byte)0x17, (byte)0x97, (byte)0x57, (byte)0xd7, (byte)0x37, (byte)0xb7, (byte)0x77, (byte)0xf7,
	   (byte)0x0f, (byte)0x8f, (byte)0x4f, (byte)0xcf, (byte)0x2f, (byte)0xaf, (byte)0x6f, (byte)0xef,
	   (byte)0x1f, (byte)0x9f, (byte)0x5f, (byte)0xdf, (byte)0x3f, (byte)0xbf, (byte)0x7f, (byte)0xff
	};
	
	// From Effective Java 2nd Edition. 
   	public static List<Integer> asList(final int[] a) 
   	{
   		if (a == null)
   			throw new NullPointerException();
   		return new AbstractList<Integer>() {// Concrete implementation built atop skeletal implementation
   			public Integer get(int i) {
   				return a[i]; 
   			}
   			
   			@Override public Integer set(int i, Integer val) {
   				int oldVal = a[i];
   				a[i] = val; 
   				return oldVal;
   			}
   			public int size() {
   				return a.length;
   			}
   		};
   	}
	
	public static void bubbleSort(int[] array) {
	    int n = array.length;
	    boolean doMore = true;
	    
	    while (doMore) {
	        n--;
	        doMore = false;  // assume this is our last pass over the array
	    
	        for (int i=0; i < n; i++) {
	            if (array[i] > array[i+1]) {
	                // exchange elements
	                int temp = array[i];
	                array[i] = array[i+1];
	                array[i+1] = temp;
	                doMore = true;  // after an exchange, must look again 
	            }
	        }
	    }
	}
	
	public static <T extends Comparable<? super T>> void bubbleSort(T[] array) {
	    int n = array.length;
	    boolean doMore = true;
	    
	    while (doMore) {
	        n--;
	        doMore = false;  // assume this is our last pass over the array
	    
	        for (int i = 0; i < n; i++) {
	            if (array[i].compareTo(array[i+1]) > 0) {
	                // exchange elements
	                T temp = array[i];
	                array[i] = array[i+1];
	                array[i+1] = temp;
	                doMore = true;  // after an exchange, must look again 
	            }
	        }
	    }
	}
	
	/**
     * Since Set doesn't allow duplicates add() return false
     * if we try to add duplicates into Set and this property
     * can be used to check if array contains duplicates.
     * 
     * @param input input array
     * @return true if input array contains duplicates, otherwise false.
     */
    public static <T> boolean checkDuplicate(T[] input) {
        Set<T> tempSet = new HashSet<T>();
        
        for (T str : input) {
            if (!tempSet.add(str)) {
                return true;
            }
        }
        
        return false;
    }
	
	public static byte[] concat(byte[] first, byte[]... rest) {
  	 	if(first == null) {
			throw new IllegalArgumentException("Firt element is null");
		}
  	 	if(rest.length == 0) return first;
		// Now the real stuff
  	  	int totalLength = first.length;
	  
		for (byte[] array : rest) {		
			totalLength += array.length;
	 	}
		
		byte[] result = new byte[totalLength];
	  
		int offset = first.length;
		
		System.arraycopy(first, 0, result, 0, offset);
	
		for (byte[] array : rest) {
			System.arraycopy(array, 0, result, offset, array.length);
			offset += array.length;
		}
		
		return result;
	}
	
	/**
	 * Type safe concatenation of arrays with upper type bound T
	 * <p>
	 * Note: if type parameter is not explicitly supplied, it will be inferred as the 
	 * upper bound for the two parameters.
	 * 
	 * @param arrays the arrays to be concatenated
	 * @return a concatenation of the input arrays
	 * @throws NullPointerException if any of the input array is null
	 */
	public static <T> T[] concat(T[]... arrays) {
		if(arrays.length == 0)
			throw new IllegalArgumentException("Varargs length is zero");
		
		if(arrays.length == 1) return arrays[0];
		
		// Now the real stuff
		int totalLength = 0;
		// Taking advantage of the compiler type inference
		Class<?> returnType = arrays.getClass().getComponentType().getComponentType();
		
		for (T[] array : arrays)	
			totalLength += array.length;
		
		@SuppressWarnings("unchecked")
		T[] result = (T[]) Array.newInstance(returnType, totalLength);
	 
		int offset = 0;
		for (T[] array : arrays) {
			System.arraycopy(array, 0, result, offset, array.length);
			offset += array.length;
		}
		
		return result;
	}
	
	/** 
	 * Type safe concatenation of arrays with upper type bound T
	 *  
     * @param type type bound for the concatenated array
     * @param arrays arrays to be concatenated
     * @return a concatenation of the arrays.
     * @throws NullPointerException if any of the arrays to be
     *         concatenated is null.
   	 */
	public static <T> T[] concat(Class<T> type, T[]... arrays) {
		if(type == null) 
			throw new IllegalArgumentException("Input type class is null");
		
		if(arrays.length == 0) { // Return a zero length array instead of null
			@SuppressWarnings("unchecked")
			T[] result = (T[]) Array.newInstance(type, 0);
			
			return result;
		}
		
		// Make sure we have at least two arrays to concatenate
		if(arrays.length == 1) return arrays[0];
		
		int totalLength = 0;	  
		for (T[] array : arrays)	
			totalLength += array.length;
		
		@SuppressWarnings("unchecked")
		T[] result = (T[]) Array.newInstance(type, totalLength);
	  
		int offset = 0;
		for (T[] array : arrays) {
			System.arraycopy(array, 0, result, offset, array.length);
			offset += array.length;
		}
		
		return result;
	}

	public static int findEqualOrLess(int[] a, int key) {
    	return findEqualOrLess(a, 0, a.length, key);
    }
	
	/**
     * Find the index of the element which is equal or less than the key.
     * The array must be sorted in ascending order.
     *
     * @param a the array to be searched
     * @param key the value to be searched for
     * @return index of the search key or index of the element which is closest to but less than the search key or
     * -1 is the search key is less than the first element of the array.
     */
    
    public static int findEqualOrLess(int[] a, int fromIndex, int toIndex, int key) {
    	int index = Arrays.binarySearch(a, fromIndex, toIndex, key);
    	
    	// -index - 1 is the insertion point if not found, so -index - 1 -1 is the less position 
    	if(index < 0) {
    		index = -index - 1 - 1;
    	}
    	
    	// The index of the element which is either equal or less than the key
    	return index;    	
    }
	
	public static <T> int findEqualOrLess(T[] a, int fromIndex, int toIndex, T key, Comparator<? super T> c) {
    	int index = Arrays.binarySearch(a, fromIndex, toIndex, key, c);
    	
    	// -index - 1 is the insertion point if not found
    	if(index < 0) {
    		index = -index - 1 - 1;
    	}
    	
    	return index;    	
    }
	
	public static <T> int findEqualOrLess(T[] a, T key, Comparator<? super T> c) {
    	return findEqualOrLess(a, 0, a.length, key, c);
    }
	
	/**
     * Flip the endian of the input byte-compacted array
     * 
     * @param input the input byte array which is byte compacted
     * @param offset the offset to start the reading input
     * @param len number of bytes to read
     * @param bits number of bits for the data before compaction
     * @param scanLineStride scan line stride to skip bits
     * @param bigEndian whether or not the input data is in big endian order
     *
     * @return a byte array with the endian flipped
     */     
   	public static byte[] flipEndian(byte[] input, int offset, int len, int bits, int scanLineStride, boolean bigEndian) {
   		long value = 0;
   		int bits_remain = 0;
   		long temp_byte = 0; // Must make this long, otherwise will give wrong result for bits > 32
   		int empty_bits = 8;
   		
   		byte[] output = new byte[input.length];
   		
   		int strideCounter = 0;
   		
   		int end = offset + len;
   		int bufIndex = 0;
    	 
   		int temp = bits;
    	boolean bigEndianOut = !bigEndian;
    	
   	  	loop:
   	  	while(true) {  			
   	   		
			if(!bigEndian)
				value = (temp_byte >> (8-bits_remain));
			else				
				value = (temp_byte & MASK[bits_remain]); 
				
			while (bits > bits_remain)
			{
				if(offset >= end) {
					break loop;
				}
				
				temp_byte = input[offset++]&0xff;
				
				if(bigEndian)
					value = ((value<<8)|temp_byte);
				else
					value |= (temp_byte<<bits_remain);
				
				bits_remain += 8;
			}
			
			bits_remain -= bits;
			
			if(bigEndian)
				value = (value>>bits_remain);		
	        
		  	// Write bits bit length value in opposite endian	    	
	    	if(bigEndianOut) {
	    		temp = bits-empty_bits;
	    		output[bufIndex] |= ((value>>temp)&MASK[empty_bits]);
	    		
	    		while(temp > 8)
				{
					output[++bufIndex] |= ((value>>(temp-8))&MASK[8]);
					temp -= 8;
				} 
	    		
	    		if(temp > 0) {
	    			output[++bufIndex] |= ((value&MASK[temp])<<(8-temp));
	    			temp -= 8;
	    		}
	       	} else { // Little endian
	       		temp = bits;
				output[bufIndex] |= ((value&MASK[empty_bits])<<(8-empty_bits));
				value >>= empty_bits;
		        temp -= empty_bits;
		        // If the code is longer than the empty_bits
				while(temp > 8) {
					output[++bufIndex] |= (value&0xff);
					value >>= 8;
					temp -= 8;
				}
				
		        if(temp > 0)
				{
		        	output[++bufIndex] |= (value&MASK[temp]);
	    			temp -= 8;
				}
			}
	    	
	    	empty_bits = -temp;
			
	    	if(++strideCounter%scanLineStride == 0) {
				empty_bits = 0;
				bits_remain = 0;	
			}			
   	  	}
   		
		return output;
	}
	
	// From http://stackoverflow.com/questions/6162651/half-precision-floating-point-in-java
   	// returns all higher 16 bits as 0 for all results
   	public static int fromFloat(float fval)
   	{
   	    int fbits = Float.floatToIntBits(fval);
   	    int sign = fbits >>> 16 & 0x8000; // sign only
   	    int val = (fbits & 0x7fffffff) + 0x1000; // rounded value

   	    if(val >= 0x47800000) // might be or become NaN/Inf
   	    {                     // avoid Inf due to rounding
   	        if( (fbits & 0x7fffffff) >= 0x47800000)
   	        {                        // is or must become NaN/Inf
   	            if(val < 0x7f800000) // was value but too large
   	                return sign | 0x7c00;  // make it +/-Inf
   	            return sign | 0x7c00 |  // remains +/-Inf or NaN
   	                (fbits & 0x007fffff) >>> 13; // keep NaN (and Inf) bits
   	        }
   	        return sign | 0x7bff;  // unrounded not quite Inf
   	    }
   	    if(val >= 0x38800000)  // remains normalized value
   	        return sign | val - 0x38000000 >>> 13; // exp - 127 + 15
   	    if(val < 0x33000000) // too small for subnormal
   	        return sign;     // becomes +/-0
   	    val = (fbits & 0x7fffffff) >>> 23;  // tmp exp for subnormal calc
   	    return sign | ((fbits & 0x7fffff | 0x800000) // add subnormal bit
   	         + (0x800000 >>> val - 102) // round depending on cut off
   	      >>> 126 - val); // div by 2^(1-(exp-127+15)) and >> 13 | exp=0
   	}
	
	/**
  	 * Since nonzero-length array is always mutable, we should return
  	 * a clone of the underlying array as BIT_REVERSE_TABLE.clone().
  	 *
  	 * @return the byte reverse table.
  	 */
  	public static byte[] getBitReverseTable() {
  		return BIT_REVERSE_TABLE.clone();
  	}
	 
	// Insertion sort
    public static void insertionsort(int[] array) {
	   insertionsort(array, 0, array.length - 1);
    }

	public static void insertionsort(int[] array, int start, int end) {
	   int j;

	   for (int i = start + 1; i < end + 1; i++)
	   {
		   
		   int temp = array[i];
		   for ( j = i; j > start && temp <= array[j-1]; j-- )
		       array[j] = array[j-1];
		   // Move temp to the right place
		   array[j] = temp;
	   }
    }
	
	// Insertion sort
    public static <T extends Comparable<? super T>> void insertionsort(T[] array) {
    	insertionsort(array, 0, array.length - 1);
    }
    
	// Insertion sort
    public static <T extends Comparable<? super T>> void insertionsort(T[] array, int start, int end) {
	   int j;

	   for (int i = start + 1; i < end + 1; i++)
	   {
		   T temp = array[i];
		   for ( j = i; j > start && temp.compareTo(array[j-1]) <= 0; j-- )
		       array[j] = array[j-1];
		   // Move temp to the right place
		   array[j] = temp;
	   }
    }
    
    // Merge sort
    public static void mergesort(int[] array) { 
	   mergesort(array, new int[array.length], 0, array.length - 1);
    }
    
    public static void mergesort(int[] array, int left, int right) {
    	if(left < 0 || right > array.length - 1) throw new IllegalArgumentException("Array index out of bounds");
        mergesort(array, new int[array.length], left, right);
    }
    
    private static void mergesort(int[] array, int[] temp, int left, int right) {
    	// check the base case
        if (left < right) {
          // Get the index of the element which is in the middle
          int middle = left + (right - left) / 2;
          // Sort the left side of the array
          mergesort(array, temp, left, middle);
          // Sort the right side of the array
          mergesort(array, temp, middle + 1, right);
          // Merge the left and the right
          merge(array, temp, left, middle, right);
        }
    }
    
    public static <T extends Comparable<? super T>> void mergesort(T[] array) {
    	mergesort(array, 0, array.length - 1);
    }
    
    public static <T extends Comparable<? super T>> void mergesort(T[] array, int left, int right) {
     	if(left < 0 || right > array.length - 1) throw new IllegalArgumentException("Array index out of bounds");
        @SuppressWarnings("unchecked")
		T[] temp = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length);
    	mergesort(array, temp, left, right);
    }
    
    // Merge sort
    private static <T extends Comparable<? super T>> void mergesort(T[] array, T[] temp, int left, int right) {
    	// check the base case
        if (left < right) {
          // Get the index of the element which is in the middle
          int middle = left + (right - left) / 2;
          // Sort the left side of the array
          mergesort(array, temp, left, middle);
          // Sort the right side of the array
          mergesort(array, temp, middle + 1, right);
          // Merge the left and the right
          merge(array, temp, left, middle, right);
        }
    }   
    
    private static <T extends Comparable<? super T>> void merge(T[] array, T[] temp, int left, int middle, int right) {
    	// Copy both parts into the temporary array
        for (int i = left; i <= right; i++) {
          temp[i] = array[i];
        }
        int i = left;
        int j = middle + 1;
        int k = left;
        while (i <= middle && j <= right) {
            if (temp[i].compareTo(temp[j]) <= 0) {
                array[k] = temp[i];
                i++;
            } else {
                array[k] = temp[j];
                j++;
            }
            k++;
        }
        while (i <= middle) {
            array[k] = temp[i];
            k++;
            i++;
        }        
    }
    
    private static void merge(int[] array, int[] temp, int left, int middle, int right) {
    	// Copy both parts into the temporary array
        for (int i = left; i <= right; i++) {
          temp[i] = array[i];
        }
        int i = left;
        int j = middle + 1;
        int k = left;
        while (i <= middle && j <= right) {
            if (temp[i] <= temp[j]) {
                array[k] = temp[i];
                i++;
            } else {
                array[k] = temp[j];
                j++;
            }
            k++;
        }
        while (i <= middle) {
            array[k] = temp[i];
            k++;
            i++;
        }        
    }
    
	/**
	 * Packs all or part of the input byte array which uses "bits" bits to use all 8 bits.
	 * 
	 * @param input input byte array
	 * @param start offset of the input array to start packing  
	 * @param bits number of bits used by the input array
	 * @param len number of bytes from the input to be packed
	 * @return the packed byte array
	 */
	public static byte[] packByteArray(byte[] input, int start, int bits, int len) {
		//
		if(bits == 8) return ArrayUtils.subArray(input, start, len);
		if(bits > 8 || bits <= 0) throw new IllegalArgumentException("Invalid value of bits: " + bits);
		
		byte[] packedBytes = new byte[(bits*len + 7)>>3];
		short mask[] = {0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff};
	    	
		int index = 0;
		int empty_bits = 8;
		int end = start + len;
		
		for(int i = start; i < end; i++) {
			// If we have enough space for input byte, one step operation
			if(empty_bits >= bits) {
				packedBytes[index] |= ((input[i]&mask[bits])<<(empty_bits-bits));
				empty_bits -= bits;				
				if(empty_bits == 0) {
					index++;
					empty_bits = 8;
				}
			} else { // Otherwise two step operation
				packedBytes[index++] |= ((input[i]>>(bits-empty_bits))&mask[empty_bits]);
				packedBytes[index] |= ((input[i]&mask[bits-empty_bits])<<(8-bits+empty_bits));
				empty_bits += (8-bits);
			}
		}
		
		return packedBytes;
	}

   	/**
	 * Packs all or part of the input byte array which uses "bits" bits to use all 8 bits.
	 * <p>
	 * We assume len is a multiplication of stride. The parameter stride controls the packing
	 * unit length and different units <b>DO NOT</b> share same byte. This happens when packing
	 * image data where each scan line <b>MUST</b> start at byte boundary like TIFF.
	 * 
	 * @param input input byte array to be packed
	 * @param stride length of packing unit
	 * @param start offset of the input array to start packing  
	 * @param bits number of bits used in each byte of the input
	 * @param len number of input bytes to be packed
	 * @return the packed byte array
	 */
	public static byte[] packByteArray(byte[] input, int stride, int start, int bits, int len) {
		//
		if(bits == 8) return ArrayUtils.subArray(input, start, len);
		if(bits > 8 || bits <= 0) throw new IllegalArgumentException("Invalid value of bits: " + bits);
		
		int bitsPerStride = bits*stride;
		int numOfStrides = len/stride;
		byte[] packedBytes = new byte[((bitsPerStride + 7)>>3)*numOfStrides];
		short mask[] = {0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff};
	    	
		int index = 0;
		int empty_bits = 8;
		int end = start + len;
		int strideCounter = 0;
		
		for(int i = start; i < end; i++) {
			// If we have enough space for input byte, one step operation
			if(empty_bits >= bits) {
				packedBytes[index] |= ((input[i]&mask[bits])<<(empty_bits-bits));
				empty_bits -= bits;
			} else { // Otherwise, split the pixel between two bytes.			
				//This will never happen for 1, 2, 4, 8 bits color depth image		
				packedBytes[index++] |= ((input[i]>>(bits-empty_bits))&mask[empty_bits]);
				packedBytes[index] |= ((input[i]&mask[bits-empty_bits])<<(8-bits+empty_bits));
				empty_bits += (8-bits);
			}
			// Check to see if we need to move to next byte
			if(++strideCounter%stride == 0 || empty_bits == 0) {
				index++;
				empty_bits = 8;			
			}
		}
		
		return packedBytes;
	}
    
    // Quick sort
    public static void quicksort(int[] array) {
	   quicksort (array, 0, array.length - 1);
    }
    
    public static void quicksort (int[] array, int start, int end) {
	   int inner = start;
	   int outer = end;
	   int mid = (start + end) / 2;
	
	   do {
		// work in from the start until we find a swap to the
		// other partition is needed 
		   while ((inner < mid) && (array[inner] <= array[mid]))
			  inner++;                         
	
		// work in from the end until we find a swap to the
		// other partition is needed
	
		   while ((outer > mid) && (array[outer] >= array[mid]))
			  outer--;                          
		
		// if inner index <= outer index, swap elements	
		   if (inner < mid && outer > mid) {
		      swap(array, inner, outer);
			  inner++;
			  outer--;
		   } else if (inner < mid) {
			  swap(array, inner, mid - 1);
			  swap(array, mid, mid - 1);
			  mid--;
		   } else if (outer >mid) {
			  swap(array, outer, mid + 1);
			  swap(array, mid, mid + 1);
			  mid++;		
		   }	
	   } while (inner !=outer);
	
	// recursion
	   if ((mid - 1) > start) quicksort(array, start, mid - 1);
 	   if (end > (mid + 1)) quicksort(array, mid + 1, end);
    }
    
    // Quick sort
    public static <T extends Comparable<? super T>> void quicksort (T[] array) {
    	quicksort(array, 0, array.length - 1);
    }
    
    // Quick sort
    public static <T extends Comparable<? super T>> void quicksort (T[] array, int low, int high) {
    	int i = low, j = high;
		// Get the pivot element from the middle of the list
		T pivot = array[low + (high-low)/2];

		// Divide into two lists
		while (i <= j) {
			// If the current value from the left list is smaller then the pivot
			// element then get the next element from the left list
			while (array[i].compareTo(pivot) < 0) {
				i++;
			}
			// If the current value from the right list is larger then the pivot
			// element then get the next element from the right list
			while (array[j].compareTo(pivot) > 0) {
				j--;
			}

			// If we have found a values in the left list which is larger then
			// the pivot element and if we have found a value in the right list
			// which is smaller then the pivot element then we exchange the
			// values.
			// As we are done we can increase i and j
			if (i <= j) {
				swap(array, i, j);
				i++;
				j--;
			}
		}
		// Recursion
		if (low < j)
			quicksort(array, low, j);
		if (i < high)
			quicksort(array, i, high);
	}
    
    // Based on java2novice.com example
    /**
     * Remove duplicate elements from an int array
     * 
     * @param input input unsorted int array
     * @return a sorted int array with unique elements
     */
    public static int[] removeDuplicates(int[] input) {
        //return if the array length is less than 2
        if(input.length < 2){
            return input;
        }
      
        // Sort the array first
        Arrays.sort(input);        
        
    	int j = 0;
        int i = 1;
              
        while(i < input.length){
            if(input[i] == input[j]){
                i++;
            } else{
                input[++j] = input[i++];
            }   
        }
        
        int[] output = new int[j + 1];
        
        System.arraycopy(input, 0, output, 0, j + 1);
        
        return output;
    }
   	
   	// Reverse the bit order (bit sex) of a byte array
	public static void reverseBits(byte[] input) {
		for(int i = input.length - 1; i >= 0; i--)
			input[i] = BIT_REVERSE_TABLE[input[i]&0xff];
	}
	
	public static byte[] reverse(byte[] array) {
		if (array == null)
			throw new IllegalArgumentException("Input array is null");
		int left = 0;
		int right = array.length - 1;
		byte tmp;
		while (left < right) {
			tmp = array[right];
			array[right] = array[left];
			array[left] = tmp;
			left++;
			right--;
		}
		
		return array;
	}

	// Reverse the array
	public static <T> void reverse(T[] data) {
	    for (int left = 0, right = data.length - 1; left < right; left++, right--) {
	        T temp = data[left];
	        data[left]  = data[right];
	        data[right] = temp;
	    }
	}
   	
    // Shell sort
    public static void shellsort(int[] array) {
    	shellsort(array, 0, array.length - 1);
    }
   	
    public static void shellsort(int[] array, int start, int end) {
    	if(start < 0 || end < 0 || start > end || end > array.length -1) throw new IllegalArgumentException("Array index out of bounds");
    	int gap = 1;
    	int len = end - start + 1;
 	    // Generate Knuth sequence 1, 4, 13, 40, 121, 364,1093, 3280, 9841 ...
    	while(gap < len) gap = 3*gap + 1;
    	while ( gap > 0 )
    	{
    		int begin = start + gap;
    		for (int i = begin; i <= end; i++)
    		{
    			int temp = array[i];
    			int j = i;
    			while ( j >= begin && temp <= array[j - gap])
    			{
    				array[j] = array[j - gap];
    				j -= gap;
    			}
    			array[j] = temp;
    		}
    		gap /= 3;
    	}
	}
    
    // Shell sort
    public static <T extends Comparable<? super T>> void shellsort(T[] array) {
    	shellsort(array, 0, array.length - 1);
    }
   	
    // Shell sort
    public static <T extends Comparable<? super T>> void shellsort(T[] array, int start, int end) {
    	if(start < 0 || end < 0 || start > end || end > array.length - 1) throw new IllegalArgumentException("Array index out of bounds");
	   	int gap = 1;
	   	int len = end - start + 1;
  	    // Generate Knuth sequence 1, 4, 13, 40, 121, 364,1093, 3280, 9841 ...
	   	while(gap < len) gap = 3*gap + 1;
	   	while ( gap > 0 )
	   	{
	   		int begin = start + gap;
	   		for (int i = begin; i <= end; i++)
	   		{
	   			T temp = array[i];
	   			int j = i;
	   			while ( j >= begin && temp.compareTo(array[j - gap]) <= 0)
	   			{
	   				array[j] = array[j - gap];
	   				j -= gap;
	   			}
	   			array[j] = temp;
	   		}
	   		gap /= 3;
	   	}
    } 	

    public static byte[] subArray(byte[] src, int offset, int len) {
		if(offset == 0 && len == src.length) return src;
		if((offset < 0 || offset >= src.length) || (offset + len > src.length))
			throw new IllegalArgumentException("Copy range out of array bounds");
		byte[] dest = new byte[len];
		System.arraycopy(src, offset, dest, 0, len);
		
		return dest;
	}
    
    private static final void swap(int[] array, int a, int b) {
	   int temp = array[a];
	   array[a] = array[b];
	   array[b] = temp;
    }
    
    private static final <T> void swap(T[] array, int a, int b) {
	   T temp = array[a];
	   array[a] = array[b];
	   array[b] = temp;
    }
    
   	public static float[] to16BitFloatArray(byte[] data, boolean bigEndian) {
		short[] shorts = (short[])toNBits(16, data, Integer.MAX_VALUE, bigEndian);
		float[] floats = new float[shorts.length];
	
		for(int i = 0; i < floats.length; i++) {
			floats[i] = toFloat(shorts[i]);
		}
		
		return floats;
	}

    /*
	 * Tries to convert 24 bit floating point sample to 32 bit float data.
	 * Up to now, there has been no way to do it correctly and there might be no
	 * correct way to do this because 24 bit is not an IEEE floating point type.
	 * 24 bit floating point images appear too dark using this conversion.
	 */	
	public static float[] to24BitFloatArray(byte[] data, boolean bigEndian) {
		int[] ints = (int[])toNBits(24, data, Integer.MAX_VALUE, bigEndian);
		float[] floats = new float[ints.length];
	
		for(int i = 0; i < floats.length; i++) {
			/**
			int bits = ints[i]<<8;
			int sign     = ((bits & 0x80000000) == 0) ? 1 : -1;
	        int exponent = ((bits & 0x7f800000) >> 23);
	        int mantissa =  (bits & 0x007fffff);

	        mantissa |= 0x00800000;
	      
	        floats[i] = (float)(sign * mantissa * Math.pow(2, exponent-150));
	        */
			floats[i] = Float.intBitsToFloat(ints[i]<<8);
		}
		
		return floats;
	}

    // Convert byte array to long array, then to integer array discarding the higher bits
	public static int[] to32BitsLongArray(byte[] data, boolean bigEndian) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(data);
		
		if (bigEndian) {
			byteBuffer.order(ByteOrder.BIG_ENDIAN);
		} else {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		}
		
		LongBuffer longBuf = byteBuffer.asLongBuffer();
		long[] array = new long[longBuf.remaining()];
		longBuf.get(array);
		
		int[] iArray = new int[array.length];
		
		int i = 0;
		
		for(long l : array) {
			iArray[i++] = (int)l;
		}
		
		return iArray;
	}
    
    public static byte[] toByteArray(int value) {
		return new byte[] {
	        (byte)value,
	        (byte)(value >>> 8),
	        (byte)(value >>> 16),
	        (byte)(value >>> 24)	            		            
	        };
	}
    
    public static byte[] toByteArray(int[] data, boolean bigEndian) {
		
		ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4);
		
		if (bigEndian) {
			byteBuffer.order(ByteOrder.BIG_ENDIAN);
		} else {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		}
        
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(data);

        byte[] array = byteBuffer.array();

		return array;
	}
    
  	public static byte[] toByteArray(long[] data, boolean bigEndian) {
		
		ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 8);
		
		if (bigEndian) {
			byteBuffer.order(ByteOrder.BIG_ENDIAN);
		} else {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		}
        
		LongBuffer longBuffer = byteBuffer.asLongBuffer();
        longBuffer.put(data);

        byte[] array = byteBuffer.array();

		return array;
	}
	
	public static byte[] toByteArray(short value) {
		 return new byte[] {
				 (byte)value, (byte)(value >>> 8)};
	}

	public static byte[] toByteArray(short[] data, boolean bigEndian) {
		
		ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 2);
		
		if (bigEndian) {
			byteBuffer.order(ByteOrder.BIG_ENDIAN);
		} else {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		}
        
		ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
        shortBuffer.put(data);

        byte[] array = byteBuffer.array();

		return array;
	}
    
    public static byte[] toByteArrayMM(int value) {
    	return new byte[] {
	        (byte)(value >>> 24),
	        (byte)(value >>> 16),
	        (byte)(value >>> 8),
	        (byte)value};
	}

    public static byte[] toByteArrayMM(short value) {
		 return new byte[] {
				 (byte)(value >>> 8), (byte)value};
	}
    
    public static double[] toDoubleArray(byte[] data, boolean bigEndian) {
		return toDoubleArray(data, 0, data.length, bigEndian);
	}
	
	public static double[] toDoubleArray(byte[] data, int offset, int len, boolean bigEndian) {
		
		ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, len);
		
		if (bigEndian) {
			byteBuffer.order(ByteOrder.BIG_ENDIAN);
		} else {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		}
		
		DoubleBuffer doubleBuf = byteBuffer.asDoubleBuffer();
		double[] array = new double[doubleBuf.remaining()];
		doubleBuf.get(array);
		
		return array;
	}

    // From http://stackoverflow.com/questions/6162651/half-precision-floating-point-in-java
	// Converts integer to float ignores the higher 16 bits
	public static float toFloat(int lbits) {
		// ignores the higher 16 bits
	    int mant = lbits & 0x03ff;            // 10 bits mantissa
	    int exp = lbits & 0x7c00;             // 5 bits exponent
	   
	    if(exp == 0x7c00 ) {                  // NaN/Inf
	        exp = 0x3fc00;                    // -> NaN/Inf
	    } else if(exp != 0) {                 // normalized value	   
	        exp += 0x1c000;                   // exp - 15 + 127
	        if( mant == 0 && exp > 0x1c400)   // smooth transition
	            return Float.intBitsToFloat(
	            		( lbits & 0x8000) << 16
	                    | exp << 13 | 0x3ff);
	    } else if(mant != 0) {                // && exp==0 -> subnormal
	    	exp = 0x1c400;                    // make it normal
	        do {
	            mant <<= 1;                   // mantissa * 2
	            exp -= 0x400;                 // decrease exp by 1
	        } while((mant & 0x400) == 0);     // while not normal
	        mant &= 0x3ff;                    // discard subnormal bit
	    }                                     // else +/-0 -> +/-0
	   
	    return Float.intBitsToFloat(          // combine all parts
	        ( lbits & 0x8000 ) << 16          // sign  << ( 31 - 15 )
	        | ( exp | mant ) << 13 );         // value << ( 23 - 10 )
	}

    public static float[] toFloatArray(byte[] data, boolean bigEndian) {
		return toFloatArray(data, 0, data.length, bigEndian);
	}

    public static float[] toFloatArray(byte[] data, int offset, int len, boolean bigEndian) {
		
		ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, len);
		
		if (bigEndian) {
			byteBuffer.order(ByteOrder.BIG_ENDIAN);
		} else {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		}
		
		FloatBuffer floatBuf = byteBuffer.asFloatBuffer();
		float[] array = new float[floatBuf.remaining()];
		floatBuf.get(array);		
		
		return array;
	}

	public static int[] toIntArray(byte[] data, boolean bigEndian) {
		return toIntArray(data, 0, data.length, bigEndian);
	}

    public static int[] toIntArray(byte[] data, int offset, int len, boolean bigEndian) {
		
		ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, len);
		
		if (bigEndian) {
			byteBuffer.order(ByteOrder.BIG_ENDIAN);
		} else {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		}
		
		IntBuffer intBuf = byteBuffer.asIntBuffer();
		int[] array = new int[intBuf.remaining()];
		intBuf.get(array);
		
		return array;
	}
	 
	public static long[] toLongArray(byte[] data, boolean bigEndian) {
		return toLongArray(data, 0, data.length, bigEndian);
	}
	
	public static long[] toLongArray(byte[] data, int offset, int len, boolean bigEndian) {
		
		ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, len);
		
		if (bigEndian) {
			byteBuffer.order(ByteOrder.BIG_ENDIAN);
		} else {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		}
		
		LongBuffer longBuf = byteBuffer.asLongBuffer();
		long[] array = new long[longBuf.remaining()];
		longBuf.get(array);
		
		return array;
	}
	
	/**
	 * Converts an input byte array to nBits data array using the smallest data type which
	 * can hold the nBits data. Each data type contains only one data element.
	 * 
	 * @param nBits number of bits for the data element
	 * @param input the input array for the data elements
	 * @param stride scan line stride used to discard remaining bits 
	 * @param bigEndian the packing order of the bits. True if bigEndian, otherwise false.
	 * 
	 * @return an array of the smallest data type which can hold the nBits data
	 */
	public static Object toNBits(int nBits, byte[] input, int stride, boolean bigEndian) {
		int value = 0;
   		int bits_remain = 0;
   		int temp_byte = 0;
   		
   		byte[] byteOutput = null;
   		short[] shortOutput = null;
   		int[] intOutput = null;
   		Object output = null;
   		
   		int outLen = (int)((input.length*8L + nBits - 1)/nBits);
   		
   		if(nBits <= 8) {
   			byteOutput = new byte[outLen];
   			output = byteOutput;
   		} else if(nBits <= 16) {
   			shortOutput = new short[outLen];
   			output = shortOutput;
   		} else if(nBits <= 32){
   			intOutput = new int[outLen];
   			output = intOutput;   			
   		} else {
   			throw new IllegalArgumentException("nBits exceeds limit - maximum 32");
   		}
   			
   		int offset = 0;
    	int index = 0;
    	
    	int strideCounter = 0;
   		
    	loop:
   	  	while(true) {  			
   	   		
			if(!bigEndian)
				value = (temp_byte >> (8-bits_remain));
			else				
				value = (temp_byte & MASK[bits_remain]); 
				
			while (nBits > bits_remain)
			{
				if(offset >= input.length) {
					break loop;
				}
				
				temp_byte = input[offset++]&0xff;
				
				if(bigEndian)
					value = ((value<<8)|temp_byte);
				else
					value |= (temp_byte<<bits_remain);
				
				bits_remain += 8;
			}
			
			bits_remain -= nBits;
			
			if(bigEndian)
				value = (value>>(bits_remain));			
	        
			value &= MASK[nBits];
			
			if(++strideCounter%stride == 0) {
				bits_remain = 0; // Discard the remaining bits			
			}
		
			if(nBits <= 8) byteOutput[index++] = (byte)value;
			else if(nBits <= 16) shortOutput[index++] = (short)value;
			else intOutput[index++] = value;
	  	}
   		
		return output;
	}
	
	public static double[] toPrimitive(Double[] doubles) {
		double[] dArray = new double[doubles.length];
		int i = 0;
		
		for (double d : doubles) {
			dArray[i++] = d;
		}
		
		return dArray;
	}
	
	public static float[] toPrimitive(Float[] floats) {
		float[] fArray = new float[floats.length];
		int i = 0;
		
		for (float f : floats) {
			fArray[i++] = f;
		}
		
		return fArray;
	}
	
	public static int[] toPrimitive(Integer[] integers) {
		int[] ints = new int[integers.length];
		int i = 0;
		
		for (int n : integers) {
			ints[i++] = n;
		}
		
		return ints;
	}
	
	public static long[] toPrimitive(Long[] longs) {
		long[] lArray = new long[longs.length];
		int i = 0;
		
		for (long l : longs) {
			lArray[i++] = l;
		}
		
		return lArray;
	}
	
	public static short[] toPrimitive(Short[] shorts) {
		short[] sArray = new short[shorts.length];
		int i = 0;
		
		for (short s : shorts) {
			sArray[i++] = s;
		}
		
		return sArray;
	}
	
	public static short[] toShortArray(byte[] data, boolean bigEndian) {
		return toShortArray(data, 0, data.length, bigEndian);		
	}
	
	public static short[] toShortArray(byte[] data, int offset, int len, boolean bigEndian) {
		
		ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, len);
		
		if (bigEndian) {
			byteBuffer.order(ByteOrder.BIG_ENDIAN);
		} else {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		}
		
		ShortBuffer shortBuf = byteBuffer.asShortBuffer();
		short[] array = new short[shortBuf.remaining()];
		shortBuf.get(array);
		
		return array;
	}
   	
   	private ArrayUtils(){} // Prevents instantiation
}