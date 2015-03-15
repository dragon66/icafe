/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 * 
 * GIFOptions.java
 *
 * Who   Date       Description
 * ====  =========  =====================================================
 * WY    21Dec2014  Added similar fields and methods to GIFFrame class  
 */

package cafe.image.options;

import cafe.image.ImageType;
import cafe.image.gif.ApplicationExtension;

public class GIFOptions extends ImageOptions<ApplicationExtension> {	
	// Default frame parameters
	private int leftPosition = 0;
	private int topPosition = 0;
	private int delay = 0;
	private int disposalMethod = DISPOSAL_UNSPECIFIED;;
	private int userInputFlag = USER_INPUT_NONE;
	private int transparencyFlag = TRANSPARENCY_INDEX_NONE;
	
	// The transparent color value in RRGGBB format.
	// The highest order byte has no effect.
	private int transparentColor = TRANSPARENCY_COLOR_NONE; // Default no transparent color
	
	public static final int DISPOSAL_UNSPECIFIED = 0;		
	public static final int DISPOSAL_LEAVE_AS_IS = 1;
	public static final int DISPOSAL_RESTORE_TO_BACKGROUND = 2;
	public static final int DISPOSAL_RESTORE_TO_PREVIOUS = 3;
	
	public static final int USER_INPUT_NONE = 0;		
	public static final int USER_INPUT_EXPECTED = 1;
	
	public static final int TRANSPARENCY_INDEX_NONE = 0;		
	public static final int TRANSPARENCY_INDEX_SET = 1;
	
	public static final int TRANSPARENCY_COLOR_NONE = -1;
	
	public GIFOptions() { }
		
	public GIFOptions(int delay) {
		this(0, 0, delay, DISPOSAL_UNSPECIFIED);
	}
		
	public GIFOptions(int delay, int disposalMethod) {
		this(0, 0, delay, disposalMethod);
	}
		
	public GIFOptions(int leftPosition, int topPosition, int delay, int disposalMethod) {
		this(leftPosition, topPosition, delay, disposalMethod, USER_INPUT_NONE, TRANSPARENCY_INDEX_NONE, TRANSPARENCY_COLOR_NONE);
	}
		
	public GIFOptions(int leftPosition, int topPosition, int delay, int disposalMethod, int userInputFlag, int transparencyFlag, int transparentColor) {
		if(disposalMethod < DISPOSAL_UNSPECIFIED || disposalMethod > DISPOSAL_RESTORE_TO_PREVIOUS)
			throw new IllegalArgumentException("Invalid disposal method: " + disposalMethod);
		if(userInputFlag < USER_INPUT_NONE || userInputFlag > USER_INPUT_EXPECTED)
			throw new IllegalArgumentException("Invalid user input flag: " + userInputFlag);
		if(transparencyFlag < TRANSPARENCY_INDEX_NONE || transparencyFlag > TRANSPARENCY_INDEX_SET)
			throw new IllegalArgumentException("Invalid transparency flag: " + transparencyFlag);
		this.leftPosition = leftPosition;
		this.topPosition = topPosition;	
		this.delay = delay;
		this.disposalMethod = disposalMethod;
		this.userInputFlag = userInputFlag;
		this.transparencyFlag = transparencyFlag;
		this.transparentColor = transparentColor;
	}
		
	public int getDelay() {
		return delay;
	}
		
	public int getDisposalMethod() {
		return disposalMethod;
	}
		
	public ImageType getImageType() {
		return ImageType.GIF;
	}
		
	
	public int getLeftPosition() {
		return leftPosition;
	}
		
	public int getTopPosition() {
		return topPosition;
	}
		
	public int getTransparencyFlag() {
		return transparencyFlag;
	}
		
	public int getTransparentColor() {
		return transparentColor;
	}
		
	public int getUserInputFlag() {
		return userInputFlag;
	}
}