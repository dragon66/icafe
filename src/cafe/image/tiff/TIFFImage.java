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
import java.util.Iterator;
import java.util.List;

import cafe.io.RandomAccessInputStream;
import cafe.io.RandomAccessOutputStream;

/**
 * TIFF image wrapper to manipulate IFD fields
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 05/23/2014
 */
public class TIFFImage implements Iterable<IFD> {
	private int numOfPages;
	private int workingPage;
	private List<IFD> ifds;
	private RandomAccessInputStream rin;

	public TIFFImage() {
		ifds = new ArrayList<IFD>();
	}
	
	public TIFFImage(RandomAccessInputStream rin) throws IOException {
		ifds = new ArrayList<IFD>();
		this.rin = rin;
		TIFFTweaker.readIFDs(ifds, rin);
		this.numOfPages = ifds.size();
		this.workingPage = 0;
	}
	
	public void addField(TiffField<?> field) {
		ifds.get(workingPage).addField(field);
	}
	
	public void addPage(IFD page) {
		ifds.add(page);
		numOfPages++;
	}
	
	public void addPage(int index, IFD page) {
		ifds.add(index, page);
		numOfPages++;
	}
	
	public TiffField<?> getField(short tag) {
		return ifds.get(workingPage).getField(tag);
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
	
	public TiffField<?> removeField(short tag) {
		return ifds.get(workingPage).removeField(tag);
	}
	
	public IFD removePage(int index) {
		IFD removed = ifds.remove(index);
		numOfPages--;
		
		return removed;
	}
	
	public void setWorkingPage(int workingPage) {
		if(workingPage >= 0 && workingPage < numOfPages)
			this.workingPage = workingPage;
		else
			throw new IllegalArgumentException("Invalid page number: " + workingPage);
	}
	
	public void write(RandomAccessOutputStream out) throws IOException {
		if(numOfPages > 1) { // Reset pageNumber if we have more than 1 pages
			for(int i = 0; i < ifds.size(); i++) {
				ifds.get(i).removeField(TiffTag.PAGE_NUMBER.getValue());
				ifds.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{(short)i, (short)(numOfPages - 1)}));
			}
		}
		TIFFTweaker.write(this, out);
	}

	@Override
	public Iterator<IFD> iterator() {
		return ifds.iterator();
	}
}