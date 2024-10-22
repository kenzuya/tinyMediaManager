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

package org.tinymediamanager.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.ui.ITmmUIFilter;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * The class AbstractSettings is the base class for our settings structure. Loading/saving is handled by this class
 */
@JsonAutoDetect
public abstract class AbstractSettings extends AbstractModelObject {
  private static final Logger   LOGGER       = LoggerFactory.getLogger(AbstractSettings.class);

  @JsonIgnore
  protected static ObjectMapper objectMapper = createObjectMapper();

  @JsonIgnore
  protected boolean             dirty;
  @JsonIgnore
  protected boolean             newConfig    = false;
  @JsonIgnore
  protected String              settingsFolder;

  @JsonIgnore
  protected ObjectWriter        objectWriter;

  @JsonIgnore
  protected Map<String, Object> unknownFields;

  protected AbstractSettings() {
    unknownFields = new HashMap<>();
    objectWriter = createObjectWriter();
  }

  protected static ObjectMapper createObjectMapper() {
    ObjectMapper objectMapper = JsonMapper.builder()
        .configure(MapperFeature.AUTO_DETECT_GETTERS, true)
        .configure(MapperFeature.AUTO_DETECT_IS_GETTERS, true)
        .configure(MapperFeature.AUTO_DETECT_SETTERS, true)
        .configure(MapperFeature.AUTO_DETECT_FIELDS, true)
        .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .defaultTimeZone(TimeZone.getDefault())
        .serializationInclusion(JsonInclude.Include.ALWAYS)
        .build();
    objectMapper.getSerializerProvider().setNullKeySerializer(new NullKeySerializer());

    return objectMapper;
  }

  /**
   * Sets the dirty flag.
   */
  protected void setDirty() {
    dirty = true;
  }

  /**
   * is this config new oder loaded?
   * 
   * @return true/false
   */
  public boolean isNewConfig() {
    return newConfig;
  }

  /**
   * get any unknown fields from the settings JSON
   * 
   * @return a map with all unknown fiels
   */
  public Map<String, Object> getUnknownFields() {
    return unknownFields;
  }

  /**
   * the JSON setter for any unknown fields
   * 
   * @param name
   *          the name of the field
   * @param value
   *          the value
   */
  @JsonAnySetter
  public void setUnknownField(String name, Object value) {
    unknownFields.put(name, value);
  }

  /**
   * create the object writer for the settings instance
   */
  protected abstract ObjectWriter createObjectWriter();

  /**
   * hook for an after loading event
   */
  protected abstract void afterLoading();

  /**
   * write the default settings values
   */
  protected abstract void writeDefaultSettings();

  /**
   * gets the actual settings folder (used in the getInstance method)
   * 
   * @return the actual settings folder
   */
  public String getSettingsFolder() {
    return settingsFolder;
  }

  /**
   * gets the acutal config filename (used in the getInstance method)
   * 
   * @return the actual config filename
   */
  public abstract String getConfigFilename();

  /**
   * gets a logger for that class
   * 
   * @return the logger
   */
  protected abstract Logger getLogger();

  /**
   * force saving the settings - independently of the dirty flag
   */
  public void forceSaveSettings() {
    dirty = true;
    saveSettings();
  }

  /**
   * save the settings to a JSON file
   */
  public void saveSettings() {
    // is there anything to save?
    if (!dirty) {
      return;
    }

    // write as JSON
    Writer writer = null;
    try {
      String settings = objectWriter.writeValueAsString(this);
      writer = new FileWriter(new File(getSettingsFolder(), getConfigFilename()));
      IOUtils.write(settings, writer);
    }
    catch (Exception e) {
      getLogger().error("saveSettings", e);
      MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "tmm.settings", "message.config.savesettingserror"));
    }
    finally {
      if (writer != null) {
        try {
          writer.close();
        }
        catch (Exception e) {
          getLogger().error("saveSettings", e);
          MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "tmm.settings", "message.config.savesettingserror"));
        }
      }
    }

    // clear dirty flag
    dirty = false;
  }

  /**
   * get an instance of the desired settings class
   * 
   * @param folder
   *          the folder to load the settings from
   * @param filename
   *          the filename to load the settings from
   * @param clazz
   *          the class we need for unmarshalling
   * @return an instance (loaded or new) for the desired settings class
   */
  protected static AbstractSettings getInstance(String folder, String filename, Class<? extends AbstractSettings> clazz) {
    AbstractSettings instance = null;

    Path cfgFolder = Paths.get(folder);
    if (!Files.exists(cfgFolder)) {
      try {
        Files.createDirectories(cfgFolder);
      }
      catch (IOException e) {
        LOGGER.warn("could not create config folder: {}", e.getMessage());
      }
    }

    // unmarshall the JSON
    try {
      LOGGER.debug("Loading settings ({}) from {}", filename, folder);
      Reader reader = new FileReader(new File(folder, filename));
      String settingsAsJson = IOUtils.toString(reader);

      ObjectReader objectReader = objectMapper.readerFor(clazz);
      instance = objectReader.readValue(settingsAsJson);
      instance.afterLoading();

      instance.settingsFolder = folder;
      instance.dirty = false;
    }
    catch (Exception e) {
      if (!(e instanceof FileNotFoundException)) {
        // log only if there are other Exceptions than the FileNotFoundException
        LOGGER.error("failed loading settings", e);
      }
      LOGGER.warn("could not load settings - creating default ones...");

      try {
        instance = clazz.getDeclaredConstructor().newInstance();
        instance.settingsFolder = folder;
        instance.newConfig = true;
        instance.dirty = true;
        instance.writeDefaultSettings();
      }
      catch (Throwable e2) {
        LOGGER.error("getInstance", e2);
        MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "tmm.settings", "message.config.loadsettingserror"));
      }
    }

    return instance;
  }

  public static class UIFilters {
    public String                    id          = "";
    public ITmmUIFilter.FilterState  state       = ITmmUIFilter.FilterState.INACTIVE;
    public ITmmUIFilter.FilterOption option      = ITmmUIFilter.FilterOption.EQ;
    public String                    filterValue = "";
  }

  public enum UniversalFilterFields {
    NOTE,
    FILENAME,
    PRODUCTION_COMPANY,
    COUNTRY,
    SPOKEN_LANGUAGE,
    ACTOR,
    PRODUCER,
    WRITER,
    DIRECTOR,
    TAGS,
    PLOT,
    TAGLINE
  }
}
