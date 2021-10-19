package pl.bzowski;

import java.nio.file.Path;

public class MyLocalFile {

  private String computerName;
  private Path path;
  private long size;

  MyLocalFile(String computerName, Path path, long size) {
    this.computerName = computerName;
    this.path = path;
    this.size = size;
  }

  long getSize() {
    return size;
  }
  
}
