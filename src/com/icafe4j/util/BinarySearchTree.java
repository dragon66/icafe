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
 * Implements an unbalanced binary search tree.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 05/05/2007
 * 
 * Based on BinarySearchTree.java by
 * @author Mark Allen Weiss
 */
public class BinarySearchTree<E extends Comparable<? super E>>
{
    /** The tree root. */
    private BinaryTreeNode<E> root;

	/**
     * Construct the tree.
     */
    public BinarySearchTree( )
    {
        root = null;
    }

    /**
     * Insert into the tree.
     * @param val the item to insert.
     */
    public void insert( E val )
    {
        root = insert( val, root );
    }

    /**
     * Remove from the tree..
     * @param val the item to remove.
     */
    public void remove( E val )
    {
       root = remove( val, root );
    }

	/**
     * Remove minimum item from the tree.
     */
    public void removeMinItem( )
    {
        root = removeMinItem( root );
    }

    /**
     * Find the smallest item in the tree.
     * @return the smallest item or null if empty.
     */
    public E findMinItem( )
    {
        return valueOf( findMinItem( root ) );
    }

    /**
     * Find the val of the largest item in the tree.
     * @return the largest item or null if empty.
     */
    public E findMaxItem( )
    {
        return valueOf( findMaxItem( root ) );
    }

    /**
     * Find an item in the tree.
     * @param val the item to search for.
     * @return the matching item or null if not found.
     */
    public E find( E val )
    {
        return valueOf( find( val, root ) );
    }

    /**
     * Make the tree logically empty.
     */
    public void makeEmpty( )
    {
        root = null;
    }

    /**
     * Test if the tree is logically empty.
     * @return true if empty, false otherwise.
     */
    public boolean isEmpty( )
    {
        return root == null;
    }

    /**
     * Internal method to get value field of the node.
     * @param n the node.
     * @return the value field or null if n is null.
     */
    E valueOf( BinaryTreeNode<E> n )
    {
        return n == null ? null : n.val;
    }

    /**
     * Internal method to insert into a subtree.
     * @param val the item to insert.
     * @param n the node that roots the tree.
     * @return the new root.
     */
    BinaryTreeNode<E> insert( E val, BinaryTreeNode<E> n )
    {
		if ( n == null)	n = new BinaryTreeNode<E>(val);
		else
		{
			BinaryTreeNode<E> parent, curr;
			for ( parent = null, curr = n; 
			      (curr != null)&&(val.compareTo(curr.val)!=0);
		            parent = curr, curr = (val.compareTo(curr.val))<0? curr.left:curr.right );
			
		    if( (curr != null)) // Duplicate item, increase the counter
				curr.freq++;  
            
			else if (val.compareTo(parent.val)<0) 
				parent.left = new BinaryTreeNode<E>( val );
            else 
				parent.right = new BinaryTreeNode<E>(val);
		}
		return n;
    }

    /**
     * Internal method to remove from a subtree.
     * @param val the item to remove.
     * @param n the node that roots the tree.
     * @return the new root.
     */
    BinaryTreeNode<E> remove( E val, BinaryTreeNode<E> n )
    {
		BinaryTreeNode<E> parent, curr;
		// Try to find the matching item
        for ( parent = null, curr = n;(curr != null)&&(val.compareTo(curr.val) != 0);
		             parent = curr, curr = (val.compareTo(curr.val))<0? 
				         curr.left:curr.right );

		if (curr == null) {;}// Not found, do nothing
		// The matching item has two children
		else if( curr.left != null && curr.right != null ) 
        {
            BinaryTreeNode<E> pre, t;
			// Find the min item in the subtree rooted at curr.right
            for ( pre = null, t = curr.right; t.left != null; pre = t, t = t.left );
			
          	if ( pre == null ) 
			{
				curr.val = curr.right.val;
				curr.right = curr.right.right;
			}
			else
			{
				curr.val = pre.left.val;
				pre.left = pre.left.right;
			}
        }
		// The matching item has only one child 
		else if ( parent != null )// The matching item is not root
		{
		     if ( curr.val.compareTo(parent.val)<0 )
				 parent.left = (curr.left != null)? curr.left:curr.right;
		     else parent.right = (curr.left != null)? curr.left:curr.right;
		}
		/** The matching item is the root of this subtree, 
		 *  remove it and return the new root
		 */
		else return (curr.left != null)? curr.left:curr.right;
		// Return the old root of the subtree
		return n;
	}

	/**
     * Internal method to remove minimum item from a subtree.
     * @param n the node that roots the tree.
     * @return the new root.
     */
    BinaryTreeNode<E> removeMinItem( BinaryTreeNode<E> n )
    {
		BinaryTreeNode<E> parent, curr;

        if( n == null ) return null;// Empty subtree
		// Find the minimum item in this subtree
		for ( parent = null, curr = n; curr.left != null; parent = curr, curr = curr.left );
        // The matching item is a leaf
		if (parent != null)  
		{
			parent.left = parent.left.right;
			return n;
		}
		// The matching item is the root itself
		return n.right;
    }    
	
	/**
     * Internal method to find the smallest item in a subtree.
     * @param n the node that roots the tree.
     * @return node containing the smallest item.
     */
    BinaryTreeNode<E> findMinItem( BinaryTreeNode<E> n )
    {
        if( n != null )
            while( n.left != null )
                n = n.left;

        return n;
    }

    /**
     * Internal method to find the largest item in a subtree.
     * @param n the node that roots the tree.
     * @return node containing the largest item.
     */
    BinaryTreeNode<E> findMaxItem( BinaryTreeNode<E> n )
    {
        if( n != null )
            while( n.right != null )
                n = n.right;

        return n;
    }

    /**
     * Internal method to find an item in a subtree.
     * @param val is the item to search for.
     * @param n the node that roots the tree.
     * @return node containing the matched item or null if no matching found.
     */
    BinaryTreeNode<E> find( E val, BinaryTreeNode<E> n )
    {
        for (; (n != null)&&(val.compareTo(n.val)!=0); 
		              n = val.compareTo(n.val)<0? n.left:n.right );
		return n;               
	}

          // Test program
    public static void main( String [ ] args )
    {
        BinarySearchTree<Integer> t = new BinarySearchTree<Integer>( );
        final int NUMS = 4000;
        final int GAP  =   37;

        System.out.println( "Checking... (no more output means success)" );

        for( int i = GAP; i != 0; i = ( i + GAP ) % NUMS )
            t.insert( new Integer( i ) );

        for( int i = 1; i < NUMS; i+= 2 )
            t.remove( new Integer( i ) );

        if( (t.findMinItem( )).intValue( ) != 2 ||
            (t.findMaxItem( )).intValue( ) != NUMS - 2 )
            System.out.println( "FindMin or FindMax error!" );

        for( int i = 2; i < NUMS; i+=2 )
             if( (t.find( new Integer( i ) )).intValue( ) != i )
                 System.out.println( "Find error1!" );

        for( int i = 1; i < NUMS; i+=2 )
        {
            if( t.find( new Integer( i ) ) != null )
                System.out.println( "Find error2!" );
        }
    }
}