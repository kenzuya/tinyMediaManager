package org.tinymediamanager.scraper.imdb;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ImdbJsonHelper {
  private ImdbJsonHelper() {
  }

  static <E> E parseObject(ObjectMapper mapper, JsonNode jsonNode, Class<E> clazz) {
    if (jsonNode == null || jsonNode.isMissingNode()) {
      return null;
    }
    JsonParser jsonParser = mapper.treeAsTokens(jsonNode);
    try {
      return mapper.readValue(jsonParser, clazz);
    }
    catch (Exception e) {
      return null;
    }
  }

  static <E> List<E> parseList(ObjectMapper mapper, JsonNode jsonNode, Class<E> clazz) {
    if (jsonNode == null || jsonNode.isMissingNode()) {
      return Collections.emptyList();
    }
    JsonParser jsonParser = mapper.treeAsTokens(jsonNode);
    try {
      return mapper.readValue(jsonParser, ImdbJsonHelper.listType(mapper, clazz));
    }
    catch (Exception e) {
      return Collections.emptyList();
    }
  }

  static <E> JavaType listType(ObjectMapper mapper, Class<E> clazz) {
    return mapper.getTypeFactory().constructCollectionType(List.class, clazz);
  }
}