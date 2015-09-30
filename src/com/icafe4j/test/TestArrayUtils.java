package com.icafe4j.test;

import java.util.Arrays;

import com.icafe4j.string.StringUtils;
import com.icafe4j.util.ArrayUtils;

public class TestArrayUtils extends TestBase {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new TestArrayUtils().test();
	}
	
	public void test(String ... args) {
	    String[]  s = {"1","2"};
        Object[]  o = {"3","4"};
        A[] a = {new A()};
        B[] b = {new B()};
        
        logger.info("--Concating arrays--");
        
        logger.info("{}", Arrays.deepToString(ArrayUtils.concat(s,s)));
    
        //This line will not compile because of the explicit type parameter String.
        //logger.info("{}", Arrays.deepToString(ArrayUtils.<String>concat(s,o)));
        //This line compiles because the inferred type is Object
        logger.info("{}", Arrays.deepToString(ArrayUtils.concat(s,o)));
        
        logger.info("{}", Arrays.deepToString(ArrayUtils.concat(Object.class,s,o)));   
        logger.info("{}", Arrays.deepToString(ArrayUtils.concat(a,b)));
        
        // Test array sorting
        final int NUMS = 200;
        final int GAP  =   37;

        logger.info( "--Sorting Integer array--" );
        
		int j=0;
		Integer[] array = new Integer[NUMS];
		
        for( int i = GAP; i != 0; i = ( i + GAP ) % NUMS ) {
            array[++j] = i;
        }
        
        logger.info("{}", Arrays.deepToString(array));
        
        ArrayUtils.mergesort(array, 1, array.length - 1);
        
        logger.info("{}", Arrays.deepToString(array));
        
        logger.info("--Packing byte array--");
        byte[] input = new byte[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3};
        logger.info("{}", StringUtils.byteArrayToHexString(ArrayUtils.packByteArray(input, 0, 5, 10)));
 	}
}

class A {
	public String toString() {return "A";}
}

class B extends A {
	public String toString(){return "B";}
}
