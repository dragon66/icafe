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

package com.icafe4j.image.compression.ccitt;

import java.io.OutputStream;

import com.icafe4j.image.compression.ImageEncoder;
import com.icafe4j.util.Updatable;

/**
 * CCITT Group 3 two dimensional encoding
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/02/2014
 */
public class G32DEncoder extends G31DEncoder implements ImageEncoder {

	private int k;
	
	// K should be greater than 1
	public G32DEncoder(OutputStream os, int scanLineWidth, int buf_length, int k, Updatable<Integer> writer) {
		super(os, scanLineWidth, buf_length, writer);
		if(k < 2) throw new IllegalArgumentException("Invalid k value: " + k);
		this.k = k;
	}
	
	/**
	 * This method assumes "len" is a multiplication of scan line length.
	 * 
	 * @param len the number of pixels to be encoded
	 */
	@Override public void encode(byte[] pixels, int start, int len) throws Exception {	
		// Calculate how many blocks of k lines we need to encode
		int totalScanLines = len/scanLineWidth;
		int numOfKLines = totalScanLines/k;
		int leftOver = totalScanLines%k;
		// We first encode each block of k lines
		for(int i = 0; i < numOfKLines; i++) {
			start = encodeKLines(pixels, start);
		}
		
		// Then deal with the leftover lines		
		if(leftOver > 0) {
			// Encode a one dimensional scan line first
			send_code_to_buffer(T4Code.EOL_PLUS_ONE, 13);
			start = encode1DLine(pixels, start);
			// Now we are doing 2-D encoding
			for(int i = 0; i < leftOver - 1; i++) {
				// This line is 2-D encoded
				send_code_to_buffer(T4Code.EOL_PLUS_ZERO, 13); 
				start = encode2DLine(pixels, start);	
			}			
		}		
		
		// Now send RTC(Return To Control) - 6 consecutive EOL + plus 1 to output
		send_code_to_buffer(T4Code.EOL_PLUS_ONE, 13);
		send_code_to_buffer(T4Code.EOL_PLUS_ONE, 13);
		send_code_to_buffer(T4Code.EOL_PLUS_ONE, 13);
		send_code_to_buffer(T4Code.EOL_PLUS_ONE, 13);
		send_code_to_buffer(T4Code.EOL_PLUS_ONE, 13);
		send_code_to_buffer(T4Code.EOL_PLUS_ONE, 13);
		
		// We need to flush the last buffer
		setExtraFlush(true);		
	}
	
	// Need to take care of processing the first and last picture element in a line: ITU-T Rec. T4 (07/2003) 4.2.1.3.4
	// ITU-T Rec. T.4 (07/2003) Figure 7/T4  - Two-dimensional coding flow diagram
	private int encodeKLines(byte[] pixels, int start) throws Exception {
		// Encode a one dimensional scan line first
		send_code_to_buffer(T4Code.EOL_PLUS_ONE, 13);
		start = encode1DLine(pixels, start);
			
		// Encode the remaining k-1 lines
		for(int i = 0; i < k - 1; i++) {
			// Now we are doing 2-D encoding
			send_code_to_buffer(T4Code.EOL_PLUS_ZERO, 13); // This line is 2-D encoded
			start = encode2DLine(pixels, start);		
		}
		
		return start;
	}
	
