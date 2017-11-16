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

package com.icafe4j.image.options;

import com.icafe4j.image.ImageType;
import com.icafe4j.image.png.Filter;

public class PNGOptions extends ImageOptions {
	//
	private boolean isApplyAdaptiveFilter;
	private int filterType = Filter.NONE;
	private int compressionLevel = 4;
	
	public int getCompressionLevel() {
		return compressionLevel;
	}
	
	public int getFilterType() {
		return filterType;
	}
	
	public ImageType getImageType() {
		return ImageType.PNG;
	}
	
	public boolean isApplyAdaptiveFilter() {
		return isApplyAdaptiveFilter;
	}
	
	public void setApplyAdaptiveFilter(boolean isApplyAdaptiveFilter) {
		this.isApplyAdaptiveFilter = isApplyAdaptiveFilter;
	}
	
	public void setCompressionLevel(int compressionLevel) {
		if(compressionLevel >= 0 && compressionLevel <= 9)
			this.compressionLevel = compressionLevel;
	}
	
	public void setFilterType(int filterType) {
		this.filterType = filterType;
	}
}
