package cafe.test;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import cafe.image.png.PNGTweaker;
import cafe.image.png.Chunk;
import cafe.image.png.ChunkType;
import cafe.image.png.TextBuilder;
import cafe.io.IOUtils;

public class TestPNGTweaker extends TestBase {
	/** PNG signature constant */
    private static final long SIGNATURE = 0x89504E470D0A1A0AL;
    
    public static void main(String[] args) throws Exception {
    	new TestPNGTweaker().test(args);
    }
    
	public void test(String ... args) throws Exception {
	    String text = PNGTweaker.read_text_chunks(args[0]);
		logger.info(text);
        logger.info("=================================");        
       
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
        	    new FileOutputStream("textinfo.txt"), "UTF-8"));
       	try {
       		out.write(text);
       	} finally {
       		out.close();
       	}
       	
        TextBuilder builder = new TextBuilder(ChunkType.ITXT);
        builder.setCompressed(true);
        builder.keyword("Author").text("Wen Yu");
        Chunk authorChunk = builder.build();
        builder.keyword("Software").text("PNGTweaker 1.0");
        Chunk softwareChunk = builder.build();
        builder.keyword("Copyright").text("Copyright 2015 Wen Yu (yuwen_66@yahoo.com).");
        Chunk copyrightChunk = builder.build();
        
        Chunk[] chunks = new Chunk[] {authorChunk, softwareChunk, copyrightChunk};
        PNGTweaker.dump_text_chunks(chunks);
        
        FileInputStream fi = new FileInputStream(args[0]);
        FileOutputStream fo = new FileOutputStream("NEW.png");
        
        PNGTweaker.insertChunks(chunks,	fi, fo);
        
        fi.close();
        fo.close();
        
        fi = new FileInputStream(args[0]);
        PNGTweaker.dump_text_chunks(fi);
        
        fi.close();
        
        PNGTweaker.remove_ancillary_chunks(args[0]);
        
        fi = new FileInputStream(args[0]);
        
        List<Chunk> list = PNGTweaker.mergeIDATChunks(PNGTweaker.readChunks(fi));
        
        fi.close();
        
        fo = new FileOutputStream("NEW_IDAT_MERGED.png");
    	
  		IOUtils.writeLongMM(fo, SIGNATURE);
	
        PNGTweaker.serializeChunks(list, fo);
        
        fo.close();
        
        PNGTweaker.splitIDATChunks(list, 8192);
        
        fo = new FileOutputStream("NEW_IDAT_SPLITTED.png");
        
        IOUtils.writeLongMM(fo, SIGNATURE);
        PNGTweaker.serializeChunks(list, fo);
        
        fo.close();
   	}
}
