/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 * 
 * Change History - most recent changes go on top of previous changes
 *
 * NativeMetadata.java
 *
 * Who   Date         Description
 * ====  =========    ===============================================
 * WY    18Mar2015    Added showMetadata()
 */

package cafe.image.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Metadata for native image formats
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/06/2015
 */
public abstract class NativeMetadata<T> {
	private List<T> metadataList;
	
	public NativeMetadata() {
		;
	}
	
	public NativeMetadata(List<T> meta) {
		metadataList = meta;
	}
	
	public void addMeta(T meta) {
		if(metadataList == null)
			metadataList = new ArrayList<T>();
		metadataList.add(meta);
	}
	
	public List<T> getMetadataList() {
		return Collections.unmodifiableList(metadataList);
	}
	
	public abstract String getMimeType();
	public abstract void showMetadata();	
}