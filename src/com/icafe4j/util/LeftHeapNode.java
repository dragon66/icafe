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
 * Basic node stored in leftist heaps
 * Note that this class is not accessible outside
 * of the package.

 * @author Mark Allen Weiss
 */
class LeftHeapNode<E>
{
     // Constructors
     LeftHeapNode( E theElement )
     {
         this( theElement, null, null );
     }

     LeftHeapNode( E theElement, LeftHeapNode<E> lt, LeftHeapNode<E> rt )
     {
         element = theElement;
         left    = lt;
         right   = rt;
         npl     = 0;
     }

     // Friendly data; accessible by other package routines
     E   element;               // The data in the node
     LeftHeapNode<E> left;         // Left child
     LeftHeapNode<E> right;        // Right child
     int          npl;          // null path length
}
