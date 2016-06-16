/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.image.options;

import com.icafe4j.image.ImageType;
import com.icafe4j.image.tiff.TiffFieldEnum.*;

public class TIFFOptions extends ImageOptions {
	private boolean isApplyPredictor;
	private Compression tiffCompression = Compression.PACKBITS;
	private PhotoMetric photoMetric = PhotoMetric.UNKNOWN;
	private ResolutionUnit resolutionUnit = ResolutionUnit.RESUNIT_INCH;
	private int defalteCompressionLevel = 4;
	private boolean writeICCProfile;
	
	private int jpegQuality = 90;
	private int xResolution = 72;
	private int yResolution = 72;
	
	public TIFFOptions() {}
	
	// Copy constructor
	public TIFFOptions(TIFFOptions options) {
		this.isApplyPredictor = options.isApplyPredictor;
		this.defalteCompressionLevel = options.defalteCompressionLevel;
		this.jpegQuality = options.jpegQuality;
		this.tiffCompression = options.tiffCompression;
	}
	
	public int getDeflateCompressionLevel() {
		return defalteCompressionLevel;
	}
	
	public ImageType getImageType() {
		return ImageType.TIFF;
	}
	
	public int getJPEGQuality() {
		return jpegQuality;
	}
	
	public PhotoMetric getPhotoMetric() {
		return photoMetric;
	}
	
	public Compression getTiffCompression() {
		return tiffCompression;
	}
	
	public ResolutionUnit getResolutionUnit() {
		return resolutionUnit;
	}
	
	public int getXResolution() {
		return xResolution;
	}
	
	public int getYResolution() {
		return yResolution;
	}
	
	public boolean isApplyPredictor() {
		return isApplyPredictor;
	}
	
	public boolean writeICCProfile() {
		return writeICCProfile;
	}
	
	public void setApplyPredictor(boolean isApplyPredictor) {
		this.isApplyPredictor = isApplyPredictor;
	}
	
	public void setDeflateCompressionLevel(int deflateCompressionLevel) {
		if(deflateCompressionLevel >= 0 && deflateCompressionLevel <= 9)
			this.defalteCompressionLevel = deflateCompressionLevel;
	}
	
	public void setJPEGQuality(int quality) {
		this.jpegQuality = quality;
	}
	
	public void setPhotoMetric(PhotoMetric photoMetric) {
		this.photoMetric = photoMetric;
	}
	
	public void setResolutionUnit(ResolutionUnit resolutionUnit) {
		this.resolutionUnit = resolutionUnit;
	}
	
	public void setTiffCompression(Compression tiffCompression) {
		this.tiffCompression = tiffCompression;
	}
	
	public void setWriteICCProfile(boolean writeICCProfile) {
		this.writeICCProfile = writeICCProfile;
	}
	
	public void setXResolution(int xResolution) {
		if(xResolution > 0)
			this.xResolution = xResolution;
	}
	
	public void setYResolution(int yResolution) {
		if(yResolution > 0)
			this.yResolution = yResolution;
	}
}