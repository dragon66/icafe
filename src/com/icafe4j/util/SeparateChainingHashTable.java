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
 * Separate chaining hash table.  
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 06/03/2007
 *
 * Based on SeparateChainingHashTable.java
 * <p>
 * Separate chaining table implementation of hash tables.
 * Note that all "matching" is based on the equals method.
 * 
 * @author Mark Allen Weiss
 */
public class SeparateChainingHashTable<E> {
	
	private static final int DEFAULT_TABLE_SIZE = 101;

	/** The array of Lists. */
	private LinkedList<E> [ ] theLists; 

	/**
	 * Construct the hash table.
	 */
	public SeparateChainingHashTable( )
	{
		this( DEFAULT_TABLE_SIZE );
	}

	/**
	 * Construct the hash table.
	 * @param size approximate table size.
	 */
	@SuppressWarnings("unchecked")
	public SeparateChainingHashTable( int size )
	{
		theLists = new LinkedList[ nextPrime( size ) ];
		for( int i = 0; i < theLists.length; i++ )
			theLists[ i ] = new LinkedList<E>( );
	}

	/**
	 * Insert into the hash table. If the item is
	 * already present, then do nothing.
	 * @param x the item to insert.
	 */
	public void put( E x )
	{
		int hash = x.hashCode();
		// And with the largest positive integer
		int currentPos = (hash & 0x7FFFFFFF) % theLists.length;

		LinkedList<E> whichList = theLists[ currentPos ];
		LinkedListItr<E> itr = whichList.find( x );

		if( itr.isPastEnd( ) )
			whichList.insert( x, whichList.zeroth( ) );
	}

	/**
	 * Remove from the hash table.
	 * @param x the item to remove.
	 */
	public void remove( E x )
	{
		int hash = x.hashCode();
		// And with the largest positive integer
		int currentPos = (hash & 0x7FFFFFFF) % theLists.length;

		theLists[ currentPos ].remove( x );
	}

	/**
	 * Find an item in the hash table.
	 * @param x the item to search for.
	 * @return the matching item, or null if not found.
	 */
	public E get( E x )
	{
		return getList( x ).retrieve( );
	}
        
	public LinkedListItr<E> getList(E x) {
		int hash = x.hashCode();
		// And with the largest positive integer
		int currentPos = (hash & 0x7FFFFFFF) % theLists.length;

		return theLists[ currentPos ].find( x );
	}

	/**
	 * Make the hash table logically empty.
	 */
	public void makeEmpty( )
	{
		for( int i = 0; i < theLists.length; i++ )
			theLists[ i ].makeEmpty( );
	}

	/**
	 * A hash routine for String objects.
	 * @param key the String to hash.
	 * @param tableSize the size of the hash table.
	 * @return the hash value.
	 */
	public static int hashString( String key, int tableSize )
	{
		int hashVal = 0;
		
		for( int i = 0; i < key.length( ); i++ )
			hashVal = 37 * hashVal + key.charAt( i );

		hashVal %= tableSize;
		if( hashVal < 0 )
			hashVal += tableSize;

		return hashVal;
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

    // Test program
	public static void main( String [ ] args )
    {
        SeparateChainingHashTable<Integer> H = new SeparateChainingHashTable<Integer>( );

        final int NUMS = 4000;
        final int GAP  =   37;
        
        System.out.println( "Checking... (no more output means success)" );
        
        for( int i = GAP; i != 0; i = ( i + GAP ) % NUMS )
        	H.put( i );
    	for( int i = 1; i < NUMS; i+= 2 )
    		H.remove( i );

        for( int i = 2; i < NUMS; i+=2 )
            if( H.get( i ).intValue( ) != i )
                System.out.println( "Find fails " + i );

        for( int i = 1; i < NUMS; i+=2 )
        {
            if( H.get( i ) != null )
                System.out.println( "OOPS!!! " +  i  );
        }
    }
}