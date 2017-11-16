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
 * Leftist heap.  
 * <p>
 * Definition: The null path length (NPL) of a tree node is the length of
 * the shortest path to a node with 0 children or 1 child. The NPL of a 
 * leaf is 0. The NPL of a NULL pointer is -1.
 * <p>
 * Definition: A leftist tree is a binary tree where at each node the null
 * path length of the left child is greater than or equal to the null path
 * length of the right child.
 * <p>
 * Definition: A leftist heap is a leftist tree where the value stored at
 * any node is less than or equal to the value stored at either of its children.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 06/15/2007
 *
 * Based on LeftistHeap.java 
 * @author Mark Allen Weiss
 */
public class LeftistHeap<E extends Comparable<? super E>> {
	private LeftHeapNode<E> root; 
	private int size;

	/**
	 * Construct the leftist heap.
	 */
	public LeftistHeap( )
	{
		root = null;
	}

	/**
	 * Merge rhs into the priority queue.
	 * rhs becomes empty. rhs must be different from this.
	 * @param rhs the other leftist heap.
	 */
	public void merge( LeftistHeap<E> rhs )
	{
		if( this == rhs )    // Avoid aliasing problems
			return;
    
		root = merge( root, rhs.root );
		size += rhs.getSize();
		rhs.root = null;
	}

	/**
	 * Internal static method to merge two roots.
	 */
	private static <T extends Comparable<? super T>> LeftHeapNode<T>
							merge( LeftHeapNode<T> h1, LeftHeapNode<T> h2 )
	{
		if( h1 == null )
			return h2;
		if( h2 == null )
			return h1;
		if( h1.element.compareTo( h2.element ) > 0 )
		// Swap the references of h1 and h2
		{
			LeftHeapNode<T> temp = h1;
			h1 = h2;
			h2 = temp;
		}
		if( h1.left == null )   // Single node
			h1.left = h2;       
		else
		{
			h1.right = merge( h1.right, h2 );
			if( h1.left.npl < h1.right.npl )
			{ // Swap h1's children
				LeftHeapNode<T> temp = h1.left;
				h1.left = h1.right;
				h1.right = temp;
			}
			h1.npl = h1.right.npl + 1;// counting root in npl
		}
		return h1;
	}

	/**
	 * Insert into the priority queue, maintaining heap order.
	 * @param x the item to insert.
	 */
	public synchronized void insert( E x )
	{
		root = merge(  root, new LeftHeapNode<E>( x ) );
		size++;
	}

	/**
	 * Find the smallest item in the priority queue.
	 * @return the smallest item, or null, if empty.
	 */
	public synchronized E findMin( )
	{
		return root == null? null:root.element;
	}

	/**
	 * Remove the smallest item from the priority queue.
	 * @return the smallest item, or null, if empty.
	 */
	public synchronized E deleteMin( )
	{
		if( root == null )
			return null;

		E minItem = root.element;
		root = merge( root.left, root.right );
		size--;
		return minItem;
	}

	/**
	 * Return this heap as an ordered array.
	 * @param incr the flag to control order.
	 */
	
	@SuppressWarnings("unchecked")
	public synchronized E[] toArray(E[] a, boolean incr) {
        
		int capacity = size;// We must copy the size first 
		int i;

		if (a.length < capacity)
			a = (E[])java.lang.reflect.Array.newInstance(
					a.getClass().getComponentType(), capacity);
		if (incr) // Incremental order
			for ( i = 0; i < capacity; i++ )
				a[i] = deleteMin();
		else // Decremental order
			for ( i = capacity; i-- > 0; )
				a[i] = deleteMin();

		if (a.length > capacity)
			a[capacity] = null;

		return a;
	}

	/**
	 * Test if the priority queue is logically empty.
	 * @return true if empty, false otherwise.
	 */
	public synchronized boolean isEmpty( )
	{
		return root == null;
	}
      
	/**
	 * @return the size of this heap.
	 */
	public synchronized int getSize()
	{
		return size;
	}

	/**
	 * Make the priority queue logically empty.
	 */
	public synchronized void makeEmpty( )
	{
		root = null;
	}

	// Test program
	public static void main( String [ ] args )
	{
		int numItems = 100;
		LeftistHeap<Integer> h  = new LeftistHeap<Integer>( );
		LeftistHeap<Integer> h1 = new LeftistHeap<Integer>( );
		int i = 37;

		System.out.println( "Checking... (no more output means success)" );

		for( i = 37; i != 0; i = ( i + 37 ) % numItems )
			if( i % 2 == 0 )
				h1.insert( i );
			else
				h.insert( i );

		h.merge( h1 );

		for( i = 1; i < numItems; i++ )
			if( h.deleteMin( ).intValue( ) != i )
				System.out.println( "Oops! " + i );
	}
}