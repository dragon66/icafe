/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * IPTCReader.java
 *
 * Who   Date       Description
 * ====  =======    ============================================================
 * WY    12Jan2015  Initial creation to read IPTC information
 */

package cafe.image.meta.iptc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cafe.util.Reader;

/**
 * IPTC image metadata reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/12/2015
 */
public class IPTCReader implements Reader {
	private List<IPTCDataSet> datasetList = new ArrayList<IPTCDataSet>();
	
	@Override
	public void read() throws IOException {
		// TODO Auto-generated method stub
	}
	
	public List<IPTCDataSet> getDataSet() {
		return Collections.unmodifiableList(datasetList);
	}
}
