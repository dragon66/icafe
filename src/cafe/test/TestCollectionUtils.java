package cafe.test;

import java.util.HashMap;
import java.util.Map;
import cafe.util.CollectionUtils;

public class TestCollectionUtils {

	public TestCollectionUtils() {}

	public static void main(String[] args) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("first", 1);
		map.put("second", 1);
		
		System.out.println(CollectionUtils.getKeysByValue(map, 1));
	}
}