/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * XMP.java
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    03Apr2016  Added new constructor XMP(String, String)
 * WY    31Mar2016  Moved to new package
 * WY    31Mar2016  Made XMP abstract and overrode write() method
 * WY    03Jul2015  Added override method getData()
 * WY    05Mar2015  Revised getMergedDocument()
 * WY    27Feb2015  Added support for ExtendedXMP data
 * WY    19Feb2015  Removed showMetadata() and added getXmpMeta()
 * WY    19Feb2015  Renamed getXMLDocument() to getXmpDocument()
 * WY    11Feb2015  Added getXMLDocument() and showMetadata()
 * WY    19Jan2015  Initial creation
 */

package com.icafe4j.image.meta.xmp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataEntry;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.string.XMLUtils;

public abstract class XMP extends Metadata {
	// Fields
	private Document xmpDocument;
	private Document extendedXmpDocument;
	//document contains the complete XML as a Tree.
	private Document mergedXmpDocument;
	private boolean hasExtendedXmp;
	private byte[] extendedXmpData;
	
	private String xmp;
		
	public XMP(byte[] data) {
		super(MetadataType.XMP, data);
	}
	
	public XMP(String xmp) {
		this(xmp, null);
	}
	
	public XMP(String xmp, String extendedXmp) {
		super(MetadataType.XMP);
		if(xmp == null) throw new IllegalArgumentException("Input XMP string is null");
		this.xmp = xmp;
		if(extendedXmp != null) { // We have ExtendedXMP
			try {
				setExtendedXMPData(XMLUtils.serializeToByteArray(XMLUtils.createXML(extendedXmp)));
			} catch (IOException e) {				
				e.printStackTrace();
			}
		}
	}
	
	public byte[] getData() {
		byte[] data = super.getData();
		if(data != null && !hasExtendedXmp)
			return data;
		try {
			return XMLUtils.serializeToByteArray(getMergedDocument());
		} catch (IOException e) {
			return null;
		}
	}
	
	public byte[] getExtendedXmpData() {
		return extendedXmpData;
	}
	
	public Document getExtendedXmpDocument() {
		if(hasExtendedXmp && extendedXmpDocument == null)
			extendedXmpDocument = XMLUtils.createXML(extendedXmpData);

		return extendedXmpDocument;
	}
	
	/**
	 * Merge the standard XMP and the extended XMP DOM
	 * <p>
	 * This is a very expensive operation, avoid if possible
	 * 
	 * @return a merged Document for the entire XMP data with the GUID from the standard XMP document removed
	 */
	public Document getMergedDocument() {
		if(mergedXmpDocument != null)
			return mergedXmpDocument;
		else if(getExtendedXmpDocument() != null) { // Merge document
			mergedXmpDocument = XMLUtils.createDocumentNode();
			Document rootDoc = getXmpDocument();
			NodeList children = rootDoc.getChildNodes();
			for(int i = 0; i< children.getLength(); i++) {
				Node importedNode = mergedXmpDocument.importNode(children.item(i), true);
				mergedXmpDocument.appendChild(importedNode);
			}
			// Remove GUID from the standard XMP
			XMLUtils.removeAttribute(mergedXmpDocument, "rdf:Description", "xmpNote:HasExtendedXMP");
			// Copy all the children of rdf:RDF element
			NodeList list = extendedXmpDocument.getElementsByTagName("rdf:RDF").item(0).getChildNodes();
			Element rdf = (Element)(mergedXmpDocument.getElementsByTagName("rdf:RDF").item(0));
		  	for(int i = 0; i < list.getLength(); i++) {
	    		Node curr = list.item(i);
	    		Node newNode = mergedXmpDocument.importNode(curr, true);
    			rdf.appendChild(newNode);
	    	}
	    	return mergedXmpDocument;
		} else
			return getXmpDocument();
	}
	
	public Document getXmpDocument() {
		ensureDataRead();		
		return xmpDocument;
	}
	
	public boolean hasExtendedXmp() {
		return hasExtendedXmp;
	}
	
	public Iterator<MetadataEntry> iterator() {
		Document doc = getMergedDocument();
		
		MetadataEntry dummy = new MetadataEntry("XMP", " Document", true);
		addNodeToEntry(doc, dummy);
		
		return dummy.getMetadataEntries().iterator();
	}
	
	private void addNodeToEntry(Node node, MetadataEntry entry) {
		if(node != null) {
			switch(node.getNodeType()) {
		        case Node.DOCUMENT_NODE: {
		            Node child = node.getFirstChild();
		            while(child != null) {
		            	addNodeToEntry(child, entry);
		            	child = child.getNextSibling();
		            }
		            break;
		        } 
		        case Node.DOCUMENT_TYPE_NODE: {
		            DocumentType doctype = (DocumentType) node;
		            entry.addEntry(new MetadataEntry("!DOCTYPE", doctype.getName()));
		            break;
		        }
		        case Node.ELEMENT_NODE: { // Element node
		            Element ele = (Element) node;
		       
		            NamedNodeMap attrs = ele.getAttributes();
		            StringBuilder attributes = new StringBuilder();
		            for(int i = 0; i < attrs.getLength(); i++) {
		                Node a = attrs.item(i);
		            	attributes.append(a.getNodeName()).append("=").append("'" + a.getNodeValue()).append("' ");
		            }
		            MetadataEntry element = new MetadataEntry(ele.getTagName(), attributes.toString().trim(), true);
		            entry.addEntry(element);
	       
		            Node child = ele.getFirstChild();
		            while(child != null) {
		            	addNodeToEntry(child, element);
		            	child = child.getNextSibling();
		            }
		            break;
		        }
		        case Node.TEXT_NODE: {
		            Text textNode = (Text)node;
		            String text = textNode.getData().trim();
		            if ((text != null) && text.length() > 0)
		                entry.addEntry(new MetadataEntry(text, ""));
		            break;
		        }
		        case Node.PROCESSING_INSTRUCTION_NODE: {
		            ProcessingInstruction pi = (ProcessingInstruction)node;
		            entry.addEntry(new MetadataEntry("?" + pi.getTarget(), pi.getData() + "?"));
		            break;
		        }
		        case Node.ENTITY_REFERENCE_NODE: {
		        	entry.addEntry(new MetadataEntry("&" + node.getNodeName() + ";", ""));
		            break;
		        }
		        case Node.CDATA_SECTION_NODE: { // Output CDATA sections
		            CDATASection cdata = (CDATASection)node;
		            entry.addEntry(new MetadataEntry("![CDATA[" + cdata.getData() + "]]", ""));
		            break;
		        }
		        case Node.COMMENT_NODE: {
		        	Comment c = (Comment)node;
		        	entry.addEntry(new MetadataEntry("!--" + c.getData() + "--", ""));
		            break;
		        }
		        default:
		            break;
			}
		}
	}
	
	public void read() throws IOException {
		if(!isDataRead) {
			if(xmp != null)
				xmpDocument = XMLUtils.createXML(xmp);
			else if(data != null)
				xmpDocument = XMLUtils.createXML(data);
			
			isDataRead = true;
		}
	}
	
	public void setExtendedXMPData(byte[] extendedXmpData) {
		this.extendedXmpData = extendedXmpData;
		hasExtendedXmp = true;
	}
	
	public abstract void write(OutputStream os) throws IOException;
}