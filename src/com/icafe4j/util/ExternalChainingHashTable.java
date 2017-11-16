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
/**
 * External chaining hash table.  
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 06/09/2007
 *
 * Based on java.util.Hashtable and
 * SeparateChainingHashTable.java
 * 
 * @author Mark Allen Weiss
 */
public class ExternalChainingHashTable<K,V> {
	
	private static final int DEFAULT_TABLE_SIZE = 101;
	private int currentSize = 0;

    /** The array of HashEntry. */
    private HashEntry<K,V> [ ] entries; 

	/**
     * Construct the hash table.
     */
    public ExternalChainingHashTable( )
    {
        this( DEFAULT_TABLE_SIZE );
    }

    /**
     * Construct the hash table.
     * @param size approximate table size.
     */
    @SuppressWarnings("unchecked")
	public ExternalChainingHashTable( int size )
    {
        entries = new HashEntry[ nextPrime( size ) ];
    }

    /**
     * Insert into the hash table. If the item is
     * already present, then do nothing.
     * @param key the item to insert.
     */
    public void put( K key, V value )
    {
		int hash = key.hashCode();
		// And with the largest positive integer
		int currentPos = (hash & 0x7FFFFFFF) % entries.length;

        HashEntry<K,V> e = entries[ currentPos ];
        
		for ( ; e != null ; e = e.next) {
             if ((e.hash == hash) && e.key.equals(key))
	              return;// Duplicate, do nothing
        }
        // Creates the new entry.
        e = new HashEntry<K,V>(hash, key, value, entries[currentPos]);
        entries[currentPos] = e;

        if( ++currentSize > entries.length / 2 )
           rehash( );
    }

    /**
     * Remove from the hash table.
     * @param key the item to remove.
     */
    public void remove( K key )
    {
	   int hash = key.hashCode();
	   // And with the largest positive integer
	   int currentPos = (hash & 0x7FFFFFFF) % entries.length;

   	   HashEntry<K,V> prev = null, e = entries[currentPos];
	   
	   for (; e != null ; prev = e, e = e.next) {
           if ((e.hash == hash) && e.key.equals(key)) {// found the key
	 		  if (prev != null) { // it's an internal entry
	                prev.next = e.next;
	          } else { // it's the first entry
	                entries[currentPos] = e.next;
	          }
			  currentSize--;
		   }
	   }
    }

    /**
     * Find an item in the hash table.
     * @param key the item to search for.
     * @return the matching item, or null if not found.
     */
    public V get( K key )
    {
		int hash = key.hashCode();
		// And with the largest positive integer
		int currentPos = (hash & 0x7FFFFFFF) % entries.length;

        HashEntry<K,V> e = entries[currentPos];
		
		for (; e != null ; e = e.next) {
           if ((e.hash == hash) && e.key.equals(key)) {
	          return e.value;
           }
		}
     	return null;
    }

	/**
     * Search for an item in the hash table.
     * @param key the item to search for.
     * @return true if a matching item found.
     */
    public boolean contains(K key)
    {
        int hash = key.hashCode();
		// And with the largest positive integer
		int currentPos = (hash & 0x7FFFFFFF) % entries.length;

        HashEntry<K,V> e = entries[currentPos];
		
		for (; e != null ; e = e.next) {
           if ((e.hash == hash) && e.key.equals(key)) {
	          return true;
           }
		}
     	return false;
	}

    /**
     * Expand the hash table.
     */
    @SuppressWarnings("unchecked")
	private void rehash( )
    {
        HashEntry<K,V> [ ] oldEntries = entries;

        // Create a new double-sized, empty table
        entries = new HashEntry[ nextPrime( 2 * oldEntries.length ) ];

        // Copy table over
        for( int i = 0; i < oldEntries.length; i++ ){
			HashEntry<K,V> cursor, e = oldEntries[i];
			int currentPos;

			while( e != null ){
				cursor = e;
				e = e.next;

                currentPos = (cursor.hash & 0x7FFFFFFF) % entries.length;
			    cursor.next = entries[currentPos];
				entries[currentPos] = cursor;
		    }
		}
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

	private static class HashEntry<K,V>
	{
		int hash;
		K key;
		V value;
		HashEntry<K,V> next;
        
		HashEntry(int hash, K key, V value, HashEntry<K,V> next)
		{
			this.hash = hash;
			this.key = key;
			this.value = value;
			this.next = next;
		}
	};

    // Test program
    public static void main( String [ ] args )
    {
        ExternalChainingHashTable<Integer, Integer> H = new ExternalChainingHashTable<Integer, Integer>( );

        final int NUMS = 4000;
        final int GAP  =   37;

        System.out.println( "Checking... (no more output means success)" );

        for( int i = GAP; i != 0; i = ( i + GAP ) % NUMS )
            H.put( i, i );
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
