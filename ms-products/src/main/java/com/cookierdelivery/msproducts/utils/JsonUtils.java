package com.cookierdelivery.msproducts.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

@Slf4j
@UtilityClass
public class JsonUtils {
  private ObjectMapper objectMapper = new ObjectMapper();

  public String toJson(Object object) {
    Assert.notNull(object, "parameter must not be null");
    try {
      return objectMapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      log.error("error trying to convert object into json", e);
      throw new IllegalArgumentException(e);
    }
  }

  public <T> T toObject(String json, Class<T> clazz) {
    Assert.hasText(json, "parameter must not be null or empty");
    Assert.notNull(clazz, "parameter clazz must not be null");
    try {
      return objectMapper.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      log.error("error trying to convert json into Object " + clazz.getName(), e);
      throw new IllegalArgumentException(e);
    }
  }
}
