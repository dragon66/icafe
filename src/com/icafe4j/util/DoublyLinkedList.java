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
 * Doubly linked list implementation.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.2 05/09/2007
 */
public class DoublyLinkedList<E> 
{
    private int count;   
	private DoublyLinkedListNode<E> head, tail;
	
	/** Here we maintain two unused list nodes
	 *  which point to head and tail.
	 */
	private final DoublyLinkedListNode<E>
		                SENTINEL1 = new DoublyLinkedListNode<E>(null);
	private final DoublyLinkedListNode<E>
		                SENTINEL2 = new DoublyLinkedListNode<E>(null);
    
	// Constructor	
    public DoublyLinkedList()
    {
   		clear();
    }

    public void addToHead(E value)
    {
        DoublyLinkedListNode<E> temp
		     = new DoublyLinkedListNode<E>(value, head, head.next());
		head.setNext(temp);
		temp.next().setPrev(temp);
		count++;
    }

    public void add(E value)
	{
		addToHead(value);
	}

    public E removeFromHead()
    {
		E value = null;
        if ( !isEmpty() )
        {
			DoublyLinkedListNode<E> temp = head.next();
			head.setNext(temp.next());
		    temp.next().setPrev(head);
		    value = temp.getValue();
			count--;
		}
        return value;
    }

	public E remove()
	{
		return removeFromHead();
	}

    public void addToTail(E value)
    {
        DoublyLinkedListNode<E> temp
			 = new DoublyLinkedListNode<E>(value, tail.prev(), tail); 
		tail.setPrev(temp);
		temp.prev().setNext(temp);
		count++;
    }

	public E removeFromTail()
    {
		E value = null;
		if(!isEmpty())
		{
            DoublyLinkedListNode<E> temp = tail.prev();
  		  	tail.setPrev(temp.prev());
		    temp.prev().setNext(tail);
		    value = temp.getValue();
			count--;
        }
	    return value;
	}

    public E peekHead()
    {  
        return (!isEmpty())? head.next().getValue():null;
    }

    public E peekTail()
	{
        return (!isEmpty())? tail.prev().getValue():null;
    }

    public boolean contains(E value)
    {
        DoublyLinkedListNode<E> temp = head.next();

		while (temp != null &&
               !temp.getValue().equals(value))
        {
            temp = temp.next();
        }
        return temp != null;
    }

	public E remove(E value)
    {
        DoublyLinkedListNode<E> temp = head.next(), prev = head;
    
        while (temp != null &&
               !temp.getValue().equals(value))
        {
            prev = temp;
            temp = temp.next();
        }
        
        if (temp != null) {
			prev.setNext(temp.next());
            if (temp.next() != null)
				temp.next().setPrev(prev);
			count--;
            return temp.getValue();
        }
		return null;
    }

    public boolean isEmpty()
	{
		return (count == 0);
	}

	@SuppressWarnings("unchecked")
	public E[] toArray(E[] a) {
        if (a.length < count)
			a = (E[])java.lang.reflect.Array.newInstance(
                                a.getClass().getComponentType(), count);

	    int i = 0;
        DoublyLinkedListNode<E> e = head.next();
		
		for (; e != tail; e = e.next())
            a[i++] = e.getValue();

        if (a.length > count)
            a[count] = null;

        return a;
    }

    public void transferList(DoublyLinkedList<E> list)
	{
		list.head = head;
		list.tail = tail;
		list.count = count;
		clear();
	}	

    public int size()
    {
        return count;
    }
    // Make the list logically empty
    public void clear()
    {
   		head = SENTINEL1;
		tail = SENTINEL2;
		head.setNext(tail);
		tail.setPrev(head);
		count = 0;
    }
	// Test program
    public static void main( String [ ] args )
    {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<Integer>( );
        final int NUMS = 4000;

        System.out.println( "Checking... (no more output means success)" );

        for( int i = 0; i < NUMS; i++)
            list.addToTail( new Integer( i ) );

        for( int i = NUMS-1; i >0; i-- )
		{
			if( list.peekTail( ) != i) 
			    System.out.println( "peekHead() error!" );
            if( list.removeFromTail( ) != i) 
                System.out.println( "removdeFromHead() error!" );
		}
        list.clear();
        for( int i = 0; i < NUMS; i++)
            list.addToTail( new Integer( i ) );

		for( int i = 0; i < NUMS; i++ )
		{
			if( list.peekHead( ) != i) 
			    System.out.println( "peekHead() error!" );
            if( list.removeFromHead( ) != i) 
                System.out.println( "removdeFromHead() error!" );
		}
    }
}