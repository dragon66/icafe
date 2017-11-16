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
 * Sorted singly linked list implementation.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 05/03/2007
 */
public class SortedSinglyLinkedList<E extends Comparable<? super E>>
{
	private int count;                   
	private SinglyLinkedListNode<E> head;
	
	public SortedSinglyLinkedList() { 
		head = null;
        count = 0;
	}

    public void add(E content)
	{
		SinglyLinkedListNode<E> prev, curr;
		
		if ( head == null ) head = new SinglyLinkedListNode<E>(content);
		
		else 
		{
		   for (prev = null, curr = head; (curr != null) && (content.compareTo(curr.getValue()) > 0);
		                              prev = curr, curr = curr.next());
           /** Duplication is not allowed 
		   if (curr == null)// Insert as new tail
			   prev.setNext(new SinglyLinkedListNode<E>(content));
		   else if(content.compareTo(curr.content)==0) {
			   curr.freq++;// Duplicate item, increase the counter of this node
			   count--;
		   }
		   else if (prev != null)
			   prev.setNext(new SinglyLinkedListNode<E>(content, curr));
		   else// Insert as new head
			   head = new SinglyLinkedListNode<E>(content, head);
		   */
		   // Duplication is allowed
		   if (prev != null)
			   prev.setNext(new SinglyLinkedListNode<E>(content, curr));
		   else// Insert as new head
			   head = new SinglyLinkedListNode<E>(content, head);
		}
		count++;
	}

	public E removeFromHead()
    {
		SinglyLinkedListNode<E> temp = head;

		if(head != null)
		{
       	   head = head.next();
		   count--;
           return temp.getValue();
		}
		return null;
    }
	
	public E removeFromTail()
    {
        SinglyLinkedListNode<E> temp = head,previous = null;

		if(temp != null)
		{
		  while (temp.next() != null) 
          {
            previous = temp;
            temp = temp.next();
          }
		  if (previous != null) previous.setNext(null);
		  else head = null;

		  count--;
		  return temp.getValue();
        }
	    return null;
	}

   /**
    * Finds the first occurrence of an item.
    * 
    * @param value the item to search for.
    * @return true if the item is found or false otherwise.
    */
	public boolean contains(E value)
    {
        SinglyLinkedListNode<E> temp = head;

		while (temp != null &&
               temp.getValue().compareTo(value) < 0)
        {
            temp = temp.next();
        }
        return (temp != null && temp.getValue().compareTo(value) == 0);
    }
   
   /**
    * Removes the first occurrence of an item.
    * 
    * @param value the item to remove.
    * @return the removed item or null if the item is not found.
    */
	public E remove(E value)
    {
        SinglyLinkedListNode<E> temp = head, previous = null;
    
        while (temp != null &&
               temp.getValue().compareTo(value) < 0)
        {
            previous = temp;
            temp = temp.next();
        }
        
        if (temp != null && temp.getValue().compareTo(value) == 0) {
			if (previous != null) 
				previous.setNext(temp.next());
			else
			    head = temp.next();
            count--;
            return temp.getValue();
        }
		return null;
    }
	
	public boolean isEmpty()
	{
		return (head == null);
	}
	
	public int size()
    {
        return count;
    }

    public void clear()
    {
		head = null;
        count = 0;
    }
    
    @SuppressWarnings("unchecked")
	public E[] toArray(E[] a) {
        if (a.length < count)
			a = (E[])java.lang.reflect.Array.newInstance(
                                a.getClass().getComponentType(), count);

	    int i = 0;
        SinglyLinkedListNode<E> e = head;
		
		for (; e != null; e = e.next())
            a[i++] = e.getValue();

        if (a.length > count)
            a[count] = null;

        return a;
    }

	// Test
	public static void main(String args[])
	{
		SortedSinglyLinkedList<Integer> t = new SortedSinglyLinkedList<Integer>();
		
        final int NUMS = 100;
        final int GAP  =   37;

        for( int i = GAP; i != 0; i = ( i + GAP ) % NUMS )
            t.add(i);
        
		// Try to add a duplicate item
		t.add(5);
        
		for (int i = 1; i < NUMS; i++ )
		{
			while (t.contains(i))
			{
				System.out.println(t.remove(i));
			}			
		}
		
		if (t.isEmpty() && t.size()==0)
		{
			System.out.println("List is empty now...");
		}
	}
}