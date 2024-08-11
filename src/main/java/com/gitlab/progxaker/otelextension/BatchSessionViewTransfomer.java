package com.gitlab.progxaker.otelextension;

import io.opentelemetry.api.trace.Span;
import java.util.Map;
import org.apache.livy.server.batch.BatchSessionView;
import scala.Option;

public class BatchSessionViewTransfomer {

  public static Boolean isBatchSessionView(Object object) {
    return object instanceof BatchSessionView;
  }

  public static void setAttributes(Span span, Object object) {
    if (object != null) {
      BatchSessionView batchSessionView = (BatchSessionView) object;

      span.setAttribute("id", batchSessionView.id());
      span.setAttribute("name", getOptionValue(batchSessionView.name()));
      span.setAttribute("owner", batchSessionView.owner());
      span.setAttribute("proxyUser", getOptionValue(batchSessionView.proxyUser()));
      span.setAttribute("state", batchSessionView.state().toLowerCase());
      span.setAttribute("appId", getOptionValue(batchSessionView.appId()));
      setAppInfoAttributes(span, batchSessionView.appInfo().asJavaMap());

      // TODO: Should the message 'stdout: \nstderr: ' be filtered out?
      span.setAttribute("log", batchSessionView.log().mkString(""));
    }
  }

  private static String getOptionValue(Option<String> option) {
    return option.isDefined() ? option.get() : null;
  }

  private static void setAppInfoAttributes(Span span, Map<String, String> appInfo) {
    appInfo.forEach(
        (key, value) -> {
          span.setAttribute("appInfo_" + key, value);
        });
  }
}
