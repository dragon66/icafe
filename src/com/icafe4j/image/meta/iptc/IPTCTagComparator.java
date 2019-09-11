/**
 * COPYRIGHT (C) 2014-2019 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.image.meta.iptc;

import java.util.Comparator;

public class IPTCTagComparator implements Comparator<IPTCTag> {

	public int compare(IPTCTag o1, IPTCTag o2) {	
		final int BEFORE = -1;
	    final int EQUAL = 0;
	    final int AFTER = 1;
	    
	    if(o1 == o2) return EQUAL;
	    
	    if (o1.getRecordNumber() < o2.getRecordNumber()) return BEFORE;
	    if (o1.getRecordNumber() > o2.getRecordNumber()) return AFTER;
	    if(o1.getRecordNumber() == o2.getRecordNumber()) {
	    	if (o1.getTag() < o2.getTag()) return BEFORE;
		    if (o1.getTag() > o2.getTag()) return AFTER;
		    return EQUAL;
	    }
	
		return EQUAL;
	}
}