	// We are going to reuse this method in G42DEncoder
	protected int encode2DLine(byte[] pixels, int start) throws Exception {
		boolean endOfLine = false;
		// First changing element is an imaginary white and we assume white is zero
		int color = 0;
		// Starting offset within the line start byte
		int currRefPos = currPos;
	
		int preRefPos = currRefPos + scanLineWidth%8;
		if(preRefPos > 7) preRefPos -= 8;
		
		int prevStart = ((((start+1)<<3) - currRefPos- scanLineWidth + 7)>>3) - 1;
		
		int bias = 7;
		a0 = -1;
		
		while(!endOfLine) {
			// Determine a1
			// Offset within the scan line
			int offset = a0 + 1;
			int begin = start + (offset - currRefPos + 7)/8;
			if(offset >= scanLineWidth) {
				a1 = scanLineWidth;
			} else {
				bias = currRefPos - (offset%8);
				if(bias < 0) bias += 8;
				if(a0 == -1) 
					color = 0; 
				else {
					if(bias == 7) color = ((pixels[begin-1])&0x01);
					else color = ((pixels[begin]>>>(bias+1))&0x01);
				}
				a1 = findChangingElement(pixels, begin, color, bias, offset, true);
				if(a1 == -1) a1 = scanLineWidth;
			}
			// Determine b1
			offset = a0 + 1;
			begin = prevStart + (offset - preRefPos + 7)/8;
				
			if(begin < 0) {
				b1 = scanLineWidth;
			} else if(offset >= scanLineWidth) {
				b1 = scanLineWidth;
			} else {
				bias = preRefPos - offset%8;
				if(bias < 0) bias +=8;
				b1 = findChangingElement(pixels, begin, color, bias, offset, false);
				if(b1 == -1) b1 = scanLineWidth; // Situated just after the last changing element on the reference line
			}
			// Determine b2
			color ^= 1;
			offset = b1 + 1;
			begin = prevStart + (offset - preRefPos + 7)/8;
			//begin += (offset - preRefPos + 7)/8;
			if(offset >= scanLineWidth) {
				b2 = scanLineWidth;
			} else {
				bias = preRefPos - (offset%8);
				if(bias <0) bias += 8;
				b2 = findChangingElement(pixels, begin, color, bias, offset, true);
				if(b2 == -1) b2 = scanLineWidth; // Situated just after the last changing element on the reference line
			}
			// Determine coding mode, and code accordingly
			// Are we in pass mode?
			if(b2 < a1) {// Yes, we are in pass code
				send_code_to_buffer(T42DCode.P.getCode(), T42DCode.P.getCodeLen());
				a0 = b2; // Update a0 position				
			} else {// No, we are not in pass mode
				int a1b1 = a1-b1;
				if(Math.abs(a1b1) <= 3) { // We are in Vertical mode
					if(a1b1 == 0)
						send_code_to_buffer(T42DCode.V0.getCode(), T42DCode.V0.getCodeLen());							
					else if(a1b1 == 1)
						send_code_to_buffer(T42DCode.VR1.getCode(), T42DCode.VR1.getCodeLen());
					else if(a1b1 == 2)
						send_code_to_buffer(T42DCode.VR2.getCode(), T42DCode.VR2.getCodeLen());
					else if(a1b1 == 3)
						send_code_to_buffer(T42DCode.VR3.getCode(), T42DCode.VR3.getCodeLen());
					else if(a1b1 == -1)
						send_code_to_buffer(T42DCode.VL1.getCode(), T42DCode.VL1.getCodeLen());
					else if(a1b1 == -2)
						send_code_to_buffer(T42DCode.VL2.getCode(), T42DCode.VL2.getCodeLen());
					else if(a1b1 == -3)
						send_code_to_buffer(T42DCode.VL3.getCode(), T42DCode.VL3.getCodeLen());
					a0 = a1; // Update a0 position
				} else { // We are in Horizontal mode
					send_code_to_buffer(T42DCode.H.getCode(), T42DCode.H.getCodeLen());
					// One-dimensional coding a0a1 and a1a2
					// First a0a1
					int len = a1-a0;
					if(a0 == -1) len--; // First changing element of the scan line
					// We are going to send out a white zero run length code
					if(len == 0) {
						T4Code code = T4WhiteCode.CODE0;
						short codeValue = code.getCode();
						int codeLen = code.getCodeLen();
						send_code_to_buffer(codeValue, codeLen);						
					} else
						outputRunLengthCode(len, color^1);
					// Then a1a2					
					offset = a1 + 1;
					begin = start + (offset - currRefPos + 7)/8;
					if(offset >= scanLineWidth) {
						a2 = scanLineWidth;
					} else {
						bias = currRefPos - (offset%8);
						if(bias <0) bias += 8;
						a2 = findChangingElement(pixels, begin, color, bias, offset, true);
						if(a2 == -1) a2 = scanLineWidth;
					}
					len = a2-a1;
					outputRunLengthCode(len, color);
					// Update a0 position
					a0 = a2; 
				}
			}
			if(a0 >= scanLineWidth) 
				endOfLine = true;
		}
		// Set current position
		currPos = currRefPos - (scanLineWidth%8);
		if(currPos < 0) currPos += 8;
		
		return start + (scanLineWidth - currRefPos + 7)/8;
	}	
	
	private int findChangingElement(byte[] pixels, int begin, int color, int bias, int offset, boolean sameLine) {
		//
		if(sameLine) return find(pixels, begin, color, bias, offset, true);
		
		int upperColor = ((a0 == -1)?0:((bias == 7)?(pixels[begin - 1]&0x01):((pixels[begin]>>>(bias+1))&0x01)));
		
		if(color == upperColor) {
			return find(pixels, begin, color, bias, offset, true);
		}
		
		return find(pixels, begin, upperColor, bias, offset, false);		
	}
		
	private int find(byte[] pixels, int begin, int color, int bias, int offset, boolean sameLine) {
		int changingElement = -1;
			
		// Determine changing element					
		while(bias >= 0) {
			if(((pixels[begin]>>>bias)&0x01)==color) {
				offset++;
				bias--;
			} else {					
				bias--;			
						
				if(sameLine) {
					changingElement = offset++;
					break;
				}
				
				offset++;	
				
				color ^= 1;
				sameLine = true;
			}	
			if(offset >= scanLineWidth) {
				break;
			}
			if(bias < 0) {
				bias = 7;
				begin++;
			}			
		}
	
		return changingElement;
	}
	
	/*
	 * Changing elements used to define coding mode.
	 * 
	 * At the start of coding line, a0 is set on an imaginary white changing
	 * element situated just before the first element on the line. 
	 */
	private int a0 = -1;
	private int a1;
	private int a2;
	private int b1;
	private int b2;
}