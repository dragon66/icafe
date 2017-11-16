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

import java.util.Calendar;

import com.icafe4j.util.Builder;

public class TIMEBuilder extends ChunkBuilder implements Builder<Chunk> {
	//
	private int year;
	private int month;
	private int day;
	private int hour;
	private int minute;
	private int second;
	
	public TIMEBuilder() {
		super(ChunkType.TIME);
	}
	
	public TIMEBuilder calendar(Calendar calendar) {
		this.year = calendar.get(Calendar.YEAR);
		this.month = calendar.get(Calendar.MONTH) + 1;
		this.day = calendar.get(Calendar.DAY_OF_MONTH);
		this.hour = calendar.get(Calendar.HOUR_OF_DAY);
		this.minute = calendar.get(Calendar.MINUTE);
		this.second = calendar.get(Calendar.SECOND);
		
		return this;
	}
	
	public TIMEBuilder year(int year) {
		if(year > Short.MAX_VALUE || year < Short.MIN_VALUE)
			throw new IllegalArgumentException("Year out of range: " + Short.MIN_VALUE + " - " +  Short.MAX_VALUE);
		this.year = year;
		return this;
	}
	
	public TIMEBuilder month(int month) {
		if(month > 12 || month < 1)
			throw new IllegalArgumentException("Month out of range: " + 1 + "-" + 12);
		this.month = month;
		return this;
	}
	
	public TIMEBuilder day(int day) {
		if(day > 31 || day < 1)
			throw new IllegalArgumentException("Day out of range: " + 1 + "-" + 31);
		this.day = day;
		return this;
	}
	
	public TIMEBuilder hour(int hour) {
		if(hour > 23 || hour < 0)
			throw new IllegalArgumentException("Hour out of range: " + 0 + "-" + 23);
		this.hour = hour;
		return this;
	}
	
	public TIMEBuilder minute(int minute) {
		if(minute > 59 || minute < 0)
			throw new IllegalArgumentException("Minute out of range: " + 0 + "-" + 59);
		this.minute = minute;
		return this;
	}
	
	public TIMEBuilder second(int second) {
		if(second > 60 || second < 0)
			throw new IllegalArgumentException("Second out of range: " + 0 + "-" + 60);
		this.second = second;
		return this;
	}
	
	/**
	 * Build the byte array representation of tIME chunk.
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
	@Override
	protected byte[] buildData() {
		return new byte[] {(byte)(year >>> 8), (byte)year, (byte)month, (byte)day, (byte)hour, (byte)minute, (byte)second};
	}
}
