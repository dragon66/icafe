/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.image.meta.png;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.png.Chunk;
import com.icafe4j.image.png.ChunkType;
import com.icafe4j.image.png.TIMEBuilder;
import com.icafe4j.image.png.TIMEReader;

public class TIMEChunk extends Metadata {
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(TIMEChunk.class);

	private static MetadataType validate(ChunkType chunkType) {
		if(chunkType == null) throw new IllegalArgumentException("ChunkType is null");
		if(chunkType == ChunkType.TIME)
			return MetadataType.PNG_TIME;
		throw new IllegalArgumentException(
				"Input ChunkType is not tIME chunk!");
	}
	
	private static void checkDate(int year, int month, int day, int hour, int minute, int second) {
		if(year > Short.MAX_VALUE || year < Short.MIN_VALUE)
			throw new IllegalArgumentException("Year out of range: " + Short.MIN_VALUE + " - " +  Short.MAX_VALUE);
		if(month > 12 || month < 1)
			throw new IllegalArgumentException("Month out of range: " + 1 + "-" + 12);
		if(day > 31 || day < 1)
			throw new IllegalArgumentException("Day out of range: " + 1 + "-" + 31);
		if(hour > 23 || hour < 0)
			throw new IllegalArgumentException("Hour out of range: " + 0 + "-" + 23);
		if(minute > 59 || minute < 0)
			throw new IllegalArgumentException("Minute out of range: " + 0 + "-" + 59);
		if(second > 60 || second < 0)
			throw new IllegalArgumentException("Second out of range: " + 0 + "-" + 60);
	}
	
	private static final String[] MONTH = 
		{"", "January", "Febrary", "March", "April",
         "May", "June", "July", "August", "September", "October",
         "November", "December"
    };
	
	private Chunk chunk;
	private int year;
	private int month;
	private int day;
	private int hour;
	private int minute;
	private int second;
	
	public TIMEChunk(Chunk chunk) {
		super(validate(chunk.getChunkType()), chunk.getData());
		this.chunk = chunk;
		ensureDataRead();
	}

	public TIMEChunk(ChunkType chunkType, int year, int month, int day, int hour, int minute, int second) {
		super(validate(chunkType));
		checkDate(year, month, day, hour, minute, second);
		this.year = year;
		this.month = month;
		this.day = day;
		this.hour = hour;
		this.minute = minute;
		this.second = second;
		isDataRead = true;
	}
	
	public Chunk getChunk() {
		if(chunk == null)
			chunk = new TIMEBuilder().year(year).month(month).day(day).hour(hour).minute(minute).second(second).build();
	
		return chunk;
	}

	public byte[] getData() {
		return getChunk().getData();
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
	
	public void read() throws IOException {
		if(!isDataRead) {
			TIMEReader reader = new TIMEReader(chunk);
			this.year = reader.getYear();
			this.month = reader.getMonth();;
			this.day = reader.getDay();
			this.hour = reader.getHour();
			this.minute = reader.getMinute();
			this.second = reader.getSecond();
			isDataRead = true;
		}
	}

	@Override
	public void showMetadata() {
		LOGGER.info("PNG tIME chunk starts =>");
		LOGGER.info("{} {} {}, {}:{}:{} UTC (Time of last modification)", day, (month > 0 && month <= 12)? MONTH[month]:"()", year, hour, minute, second);
		LOGGER.info("PNG tIME chunk ends <=");
	}
	
	public void write(OutputStream os) throws IOException {
		getChunk().write(os);
	}
}
