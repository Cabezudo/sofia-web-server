package net.cabezudo.sofia.core.html;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2019.06.18
 */
public abstract class Tag {

  private final Map<String, String> properties = new HashMap<>();
  private final int column;

  protected Tag(String data, int column) {
    this.column = column;
    String[] ps = data.trim().split("\\s");
    for (String property : ps) {
      String[] p = property.split("=");
      String propertyName = p[0];
      String propertyValue = p[1].substring(1, p[1].length() - 1);
      properties.put(propertyName, propertyValue);
    }
  }

  public boolean isSection() {
    return false;
  }

  public String getValue(String propertyName) {
    return properties.get(propertyName);
  }

  public String getId() {
    return getValue("id");
  }

  public int getColumn() {
    return column;
  }
}