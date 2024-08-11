package com.gitlab.progxaker.otelextension;

import io.opentelemetry.api.trace.SpanContext;
import java.util.concurrent.ConcurrentHashMap;

public class SpanContextStorage {
  private static final ConcurrentHashMap<String, SpanContext> CONTEXT_MAP =
      new ConcurrentHashMap<>();

  public static void set(String key, SpanContext context) {
    CONTEXT_MAP.put(key, context);
  }

  public static SpanContext get(String key) {
    return CONTEXT_MAP.get(key);
  }

  public static void remove(String key) {
    CONTEXT_MAP.remove(key);
  }
}
