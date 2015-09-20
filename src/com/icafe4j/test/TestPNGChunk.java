package com.icafe4j.test;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import com.icafe4j.image.png.*;

public class TestPNGChunk extends TestBase {

	public static void main(String[] args) {
		new TestPNGChunk().test();
	}
	
	public void test(String ... args) {
		List<Chunk> list = new ArrayList<Chunk>();
		list.add(new Chunk(ChunkType.UNKNOWN, 0, null, 0));
        list.add(new Chunk(ChunkType.IEND, 0, null, 0));
        list.add(new Chunk(ChunkType.IHDR, 0, null, 0));
        list.add(new UnknownChunk(0x000000, 0, null, 0));
   
        list.add(new Chunk(ChunkType.PLTE, 0, null, 0));
        list.add(new Chunk(ChunkType.IDAT, 0, null, 0));
        list.add(new Chunk(ChunkType.IDAT, 0, null, 0));
        list.add(new Chunk(ChunkType.GAMA, 0, null, 0));
        list.add(new Chunk(ChunkType.TRNS, 0, null, 0));
        Collections.sort(list);
        logger.info("{}", list);  
	}
}
