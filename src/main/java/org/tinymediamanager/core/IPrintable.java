package org.tinymediamanager.core;

public interface IPrintable {
  /**
   * return a printable value of the given object
   * 
   * @return a printable value (or toString() if there is no other implementation)
   */
  default String toPrintable() {
    return toString();
  }
}
