package com.gitlab.progxaker.otelextension;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static java.util.Arrays.asList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class ApacheLivyInstrumentationModule extends InstrumentationModule {

    public ApacheLivyInstrumentationModule() {
      super("apache-livy", "apache-livy-0-8");
    }

    @Override
    public boolean isHelperClass(String className) {
      return className.startsWith("com.gitlab.progxaker.otelextension");
    }

    @Override
    public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
      return hasClassesNamed(
          "org.apache.livy.server.batch.BatchSession",
          "org.apache.livy.server.batch.BatchSessionServlet",
          "org.apache.livy.Utils",
          "org.apache.livy.utils.SparkApp",
          "org.apache.livy.utils.SparkProcessBuilder");
    }

    @Override
    public List<TypeInstrumentation> typeInstrumentations() {
      return asList(
          new BatchSessionInstrumentation(),
          new BatchSessionServletInstrumentation(),
          new UtilsInstrumentation(),
          new SparkAppInstrumentation(),
          new SparkProcessBuilderInstrumentation());
    }
}
