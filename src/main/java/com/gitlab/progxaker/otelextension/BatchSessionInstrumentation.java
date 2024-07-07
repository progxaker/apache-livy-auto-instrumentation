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
        return named("org.apache.livy.server.batch.BatchSession");
    }

    @Override
    public void transform(TypeTransformer transformer) {
        transformer.applyAdviceToMethod(
          isMethod()
            .and(takesArguments(0))
            .and(named("start")),
          this.getClass().getName() + "$MethodAdvice");
    }

    @SuppressWarnings("unused")
    public static class MethodAdvice {
        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void onEnter(@Advice.Local("otelSpan") Span span,
                                   @Advice.Local("otelScope") Scope scope) {
            Tracer tracer = GlobalOpenTelemetry.getTracer("apache-livy", "0.8");

            span = tracer.spanBuilder("Start a batch session").startSpan();
            scope = span.makeCurrent();
        }

        @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
        public static void onExit(@Advice.Local("otelSpan") Span span,
                                  @Advice.Local("otelScope") Scope scope,
                                  @Advice.Thrown Throwable exception) {
          if (scope == null) {
            return;
          }
          scope.close();
          span.end();
        }
    }
}
