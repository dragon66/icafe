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
 * Binary tree node.  
 *
 * Adapted by
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/28/2007
 * 
 * @author Mark Allen Weiss
 */
class BinaryTreeNode<E>
{
	int freq;
	E val;      // The data in the node
    BinaryTreeNode<E> left; // Left child
    BinaryTreeNode<E> right;// Right child
    // Constructors
    BinaryTreeNode( E val )
    {
        this.val = val;
		this.freq = 1;
        left = right = null;
    }
}