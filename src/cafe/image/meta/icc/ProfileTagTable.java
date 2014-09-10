/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.meta.icc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cafe.io.IOUtils;
import cafe.io.RandomAccessInputStream;

/**
 * ICC Profile Tag Table
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 06/16/2014
 */
public class ProfileTagTable {
	private int tagCount;
	private Map<Integer, TagEntry> tagEntries = new HashMap<Integer, TagEntry>();
	
	public static class TagEntry implements Comparable<TagEntry> {
		private int profileTag;
		private int dataOffset;
		private int dataLength;
		private byte[] data;
		
		public TagEntry(int profileTag, int dataOffset, int dataLength, byte[] data) {
			this.profileTag  = profileTag;
			this.dataOffset = dataOffset;
			this.dataLength = dataLength;
			this.data = data;
		}
		
		@Override
		public int compareTo(TagEntry o) {
			return (int)((this.profileTag&0xffffffffL) - (o.profileTag&0x0ffffffffL));
		}
		
		public int getProfileTag() {
			return profileTag;
		}
		
		public int getDataOffset() {
			return dataOffset;
		}
		
		public int getDataLength() {
			return dataLength;
		}
		
		public byte[] getData() {
			return data;
		}		
	}
	
	public ProfileTagTable() {}
	
	public void addTagEntry(TagEntry tagEntry) {
		tagEntries.put(tagEntry.getProfileTag(), tagEntry);
	}
	
	public void read(RandomAccessInputStream is) throws IOException {
		tagCount = IOUtils.readIntMM(is);
		// Read each tag
		for(int i = 0; i < tagCount; i++) {
			int tagSignature = IOUtils.readIntMM(is);
			ProfileTag tag = ProfileTag.fromInt(tagSignature);
			int dataOffset = is.readInt();
			int dataLength = is.readInt();
			long streamOffset = is.getStreamPointer();
			is.seek(dataOffset);
			
			byte[] temp = new byte[dataLength];
			is.read(temp);
			
			is.seek(streamOffset);
			
			tagEntries.put(tagSignature, new TagEntry(tag.getValue(), dataOffset, dataLength, temp));
		}
	}
	
	public int getTagCount() {
		return tagCount;
	}
	
	public List<TagEntry> getTagEntries() {
		return new ArrayList<TagEntry>(tagEntries.values());
	}
	
	public void showTable() {
		System.out.println("*** Start of ICC_Profile Tag Table ***");
		System.out.println("Tag Count: " + tagCount);
		List<TagEntry> list = getTagEntries();
		Collections.sort(list);
		int count = 0;
		for(TagEntry tagEntry:list) {
			System.out.print("Tag# " + count++);
			System.out.print(", Tag Name: " + ProfileTag.fromInt(tagEntry.getProfileTag()));
			System.out.print(", Data Offset: " + tagEntry.getDataOffset());
			System.out.println(", Data Length: " + tagEntry.getDataLength());
		}
		System.out.println("*** End of ICC_Profile Tag Table ***");
	}
}