package pl.bzowski;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.github.sardine.Sardine;

public class SardineLocalCache {

  private final Sardine sardine;
  private final Set<String> cache = new HashSet<>();

  public SardineLocalCache(Sardine sardine) {
    this.sardine = sardine;
  }

  private boolean existsInCache(String string) {
    return cache.contains(string);
  }

  public void createIfNotExists(String string) {
    try {
      if (!existsInCache(string)) {
        if (sardine.exists(string)) {
          cache.add(string);
        } else {
          sardine.createDirectory(string);
          cache.add(string);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
