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

/*
    Apache Livy uses the org.scalatra.Created object to return the HTTP response
    https://github.com/apache/incubator-livy/blob/03ceb4ae1f764b7299998ffafffda0a18d8a1602/server/src/main/scala/org/apache/livy/server/SessionServlet.scala#L139
*/
public class BatchSessionViewInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return named("org.scalatra.Created$");
    }

    @Override
    public void transform(TypeTransformer transformer) {
        transformer.applyAdviceToMethod(
          isMethod()
            .and(named("apply"))
            .and(takesArguments(2))
            .and(takesArgument(0, Object.class))
            .and(takesArgument(1, named("scala.collection.immutable.Map"))),
          this.getClass().getName() + "$MethodAdvice");
    }

    @SuppressWarnings("unused")
    public static class MethodAdvice {
        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void onEnter(@Advice.Argument(0) Object body,
                                   @Advice.Local("otelSpan") Span span,
                                   @Advice.Local("otelScope") Scope scope) {
            if (BatchSessionViewTransfomer.isBatchSessionView(body)) {
                Tracer tracer = GlobalOpenTelemetry.getTracer("apache-livy", "0.8");

                span = tracer.spanBuilder("Send an HTTP response").startSpan();
                BatchSessionViewTransfomer.setAttributes(span, body);

                scope = span.makeCurrent();
            }
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
