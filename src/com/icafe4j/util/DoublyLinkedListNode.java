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
 * Doubly linked list node implementation.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 05/09/2007
 */ 
class DoublyLinkedListNode<E>
{
    E value; 
	int freq;
	DoublyLinkedListNode<E> prev;
    DoublyLinkedListNode<E> next; 

    DoublyLinkedListNode(E value, DoublyLinkedListNode<E> prev, DoublyLinkedListNode<E> next)
    {
        this.value = value;
		this.freq = 0;
		this.prev = prev;
        this.next = next;
    }

    DoublyLinkedListNode(E value)
    {
        this(value, null, null);
    }

    E getValue()
    {
        return value;
    }

    void setValue(E value)
    {
        this.value = value;
    }

	DoublyLinkedListNode<E> prev()
	{
		return prev;
	}

	void setPrev(DoublyLinkedListNode<E> prev)
    {
        this.prev = prev;
    }

	DoublyLinkedListNode<E> next()
	{
		return next;
	}

	void setNext(DoublyLinkedListNode<E> next)
    {
        this.next = next;
    }
}