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
 * IFD.java
 *
 * Who   Date       Description
 * ====  =========  =======================================================================
 * WY    02Apr2014  Added setNextIFDOffset() to work with the case of non-contiguous IFDs
 * WY    30Mar2014  Added children map, changed write() method to write child nodes as well.
 */

package cafe.image.tiff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import cafe.io.RandomAccessOutputStream;

/**
 * Image File Directory
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/04/2013
 */
public final class IFD {
		
	public IFD() {}
	
	// Copy constructor
	public IFD(IFD other) {
		// Defensive copy
		this.children = Collections.unmodifiableMap(other.children);
		this.tiffFields = Collections.unmodifiableMap(other.tiffFields);
		this.startOffset = other.startOffset;
		this.endOffset = other.endOffset;
	}

	public void addChild(Tag tag, IFD child) {
		children.put(tag, child);
	}
	
	public void addField(TiffField<?> tiffField) {
		tiffFields.put(tiffField.getTag(), tiffField);
	}
	
	public void addFields(Collection<TiffField<?>> tiffFields) {
		for(TiffField<?> field : tiffFields) {
			addField(field);
		}
	}
	
	public int getEndOffset() {
		return endOffset;
	}
	
	public TiffField<?> getField(short tag) {
		return tiffFields.get(tag);
	}
	
	/** Get all the fields for this IFD from the internal map. */
	public Collection<TiffField<?>> getFields() {
		return Collections.unmodifiableCollection(tiffFields.values());
	}
	
	public int getSize() {
		return tiffFields.size();
	}
	
	public int getStartOffset() {
		return startOffset;
	}
	
	/** Remove a specific field associated with the given tag */
	public TiffField<?> removeField(short tag) {
		return tiffFields.remove(tag);
	}
	
	/** Remove all the entries from the IDF fields map */
	public void removeAllFields() {
		tiffFields.clear();
	}
	
	/**
	 * Set the next IFD offset pointer
	 * <p>
	 * Note: This should <em>ONLY</em> be called
	 * after the current IFD has been written to the RandomAccessOutputStream
	 *  
	 * @param os RandomAccessOutputStream
	 * @param nextOffset next IFD offset value
	 * @throws IOException
	 */
	public void setNextIFDOffset(RandomAccessOutputStream os, int nextOffset) throws IOException {
		os.seek(endOffset - 4);
		os.writeInt(nextOffset);
	}
	
	/** Write this IFD and all the children, if any, to the output stream
	 * 
	 * @param os RandomAccessOutputStream
	 * @param offset stream offset to write this IFD
	 * 
	 * @throws IOException
	 */
	public int write(RandomAccessOutputStream os, int offset) throws IOException {
		startOffset = offset;
		// Write this IFD and its children, if any, to the RandomAccessOutputStream
		List<TiffField<?>> list = new ArrayList<TiffField<?>>(tiffFields.values());
		// Make sure tiffFields are in incremental order.
		Collections.sort(list);
		os.seek(offset);
		os.writeShort(list.size());
		offset += 2;
		endOffset = offset + list.size() * 12 + 4;			
		// The first available offset to write tiffFields. 
		int toOffset = endOffset;
		os.seek(offset); // Set first field offset.
				
		for (TiffField<?> tiffField : list)
		{
			toOffset = tiffField.write(os, toOffset);
			offset += 12; // Move to next field. Each field is of fixed length 12.
			os.seek(offset); // Reset position to next directory field.
		}
		
		/* Set the stream position at the end of the IFD to update
		 * next IFD offset
		 */
		os.seek(offset);
		os.writeInt(0);	// Set next IFD offset to default 0 
		
		// Write sub IFDs if any (we assume bare-bone sub IFDs pointed by long field type with no image data associated)
		if(children.size() > 0) {
			for (Map.Entry<Tag, IFD> entry : children.entrySet()) {
			    Tag key = entry.getKey();
			    IFD value = entry.getValue();
			    // Update parent field if present, otherwise skip
			    TiffField<?> tiffField = this.getField(key.getValue());
			    if(tiffField != null) {
			    	int dataPos = tiffField.getDataOffset();
					os.seek(dataPos);
					os.writeInt(toOffset);
					os.seek(toOffset);
					toOffset = value.write(os, toOffset);
			    }
		    }
		}
			
		return toOffset;
	}
	
	/**
	 * Create a children map for sub IFDs. A sub IFD is associated with a tag of the current IFD
	 * which serves as pointer to the sub IFD.
	 */	 
	private Map<Tag, IFD> children = new HashMap<Tag, IFD>();
	/** Create a fields map to hold all of the fields for this IFD */
	private Map<Short, TiffField<?>> tiffFields = new HashMap<Short, TiffField<?>>();
	private int endOffset;
	private int startOffset;
}