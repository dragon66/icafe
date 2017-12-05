package com.icafe4j.image.compression.huffman;

import com.icafe4j.image.compression.ccitt.T4Code;
import com.icafe4j.image.compression.ccitt.T4WhiteCode;

public class T4WhiteCodeHuffmanTree extends T4CodeHuffmanTree {
		
	public T4CodeHuffmanTreeNode generate() {
		
		T4CodeHuffmanTreeNode root = new T4CodeHuffmanTreeNode();
		T4CodeHuffmanTreeNode curr = root;
		
		for(T4Code code : T4WhiteCode.values()) {
			if(code == T4WhiteCode.UNKNOWN) continue;
			curr = root;			
			int len = code.getCodeLen();
			short value = code.getCode();
			for(int i = 0; i < len; i++) {
				T4CodeHuffmanTreeNode newNode = new T4CodeHuffmanTreeNode();
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
		
		return root;
	}
}