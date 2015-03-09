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

import cafe.image.ImageType;
import cafe.image.tiff.TiffField;
import cafe.image.tiff.TiffFieldEnum.*;

public class TIFFOptions extends ImageOptions<TiffField<?>> {
	private boolean isApplyPredictor;
	private Compression tiffCompression = Compression.PACKBITS;
	private PhotoMetric photoMetric = PhotoMetric.UNKNOWN;
	private int defalteCompressionLevel = 4;
	private boolean writeICCProfile;
	
	private int jpegQuality = 90;
	
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
	
	public void setTiffCompression(Compression tiffCompression) {
		this.tiffCompression = tiffCompression;
	}
	
	public void setWriteICCProfile(boolean writeICCProfile) {
		this.writeICCProfile = writeICCProfile;
	}
}