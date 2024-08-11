package com.gitlab.progxaker.otelextension;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class BatchSessionInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("org.apache.livy.server.batch.BatchSession")
        .or(named("org.apache.livy.server.batch.BatchSession$"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod().and(takesArguments(0)).and(named("start")),
        this.getClass().getName() + "$ClassStartMethodAdvice");

    transformer.applyAdviceToMethod(
        isMethod().and(takesArguments(9)).and(named("create")),
        this.getClass().getName() + "$ObjectCreateMethodAdvice");
  }

  @SuppressWarnings("unused")
  public static class ClassStartMethodAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Local("otelSpan") Span span, @Advice.Local("otelScope") Scope scope) {
      Tracer tracer = GlobalOpenTelemetry.getTracer("apache-livy", "0.8");

      span = tracer.spanBuilder("Start a batch session").startSpan();
      scope = span.makeCurrent();
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void onExit(
        @Advice.Local("otelSpan") Span span,
        @Advice.Local("otelScope") Scope scope,
        @Advice.Thrown Throwable exception) {
      if (scope == null) {
        return;
      }
      scope.close();
      span.end();
    }
  }

  @SuppressWarnings("unused")
  public static class ObjectCreateMethodAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Argument(2) Object request,
        @Advice.Local("otelSpan") Span span,
        @Advice.Local("otelScope") Scope scope) {
      Span.current().setAttribute("http.request.body.content", request.toString());
    }
  }
}
