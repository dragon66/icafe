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

package com.icafe4j.image.gif;

import java.awt.image.BufferedImage;

/** 
 * Wrapper for GIF frame
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 11/17/2014
 */
public class GIFFrame {
	// Frame parameters
	private BufferedImage frame;
	private int leftPosition;
	private int topPosition;
	private int frameWidth;
	private int frameHeight;
	private int delay;
	private int disposalMethod = DISPOSAL_UNSPECIFIED;
	private int userInputFlag = USER_INPUT_NONE;
	private int transparencyFlag = TRANSPARENCY_INDEX_NONE;
	
	// The transparent color value in RRGGBB format.
	// The highest order byte has no effect.
	private int transparentColor = TRANSPARENCY_COLOR_NONE; // Default no transparent color
	
	public static final int DISPOSAL_UNSPECIFIED = 0;
	public static final int DISPOSAL_LEAVE_AS_IS = 1;
	public static final int DISPOSAL_RESTORE_TO_BACKGROUND = 2;
	public static final int DISPOSAL_RESTORE_TO_PREVIOUS = 3;
	// Values between 4-7 inclusive
	public static final int DISPOSAL_TO_BE_DEFINED = 7;
	
	public static final int USER_INPUT_NONE = 0;
	public static final int USER_INPUT_EXPECTED = 1;
	
	public static final int TRANSPARENCY_INDEX_NONE = 0;
	public static final int TRANSPARENCY_INDEX_SET = 1;
	
	public static final int TRANSPARENCY_COLOR_NONE = -1;
	
	public GIFFrame(BufferedImage frame) {
		this(frame, 0, 0, 0, GIFFrame.DISPOSAL_UNSPECIFIED);
	}
	
	public GIFFrame(BufferedImage frame, int delay) {
		this(frame, 0, 0, delay, GIFFrame.DISPOSAL_UNSPECIFIED);
	}
	
	public GIFFrame(BufferedImage frame, int delay, int disposalMethod) {
		this(frame, 0, 0, delay, disposalMethod);
	}
	
	public GIFFrame(BufferedImage frame, int leftPosition, int topPosition, int delay, int disposalMethod) {
		this(frame, leftPosition, topPosition, delay, disposalMethod, USER_INPUT_NONE, TRANSPARENCY_INDEX_NONE, TRANSPARENCY_COLOR_NONE);
	}
	
	public GIFFrame(BufferedImage frame, int leftPosition, int topPosition, int delay, int disposalMethod, int userInputFlag, int transparencyFlag, int transparentColor) {
		if(frame == null) throw new IllegalArgumentException("Null input image");
		if(disposalMethod < DISPOSAL_UNSPECIFIED || disposalMethod > DISPOSAL_TO_BE_DEFINED)
			throw new IllegalArgumentException("Invalid disposal method: " + disposalMethod);
		if(userInputFlag < USER_INPUT_NONE || userInputFlag > USER_INPUT_EXPECTED)
			throw new IllegalArgumentException("Invalid user input flag: " + userInputFlag);
		if(transparencyFlag < TRANSPARENCY_INDEX_NONE || transparencyFlag > TRANSPARENCY_INDEX_SET)
			throw new IllegalArgumentException("Invalid transparency flag: " + transparencyFlag);
		if(leftPosition < 0 || topPosition < 0)
			throw new IllegalArgumentException("Negative coordinates for frame top-left position");
		if(delay < 0) delay = 0;
		this.frame = frame;
		this.leftPosition = leftPosition;
		this.topPosition = topPosition;	
		this.delay = delay;
		this.disposalMethod = disposalMethod;
		this.userInputFlag = userInputFlag;
		this.transparencyFlag = transparencyFlag;
		this.frameWidth = frame.getWidth();
		this.frameHeight = frame.getHeight();
		this.transparentColor = transparentColor;
	}
	
	public int getDelay() {
		return delay;
	}
	
	public int getDisposalMethod() {
		return disposalMethod;
	}
	
	public BufferedImage getFrame() {
		return frame;
	}
	
	public int getFrameHeight() {
		return frameHeight;
	}
	
	public int getFrameWidth() {
		return frameWidth;
	}
	
	public int getLeftPosition() {
		return leftPosition;
	}
	
	public int getTopPosition() {
		return topPosition;
	}
	
	public int getTransparentColor() {
		return transparentColor;
	}
	
	public int getTransparencyFlag() {
		return transparencyFlag;
	}
	
	public int getUserInputFlag() {
		return userInputFlag;
	}
 }