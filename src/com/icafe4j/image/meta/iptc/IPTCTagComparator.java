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
