package pl.bzowski;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

import com.google.common.net.UrlEscapers;

public class MyLocalFile {

  private String computerName;
  private Path path;
  private long size;

  public MyLocalFile(String computerName, Path path, long size) {
    this.computerName = computerName;
    this.path = path;
    this.size = size;
  }

  long getSize() {
    return size;
  }

  public String getComputedPath() throws UnsupportedEncodingException {
    return UrlEscapers.urlFragmentEscaper().escape(computerName + "/" + path.toString().replace("D:\\", "D\\").replaceAll("\\\\", "/")).toString();
  }

  public String getFilePath() {
    return path.toString();
  }

  public String getFileDirectory() {
    return UrlEscapers.urlFragmentEscaper().escape(computerName + "/" + path.getParent().toString().replace("D:\\", "D\\").replaceAll("\\\\", "/")).toString();
  }
  
}
