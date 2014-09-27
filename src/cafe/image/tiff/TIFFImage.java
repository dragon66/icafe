/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.tiff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cafe.io.RandomAccessInputStream;
import cafe.io.RandomAccessOutputStream;

/**
 * TIFF image wrapper to manipulate IFD fields
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 05/23/2014
 */
public class TIFFImage {
	private int numOfPages;
	private int workingPage;
	private List<IFD> ifds;
	private RandomAccessInputStream rin;
	private RandomAccessOutputStream rout;

	public TIFFImage(RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		ifds = new ArrayList<IFD>();
		this.rin = rin;
		this.rout = rout;
		TIFFTweaker.readIFDs(ifds, rin);
		this.numOfPages = ifds.size();
		this.workingPage = 0;
	}
	
	public void addField(TiffField<?> field) {
		ifds.get(workingPage).addField(field);
	}
	
	public List<IFD> getIFDs() {
		return Collections.unmodifiableList(ifds);
	}
	
	public RandomAccessInputStream getInputStream() {
		return rin;
	}
	
	public int getNumOfPages() {
		return numOfPages;
	}
	
	public RandomAccessOutputStream getOutputStream() {
		return rout;
	}
	
	public TiffField<?> getField(short tag) {
		return ifds.get(workingPage).getField(tag);
	}
	
	public TiffField<?> removeField(short tag) {
		return ifds.get(workingPage).removeField(tag);
	}
	
	public void setWorkingPage(int workingPage) {
		if(workingPage >= 0 && workingPage < numOfPages)
			this.workingPage = workingPage;
		else
			throw new IllegalArgumentException("Invalid page number: " + workingPage);
	}
	
	public void write() throws IOException {
		TIFFTweaker.write(this);
	}
}