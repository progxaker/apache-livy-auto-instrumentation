package com.gitlab.progxaker.otelextension;

import static net.bytebuddy.matcher.ElementMatchers.named;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class SparkProcAppInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return named("org.apache.livy.utils.SparkProcApp");
    }

    @Override
    public void transform(TypeTransformer transformer) {
        /*
            TODO: Use the following objects to set a Span attribute
                (!) .or(named("changeState"))
                (?) .or(named("state"))
                (?) .or(named("kill"))
        */
        transformer.applyAdviceToMethod(
            named("$anonfun$waitThread$1"),
            this.getClass().getName() + "$AnonFunctionAdvice");
    }

    @SuppressWarnings("unused")
    public static class AnonFunctionAdvice {
        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static Scope onEnter(@Advice.Local("otelSpan") Span span,
                                   @Advice.Local("otelScope") Scope scope) {
            Tracer tracer = GlobalOpenTelemetry.getTracer("apache-livy", "0.8");
            SpanBuilder spanBuilder = tracer.spanBuilder("Run a Spark application");

            String threadName = Thread.currentThread().getName();
            if (threadName.startsWith("SparProcApp_")) {
                SpanContext livyAppContext = SpanContextStorage.get(threadName);
                if (livyAppContext != null) {
                    spanBuilder.addLink(livyAppContext);
                    SpanContextStorage.remove(threadName);
                }
            }

            span = spanBuilder.startSpan();
            scope = span.makeCurrent();

            return scope;
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void onExit(@Advice.Local("otelSpan") Span span,
                                  @Advice.Local("otelScope") Scope scope,
                                  @Advice.Thrown Throwable throwable) {
            if (scope == null) {
                return;
            }
            scope.close();
            span.end();
        }
    }
}
