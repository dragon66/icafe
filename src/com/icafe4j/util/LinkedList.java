// LinkedList class
//
// CONSTRUCTION: with no initializer
// Access is via LinkedListItr class
//
// ******************PUBLIC OPERATIONS*********************
// boolean isEmpty( )     --> Return true if empty; else false
// void makeEmpty( )      --> Remove all items
// LinkedListItr zeroth( )--> Return position to prior to first
// LinkedListItr first( ) --> Return first position
// void insert( x, p )    --> Insert x after current iterator position p
// void remove( x )       --> Remove x
// LinkedListItr find( x )
//                        --> Return position that views x
// LinkedListItr findPrevious( x )
//                        --> Return position prior to x
// ******************ERRORS********************************
// No special errors

package com.icafe4j.util;// Temporarily put in this package

/**
 * Linked list implementation of the list
 * using a header node.
 * Access to the list is via LinkedListItr.
 * 
 * @author Mark Allen Weiss
 * @see LinkedListItr
 */
public class LinkedList<E>
{
    /**
     * Construct the list
     */
    public LinkedList( )
    {
        header = new ListNode<E>( null );
    }

    /**
     * Test if the list is logically empty.
     * @return true if empty, false otherwise.
     */
    public boolean isEmpty( )
    {
        return header.next == null;
    }

    /**
     * Make the list logically empty.
     */
    public void makeEmpty( )
    {
        header.next = null;
    }


    /**
     * Return an iterator representing the header node.
     */
    public LinkedListItr<E> zeroth( )
    {
        return new LinkedListItr<E>( header );
    }

    /**
     * Return an iterator representing the first node in the list.
     * This operation is valid for empty lists.
     */
    public LinkedListItr<E> first( )
    {
        return new LinkedListItr<E>( header.next );
    }

    /**
     * Insert after p.
     * @param x the item to insert.
     * @param p the position prior to the newly inserted item.
     */
    public void insert( E x, LinkedListItr<E> p )
    {
        if( p != null && p.current != null )
            p.current.next = new ListNode<E>( x, p.current.next );
    }

    /**
     * Return iterator corresponding to the first node containing an item.
     * @param x the item to search for.
     * @return an iterator; iterator isPastEnd if item is not found.
     */
    public LinkedListItr<E> find( E x )
    {
    	ListNode<E> itr = header.next; /* 1*/

    	while( itr != null && !itr.element.equals( x ) ) /* 2*/
    		itr = itr.next; /* 3*/

    	return new LinkedListItr<E>( itr ); /* 4*/ 
    }

    /**
     * Return iterator prior to the first node containing an item.
     * @param x the item to search for.
     * @return appropriate iterator if the item is found. Otherwise, the
     * iterator corresponding to the last element in the list is returned.
     */
    public LinkedListItr<E> findPrevious( E x )
    {
    	ListNode<E> itr = header; /* 1*/

    	while( itr.next != null && !itr.next.element.equals( x ) ) /* 2*/
    		itr = itr.next; /* 3*/

    	return new LinkedListItr<E>( itr ); /* 4*/
    }

    /**
     * Remove the first occurrence of an item.
     * @param x the item to remove.
     */
    public void remove( E x )
    {
        LinkedListItr<E> p = findPrevious( x );

        if( p.current.next != null )
            p.current.next = p.current.next.next;  // Bypass deleted node
    }

    // Simple print method
    private static <T> void printList( LinkedList<T> theList )
    {
        if( theList.isEmpty( ) )
            System.out.print( "Empty list" );
        else
        {
            LinkedListItr<T> itr = theList.first( );
            for( ; !itr.isPastEnd( ); itr.advance( ) )
                System.out.print( itr.retrieve( ) + " " );
        }

        System.out.println( );
    }

    private ListNode<E> header;


    public static void main( String [ ] args )
    {
        LinkedList<Integer>  theList = new LinkedList<Integer>( );
        LinkedListItr<Integer> theItr;
        int i;

        theItr = theList.zeroth( );
        printList( theList );

        for( i = 0; i < 10; i++ )
        {
            theList.insert( i , theItr );
            printList( theList );
            theItr.advance( );
        }

        for( i = 0; i < 10; i += 2 )
            theList.remove( i );

        for( i = 0; i < 10; i++ )
            if( ( i % 2 == 0 ) != ( theList.find( i ).isPastEnd( ) ) )
                System.out.println( "Find fails!" );

        System.out.println( "Finished deletions" );
        printList( theList );
    }
}