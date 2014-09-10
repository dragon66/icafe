package cafe.test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import cafe.io.FileCacheRandomAccessInputStream;
import cafe.io.MemoryCacheRandomAccessOutputStream;
import cafe.io.RandomAccessInputStream;
import cafe.io.RandomAccessOutputStream;
import cafe.io.ReadStrategyII;
import cafe.io.ReadStrategyMM;
import cafe.io.WriteStrategyII;

public class TestRandomStream {

	public TestRandomStream() {	}

	public static void main(String[] args) throws IOException{
		FileOutputStream fo = new FileOutputStream("test.txt");
		RandomAccessOutputStream randout = new MemoryCacheRandomAccessOutputStream(fo);
		randout.setWriteStrategy(WriteStrategyII.getInstance());
		randout.writeUTF("\u4e2d\u5c0f");
		randout.writeChar('F');
		randout.writeInt(100);
		randout.writeShort(25);
		randout.write(10);
		randout.write(15);
		randout.writeS15Fixed16Number(-10.5f);
		randout.writeU16Fixed16Number(10.5f);
		randout.writeU8Fixed8Number(10.5f);
		randout.seek(0);
		randout.writeToStream(randout.getLength());
		randout.close();
		fo.close();
		
		FileInputStream fin = new FileInputStream("test.txt");
		RandomAccessInputStream randin = new FileCacheRandomAccessInputStream(fin);
		randin.setReadStrategy(ReadStrategyMM.getInstance());
		System.out.println(randin.readUTF());
		/**
		 *  Due to the current implementation, writeUTF and readUTF are machine or byte sequence independent
		 *  but writeChar and readChar are. So we switch back to the same byte sequence as the stream which
		 *  writes out the char before reading it.
		 */
		randin.setReadStrategy(ReadStrategyII.getInstance());
		System.out.println(randin.readChar());
		System.out.println(randin.readInt());
		System.out.println(randin.readShort());
		System.out.println(randin.read());
		System.out.println(randin.read());
		System.out.println(randin.readS15Fixed16Number());
		System.out.println(randin.readU16Fixed16Number());
		System.out.println(randin.readU8Fixed8Number());
		randin.close();
		fin.close();
	}
}
