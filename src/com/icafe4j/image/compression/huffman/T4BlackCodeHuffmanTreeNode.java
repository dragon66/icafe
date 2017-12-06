package com.icafe4j.image.compression.huffman;

import com.icafe4j.image.compression.ccitt.T4BlackCode;
import com.icafe4j.image.compression.ccitt.T4Code;

public class T4BlackCodeHuffmanTreeNode extends T4CodeHuffmanTreeNode {
	// Single instance
	private static T4CodeHuffmanTreeNode root;
	
	static {
		root = new T4BlackCodeHuffmanTreeNode();
		T4CodeHuffmanTreeNode curr = root;
		
		for(T4Code code : T4BlackCode.values()) {
			if(code == T4BlackCode.UNKNOWN) continue;
			curr = root;			
			int len = code.getCodeLen();
			short value = code.getCode();
			for(int i = 0; i < len; i++) {
				T4CodeHuffmanTreeNode newNode = new T4BlackCodeHuffmanTreeNode();
				if(((value>>(16 - i - 1))&0x01) == 0) {
					if(curr.left() == null) {
						curr.setLeft(newNode);
						curr = newNode;
					}
					else curr = curr.left();
				} else {
					if(curr.right() == null) {
						curr.setRight(newNode);
						curr = newNode;
					}
					else curr = curr.right();
				}				
			}
			curr.setValue(code.getRunLen());
		}
	}
	
	private T4BlackCodeHuffmanTreeNode() {} // Prevent from instantiation

	public static T4CodeHuffmanTreeNode getInstance() {		
		return root;
	}
}