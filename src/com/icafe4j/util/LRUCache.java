/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.Collection;

/**
 * LRUCache.java.
 * 
 * A wrapper class implements Least-Recently-Used cache backed by a LinkedHashMap.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 09/19/2012
 * @see com.icafe4j.util.SoftHashMap
 */
public class LRUCache<K,V> implements Map<K,V>, java.io.Serializable {

	private static final long serialVersionUID = 4874219673008997966L;
	
	private Map<K,V> map = null;
	
	public LRUCache() {
	   this(10, false);	
	}
	
	public LRUCache(boolean isThreadsafe) {
		this(10, isThreadsafe);
	}
	
	public LRUCache(final int limit, boolean isThreadsafe) {
		map = new LinkedHashMap<K,V>(16, 0.75f, true) {
			    private static final long serialVersionUID = 3195834455030574184L;

				public boolean removeEldestEntry (Map.Entry<K,V> eldest){
					return size() > limit;
				}
			};
		
		if(isThreadsafe) {
			map = Collections.synchronizedMap(map);
		}	
	}	
	// Delegates all other method calls to the underlying Map object
	public V put(K key,V value)	{ return map.put(key, value); }
	
	public V get(Object key) { return map.get(key); }
	
	public V remove(Object key)	{ return map.remove(key); }
	
	public void clear()	{ map.clear(); }
	
	public boolean containsKey(Object key) { return map.containsKey(key); }
	
	public boolean containsValue(Object value) { return map.containsValue(value); }
	
	public int size() {	return map.size(); }
	
	public Set<Map.Entry<K,V>> entrySet() { return map.entrySet(); }
	
	public Collection<V> values() { return map.values(); }	
	
 	public void putAll(Map<? extends K,? extends V> m) { map.putAll(m); }
 	
 	public Set<K> keySet() { return map.keySet(); } 
 	
 	public boolean isEmpty() { return map.isEmpty(); }
}