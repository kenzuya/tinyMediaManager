package org.tinymediamanager.scraper.util;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtils.class);

  private JsonUtils() {
  }

  public static <E> E parseObject(ObjectMapper mapper, JsonNode jsonNode, Class<E> clazz) {
    if (jsonNode == null || jsonNode.isMissingNode()) {
      LOGGER.warn("JsonNode NULL or missing");
      return null;
    }
    JsonParser jsonParser = mapper.treeAsTokens(jsonNode);
    try {
      return mapper.readValue(jsonParser, clazz);
    }
    catch (Exception e) {
      LOGGER.warn("mapping to {} failed: {}", clazz, e);
      return null;
    }
  }

  public static <E> List<E> parseList(ObjectMapper mapper, JsonNode jsonNode, Class<E> clazz) {
    if (jsonNode == null || jsonNode.isMissingNode()) {
      LOGGER.warn("JsonNode NULL or missing");
      return Collections.emptyList();
    }
    JsonParser jsonParser = mapper.treeAsTokens(jsonNode);
    try {
      return mapper.readValue(jsonParser, JsonUtils.listType(mapper, clazz));
    }
    catch (Exception e) {
      LOGGER.warn("mapping to {} failed: {}", clazz, e);
      return Collections.emptyList();
    }
  }

  public static <E> JavaType listType(ObjectMapper mapper, Class<E> clazz) {
    return mapper.getTypeFactory().constructCollectionType(List.class, clazz);
  }
}