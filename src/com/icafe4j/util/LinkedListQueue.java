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
 * Queue implementation using singly linked list.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 05/07/2007
 */
public class LinkedListQueue<E> 
{
    private SinglyLinkedList<E> list;

    public LinkedListQueue()
    {
        list = new SinglyLinkedList<E>();
    }

    public void enqueue(E value)
    {
        list.addToHead(value);
    }

    public E dequeue()
    {
		return list.removeFromTail();
    }

    public E peek()
    {  
        return list.peekTail();
    }

    public boolean contains(E value)
    {
        return list.contains(value);
    }

	public E remove(E value)
    {
        return list.remove(value);
    }

    public boolean isEmpty()
	{
		return list.isEmpty();
	}

    public int size()
    {
        return list.size();
    }

    public void clear()
    {
		list.clear();
    }
}