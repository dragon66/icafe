package com.icafe4j.image.compression.huffman;

public abstract class T4CodeHuffmanTreeNode {
	
	private int value;
	
	private T4CodeHuffmanTreeNode left;
    private T4CodeHuffmanTreeNode right;
	
	
	T4CodeHuffmanTreeNode() {}
	
	T4CodeHuffmanTreeNode(int value) {
		this.value = value;
	}
	
	public T4CodeHuffmanTreeNode left() {
		return left;
	}
	
	public T4CodeHuffmanTreeNode right() {
		return right;
	}
	
	void setLeft(T4CodeHuffmanTreeNode left) {
		this.left = left;
	}
	
	void setRight(T4CodeHuffmanTreeNode right) {
		this.right = right;
	}
	
	void setValue(int value) {
		this.value = value;
	}
	
	public int value() {
		return value;
	}
}