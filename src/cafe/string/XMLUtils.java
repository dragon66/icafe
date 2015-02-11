/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 * 
 * Change History - most recent changes go on top of previous changes
 *
 * StringUtils.java
 *
 * Who   Date       Description
 * ====  =========  ==============================================================
 * WY    23Jan2015  Initial creation - moved XML related methods to here
 */


package cafe.string;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLUtils {
	public static void addChild(Node parent, Node child) {
		parent.appendChild(child);
	}
	
	public static void addText(Document doc, Node parent, String data) {
		parent.appendChild(doc.createTextNode(data));
	}
	
	// Create an empty Document node
	public static Document createDocumentNode() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = null;
	    
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
		return builder.newDocument();
	}
	
	public static Node createElement(Document doc, String tagName) {
		return doc.createElement(tagName);
	}
	
	public static Document createXML(byte[] xml) {
		//Get the DOM Builder Factory
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//Get the DOM Builder
		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		//Load and Parse the XML document
		//document contains the complete XML as a Tree.
		Document document = null;
		try {
			try {
				document = builder.parse(new ByteArrayInputStream(xml), "UTF-8");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (SAXException e) {
			e.printStackTrace();
		}
		
		return document;
	}
	
	public static Document createXML(String xml) {
		//Get the DOM Builder Factory
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//Get the DOM Builder
		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		//Load and Parse the XML document
		//document contains the complete XML as a Tree.
		Document document = null;
		InputSource source = new InputSource(new StringReader(xml));
		try {
			try {
				document = builder.parse(source);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (SAXException e) {
			e.printStackTrace();
		}
		
		return document;		 
	}
	
	public static String escapeXML(String input) 
	{
		Iterator<Character> itr = StringUtils.stringIterator(input);
		StringBuilder result = new StringBuilder();		
		
		while (itr.hasNext())
		{
			Character c = itr.next();
			
			switch (c)
			{
				case '"':
					result.append("&quot;");
					break;
				case '\'':
					result.append("&apos;");
					break;
				case '<':
					result.append("&lt;");
					break;
				case '>':
					result.append("&gt;");
					break;
				case '&':
					result.append("&amp;");
					break;
				default:
					result.append(c);
			}
		}
		
		return result.toString();
	}
	
	public static void printNode(Node node, String indent) {
		if(node != null) {
			if(indent == null) indent = "";
			switch(node.getNodeType()) {
		        case Node.DOCUMENT_NODE: {
		            Node child = node.getFirstChild();
		            while(child != null) {
		            	printNode(child, indent);
		            	child = child.getNextSibling();
		            }
		            break;
		        } 
		        case Node.DOCUMENT_TYPE_NODE: {
		            DocumentType doctype = (DocumentType) node;
		            System.out.println("<!DOCTYPE " + doctype.getName() + ">");
		            break;
		        }
		        case Node.ELEMENT_NODE: { // Element node
		            Element ele = (Element) node;
		            System.out.print(indent + "<" + ele.getTagName());
		            NamedNodeMap attrs = ele.getAttributes(); 
		            for(int i = 0; i < attrs.getLength(); i++) {
		                Node a = attrs.item(i);
		                System.out.print(" " + a.getNodeName() + "='" + 
		                          escapeXML(a.getNodeValue()) + "'");
		            }
		            System.out.println(">");
	
		            String newindent = indent + "    ";
		            Node child = ele.getFirstChild();
		            while(child != null) {
		            	printNode(child, newindent);
		            	child = child.getNextSibling();
		            }
	
		            System.out.println(indent + "</" + ele.getTagName() + ">");
		            break;
		        }
		        case Node.TEXT_NODE: {
		            Text textNode = (Text)node;
		            String text = textNode.getData().trim();
		            if ((text != null) && text.length() > 0)
		                System.out.println(indent + escapeXML(text));
		            break;
		        }
		        case Node.PROCESSING_INSTRUCTION_NODE: {
		            ProcessingInstruction pi = (ProcessingInstruction)node;
		            System.out.println(indent + "<?" + pi.getTarget() +
		                               " " + pi.getData() + "?>");
		            break;
		        }
		        case Node.ENTITY_REFERENCE_NODE: {
		            System.out.println(indent + "&" + node.getNodeName() + ";");
		            break;
		        }
		        case Node.CDATA_SECTION_NODE: {           // Output CDATA sections
		            CDATASection cdata = (CDATASection)node;
		            System.out.println(indent + "<" + "![CDATA[" + cdata.getData() +
		                        "]]" + ">");
		            break;
		        }
		        case Node.COMMENT_NODE: {
		        	Comment c = (Comment)node;
		            System.out.println(indent + "<!--" + c.getData() + "-->");
		            break;
		        }
		        default:
		            System.err.println("Unknown node: " + node.getClass().getName());
		            break;
			}
		}
	}
	
	public static void showXML(Document document) {
		printNode(document,"");
	}
}
