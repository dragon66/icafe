/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * Slice.java
 *
 * Who   Date       Description
 * ====  =========  ===============================================================
 */

package com.icafe4j.image.meta.adobe;

@SuppressWarnings("unused")
public class Slice {	
	private int id;
	private int groupId;
	private int origin;
	private int associatedLayerId;
	private String name; // Unicode string	 	
	private int type;
	private int left;
	private int top;
	private int right;
	private int bottom;
	private String URL; // Unicode string
	private String target; // Unicode string
	private String message; // Unicode string
	private String altTag;// Unicode string
	private boolean isCellTextHTML;
	private String cellText; // Unicode string
	private int horiAlignment;
	private int vertAlignment;
	private int alpha;
	private int red;
	private int green;
	private int blue;
	private byte[] extraData;
	private int descriptorVersion;
	private Descriptor descriptor;
	
	public static final class Descriptor {
		private String name; // Unicode string
		private int numOfItems;
	}
}
