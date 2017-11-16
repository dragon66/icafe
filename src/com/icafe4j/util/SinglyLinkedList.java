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
 * Singly linked list implementation.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.2 05/07/2007
 */
public class SinglyLinkedList<E> 
{
    private int count;                   
    private SinglyLinkedListNode<E> head;

    public SinglyLinkedList()
    {
        head = null;
        count = 0;
    }

    public void addToHead(E value)
    {
        head = new SinglyLinkedListNode<E>(value, head);
        count++;
    }

	public void add(E value)
	{
		addToHead(value);
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

	public E remove()
	{
		return removeFromHead();
	}

    public void addToTail(E value)
    {
        SinglyLinkedListNode<E> temp = head, previous = null;

		for (; temp != null; previous = temp, temp = temp.next());
		
		if(previous != null) previous.setNext( new SinglyLinkedListNode<E>(value,null));
        else head = new SinglyLinkedListNode<E>(value,null); 
		count++;
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

    public E peekHead()
    {  
        return (head != null)? head.getValue():null;
    }

    public E peekTail()
	{
        SinglyLinkedListNode<E> temp = head, previous = null;
		for (; temp != null; previous = temp, temp = temp.next());
		return (previous != null)? previous.getValue():null;
    }

    public boolean contains(E value)
    {
        SinglyLinkedListNode<E> temp = head;

		while (temp != null &&
               !temp.getValue().equals(value))
        {
            temp = temp.next();
        }
        return temp != null;
    }

	public E remove(E value)
    {
        SinglyLinkedListNode<E> temp = head, previous = null;
    
        while (temp != null &&
               !temp.getValue().equals(value))
        {
            previous = temp;
            temp = temp.next();
        }
        
        if (temp != null) {
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
    
    // Iteratively reverses the list
    public void reverse() {
        
    	if(head == null) { return;}
        
        SinglyLinkedListNode<E> currNode, nextNode , loopNode;
        currNode = head; nextNode = head.next; head.next = null;

        while(nextNode != null) {
            loopNode = nextNode.next;
            nextNode.next = currNode;
            currNode = nextNode;
            nextNode = loopNode;
        }
        
        head = currNode;
    }
    
    // Recursively reverses the list
    public void reverse(SinglyLinkedListNode<E> curr) {
        if(curr == null) return;
        if(curr.next == null) { // We are at the tail
            head = curr;
            return;
        }
        reverse(curr.next);
        curr.next.next = curr;
        curr.next = null;
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
    
    public void transferList(SinglyLinkedList<E> list)
	{
		list.head = head;
		list.count = count;
		clear();
	}    
}

