package cafe.test;

import cafe.image.png.*;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class TestPNGChunk {

	public static void main(String[] args) {
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
        System.out.println(list);  
	}
}
