package com.gitlab.progxaker.otelextension;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
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

public class UtilsInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return named("org.apache.livy.Utils$");
    }

    @Override
    public void transform(TypeTransformer transformer) {
        transformer.applyAdviceToMethod(
          isMethod()
              .and(named("startDaemonThread"))
              .and(takesArguments(2))
              .and(takesArgument(0, String.class)),
          this.getClass().getName() + "$MethodAdvice");
    }

    @SuppressWarnings("unused")
    public static class MethodAdvice {
        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static Scope onEnter(@Advice.Local("otelSpan") Span span,
                                    @Advice.Local("otelScope") Scope scope) {
            Tracer tracer = GlobalOpenTelemetry.getTracer("apache-livy", "0.8");

            span = tracer.spanBuilder("startDaemonThread").startSpan();
            scope = span.makeCurrent();

            return scope;
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
