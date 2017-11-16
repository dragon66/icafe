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

package com.icafe4j.util;// Temporarily put in this package

/**
 * A hash table using primitive integer keys. 
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/06/2008
 * 
 * Based on
 * QuadraticProbingHashTable.java
 * <p>
 * Probing table implementation of hash tables.
 * Note that all "matching" is based on the equals method.
 * 
 * @author Mark Allen Weiss
 */
 public class IntHashtable<E> {

    private static final int DEFAULT_TABLE_SIZE = 257;

    /** The array of HashEntry. */
    private HashEntry<E> [ ] array;   // The array of HashEntry
    private int currentSize;       // The number of occupied cells
  
	/**
     * Construct the hash table.
     */
    public IntHashtable( )
    {
        this( DEFAULT_TABLE_SIZE );
    }

    /**
     * Construct the hash table.
     * @param size the approximate initial size.
     */
    @SuppressWarnings("unchecked")
	public IntHashtable( int size )
    {
        array = new HashEntry [size];
        makeEmpty( );
    }

    /**
     * Insert into the hash table. If the item is
     * already present, do nothing.
     * @param key the item to insert.
     */
    public void put( int key, E value )
    {
        // Insert key as active
        int currentPos = locate( key );
        if( isActive( currentPos ) )
            return;

        array[ currentPos ] = new HashEntry<E> ( key, value, true );

        // Rehash
        if( ++currentSize > array.length / 2 )
            rehash( );
    }

    /**
     * Expand the hash table.
     */
    @SuppressWarnings("unchecked")
	private void rehash( )
    {
        HashEntry<E> [ ] oldArray = array;

        // Create a new double-sized, empty table
        array = new HashEntry [ nextPrime( 2 * oldArray.length ) ];
        currentSize = 0;

        // Copy table over
        for( int i = 0; i < oldArray.length; i++ )
            if( oldArray[i] != null && oldArray[i].isActive )
                put( oldArray[i].key, oldArray[i].value );

        return;
    }

    /**
     * Method that performs quadratic probing resolution.
     * @param key the item to search for.
     * @return the index of the item.
     */
    private int locate( int key )
    {
        int collisionNum = 0;

		// And with the largest positive integer
		int currentPos = (key & 0x7FFFFFFF) % array.length;

        while( array[ currentPos ] != null &&
                array[ currentPos ].key != key )
        {
            currentPos += 2 * ++collisionNum - 1;  // Compute ith probe
            if( currentPos >= array.length )       // Implement the mod
              currentPos -= array.length;
        }
        return currentPos;
    }

    /**
     * Remove from the hash table.
     * @param key the item to remove.
     */
    public void remove( int key )
    {
        int currentPos = locate( key );
        if( isActive( currentPos ) )
		{
            array[ currentPos ].isActive = false;
			currentSize--;
		}
    }
    
	/**
     * Search for an item in the hash table.
     * @param key the item to search for.
     * @return true if a matching item found.
     */
    public boolean contains(int key)
    {
        return isActive( locate( key ) );
	}

    /**
     * Find an item in the hash table.
     * @param key the item to search for.
     * @return the value of the matching item.
     */
    public E get( int key )
    {
        int currentPos = locate( key );
        return isActive( currentPos ) ? array[ currentPos ].value : null;
    }

    /**
     * Return true if currentPos exists and is active.
     * @param currentPos the result of a call to findPos.
     * @return true if currentPos is active.
     */
    private boolean isActive( int currentPos )
    {
        return array[ currentPos ] != null && array[ currentPos ].isActive;
    }

    /**
     * Make the hash table logically empty.
     */
    public void makeEmpty( )
    {
        currentSize = 0;
        for( int i = 0; i < array.length; i++ )
            array[ i ] = null;
    }
    
    /**
     * Internal method to find a prime number at least as large as n.
     * @param n the starting number (must be positive).
     * @return a prime number larger than or equal to n.
     */
    private static int nextPrime( int n )
    {
        if( n % 2 == 0 )
            n++;

        for( ; !isPrime( n ); n += 2 )
            ;

        return n;
    }

    /**
     * Internal method to test if a number is prime.
     * Not an efficient algorithm.
     * @param n the number to test.
     * @return the result of the test.
     */
    private static boolean isPrime( int n )
    {
        if( n == 2 || n == 3 )
            return true;

        if( n == 1 || n % 2 == 0 )
            return false;

        for( int i = 3; i * i <= n; i += 2 )
            if( n % i == 0 )
                return false;

        return true;
    }
    
    // The basic entry stored in ProbingHashTable
    private static class HashEntry<V>
    {
       int key;         // the key
       V value;       // the value
       boolean  isActive;  // false if deleted
  
  	       @SuppressWarnings("unused")
       HashEntry( int k, V val )
       {
           this( k, val, true );
       }

       HashEntry( int k, V val, boolean i )
       {
           key = k;
	       value = val;
           isActive  = i;
       }
    }
    
    // Simple main
    public static void main( String [ ] args )
    {
        IntHashtable<Integer> H = new IntHashtable<Integer> ( );

        final int NUMS = 4000;
        final int GAP  =   37;

        System.out.println( "Checking... (no more output means success)" );


        for( int i = GAP; i != 0; i = ( i + GAP ) % NUMS )
            H.put( i, new Integer(i) );
        for( int i = 1; i < NUMS; i+= 2 )
            H.remove( i );

        for( int i = 2; i < NUMS; i+=2 )
            if( H.get(i) != i )
                System.out.println( "Find fails " + i );

        for( int i = 1; i < NUMS; i+=2 )
        {
            if( H.get(i) != null )
                System.out.println( "OOPS!!! " +  i  );
        }
    }
 }