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

/**
 * Encapsulates a JPEG sample component
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/09/2013
 */
public class Component {
	//
	private byte id;
	private byte hSampleFactor;
	private byte vSampleFactor;
	private byte qTableNumber;
	
	private byte acTableNumber;
	private byte dcTableNumber;
	
	Component(byte id, byte hSampleFactor, byte vSampleFactor, byte qTableNumber) {
		this.id = id;
		this.hSampleFactor = hSampleFactor;
		this.vSampleFactor = vSampleFactor;
		this.qTableNumber = qTableNumber;
	}
	
	public byte getACTableNumber() {
		return acTableNumber;
	}
	
	public byte getDCTableNumber() {
		return dcTableNumber;
	}
	
	public byte getId() {
		return id;		
	}
	
	public byte getHSampleFactor() {
		return hSampleFactor;
	}
	
	public byte getVSampleFactor() {
		return vSampleFactor;
	}
	
	public byte getQTableNumber() {
		return qTableNumber;
	}
	
	public void setACTableNumber(byte acTableNumber) {
		this.acTableNumber = acTableNumber;
	}
	
	public void setDCTableNumber(byte dcTableNumber) {
		this.dcTableNumber = dcTableNumber;
	}
}
