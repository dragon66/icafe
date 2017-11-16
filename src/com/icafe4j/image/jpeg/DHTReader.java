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

package com.icafe4j.image.jpeg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.util.Reader;

/**
 * JPEG DHT segment reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/12/2013
 */
public class DHTReader implements Reader {
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(DHTReader.class);
		
	private Segment segment;
	private List<HTable> dcTables = new ArrayList<HTable>(3);
	private List<HTable> acTables = new ArrayList<HTable>(3);
	
	public DHTReader(Segment segment) throws IOException {
		//
		if(segment.getMarker() != Marker.DHT) {
			throw new IllegalArgumentException("Not a valid DHT segment!");
		}
		
		this.segment = segment;
		read();
	}
	
	public List<HTable> getDCTables() {
		return dcTables;
	}
		
	public List<HTable> getACTables() {
		return acTables;
	}	
	
	public void read() throws IOException {
		//
		byte[] data = segment.getData();		
		int len = segment.getLength();
		len -= 2;//
		
		int offset = 0;
		
		while (len > 0)
		{
			int HT_info = data[offset++];
			
			int HT_class = (HT_info>>4)&0x01;// 0=DC table, 1=AC table
			int HT_destination_id = (HT_info&0x0f);// Huffman tables number
			byte[] bits = new byte[16];
			byte[] values;
           
			int count = 0;
			
			for (int i = 0; i < 16; i++)
			{
				bits[i] = data[offset + i];
				count += (bits[i]&0xff);
			}
						
            if (count > 256)
			{
				LOGGER.error("invalid huffman code count!");			
				return;
			}
            
            offset += 16;
            
            values = new byte[count];
			
      		for (int i=0; i<count; i++)
			{
                values[i] = data[offset + i];
			}
      		
      		offset += count;			
			len -= (1+16+count);
			
			HTable table = new HTable(HT_class, HT_destination_id, bits, values);
			
			if(HT_class == HTable.DC_CLAZZ) {
				dcTables.add(table);
			}
			else if(HT_class == HTable.AC_CLAZZ) {
				acTables.add(table);
			}
			else {
				LOGGER.error("Invalid component class value: " + HT_class);
				return;
			}			
		}
	}
}
