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

package com.icafe4j.image.png;

import java.io.IOException;

import com.icafe4j.util.Reader;

public class TIMEReader implements Reader {
	//
	private int year;
	private int month;
	private int day;
	private int hour;
	private int minute;
	private int second;
	private Chunk chunk;
	
	public TIMEReader(Chunk chunk) {
		if(chunk == null) throw new IllegalArgumentException("Input chunk is null");
		
		if (chunk.getChunkType() != ChunkType.TIME) {
			throw new IllegalArgumentException("Not a valid tIME chunk.");
		}
		
		this.chunk = chunk;
		
		try {
			read();
		} catch (IOException e) {
			throw new RuntimeException("TIMEReader: error reading chunk");
		}
	}
	
	public int getDay() {
		return day;
	}
	
	public int getHour() {
		return hour;
	}
	
	public int getMinute() {
		return minute;
	}
	
	public int getMonth() {
		return month;
	}
	
	public int getSecond() {
		return second;
	}
	
	public int getYear() {
		return year;		
	}
	
	/**
	 * Read the tIME chunk.
	 * <p>
	 * The tIME chunk gives the time of the last image modification (not the time of initial image creation). It contains: 
	 * <pre>
	 *  Year:   2 bytes (complete; for example, 1995, not 95)
	 *  Month:  1 byte (1-12)
	 *  Day:    1 byte (1-31)
	 *  Hour:   1 byte (0-23)
	 *  Minute: 1 byte (0-59)
	 *  Second: 1 byte (0-60) (yes, 60, for leap seconds; not 61, a common error)
	 *  </pre>
	 */   
	public void read() throws IOException {
		byte[] data = chunk.getData();
		
		if(data.length < 7) throw new RuntimeException("TimeReader: input data too short");
		
		this.year = ((data[0]&0xff) << 8 | data[1]&0xff); // Unsigned short (Motorola)
		this.month = data[2]&0xff;
		this.day = data[3]&0xff;
		this.hour = data[4]&0xff;
		this.minute = data[5]&0xff;
		this.second = data[6]&0xff;
	}
}
