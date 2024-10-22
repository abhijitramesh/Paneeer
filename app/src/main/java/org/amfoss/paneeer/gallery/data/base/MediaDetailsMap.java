package org.amfoss.paneeer.gallery.data.base;

import java.util.HashMap;
import java.util.TreeMap;

public class MediaDetailsMap<K, V> extends HashMap<String, String> {
  private TreeMap<Integer, String> keys;

  public MediaDetailsMap() {
    super();
    keys = new TreeMap<Integer, String>();
  }

  public String getValue(int index) {
    String key = keys.get(index);
    return super.get(key);
  }

  public String getLabel(int index) {
    return keys.get(index);
  }

  @Override
  public String put(String key, String value) {
    keys.put(keys.size(), key);
    return super.put(key, value);
  }

  public int[] getKeySet() {
    int[] array = new int[keys.size()];
    for (int i = 0; i < keys.size(); i++) array[i] = i;
    return array;
  }
}
