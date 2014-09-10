package cafe.test;

import java.util.Arrays;

import cafe.string.StringUtils;
import cafe.util.ArrayUtils;

public class TestArrayUtils {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    String[]  s = {"1","2"};
        Object[]  o = {"3","4"};
        A[] a = {new A()};
        B[] b = {new B()};
        
        System.out.println("--Concating arrays--");
        
        //This line will not compile because of the explicit type parameter String.
        //System.out.println(Arrays.deepToString(ArrayUtils.<String>concat(s,o)));
        //This line compiles because the inferred type is Object
        System.out.println(Arrays.deepToString(ArrayUtils.concat(s,s)));
        System.out.println(Arrays.deepToString(ArrayUtils.concat(s,o)));
        System.out.println(Arrays.deepToString(ArrayUtils.concat(s,o,Object[].class)));   
        System.out.println(Arrays.deepToString(ArrayUtils.concat(a,b)));
        
        // Test array sorting
        final int NUMS = 200;
        final int GAP  =   37;

        System.out.println( "--Sorting Integer array--" );
        
		int j=0;
		Integer[] array = new Integer[NUMS];
		
        for( int i = GAP; i != 0; i = ( i + GAP ) % NUMS ){
            array[++j] = new Integer( i );
        }
        
        System.out.print(Arrays.deepToString(array));
        System.out.println();
        
        ArrayUtils.shellsort(array,1, array.length-1);

        System.out.println(Arrays.deepToString(array));
        
        System.out.println("--Packing byte array--");
        byte[] input = new byte[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3};
        System.out.println(StringUtils.byteArrayToHexString(ArrayUtils.packByteArray(input, 0, 5, 10)));
 	}
}

class A 
{
	public String toString() {return "A";}
}

class B extends A 
{
	public String toString(){return "B";}
}
