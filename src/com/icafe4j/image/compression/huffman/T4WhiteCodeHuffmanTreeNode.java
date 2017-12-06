package com.icafe4j.image.compression.huffman;

import com.icafe4j.image.compression.ccitt.T4Code;
import com.icafe4j.image.compression.ccitt.T4WhiteCode;

public class T4WhiteCodeHuffmanTreeNode extends T4CodeHuffmanTreeNode {
	// Single instance
	private static T4CodeHuffmanTreeNode root;
	
	static {
		root = new T4WhiteCodeHuffmanTreeNode();
		T4CodeHuffmanTreeNode curr = root;
		
		for(T4Code code : T4WhiteCode.values()) {
			if(code == T4WhiteCode.UNKNOWN) continue;
			curr = root;			
			int len = code.getCodeLen();
			short value = code.getCode();
			for(int i = 0; i < len; i++) {
				T4CodeHuffmanTreeNode newNode = new T4WhiteCodeHuffmanTreeNode();
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
	
	private T4WhiteCodeHuffmanTreeNode() {} // Prevent from instantiation

	public static T4CodeHuffmanTreeNode getInstance() {		
		return root;
	}
}