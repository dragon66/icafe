/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.meta;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Base class for image metadata.
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/12/2015
 */
public class Metadata {
	private MetadataType type;
	private byte[] data;
	
	public Metadata(MetadataType type, byte[] data) {
		this.type = type;
		this.data = data;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public MetadataType getType() {
		return type;
	}

	/**
	 * Writes the metadata out to the output stream
	 * 
	 * @param out OutputStream to write the metadata to
	 * @throws IOException
	 */
	public void write(OutputStream out) throws IOException {
		out.write(data);
	}
}
