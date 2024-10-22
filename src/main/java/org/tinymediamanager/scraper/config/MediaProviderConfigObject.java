/*
 * Copyright 2012 - 2024 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.scraper.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used for holding a config setting
 * 
 * @author Myron Boyle
 */
public class MediaProviderConfigObject {
  public enum ConfigType {
    TEXT,
    BOOL,
    SELECT,
    SELECT_INDEX,
    INTEGER,
    LABEL, // just for labeling in the UI
    MULTI_SELECT
  }

  private static final Logger LOGGER          = LoggerFactory.getLogger(MediaProviderConfigObject.class);

  String                      key             = "";
  String                      keyDescription  = "";
  String                      value           = "";
  String                      defaultValue    = "";
  boolean                     returnListAsInt = false;
  boolean                     encrypt         = false;
  boolean                     visible         = true;
  ConfigType                  type            = ConfigType.TEXT;
  List<String>                possibleValues  = new ArrayList<>();

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  /**
   * short description for key, to display in GUI<br>
   * if empty, we are returning the key (as before)
   */
  public String getKeyDescription() {
    return keyDescription.isEmpty() ? key : keyDescription;
  }

  /**
   * short description for key, to display in GUI<br>
   * 
   * @param keyDescription
   */
  public void setKeyDescription(String keyDescription) {
    this.keyDescription = keyDescription;
  }

  public boolean isEmpty() {
    return key.isEmpty();
  }

  /**
   * gets the configured value, or the default one
   * 
   * @return
   */
  public String getValue() {
    String ret = "";
    switch (type) {
      case SELECT:
        ret = getValueAsString();
        break;

      case SELECT_INDEX:
        Integer i = getValueIndex();
        ret = (i == null || i < 0) ? "" : String.valueOf(i);
        break;

      case BOOL:
        return String.valueOf(getValueAsBool());

      case INTEGER:
        return String.valueOf(getValueAsInteger());

      case TEXT:
      default:
        return this.value;
    }
    return ret;
  }

  public String getValueAsString() {
    if (type == ConfigType.SELECT && !possibleValues.contains(this.value)) {
      return this.defaultValue;
    }
    return this.value;
  }

  public boolean getValueAsBool() {
    boolean bool = Boolean.FALSE;
    if (type != ConfigType.BOOL) {
      return bool;
    }
    if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) { // always false when unparseable :/
      bool = Boolean.parseBoolean(value);
    }
    else {
      bool = Boolean.parseBoolean(defaultValue);
    }
    return bool;
  }

  public Integer getValueAsInteger() {
    Integer integer = null;
    if (type != ConfigType.INTEGER) {
      return null;
    }
    try {
      integer = Integer.parseInt(value);
    }
    catch (Exception e) {
      try {
        integer = Integer.parseInt(defaultValue);
      }
      catch (Exception e1) {
        // ignored
      }
    }
    return integer;
  }

  public Integer getValueIndex() {
    // FIXME: Index is just stored in value? return 1:1 ?!? no example found yet...
    Integer ret;
    if (type != ConfigType.SELECT && type != ConfigType.SELECT_INDEX) {
      return null;
    }
    ret = possibleValues.indexOf(value);
    if (ret == -1) {
      ret = possibleValues.indexOf(defaultValue);
      if (ret == -1) {
        ret = null;
      }
    }
    return ret;
  }

  public void setValue(String value) {
    if (type == ConfigType.SELECT && !possibleValues.contains(value)) {
      return;
    }
    this.value = value;
  }

  public void setValue(boolean value) {
    if (type != ConfigType.BOOL) {
      LOGGER.trace("This is not a boolean configuration object - setting keep current value");
    }
    else {
      this.value = String.valueOf(value);
    }
  }

  public void setValue(Integer value) {
    if (type != ConfigType.INTEGER) {
      LOGGER.trace("This is not an Integer configuration object - setting keep current value");
    }
    else {
      this.value = String.valueOf(value);
    }
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    if (type == ConfigType.SELECT && !possibleValues.contains(defaultValue)) {
      LOGGER.trace("Will not set defaultValue '{}={}' - since it is not in the list of possible values!", key, defaultValue);
    }
    else {
      this.defaultValue = defaultValue;
    }
  }

  public boolean isReturnListAsInt() {
    return returnListAsInt;
  }

  public void setReturnListAsInt(boolean returnListAsInt) {
    this.returnListAsInt = returnListAsInt;
  }

  public List<String> getPossibleValues() {
    return possibleValues;
  }

  public void setPossibleValues(List<String> possibleValues) {
    this.possibleValues = possibleValues;
  }

  public void addPossibleValues(String possibleValue) {
    this.possibleValues.add(possibleValue);
  }

  public ConfigType getType() {
    return type;
  }

  public void setType(ConfigType type) {
    this.type = type;
  }

  public boolean isEncrypt() {
    return encrypt;
  }

  public void setEncrypt(boolean encrypt) {
    this.encrypt = encrypt;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  /**
   * option can be "hidden" in GUI, but be still an option!
   * 
   * @return
   */
  public boolean isVisible() {
    return visible;
  }

  /**
   * option can be "hidden" in GUI, but be still an option!
   * 
   * @param visible
   */
  public void setVisible(boolean visible) {
    this.visible = visible;
  }
}
