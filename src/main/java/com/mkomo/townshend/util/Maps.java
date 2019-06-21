package com.mkomo.townshend.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class Maps {

	@SuppressWarnings("unchecked")
	public static <K,V> Map<K,V> addAll(Map<K,V> map, Object ...kv) {
		for (int i = 0; i < kv.length; i+=2) {
			map.put((K)kv[i], (V)kv[i+1]);
		}
		return map;
	}

	public static <K,V> Map<K,V> of(Object ...kv) {
		Map<K,V> map = new LinkedHashMap<K,V>();
		return addAll(map, kv);
	}

}
