/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
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
 * WY    25Nov2014  Added removeDuplicates() to sort and remove duplicates from int arrays
 * WY    12Nov2014  Changed the argument sequence for flipEndian()
 * WY    11Nov2014  Changed flipEndian() to include scan line stride to skip bits
 * WY    11Nov2014  Added toNBits() to convert byte array to nBits data unit
 * WY    28Oct2014  Added flipEndian() to work with TIFTweaker mergeTiffImagesEx()
 */

package cafe.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.lang.reflect.Array;
import java.nio.ByteOrder;
import java.util.AbstractList;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Array utility class 
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 09/18/2012
 */
public class ArrayUtils 
{
	public static void bubbleSort(int[] array) {
	    int n = array.length;
	    boolean doMore = true;
	    
	    while (doMore) {
	        n--;
	        doMore = false;  // assume this is our last pass over the array
	    
	        for (int i=0; i<n; i++) {
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
	    
	        for (int i=0; i<n; i++) {
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

	public static int[] byteArrayToIntArray(byte[] data, boolean bigEndian) {
		return byteArrayToIntArray(data, 0, data.length, bigEndian);
	}
	
	public static int[] byteArrayToIntArray(byte[] data, int offset, int len, boolean bigEndian) {
		
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
	
	public static short[] byteArrayToShortArray(byte[] data, boolean bigEndian) {
		return byteArrayToShortArray(data, 0, data.length, bigEndian);		
	}
	
	public static short[] byteArrayToShortArray(byte[] data, int offset, int len, boolean bigEndian) {
		
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

	public static byte[] concat(byte[] first, byte[] second) {
		
		int firstLen = first.length;
		int secondLen = second.length;
		
		if (firstLen == 0) {
			return second;
		}
		
		if (secondLen == 0) {
			return first;
		}	
		
	    byte[] result = new byte[firstLen + secondLen];
   
		System.arraycopy(first, 0, result, 0, firstLen);
		System.arraycopy(second, 0, result, firstLen, secondLen);
    
		return result;
    }
	
	public static byte[] concat(byte[] first, byte[]... rest) {
  	  
		int totalLength = first.length;	  
	  
		for (byte[] array : rest) {		
			totalLength += array.length;
	 	}
		
		byte[] result = new byte[totalLength];
	  
		int offset = first.length;
		
		if (offset != 0)
	  		System.arraycopy(first, 0, result, 0, offset);
	
		for (byte[] array : rest) {
			System.arraycopy(array, 0, result, offset, array.length);
			offset += array.length;
		}
		
		return result;
	}
	
	/**
	 * If type parameter is not explicitly supplied, it will be inferred as the 
	 * upper bound for the two parameters.
	 * 
	 * @param first the first array to be concatenated
	 * @param second the second array to be concatenated
	 * @return a concatenation of the first and the second arrays
	 */
	public static <T> T[] concat(T[] first, T[] second) {
		
		int firstLen = first.length;
		int secondLen = second.length;
		
		if (firstLen == 0) {
			return second;
		}
		
		if (secondLen == 0) {
			return first;
		}	
		
	    // For JDK1.6+, use the following two lines instead.
	    //T[] result = java.util.Arrays.copyOf(first, first.length + second.length);
        //System.arraycopy(second, 0, result, first.length, second.length);		
		
		@SuppressWarnings("unchecked")	
		T[] result = (T[]) Array.newInstance(first.getClass().getComponentType(), firstLen + secondLen);
   
		System.arraycopy(first, 0, result, 0, firstLen);
		System.arraycopy(second, 0, result, firstLen, secondLen);
    
		return result;
    }

	public static <T> T[] concat(T[] first, T[]... rest) {
	  
		int totalLength = first.length;	  
	  
		for (T[] array : rest) {		
			totalLength += array.length;
	 	}
		@SuppressWarnings("unchecked")
		T[] result = (T[]) Array.newInstance(first.getClass().getComponentType(), totalLength);
	  
		int offset = first.length;
		
		if(offset != 0)
			System.arraycopy(first, 0, result, 0, offset);
	
		for (T[] array : rest) {
			System.arraycopy(array, 0, result, offset, array.length);
			offset += array.length;
		}
		
		return result;
	}

   	/** 
	 * Concatenates two arrays to a new one with type newType
	 * 
     * @param first the first array to be concatenated
     * @param second the second array to be concatenated
     * @param newType type bound for the concatenated array
     * @return a concatenation of the first and the second arrays.
     * @throws NullPointerException if <tt>first</tt> or <tt>second</tt> is null
     * @throws ArrayStoreException if an element copied from
     *     <tt>first</tt> or <tt>second</tt> is not of a runtime type that can be stored in
     *     an array of class <tt>newType</tt>
  	 */
    public static <T,U,V> T[] concat(U[] first, V[] second, Class<? extends T[]> newType) {
		
		int firstLen = first.length;
		int secondLen = second.length;
		
		if (firstLen == 0) {
			@SuppressWarnings("unchecked")	
			T[] returnValue = (T[])second;
			return returnValue;
		}
		
		if (secondLen == 0) {
			@SuppressWarnings("unchecked")	
			T[] returnValue = (T[])first;
			return returnValue;
		}	
		
	    @SuppressWarnings("unchecked")
	    // Need to cast to Object before using == operator, so that they have the
	    // same common super class.
	    T[] result = ((Object)newType == (Object)Object[].class)
                     ? (T[]) new Object[firstLen + secondLen]
                     : (T[]) Array.newInstance(newType.getComponentType(), firstLen + secondLen);
		   
		System.arraycopy(first, 0, result, 0, firstLen);
		System.arraycopy(second, 0, result, firstLen, secondLen);
    
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
   		int temp = 0;
   		int bits_remain = 0;
   		int temp_byte = 0;
   		int empty_bits = 8;
   		
   		byte[] output = new byte[input.length];
   		
   		int strideCounter = 0;
   		
   		// Bit mask 0 - 32 bits
   		int[] MASK = {0x00,0x001,0x003,0x007,0x00f,0x01f,0x03f,0x07f,0x0ff,0x1ff,0x3ff,0x7ff,0xfff,
   				      0x1fff, 0x3fff, 0x7fff, 0xffff, 0x1ffff, 0x3ffff, 0x7ffff, 0xfffff,
   				      0x1fffff, 0x3fffff, 0x7fffff, 0xffffff, 0x1ffffff, 0x3ffffff, 0x7ffffff,
   				      0xfffffff, 0x1fffffff, 0x3fffffff, 0x7fffffff, 0xffffffff};
   		
   		int end = offset + len;
   		int bufIndex = 0;
    	 
   		int temp1 = bits;
    	boolean bigEndianOut = !bigEndian;
    	
   	  	loop:
   	  	while(true) {  			
   	   		
			if(!bigEndian)
				temp = (temp_byte >> (8-bits_remain));
			else				
				temp = (temp_byte & MASK[bits_remain]); 
				
			while (bits > bits_remain)
			{
				if(offset >= end) {
					break loop;
				}
				
				temp_byte = input[offset++]&0xff;
				
				if(bigEndian)
					temp = ((temp<<8)|temp_byte);
				else
					temp |= (temp_byte<<bits_remain);
				
				bits_remain += 8;
			}
			
			bits_remain -= bits;
			
			if(bigEndian)
				temp = (temp>>(bits_remain));		
	        
			int value = (temp&MASK[bits]);	
		
	   	  	//////////////////////// Write bits bit length value in opposite endian	    	
	    	if(bigEndianOut) {
	    		temp1 = bits-empty_bits;
	    		output[bufIndex] |= ((value>>>temp1)&MASK[empty_bits]);
	    		
	    		while(temp1 > 8)
				{
					output[++bufIndex] |= ((value>>>(temp1-8))&MASK[8]);
					temp1 -= 8;
				} 
	    		
	    		if(temp1 > 0) {
	    			output[++bufIndex] |= ((value&MASK[temp1])<<(8-temp1));
	    			temp1 -= 8;
	    		}
	       	} else { // Little endian
	       		temp1 = bits;
				output[bufIndex] |= ((value&MASK[empty_bits])<<(8-empty_bits));
				value >>= empty_bits;
		        temp1 -= empty_bits;
		        // If the code is longer than the empty_bits
				while(temp1 > 8) {
					output[++bufIndex] |= (value&0xff);
					value >>= 8;
					temp1 -= 8;
				}
				
		        if(temp1 > 0)
				{
		        	output[++bufIndex] |= (value&MASK[temp1]);
	    			temp1 -= 8;
				}
			}
	    	
	    	empty_bits = -temp1;
			
	    	if(++strideCounter%scanLineStride == 0) {
				empty_bits = 0;
				bits_remain = 0;	
			}			
   	  	}
   		
		return output;
	}
   	
    // Insertion sort
    public static void insertionsort(int[] array)
    {
	   insertionsort(array, 0, array.length-1);
    } 	

    public static void insertionsort(int[] array, int start, int end)
    {
	   int j;

	   for (int i = start+1; i < end+1; i++)
	   {
		   int temp = array[i];
		   for ( j = i; j > start && temp <= array[j-1]; j-- )
		       array[j] = array[j-1];
		   // Move temp to the right place
		   array[j] = temp;
	   }
    }
    
    // Insertion sort
    public static <T extends Comparable<? super T>> void insertionsort(T[] array, int start, int end)
    {
	   int j;

	   for (int i = start+1; i < end+1; i++)
	   {
		   T temp = array[i];
		   for ( j = i; j > start && temp.compareTo(array[j-1]) <= 0; j-- )
		       array[j] = array[j-1];
		   // Move temp to the right place
		   array[j] = temp;
	   }
    }
    
   	// From Effective Java 2nd Edition. 
   	public static List<Integer> intArrayAsList(final int[] a) 
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

    public static byte[] intArrayToByteArray(int[] data, boolean bigEndian) {
		
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

    public static byte[] intToByteArray(int value) {
		return new byte[] {
	        (byte)value,
	        (byte)(value >>> 8),
	        (byte)(value >>> 16),
	        (byte)(value >>> 24)	            		            
	        };
	}
    
    public static byte[] intToByteArrayMM(int value) {
    	return new byte[] {
	        (byte)(value >>> 24),
	        (byte)(value >>> 16),
	        (byte)(value >>> 8),
	        (byte)value};
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

    // Shell sort
    public static void shellsort(int[] array)
    {
	   shellsort(array, 0, array.length-1);
    }

    public static void shellsort(int[] array, int start, int end)
    {
	   int mid = (start + end)/2;

	   while ( mid > start )
	   {
		   for (int i = mid; i <= end; i++)
		   {
			   int temp = array[i];
			   int j = i;
			   while ( j >= mid && temp <= array[j - mid + start])
			   {
				   array[j] = array[j - mid + start];
				   j -= (mid - start);
			   }
			   array[j] = temp;
		   }
		   mid = (start + mid)/2;
	   }
    }

    // Shell sort
    public static <T extends Comparable<? super T>> void shellsort(T[] array, int start, int end)
    {
	   int mid = (start + end)/2;
	   while ( mid > start )
	   {
		   for (int i = mid; i <= end; i++)
		   {
			   T temp = array[i];
			   int j = i;
			   while ( j >= mid && temp.compareTo(array[j - mid + start]) <= 0)
			   {
				   array[j] = array[j - mid + start];
				   j -= (mid - start);
			   }
			   array[j] = temp;
		   }
		   mid = (start + mid)/2;
	   }
    }

	public static byte[] shortArrayToByteArray(short[] data, boolean bigEndian) {
		
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

    public static byte[] shortToByteArray(short value) {
		 return new byte[] {
				 (byte)value, (byte)(value >>> 8)};
	}
	 
	public static byte[] shortToByteArrayMM(short value) {
		 return new byte[] {
				 (byte)(value >>> 8), (byte)value};
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
	
	private static final <T extends Comparable<? super T>> void swap(T[] array, int a, int b) {
	   T temp = array[a];
	   array[a] = array[b];
	   array[b] = temp;
    }
	
	/**
	 * Convert an input byte array to nBits data array using the smallest data type which
	 * can hold the nBits data. Each data type contains only one data element
	 * 
	 * @param nBits number of bits for the data element
	 * @param input the input array for the data elements
	 * @param stride scan line stride used to discard remaining bits 
	 * @param bigEndian the packing order of the bits
	 * 
	 * @return an array of the smallest data type which can hold the nBits data
	 */
	public static Object toNBits(int nBits, byte[] input, int stride, boolean bigEndian) {
		int temp = 0;
   		int bits_remain = 0;
   		int temp_byte = 0;
   		
   		byte[] byteOutput = null;
   		short[] shortOutput = null;
   		int[] intOutput = null;   		
   		Object output = null;
   		
   		int outLen = (input.length*8 + nBits - 1)/nBits;
   		
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
   		// Bit mask 0 - 32 bits
   		int[] MASK = {0x00,0x001,0x003,0x007,0x00f,0x01f,0x03f,0x07f,0x0ff,0x1ff,0x3ff,0x7ff,0xfff,
   				      0x1fff, 0x3fff, 0x7fff, 0xffff, 0x1ffff, 0x3ffff, 0x7ffff, 0xfffff,
   				      0x1fffff, 0x3fffff, 0x7fffff, 0xffffff, 0x1ffffff, 0x3ffffff, 0x7ffffff,
   				      0xfffffff, 0x1fffffff, 0x3fffffff, 0x7fffffff, 0xffffffff // 32 bits
   				    };
   		
   		int offset = 0;
    	int index = 0;
    	
    	int strideCounter = 0;
   		
    	loop:
   	  	while(true) {  			
   	   		
			if(!bigEndian)
				temp = (temp_byte >> (8-bits_remain));
			else				
				temp = (temp_byte & MASK[bits_remain]); 
				
			while (nBits > bits_remain)
			{
				if(offset >= input.length) {
					break loop;
				}
				
				temp_byte = input[offset++]&0xff;
				
				if(bigEndian)
					temp = ((temp<<8)|temp_byte);
				else
					temp |= (temp_byte<<bits_remain);
				
				bits_remain += 8;
			}
			
			bits_remain -= nBits;
			
			if(bigEndian)
				temp = (temp>>(bits_remain));			
	        
			int value = (temp&MASK[nBits]);
			
			if(++strideCounter%stride == 0) {
				bits_remain = 0;				
			}
		
			if(nBits <= 8)
	   	  		byteOutput[index++] = (byte)value;
			else if(nBits <= 16) shortOutput[index++] = (short)value;
			else intOutput[index++] = value;
   	  	}
   		
		return output;
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
		
		for (Integer n : integers) {
			ints[i++] = n;
		}
		
		return ints;
	}
   	
   	private ArrayUtils(){} // Prevents instantiation
}
