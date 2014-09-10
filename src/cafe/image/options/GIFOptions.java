/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.options;

import cafe.image.core.ImageType;

public class GIFOptions extends ImageOptions {
	// Fields
	private boolean requireUserInput;
	int disposalMethod;
	// Constants
	public static int DISPOSAL_UNSPECIFIED = 0;	
	public static int DISPOSAL_NONE = 1;	
	public static int DISPOSAL_RESTORE_TO_BACKGROUND = 2;
	public static int DISPOSAL_RESTORE_TO_PREVIOUS = 3;
	
	public int getDispoalMethod() {
		return disposalMethod;
	}
	
	public ImageType getImageType() {
		return ImageType.GIF;
	}
	
	public boolean getRequireUserInput() {
		return requireUserInput;
	}
	
	public void setDisposalMethod(int disposalMethod) {
		if(disposalMethod < DISPOSAL_UNSPECIFIED || disposalMethod > DISPOSAL_RESTORE_TO_PREVIOUS)
			throw new IllegalArgumentException("Invalid disposal method: " + disposalMethod);
		this.disposalMethod = disposalMethod;
	}
	
	public void setRequireUserInput(boolean requireUserInput) {
		this.requireUserInput = requireUserInput;
	}
}